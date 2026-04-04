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

import com.ppwx.easysearch.qp.correction.CorrectionResult;
import com.ppwx.easysearch.qp.correction.SpellCorrectionEngine;

/**
 * 拼写纠错阶段：对干预后的 query 进行拼写纠错检查。
 * <p>
 * 受 {@link ProcessOptions#isEnableSpellCorrection()} 控制。
 * 若关闭则跳过，并将 correctedQuery 透传为 intervenedQuery。
 * <p>
 * 读取 {@link QueryContext#getIntervenedQuery()}（若为空则使用 normalizedQuery/originalQuery），
 * 输出到 {@link QueryContext#setCorrectedQuery(String)} 和 {@link QueryContext#setCorrectionResult(CorrectionResult)}。
 */
public class SpellCorrectionStage extends AbstractStage {

    public static final String STAGE_NAME = "spellCorrection";

    private final SpellCorrectionEngine engine;

    public SpellCorrectionStage(SpellCorrectionEngine engine) {
        super(STAGE_NAME);
        this.engine = engine;
    }

    @Override
    protected void doProcess(QueryContext ctx) {
        String input = ctx.getIntervenedQuery() != null
                ? ctx.getIntervenedQuery()
                : (ctx.getNormalizedQuery() != null ? ctx.getNormalizedQuery() : ctx.getOriginalQuery());

        ProcessOptions options = ctx.getOptions();
        if (options != null && !options.isEnableSpellCorrection()) {
            ctx.setCorrectedQuery(input);
            ctx.putTrace(STAGE_NAME + ".skipped", true);
            return;
        }

        if (!engine.isLoaded()) {
            ctx.setCorrectedQuery(input);
            ctx.putTrace(STAGE_NAME + ".skipped", true);
            return;
        }

        CorrectionResult result = engine.correct(input);
        ctx.setCorrectionResult(result);
        ctx.setCorrectedQuery(result.getCorrectedQuery());
        ctx.putTrace(STAGE_NAME + ".hit", result.hasCorrections());
        ctx.putTrace(STAGE_NAME + ".autoCorrect", result.isAutoCorrect());
    }
}
