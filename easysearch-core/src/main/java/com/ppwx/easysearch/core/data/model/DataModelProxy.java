/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: DataModelProxy
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/12/12 17:18
 * Description: DataModel代理
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
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/12 17:18
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