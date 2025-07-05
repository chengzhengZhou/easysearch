/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: GaussNormalization
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/7/26 21:00
 * Description: 高斯衰减函数
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
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/07/26 21:00
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
     * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
     * @param mean 均值
     * @param standardDeviation 方差
     * @date 2023/7/27 11:17
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