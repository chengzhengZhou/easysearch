/**
 * Copyright (C), 2010-2024, 爱回收
 * FileName: ObjectColumn
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2024/2/18 17:06
 * Description:
 */
package com.ppwx.easysearch.core.data.element;

import com.ppwx.easysearch.core.data.Column;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

/**
 * java对象
 *
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2024/02/18 17:06
 * @since 1.0.0
 */
public class ObjectColumn<T> extends Column {

    public ObjectColumn() {
        this(null);
    }

    public ObjectColumn(T object) {
        super(object, Type.DYNAMIC, 64);
    }

    public T getObject() {
        return (T) getRawData();
    }

    @Override
    public Long asLong() {
        return null;
    }

    @Override
    public Double asDouble() {
        return null;
    }

    @Override
    public String asString() {
        return null;
    }

    @Override
    public Date asDate() {
        return null;
    }

    @Override
    public Date asDate(String dateFormat) {
        return null;
    }

    @Override
    public byte[] asBytes() {
        return new byte[0];
    }

    @Override
    public Boolean asBoolean() {
        return null;
    }

    @Override
    public BigDecimal asBigDecimal() {
        return null;
    }

    @Override
    public BigInteger asBigInteger() {
        return null;
    }
}