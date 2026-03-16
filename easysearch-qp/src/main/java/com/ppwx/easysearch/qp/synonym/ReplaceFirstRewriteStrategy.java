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
