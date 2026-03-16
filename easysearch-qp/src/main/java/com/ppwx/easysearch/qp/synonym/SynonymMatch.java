package com.ppwx.easysearch.qp.synonym;

import java.util.Objects;

/**
 * 同义词匹配结果：在 query 中的 span 及对应的同义属性。
 */
public class SynonymMatch {

    private final int startIndex;
    private final int endIndex;
    private final String source;
    private final SynonymAttribute attribute;

    public SynonymMatch(int startIndex, int endIndex, String source, SynonymAttribute attribute) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.source = source;
        this.attribute = attribute;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public String getSource() {
        return source;
    }

    public SynonymAttribute getAttribute() {
        return attribute;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SynonymMatch that = (SynonymMatch) o;
        return startIndex == that.startIndex && endIndex == that.endIndex
                && Objects.equals(source, that.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startIndex, endIndex, source);
    }
}
