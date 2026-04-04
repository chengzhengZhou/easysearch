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

import com.ppwx.easysearch.qp.format.WordFormat;

/**
 * 输入预处理阶段：对原始 query 做格式化（大小写统一、全半角转换、截断等）。
 * <p>
 * 读取 {@link QueryContext#getOriginalQuery()}，输出到 {@link QueryContext#setNormalizedQuery(String)}。
 */
public class FormatStage extends AbstractStage {

    public static final String STAGE_NAME = "format";

    private final WordFormat wordFormat;

    public FormatStage(WordFormat wordFormat) {
        super(STAGE_NAME);
        this.wordFormat = wordFormat;
    }

    @Override
    protected void doProcess(QueryContext ctx) {
        String original = ctx.getOriginalQuery();
        if (original == null) {
            ctx.setNormalizedQuery(null);
            return;
        }
        String normalized = wordFormat.format(new StringBuilder(original)).toString();
        ctx.setNormalizedQuery(normalized);
    }
}
