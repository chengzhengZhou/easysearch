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