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

import java.util.Objects;

/**
 * 联想词条目：词表中每条联想词的数据载体。
 * <p>
 * 作为 Trie 节点值和各模块间传递的核心数据结构。
 * equals/hashCode 基于 {@link #text} 字段，确保融合去重正确。
 */
public class SuggestionEntry {

    /** 联想词文本 */
    private final String text;

    /** 权重（搜索频次、CTR 等综合分） */
    private final long weight;

    /** 类目标签（可选，用于多样性控制或品类过滤） */
    private final String category;

    public SuggestionEntry(String text, long weight) {
        this(text, weight, null);
    }

    public SuggestionEntry(String text, long weight, String category) {
        this.text = text;
        this.weight = weight;
        this.category = category;
    }

    public String getText() {
        return text;
    }

    public long getWeight() {
        return weight;
    }

    public String getCategory() {
        return category;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SuggestionEntry that = (SuggestionEntry) o;
        return Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text);
    }

    @Override
    public String toString() {
        return "SuggestionEntry{" +
                "text='" + text + '\'' +
                ", weight=" + weight +
                ", category='" + category + '\'' +
                '}';
    }
}
