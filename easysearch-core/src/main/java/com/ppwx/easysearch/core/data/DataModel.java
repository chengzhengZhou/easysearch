/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: DataModel
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/12/7 19:45
 * Description: DataModel
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
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/07 19:45
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