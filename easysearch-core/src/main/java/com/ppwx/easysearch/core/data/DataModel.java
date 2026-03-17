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

package com.ppwx.easysearch.core.data;

import com.ppwx.easysearch.core.common.SearchConstants;
import io.netty.util.AttributeMap;

/**
 *
 * DataModel
 * 实现数据集的加载
 * 统一约束{@link com.ppwx.easysearch.core.data.model.Key} 的GLOBAL_ID和GLOBAL_SCORE两个字段
 * 后续阶段均需要使用，在数据输入阶段就预先处理好值
 * <p/>
 * DataModel作为数据源和{@link com.ppwx.easysearch.core.pipeline.DataHandler} 计算分离
 * 所有动态参数应该绑定到{@link DataModel} 上
 *
 * @since 1.0.0
 */
public interface DataModel extends AttributeMap {

    /**
     * 加载数据模型
     *
     * @param
     * @return void
     */
    void loadDataModel();

    /**
     * 获取数据集
     *
     * @param
     * @return com.ppwx.easysearch.core.data.DataSet
     */
    DataSet getDataset();

    /**
     * 设置数据集
     *
     * @param dataSet
     * @return void
     */
    void setDataSet(DataSet dataSet);

}