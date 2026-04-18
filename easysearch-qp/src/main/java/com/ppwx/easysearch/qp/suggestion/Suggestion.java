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

package com.ppwx.easysearch.qp.suggestion;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 联想词返回结果：经过多路召回融合后的最终输出。
 * <p>
 * 实现 {@link Comparable}，按 score 降序排列（score 越高越靠前）。
 */
public class Suggestion implements Comparable<Suggestion> {

    /** 联想词文本 */
    private final String text;

    /** 融合后的综合得分 */
    private double score;

    /** 命中的召回来源集合（如 prefix、pinyin、inverted） */
    private final Set<String> sources;

    /** 高亮信息（用户已输入的前缀部分标记，可选） */
    private String highlight;

    public Suggestion(String text, double score) {
        this.text = text;
        this.score = score;
        this.sources = new LinkedHashSet<>();
    }

    public Suggestion(String text, double score, Set<String> sources) {
        this.text = text;
        this.score = score;
        this.sources = sources != null ? sources : new LinkedHashSet<>();
    }

    public String getText() {
        return text;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public Set<String> getSources() {
        return sources;
    }

    public void addSource(String source) {
        this.sources.add(source);
    }

    public String getHighlight() {
        return highlight;
    }

    public void setHighlight(String highlight) {
        this.highlight = highlight;
    }

    @Override
    public int compareTo(Suggestion other) {
        // 降序：score 越高排越前
        return Double.compare(other.score, this.score);
    }

    @Override
    public String toString() {
        return "Suggestion{" +
                "text='" + text + '\'' +
                ", score=" + score +
                ", sources=" + sources +
                '}';
    }
}
