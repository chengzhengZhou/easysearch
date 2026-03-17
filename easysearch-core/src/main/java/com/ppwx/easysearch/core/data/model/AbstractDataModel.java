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
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.DefaultAttributeMap;

/**
 *
 * DataModel基类
 *
 * @since 1.0.0
 */
public abstract class AbstractDataModel implements DataModel {
    /**
     * 数据集
     */
    protected DataSet dataSet;
    /**
     * 动态参数
     */
    private DefaultAttributeMap attributeMap;

    public AbstractDataModel() {
        this.attributeMap = new DefaultAttributeMap();
    }

    @Override
    public DataSet getDataset() {
        return this.dataSet;
    }

    @Override
    public void setDataSet(DataSet dataSet) {
        this.dataSet = dataSet;
    }

    @Override
    public <T> Attribute<T> attr(AttributeKey<T> attributeKey) {
        return attributeMap.attr(attributeKey);
    }

    @Override
    public <T> boolean hasAttr(AttributeKey<T> attributeKey) {
        return attributeMap.hasAttr(attributeKey);
    }
}