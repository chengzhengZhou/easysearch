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

package com.ppwx.easysearch.cf.model;

import com.ppwx.easysearch.cf.rating.RecEvaluator;
import net.librec.recommender.item.ItemEntry;

import java.util.Collection;
import java.util.List;

/**
 *
 * 预测模型接口
 *
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
     * @param evaluators
     * @return void
     */
    void acceptEvaluators(List<RecEvaluator> evaluators);
}