package com.ppwx.easysearch.qp.synonym;

import java.util.*;

/**
 * 拓展策略：对每个匹配用其每个目标替换，生成多条 query（去重）。
 * 若多个匹配，仅对第一个匹配做多目标展开；其余匹配用第一个目标替换（避免组合爆炸）。
 */
public class ExpandOrStrategy implements ExpandStrategy {

    @Override
    public List<String> expand(String query, List<SynonymMatch> matches) {
        if (query == null) {
            return Collections.singletonList(null);
        }
        if (matches == null || matches.isEmpty()) {
            return Collections.singletonList(query);
        }

        Set<String> result = new LinkedHashSet<>();
        SynonymMatch first = matches.get(0);
        List<String> targets = first.getAttribute().getTargets();

        if (targets.isEmpty()) {
            result.add(query);
        } else {
            for (String target : targets) {
                String replaced = query.substring(0, first.getStartIndex())
                        + target
                        + query.substring(first.getEndIndex());
                result.add(replaced);
            }
        }

        if (matches.size() > 1) {
            ReplaceAllRewriteStrategy replaceAll = new ReplaceAllRewriteStrategy();
            result.add(replaceAll.rewrite(query, matches));
        }

        return new ArrayList<>(result);
    }
}
