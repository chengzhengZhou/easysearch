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

package com.ppwx.easysearch.qp.synonym;

import java.util.List;

/**
 * 改写策略：仅替换第一个匹配项（按起始位置），用其第一个目标词。
 */
public class ReplaceFirstRewriteStrategy implements RewriteStrategy {

    @Override
    public String rewrite(String query, List<SynonymMatch> matches) {
        if (query == null) {
            return null;
        }
        if (matches == null || matches.isEmpty()) {
            return query;
        }
        SynonymMatch first = matches.get(0);
        String replacement = first.getAttribute().getFirstTarget();
        if (replacement.isEmpty()) {
            return query;
        }
        return query.substring(0, first.getStartIndex())
                + replacement
                + query.substring(first.getEndIndex());
    }
}
