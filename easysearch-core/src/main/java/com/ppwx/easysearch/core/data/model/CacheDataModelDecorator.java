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

import com.google.common.cache.Cache;
import com.ppwx.easysearch.core.data.DataModel;
import com.ppwx.easysearch.core.data.DataSet;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

/**
 *
 * 数据缓存装饰器
 * model本身需要承载参数变量不宜直接缓存，而{@link DataSet}则只包含数据本身，适宜缓存
 *
 * @since 1.0.0
 */
@Deprecated
public class CacheDataModelDecorator implements DataModel {
    /**
     * 装饰对象实例
     */
    private DataModel target;
    /**
     * 数据集
     */
    protected DataSet dataSet;
    /**
     * 缓存key
     */
    private String cacheKey;
    /**
     * 缓存组件
     */
    private Cache<String, DataSet> cache;

    public CacheDataModelDecorator(DataModel target, Cache<String, DataSet> cache) {
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