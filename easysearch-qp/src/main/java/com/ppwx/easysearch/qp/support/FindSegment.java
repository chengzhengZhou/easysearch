package com.ppwx.easysearch.qp.support;

import cn.hutool.core.lang.DefaultSegment;

public class FindSegment extends DefaultSegment<Integer> {
    /**
     * 词条
     */
    private final String term;

    public FindSegment(String term, int start, int end) {
        super(start, end);
        this.term = term;
    }

    public String getTerm() {
        return term;
    }

    @Override
    public String toString() {
        return "FindSegment{" +
                "term='" + term + '\'' +
                ", startIndex=" + startIndex +
                ", endIndex=" + endIndex +
                '}';
    }
}
