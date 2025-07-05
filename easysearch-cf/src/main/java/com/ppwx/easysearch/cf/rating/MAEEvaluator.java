package com.ppwx.easysearch.cf.rating;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className MAEEvaluator
 * @description 平均值
 * @date 2024/10/19 18:16
 **/
public class MAEEvaluator implements RecEvaluator {

    private int size = 0;

    private double mae = 0.0;

    @Override
    public void add(double predict, double actual) {
        if (Double.isNaN(predict) || Double.isNaN(actual)) {
            return;
        }
        mae += Math.abs(actual - predict);
        ++size;
    }

    @Override
    public double evaluate() {
        return size > 0 ? mae / size : 0.0d;
    }

    @Override
    public int getPredictSize() {
        return this.size;
    }

}
