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
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

/**
 *
 * 数据缓存装饰器
 * model本身需要承载参数变量不宜直接缓存，而{@link DataSet}则只包含数据本身，适宜缓存
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2024/11/28 15:00
 * @since 1.0.0
 */
public class DataModelCacheDecorator implements DataModel {
    /**
     * 装饰对象实例
     */
    protected DataModel target;
    /**
     * 数据集
     */
    protected DataSet dataSet;
    /**
     * 缓存key
     */
    protected String cacheKey;
    /**
     * 缓存组件
     */
    protected ICache<String, DataSet> cache;

    public DataModelCacheDecorator(DataModel target, ICache<String, DataSet> cache) {
        this.target = target;
        this.cache = cache;
        this.cacheKey = this.getClass().getName();
    }

    public void setCacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
    }

    @Override
    public void loadDataModel() {
        if ((this.dataSet = cache.getIfPresent(cacheKey)) == null) {
            target.loadDataModel();
        }
    }

    @Override
    public DataSet getDataset() {
        if (this.dataSet == null) {
            this.dataSet = cache.getIfPresent(cacheKey);
            if (this.dataSet == null) {
                this.dataSet = target.getDataset();
                if (this.dataSet != null) {
                    cache.put(cacheKey, this.dataSet);
                }
            }
        }
        return this.dataSet == null ? null : (DataSet) this.dataSet.clone();
    }

    @Override
    public void setDataSet(DataSet dataSet) {
        target.setDataSet(dataSet);
        this.dataSet = dataSet;
    }

    @Override
    public <T> Attribute<T> attr(AttributeKey<T> attributeKey) {
        return target.attr(attributeKey);
    }

    @Override
    public <T> boolean hasAttr(AttributeKey<T> attributeKey) {
        return target.hasAttr(attributeKey);
    }
}