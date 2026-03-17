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

import com.ppwx.easysearch.core.data.Column;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

/**
 * java对象
 *
 *
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