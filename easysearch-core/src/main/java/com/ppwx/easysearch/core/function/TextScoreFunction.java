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

package com.ppwx.easysearch.core.function;

import com.ppwx.easysearch.core.data.Column;

/**
 * 算分接口
 *
 * @since 1.0.0
 */
public interface TextScoreFunction extends Function<Double> {

    /**
     * 计算分值
     *
     * @param value
     * @return double
     */
    double score(String value);

    @Override
    Double apply(Column column);
}
