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

package com.ppwx.easysearch.qp.ner;

import com.ppwx.easysearch.qp.tokenizer.Token;

import java.util.Collection;
import java.util.List;

/**
 * @className EntityRecognizer
 * @description 实体识别
 **/
public interface EntityRecognizer {

    /**
     * 识别实体
     *
     * @param originText 原始文本
     * @param tokens     分词结果
     * @return 实体列表
     */
    Collection<Entity> extractEntities(String originText, List<Token> tokens);

}
