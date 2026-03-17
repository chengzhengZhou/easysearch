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
import com.ppwx.easysearch.core.function.AbstractScoreFunction;

import java.util.List;

/**
 *
 * 最小-最大归一化
 * 该方法将数据线性地映射到指定的最小值和最大值之间。公式如下：
 * normalized_value = (value - min_value) / (max_value - min_value)
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/07/04 20:20
 * @since 1.0.0
 */
public class MinMaxNormalizeFunc extends AbstractScoreFunction {
    /**
     * 构造器
     *
     * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
     * @param
     * @date 2023/7/7 0:00
     * @return
     */
    public MinMaxNormalizeFunc() {
        super();
    }

    /**
     * 构造器
     *
     * @param columns
     * @return
     * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
     * @date 2023/7/4 19:46
     */
    public MinMaxNormalizeFunc(List<Column> columns) {
        super(columns);
    }

    @Override
    public double score(Number value) {
        double diff = max() - min();
        if (Double.compare(diff, 0.0) == 0) {
            return diff;
        }
        return ((value.doubleValue() - min())/(max() - min()));
    }

}