/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: DynamicColumn
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/12/7 11:58
 * Description: JSONObject column
 */
package com.ppwx.easysearch.core.data.element;

import com.alibaba.fastjson.util.TypeUtils;
import com.ppwx.easysearch.core.data.Column;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

/**
 *
 * 动态字段 column
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/07 11:58
 * @since 1.0.0
 */
public class DynamicColumn extends Column {

    public DynamicColumn() {
        this(null);
    }

    public DynamicColumn(Object object) {
        super(object, Type.DYNAMIC, 0);
    }

    @Override
    public Long asLong() {
        return TypeUtils.castToLong(getRawData());
    }

    @Override
    public Double asDouble() {
        return TypeUtils.castToDouble(getRawData());
    }

    @Override
    public String asString() {
        return TypeUtils.castToString(getRawData());
    }

    @Override
    public Date asDate() {
        return TypeUtils.castToDate(getRawData());
    }

    @Override
    public Date asDate(String dateFormat) {
        return asDate();
    }

    @Override
    public byte[] asBytes() {
        return TypeUtils.castToBytes(getRawData());
    }

    @Override
    public Boolean asBoolean() {
        return TypeUtils.castToBoolean(getRawData());
    }

    @Override
    public BigDecimal asBigDecimal() {
        return TypeUtils.castToBigDecimal(getRawData());
    }

    @Override
    public BigInteger asBigInteger() {
        return TypeUtils.castToBigInteger(getRawData());
    }
}