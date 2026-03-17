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
 * 使用原文切片的策略：按首尾 token 的 offset 在 originText 上截取，保持与原文一致，便于与索引匹配。
 */
public class OriginalSliceStrategy implements TokenSpanToValueStrategy {

    @Override
    public String toValue(String originText, List<Token> tokens, int startTokenIdx, int endTokenIdx) {
        if (originText == null || tokens == null || startTokenIdx < 0 || endTokenIdx > tokens.size() || startTokenIdx >= endTokenIdx) {
            return "";
        }
        Token startToken = tokens.get(startTokenIdx);
        Token endToken = tokens.get(endTokenIdx - 1);
        int start = Math.min(startToken.getStartIndex(), originText.length());
        int end = Math.min(endToken.getEndIndex(), originText.length());
        if (start >= end) {
            return "";
        }
        return originText.substring(start, end);
    }
}
