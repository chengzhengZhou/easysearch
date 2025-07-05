/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: DynamicColumn
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/12/7 11:58
 * Description: JSONObject column
 */
package com.ppwx.easysearch.core.data.element;

import com.alibaba.fastjson.JSON;
import com.ppwx.easysearch.core.data.Column;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

/**
 *
 * json对象 column
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/07 11:58
 * @since 1.0.0
 */
public class JsonColumn extends Column {

    public JsonColumn() {
        this(null);
    }

    public JsonColumn(JSON json) {
        super(json, Type.DYNAMIC, 0);
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
        return getRawData() == null ? null : getRawData().toString();
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
        return null;
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