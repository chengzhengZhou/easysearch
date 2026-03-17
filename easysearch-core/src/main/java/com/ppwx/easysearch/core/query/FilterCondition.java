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

package com.ppwx.easysearch.core.query;

import java.util.List;

/**
 * @className FilterCondition
 * @description 过滤条件
 **/
public class FilterCondition {

    private String field;

    private String value;

    private String minValue;

    private String maxValue;

    private List<String> multiValues;
    /**
     * 标识是否条件非
     */
    private final boolean negative;
    /**
     * 标识区间
     */
    private final boolean between;
    /**
     * 是否多值
     */
    private final boolean multi;

    public FilterCondition(String field, String value, boolean negative) {
        this.field = field;
        this.value = value;
        this.negative = negative;
        this.between = false;
        this.multi = false;
    }

    public FilterCondition(String field, String minValue, String maxValue, boolean negative) {
        this.field = field;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.negative = negative;
        this.between = true;
        this.multi = false;
    }

    public FilterCondition(String field, List<String> multiValues, boolean negative) {
        this.field = field;
        this.multiValues = multiValues;
        this.negative = negative;
        this.between = false;
        this.multi = true;
    }

    public String getValue() {
        return value;
    }

    public String getMinValue() {
        return minValue;
    }

    public String getMaxValue() {
        return maxValue;
    }

    public List<String> getMultiValues() {
        return multiValues;
    }

    public boolean isNegative() {
        return negative;
    }

    public boolean isBetween() {
        return between;
    }

    public boolean isMulti() {
        return multi;
    }

    void setValue(String value) {
        this.value = value;
    }

    void setMinValue(String minValue) {
        this.minValue = minValue;
    }

    void setMaxValue(String maxValue) {
        this.maxValue = maxValue;
    }

    void addValue(String value) {
        multiValues.add(value);
    }

    public String getField() {
        return field;
    }

    @Override
    public String toString() {
        return "FilterCondition{" +
                "field='" + field + '\'' +
                ", value='" + value + '\'' +
                ", minValue='" + minValue + '\'' +
                ", maxValue='" + maxValue + '\'' +
                ", multiValues=" + multiValues +
                ", negative=" + negative +
                ", between=" + between +
                ", multi=" + multi +
                '}';
    }
}
