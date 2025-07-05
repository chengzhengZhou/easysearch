/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: DataSet
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/12/6 20:37
 * Description:
 */
package com.ppwx.easysearch.core.data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 *
 * 数据集
 * 对源数据进行抽象统一
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/06 20:37
 * @since 1.0.0
 */
public interface DataSet extends Cloneable, Serializable {
    /**
     * 数据集数量
     *
     * @return int
     */
    int size();

    /**
     * 获取id字段
     *
     * @return Column
     */
    String idColumn();

    /**
     * 获取字段名称
     *
     * @return
     */
    List<String> columnNames();

    /**
     * 获取指定行
     * 如果找不到则返回Null
     *
     * @param idx
     * @return java.util.Map<java.lang.String,com.ppwx.easysearch.core.data.Column>
     */
    Map<String, Column> row(int idx);

    /**
     * 获取指定列
     * 如果找不到则返回Null
     *
     * @param idx
     * @return java.util.List<com.ppwx.easysearch.core.data.Column>
     */
    List<Column> column(int idx);

    /**
     * 根据名称获取指定列
     * 如果找不到则返回Null
     *
     * @param filed
     * @return java.util.List<com.ppwx.easysearch.core.data.Column>
     */
    List<Column> column(String filed);
    /**
     * 根据id查找元素
     * 如果找不到则返回Null
     *
     * @param id
     * @return java.util.Map<java.lang.String,com.ppwx.easysearch.core.data.Column>
     */
    Map<String, Column> find(Object id);

    /**
     * 设置字段值
     *
     * @param idx
     * @param field
     * @param column
     * @return com.ppwx.easysearch.core.data.Column
     */
    Column set(int idx, String field, Column column);

    /**
     * 返回所有的元素
     *
     * @param
     * @return java.util.List<java.util.Map<java.lang.String,com.ppwx.easysearch.core.data.Column>>
     */
    List<Map<String, Column>> all();

    /**
     * 克隆
     *
     * @return java.lang.Object
     */
    Object clone();
}