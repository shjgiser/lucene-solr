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

import java.util.Collection;
import java.util.List;

import org.apache.solr.core.SolrInfoMBean;

/**
 * Extension of {@link SolrInfoMBean} for use by objects that
 * expose metrics through {@link SolrCoreMetricManager}.
 */
public interface SolrMetricProducer extends SolrInfoMBean {

  /**
   * Initializes metrics specific to this producer
   * @param registry registry name where metrics are registered
   * @param linkedRegistries other registries where changes to these metrics will be tracked, too,
   *                         via {@link LinkedMetric} implementations.
   * @param scope scope of the metrics (eg. handler name) to separate metrics of
   *              instances of the same component executing in different contexts
   * @return registered (or existing) unqualified names of metrics specific to this producer.
   */
  Collection<String> initializeMetrics(String registry, List<String> linkedRegistries, String scope);
}