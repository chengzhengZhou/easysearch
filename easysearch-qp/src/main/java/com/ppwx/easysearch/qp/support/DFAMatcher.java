package com.ppwx.easysearch.qp.support;

import java.util.List;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className DFAMatcher
 * @description 基于DFA算法的匹配
 * @date 2024/10/9 15:59
 **/
public class DFAMatcher implements EntityMatcher {

    private int order;

    /**
     * 词典树
     */
    private final WordMatchTree dicTree;
    /**
     * 词典标题
     */
    private final String name;

    public DFAMatcher(String title, WordMatchTree dicTree, int order) {
        this.name = title;
        this.dicTree = dicTree;
        this.order = order;
    }

    @Override
    public List<FoundWord> matchAll(String val) {
        return dicTree.matchAllWords(val.toLowerCase());
    }

    public String getName() {
        return name;
    }

    @Override
    public int getOrder() {
        return this.order;
    }
}
