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

package com.ppwx.easysearch.core.function.normalize;

import com.ppwx.easysearch.core.data.Column;
import com.ppwx.easysearch.core.function.TextScoreFunction;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * 数据映射分值归一
 *
 * @since 1.0.0
 */
public class MappingScoreNormalizeFunc implements TextScoreFunction {
    
    private Map<String, Double> mapping;
    
    private double defaultScore;
    /**
     * 构造器
     *
     * @param mapping
     * @param defaultScore
     * @return
     */
    public MappingScoreNormalizeFunc(Map<String, Double> mapping, double defaultScore) {
        assert mapping != null;
        this.mapping = new HashMap<>(mapping);
        this.defaultScore = defaultScore;
    }

    @Override
    public double score(String value) {
        return mapping.containsKey(value) ? mapping.get(value) : defaultScore;
    }

    @Override
    public Double apply(Column column) {
        return score(column.asString());
    }
}