/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.solr.metrics;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import org.apache.lucene.util.TestUtil;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.core.PluginInfo;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.metrics.reporters.MockMetricReporter;
import org.apache.solr.schema.FieldType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SolrCoreMetricManagerTest extends SolrTestCaseJ4 {
  private static final int MAX_ITERATIONS = 100;

  private SolrCoreMetricManager metricManager;

  @Before
  public void beforeTest() throws Exception {
    initCore("solrconfig-basic.xml", "schema.xml");
    metricManager = new SolrCoreMetricManager(h.getCore());
  }

  @After
  public void afterTest() throws IOException {
    metricManager.close();
    assertTrue(SolrMetricManager.getReporters(metricManager.getRegistryName()).isEmpty());
    deleteCore();
  }

  @Test
  public void testRegisterMetrics() {
    Random random = random();

    String scope = SolrMetricTestUtils.getRandomScope(random);
    SolrInfoMBean.Category category = SolrMetricTestUtils.getRandomCategory(random);
    Map<String, Counter> metrics = SolrMetricTestUtils.getRandomMetrics(random);
    SolrMetricProducer producer = SolrMetricTestUtils.getProducerOf(category, scope, metrics);
    try {
      metricManager.registerMetricProducer(scope, producer);
      assertNotNull(scope);
      assertNotNull(category);
      assertNotNull(metrics);
      assertRegistered(scope, metrics, metricManager);
    } catch (final IllegalArgumentException e) {
      assertTrue("expected at least one null but got: scope="+scope+" category="+category+" metrics="+metrics,
          (scope == null || category == null || metrics == null));
      assertRegistered(scope, new HashMap<>(), metricManager);
    }
  }

  @Test
  public void testRegisterMetricsWithReplacements() {
    Random random = random();

    Map<String, Counter> registered = new HashMap<>();
    String scope = SolrMetricTestUtils.getRandomScope(random, true);
    SolrInfoMBean.Category category = SolrMetricTestUtils.getRandomCategory(random, true);

    int iterations = TestUtil.nextInt(random, 0, MAX_ITERATIONS);
    for (int i = 0; i < iterations; ++i) {
      Map<String, Counter> metrics = SolrMetricTestUtils.getRandomMetricsWithReplacements(random, registered);
      if (metrics.isEmpty()) {
        continue;
      }
      SolrMetricProducer producer = SolrMetricTestUtils.getProducerOf(category, scope, metrics);
      metricManager.registerMetricProducer(scope, producer);
      registered.putAll(metrics);
      assertRegistered(scope, registered, metricManager);
    }
  }

  @Test
  public void testLoadReporter() throws Exception {
    Random random = random();

    String className = MockMetricReporter.class.getName();
    String reporterName = TestUtil.randomUnicodeString(random);

    Map<String, Object> attrs = new HashMap<>();
    attrs.put(FieldType.CLASS_NAME, className);
    attrs.put(CoreAdminParams.NAME, reporterName);

    boolean shouldDefineConfigurable = random.nextBoolean();
    String configurable = TestUtil.randomUnicodeString(random);
    if (shouldDefineConfigurable) attrs.put("configurable", configurable);

    boolean shouldDefinePlugin = random.nextBoolean();
    PluginInfo pluginInfo = shouldDefinePlugin ? new PluginInfo(TestUtil.randomUnicodeString(random), attrs) : null;

    try {
      SolrMetricManager.loadReporter(metricManager.getRegistryName(), metricManager.getCore().getResourceLoader(), pluginInfo);
      assertNotNull(pluginInfo);
      Map<String, SolrMetricReporter> reporters = SolrMetricManager.getReporters(metricManager.getRegistryName());
      assertTrue("reporters.size should be > 0, but was + " + reporters.size(), reporters.size() > 0);
      assertNotNull("reporter " + reporterName + " not present among " + reporters, reporters.get(reporterName));
      assertTrue("wrong reporter class: " + reporters.get(reporterName), reporters.get(reporterName) instanceof MockMetricReporter);
    } catch (IllegalArgumentException e) {
      assertTrue(pluginInfo == null || attrs.get("configurable") == null);
      assertNull(SolrMetricManager.getReporters(metricManager.getRegistryName()).get(reporterName));
    }
  }

  private static void assertRegistered(String scope, Map<String, Counter> newMetrics, SolrCoreMetricManager metricManager) {
    if (scope == null) {
      return;
    }
    String filter = "." + scope + ".";
    MetricRegistry registry = SolrMetricManager.registry(metricManager.getRegistryName());
    assertEquals(newMetrics.size(), registry.getMetrics().
        keySet().stream().filter(s -> s.contains(filter)).count());

    Map<String, Metric> registeredMetrics = registry.getMetrics().
        entrySet().stream().filter(e -> e.getKey() != null && e.getKey().contains(filter)).
        collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
    for (Map.Entry<String, Metric> entry : registeredMetrics.entrySet()) {
      String name = entry.getKey();
      Metric expectedMetric = entry.getValue();

      Metric actualMetric = registry.getMetrics().get(name);

      assertNotNull(actualMetric);
      assertEquals(expectedMetric, actualMetric);
    }
  }
}