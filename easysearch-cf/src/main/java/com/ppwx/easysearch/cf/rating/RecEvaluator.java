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

package com.ppwx.easysearch.cf.rating;

/**
 * @className RecEvaluator
 * @description 评分预估
 **/
public interface RecEvaluator {

    /**
     * @description 添加评分
     * @param predict 预测的分值
     * @param actual  实际分值
     * @return void
     */
    void add(double predict, double actual);

    /**
     * @description 计算评估指标
     * @return double
     */
    double evaluate();

    /**
     * @description 计算的记录数
     * @return int
     */
    int getPredictSize();
}
