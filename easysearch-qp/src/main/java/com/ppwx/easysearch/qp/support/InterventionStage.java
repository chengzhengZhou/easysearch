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

import com.ppwx.easysearch.qp.intervention.InterventionService;

/**
 * 干预阶段：对预处理后的 query 执行整句/词表干预改写。
 * <p>
 * 读取 {@link QueryContext#getNormalizedQuery()}（若为空则使用 originalQuery），
 * 输出到 {@link QueryContext#setIntervenedQuery(String)}。
 */
public class InterventionStage extends AbstractStage {

    public static final String STAGE_NAME = "intervention";

    private final InterventionService interventionService;

    public InterventionStage(InterventionService interventionService) {
        super(STAGE_NAME);
        this.interventionService = interventionService;
    }

    @Override
    protected void doProcess(QueryContext ctx) {
        String input = ctx.getNormalizedQuery() != null
                ? ctx.getNormalizedQuery()
                : ctx.getOriginalQuery();

        String rewritten = interventionService.rewrite(input);
        ctx.setIntervenedQuery(rewritten);

        boolean hit = (rewritten != null && !rewritten.equals(input));
        ctx.putTrace(STAGE_NAME + ".hit", hit);
    }
}
