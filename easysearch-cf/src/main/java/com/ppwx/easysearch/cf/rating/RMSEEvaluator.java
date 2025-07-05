package com.ppwx.easysearch.cf.rating;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className RMSEEvaluator
 * @description 均方差
 * @date 2024/10/19 19:30
 **/
public class RMSEEvaluator implements RecEvaluator {

    private int size = 0;

    private double rmse = 0.0;

    @Override
    public void add(double predict, double actual) {
        if (Double.isNaN(predict) || Double.isNaN(actual)) {
            return;
        }
        rmse += Math.pow(actual - predict, 2);
        ++size;
    }

    @Override
    public double evaluate() {
        return size > 0 ? Math.sqrt(rmse / size) : 0.0d;
    }

    @Override
    public int getPredictSize() {
        return size;
    }
}
