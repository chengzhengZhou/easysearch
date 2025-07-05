/**
 * Copyright (C), 2010-2024, 爱回收
 * FileName: ColumnFactory
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2024/6/27 15:18
 * Description: Column工厂磊
 */
package com.ppwx.easysearch.core.data.element;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import com.ppwx.easysearch.core.data.Column;

import java.util.Date;
import java.util.Map;

/**
 *
 * Column工厂类
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2024/06/27 15:18
 * @since 1.0.0
 */
public class ColumnFactory {
    /**
     * singleton
     */
    private static final Map<Class, Column> cache;

    // init
    static {
        cache = Maps.newHashMap();
        cache.put(BoolColumn.class, new BoolColumn());
        cache.put(BytesColumn.class, new BytesColumn());
        cache.put(DateColumn.class, new DateColumn());
        cache.put(DoubleColumn.class, new DoubleColumn());
        cache.put(DynamicColumn.class, new DynamicColumn());
        cache.put(JsonColumn.class, new JsonColumn());
        cache.put(LongColumn.class, new LongColumn());
        cache.put(ObjectColumn.class, new ObjectColumn<>());
        cache.put(StringColumn.class, new StringColumn());
    }

    /**
     * 空实例缓存
     *
     * @param clazz
     * @return com.ppwx.easysearch.core.data.Column
     */
    public static Column emptyColumn(Class<? extends Column> clazz) {
        return cache.get(clazz);
    }

    /**
     * bool
     *
     * @param bool
     * @return com.ppwx.easysearch.core.data.element.BoolColumn
     */
    public static BoolColumn createBool(Boolean bool) {
        if (bool == null) {
            return (BoolColumn) cache.get(BoolColumn.class);
        }
        return new BoolColumn(bool);
    }

    /**
     * btyes
     *
     * @param bytes
     * @return com.ppwx.easysearch.core.data.element.BytesColumn
     */
    public static BytesColumn createBytes(byte[] bytes) {
        if (bytes == null) {
            return (BytesColumn) cache.get(BytesColumn.class);
        }
        return new BytesColumn(bytes);
    }

    /**
     * date
     *
     * @param date
     * @return com.ppwx.easysearch.core.data.element.DateColumn
     */
    public static DateColumn createDate(Date date) {
        if (date == null) {
            return (DateColumn) cache.get(DateColumn.class);
        }
        return new DateColumn(date);
    }

    /**
     * double
     *
     * @param data
     * @return com.ppwx.easysearch.core.data.element.DoubleColumn
     */
    public static DoubleColumn createDouble(Double data) {
        if (data == null) {
            return (DoubleColumn) cache.get(DoubleColumn.class);
        }
        return new DoubleColumn(data);
    }

    /**
     * dynamic
     *
     * @param data
     * @return com.ppwx.easysearch.core.data.element.DynamicColumn
     */
    public static DynamicColumn createDynamic(Object data) {
        if (data == null) {
            return (DynamicColumn) cache.get(DynamicColumn.class);
        }
        return new DynamicColumn(data);
    }

    /**
     * json
     *
     * @param data
     * @return com.ppwx.easysearch.core.data.element.JsonColumn
     */
    public static JsonColumn createJson(JSON data) {
        if (data == null) {
            return (JsonColumn) cache.get(JsonColumn.class);
        }
        return new JsonColumn(data);
    }

    /**
     * long
     *
     * @param data
     * @return com.ppwx.easysearch.core.data.element.LongColumn
     */
    public static LongColumn createLong(Long data) {
        if (data == null) {
            return (LongColumn) cache.get(LongColumn.class);
        }
        return new LongColumn(data);
    }

    /**
     * object
     *
     * @param data
     * @return com.ppwx.easysearch.core.data.element.ObjectColumn
     */
    public static  ObjectColumn createObject(Object data) {
        if (data == null) {
            return (ObjectColumn) cache.get(ObjectColumn.class);
        }
        return new ObjectColumn<>(data);
    }

    /**
     * string
     *
     * @param data
     * @return com.ppwx.easysearch.core.data.element.StringColumn
     */
    public static StringColumn createString(String data) {
        if (data == null) {
            return (StringColumn) cache.get(StringColumn.class);
        }
        return new StringColumn(data);
    }
}