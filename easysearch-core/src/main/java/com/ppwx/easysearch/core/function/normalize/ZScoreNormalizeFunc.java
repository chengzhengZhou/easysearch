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
 * Z-score标准化
 * 该方法通过减去均值并除以标准差来将数据转换为具有零均值和单位方差的分布。公式如下：
 * normalized_value = (value - mean) / standard_deviation
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/07/04 20:33
 * @since 1.0.0
 */
public class ZScoreNormalizeFunc extends AbstractScoreFunction {
    /**
     * 构造器
     *
     * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
     * @param
     * @date 2023/7/6 23:59
     * @return
     */
    public ZScoreNormalizeFunc() {
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
    public ZScoreNormalizeFunc(List<Column> columns) {
        super(columns);
    }

    @Override
    public double score(Number value) {
        double standardDeviation = standardDeviation();
        if (Double.compare(standardDeviation, 0.0) == 0) {
            return 0.0;
        }
        return (value.doubleValue() - mean()) / standardDeviation();
    }

}