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

package com.ppwx.easysearch.qp.support;

import com.ppwx.easysearch.qp.tokenizer.Token;
import com.ppwx.easysearch.qp.tokenizer.Tokenizer;

import java.util.List;

/**
 * 分词阶段：对当前最终 query 执行分词。
 * <p>
 * 读取 {@link QueryContext#getCurrentQuery()}，
 * 输出到 {@link QueryContext#setTokens(List)}。
 */
public class TokenizerStage extends AbstractStage {

    public static final String STAGE_NAME = "tokenizer";

    private final Tokenizer tokenizer;

    public TokenizerStage(Tokenizer tokenizer) {
        super(STAGE_NAME);
        this.tokenizer = tokenizer;
    }

    @Override
    protected void doProcess(QueryContext ctx) {
        String input = ctx.getCurrentQuery();
        if (input == null) {
            return;
        }
        List<Token> tokens = tokenizer.tokenize(input);
        ctx.setTokens(tokens);
        ctx.putTrace(STAGE_NAME + ".tokenCount", tokens != null ? tokens.size() : 0);
    }
}
