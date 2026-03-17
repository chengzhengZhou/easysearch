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
 * 无空格拼接各 token 文本的策略，适用于索引侧为紧凑写法的场景。
 */
public class NoSpaceJoinStrategy implements TokenSpanToValueStrategy {

    @Override
    public String toValue(String originText, List<Token> tokens, int startTokenIdx, int endTokenIdx) {
        if (tokens == null || startTokenIdx < 0 || endTokenIdx > tokens.size() || startTokenIdx >= endTokenIdx) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = startTokenIdx; i < endTokenIdx; i++) {
            String text = tokens.get(i).getText();
            if (text != null) {
                sb.append(text);
            }
        }
        return sb.toString();
    }
}
