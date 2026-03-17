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

package com.ppwx.easysearch.qp.ner.normalizer;

import com.ppwx.easysearch.qp.tokenizer.Token;

import java.util.List;

/**
 * 从 token 片段得到用于归一化的候选字符串的策略。
 * 用于 CRF 等多 token 实体识别场景，可按实体类型选用不同策略，避免单一空格拼接破坏原文格式导致召回过宽。
 */
public interface TokenSpanToValueStrategy {

    /**
     * 根据原文与 token 区间生成候选字符串（供后续按类型归一化使用）。
     *
     * @param originText     原始查询文本
     * @param tokens         token 列表
     * @param startTokenIdx  实体起始 token 下标（含）
     * @param endTokenIdx    实体结束 token 下标（不含）
     * @return 候选字符串，不应为 null
     */
    String toValue(String originText, List<Token> tokens, int startTokenIdx, int endTokenIdx);
}
