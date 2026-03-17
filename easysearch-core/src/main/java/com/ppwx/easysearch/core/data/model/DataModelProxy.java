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
import com.ppwx.easysearch.core.util.DataModelThreadHolder;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

/**
 *
 * DataModel代理
 * 从线程副本中获取实际调用{@link DataModel}
 *
 * @since 1.0.0
 */
public class DataModelProxy implements DataModel {

    private DataModel target() {
        return DataModelThreadHolder.get();
    }

    @Override
    public void loadDataModel() {
        target().loadDataModel();
    }

    @Override
    public DataSet getDataset() {
        return target().getDataset();
    }

    @Override
    public void setDataSet(DataSet dataSet) {
        target().setDataSet(dataSet);
    }

    @Override
    public <T> Attribute<T> attr(AttributeKey<T> key) {
        return target().attr(key);
    }

    @Override
    public <T> boolean hasAttr(AttributeKey<T> key) {
        return target().hasAttr(key);
    }
}