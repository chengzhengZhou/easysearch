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
