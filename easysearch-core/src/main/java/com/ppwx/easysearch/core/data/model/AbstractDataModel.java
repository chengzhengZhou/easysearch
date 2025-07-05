/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: AbstractDataModel
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/12/12 16:31
 * Description: DataModel基类
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
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/12 16:31
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