/**
 * Copyright (C), 2010-2024, 爱回收
 * FileName: CacheDataModelDecorator
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2024/2/20 15:00
 * Description: 数据缓存装饰器
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
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2024/11/28 15:00
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