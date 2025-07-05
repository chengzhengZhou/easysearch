package com.ppwx.easysearch.cf.rating;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className RecEvaluator
 * @description 评分预估
 * @date 2024/10/19 18:10
 **/
public interface RecEvaluator {

    /**
     * @description 添加评分
     * @author ext.ahs.zhouchzh1@jd.com
     * @date 2024/10/19 18:14
     * @param predict 预测的分值
     * @param actual  实际分值
     * @return void
     */
    void add(double predict, double actual);

    /**
     * @description 计算评估指标
     * @author ext.ahs.zhouchzh1@jd.com
     * @date 2024/10/19 18:14
     * @return double
     */
    double evaluate();

    /**
     * @description 计算的记录数
     * @author ext.ahs.zhouchzh1@jd.com
     * @date 2024/10/19 19:23
     * @return int
     */
    int getPredictSize();
}
