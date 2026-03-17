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
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/07/06 10:52
 * @since 1.0.0
 */
public class MappingScoreNormalizeFunc implements TextScoreFunction {
    /** 分值映射表 */
    private Map<String, Double> mapping;
    /** 未映射时的默认值 */
    private double defaultScore;
    /**
     * 构造器
     *
     * @param mapping
     * @param defaultScore
     * @return
     * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
     * @date 2023/7/4 19:46
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