/*
 * Copyright 2026 chengzhengZhou
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ppwx.easysearch.core.data.model;

import com.ppwx.easysearch.core.data.DataModel;
import com.ppwx.easysearch.core.data.DataSet;
import com.ppwx.easysearch.core.metrics.Metrics;
import com.ppwx.easysearch.core.metrics.MetricsRegistry;
import com.ppwx.easysearch.core.metrics.Snapshot;

import java.util.concurrent.TimeUnit;

/**
 *
 * 数据缓存装饰器
 *
 *
 * @since 1.0.0
 */
public class DataModelAutoCacheDecorator extends DataModelCacheDecorator {

    /**
     * Metrics register
     */
    private MetricsRegistry<Metrics> metricsRegistry;
    /**
     * 调用次数阈值
     */
    private final int callThreshold;
    /**
     * 10
     */
    private final int timeWindowSizeInSeconds;

    public DataModelAutoCacheDecorator(DataModel target, ICache<String, DataSet> cache, MetricsRegistry<Metrics> metricsRegistry) {
        this(target, cache, metricsRegistry, 50, 10);
    }

    public DataModelAutoCacheDecorator(DataModel target, ICache<String, DataSet> cache,
                                       MetricsRegistry<Metrics> metricsRegistry, int callThreshold, int timeWindowSizeInSeconds) {
        super(target, cache);
        this.metricsRegistry = metricsRegistry;
        this.callThreshold = callThreshold;
        this.timeWindowSizeInSeconds = timeWindowSizeInSeconds;
    }

    @Override
    public void loadDataModel() {
        metricsRegistry.metrics(cacheKey, timeWindowSizeInSeconds)
                .record(1, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        if (checkIfThresholdExceeded(cacheKey)) {
            if ((this.dataSet = cache.getIfPresent(cacheKey)) == null) {
                target.loadDataModel();
            }
        } else {
            target.loadDataModel();
        }
    }

    @Override
    public DataSet getDataset() {
        if (this.dataSet == null) {
            if (checkIfThresholdExceeded(cacheKey)) {
                this.dataSet = cache.getIfPresent(cacheKey);
                if (this.dataSet == null) {
                    this.dataSet = target.getDataset();
                    if (this.dataSet != null) {
                        cache.put(cacheKey, this.dataSet);
                    }
                }
            } else {
                this.dataSet = target.getDataset();
            }
        }
        return this.dataSet == null ? null : (DataSet) this.dataSet.clone();
    }

    private boolean checkIfThresholdExceeded(String cacheKey) {
        Metrics metrics = metricsRegistry.metrics(cacheKey, timeWindowSizeInSeconds);
        Snapshot snapshot = metrics.getSnapshot();
        return snapshot.getNumberOfSuccessfulCalls() >= callThreshold;
    }

}