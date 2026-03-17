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

package com.ppwx.easysearch.core.similarity;

import java.util.function.Function;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @interface SequenceSimilarityScore
 * @description 字符序列相似度
 * @date 2024/10/12 15:31
 **/
public interface SequenceSimilarityScore<R> {

    /**
     * @description 以字符为细腻度的相似度计算
     * @author ext.ahs.zhouchzh1@jd.com
     * @date 2024/10/12 14:33
     * @param var1
     * @param var2
     * @param charFilter 字符过滤
     * @return R
     */
    R apply(CharSequence var1, CharSequence var2, Function<Character, Boolean> charFilter);

}
