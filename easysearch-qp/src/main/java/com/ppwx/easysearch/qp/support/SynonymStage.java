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

import com.ppwx.easysearch.qp.synonym.SynonymService;

import java.util.List;

/**
 * 同义词阶段：对当前 query 执行同义词改写或扩展。
 * <p>
 * 根据 {@link ProcessOptions} 决定使用 rewrite 还是 expand 模式：
 * <ul>
 *   <li>rewrite 模式：输出到 {@link QueryContext#setRewrittenQuery(String)}</li>
 *   <li>expand 模式：输出到 {@link QueryContext#setExpandedQueries(List)}</li>
 * </ul>
 */
public class SynonymStage extends AbstractStage {

    public static final String STAGE_NAME = "synonym";

    private final SynonymService synonymService;

    public SynonymStage(SynonymService synonymService) {
        super(STAGE_NAME);
        this.synonymService = synonymService;
    }

    @Override
    protected void doProcess(QueryContext ctx) {
        ProcessOptions options = ctx.getOptions();
        boolean doRewrite = options == null || options.isEnableSynonymRewrite();
        boolean doExpand = options != null && options.isEnableSynonymExpand();

        if (!doRewrite && !doExpand) {
            ctx.putTrace(STAGE_NAME + ".skipped", true);
            return;
        }

        String input = ctx.getCurrentQuery();
        if (input == null) {
            return;
        }

        if (doRewrite) {
            String rewritten = synonymService.rewrite(input);
            ctx.setRewrittenQuery(rewritten);
            boolean hit = (rewritten != null && !rewritten.equals(input));
            ctx.putTrace(STAGE_NAME + ".rewrite.hit", hit);
        }

        if (doExpand) {
            List<String> expanded = synonymService.expand(input);
            ctx.setExpandedQueries(expanded);
            ctx.putTrace(STAGE_NAME + ".expand.count", expanded != null ? expanded.size() : 0);
        }
    }
}
