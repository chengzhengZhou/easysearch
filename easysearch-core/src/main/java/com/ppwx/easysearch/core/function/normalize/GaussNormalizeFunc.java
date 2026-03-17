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
import com.ppwx.easysearch.core.function.NumberScoreFunction;
import org.apache.commons.math.util.FastMath;

/**
 *
 * 高斯衰减函数
 * f(x)=e^{(-(x-a)^2/2b^2)}
 * 其中a为均值，b为标准方差
 *
 * @since 1.0.0
 */
public class GaussNormalizeFunc implements NumberScoreFunction<Double> {
    /**
     * 均值
     */
    private double mean;
    /**
     * 方差
     */
    private double standardDeviation;

    public GaussNormalizeFunc() {
        this.mean = 0D;
        this.standardDeviation = 1;
    }

    /**
     * 带参构造
     *
     * @param mean 均值
     * @param standardDeviation 方差
     * @return
     */
    public GaussNormalizeFunc(double mean, double standardDeviation) {
        super();
        this.mean = mean;
        this.standardDeviation = standardDeviation;
    }

    @Override
    public double score(Number value) {
        double x0 = value.doubleValue() - mean;
        return FastMath.exp(-x0 * x0 / (2 * standardDeviation * standardDeviation));
    }

    @Override
    public Double apply(Column column) {
        return score(column.asDouble());
    }

}