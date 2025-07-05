/**
 * Copyright (C), 2010-2024, 爱回收
 * FileName: Model
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2024/7/26 17:42
 * Description: 预测模型接口
 */
package com.ppwx.easysearch.cf.model;

import com.ppwx.easysearch.cf.rating.RecEvaluator;
import net.librec.recommender.item.ItemEntry;

import java.util.Collection;
import java.util.List;

/**
 *
 * 预测模型接口
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2024/07/26 17:42
 * @since 1.0.0
 */
public interface Model {

    /**
     * 加载模型
     *
     * @param
     * @return void
     */
    void loadModel();

    /**
     * 保存模型
     *
     * @param
     * @return void
     */
    void saveModel();

    /**
     * 训练模型
     *
     * @return void
     */
    void train();

    /**
     * 设置为预测模式
     *
     * @return void
     */
    void predict();

    /**
     * 预测
     *
     * @param userId
     * @param itemId
     * @return double
     */
    double predict(String userId, String itemId);

    /**
     * 批量预测
     *
     * @param userId
     * @param items
     * @return java.util.List<net.librec.recommender.item.ItemEntry<java.lang.String,java.lang.Double>>
     */
    List<ItemEntry<String, Double>> batchPredict(String userId, Collection<String> items);

    /**
     * @description 模型评估
     * @author ext.ahs.zhouchzh1@jd.com
     * @date 2024/10/22 14:03
     * @param evaluators
     * @return void
     */
    void acceptEvaluators(List<RecEvaluator> evaluators);
}