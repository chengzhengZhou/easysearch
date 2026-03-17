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
 * 改写策略：按起始位置顺序替换所有匹配项，每处用该匹配的第一个目标词。
 */
public class ReplaceAllRewriteStrategy implements RewriteStrategy {

    @Override
    public String rewrite(String query, List<SynonymMatch> matches) {
        if (query == null) {
            return null;
        }
        if (matches == null || matches.isEmpty()) {
            return query;
        }
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;
        for (SynonymMatch m : matches) {
            sb.append(query, lastEnd, m.getStartIndex());
            String replacement = m.getAttribute().getFirstTarget();
            if (!replacement.isEmpty()) {
                sb.append(replacement);
            } else {
                sb.append(query, m.getStartIndex(), m.getEndIndex());
            }
            lastEnd = m.getEndIndex();
        }
        sb.append(query.substring(lastEnd));
        return sb.toString();
    }
}
