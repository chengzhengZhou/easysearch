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

import com.ppwx.easysearch.qp.ner.Entity;
import com.ppwx.easysearch.qp.ner.EntityRecognizer;
import com.ppwx.easysearch.qp.tokenizer.Token;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 实体识别阶段：基于分词结果提取实体。
 * <p>
 * 受 {@link ProcessOptions#isEnableNer()} 控制。
 * 读取 {@link QueryContext#getCurrentQuery()} 和 {@link QueryContext#getTokens()}，
 * 输出到 {@link QueryContext#setEntities(List)}。
 */
public class NerStage extends AbstractStage {

    public static final String STAGE_NAME = "ner";

    private final EntityRecognizer entityRecognizer;

    public NerStage(EntityRecognizer entityRecognizer) {
        super(STAGE_NAME);
        this.entityRecognizer = entityRecognizer;
    }

    @Override
    protected void doProcess(QueryContext ctx) {
        ProcessOptions options = ctx.getOptions();
        if (options != null && !options.isEnableNer()) {
            ctx.putTrace(STAGE_NAME + ".skipped", true);
            return;
        }

        String originText = ctx.getCurrentQuery();
        List<Token> tokens = ctx.getTokens();
        if (originText == null || tokens == null || tokens.isEmpty()) {
            ctx.setEntities(Collections.<Entity>emptyList());
            return;
        }

        Collection<Entity> entities = entityRecognizer.extractEntities(originText, tokens);
        ctx.setEntities(entities != null ? new ArrayList<>(entities) : Collections.<Entity>emptyList());
        ctx.putTrace(STAGE_NAME + ".entityCount", ctx.getEntities().size());
    }
}
