package com.ppwx.easysearch.qp.synonym;

import java.util.List;

/**
 * 改写策略：根据匹配结果将 query 改写为一条新 query。
 */
@FunctionalInterface
public interface RewriteStrategy {

    /**
     * 将 query 按 matches 改写为一条字符串。
     *
     * @param query   原始查询
     * @param matches 同义词匹配结果（通常来自 SynonymEngine.match）
     * @return 改写后的单条 query；无匹配时建议返回原 query
     */
    String rewrite(String query, List<SynonymMatch> matches);
}
