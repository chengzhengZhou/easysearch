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

/**
 * 单路召回结果：包含候选词条及其在该路中的排名。
 * <p>
 * 排名（rank）从 1 开始，供融合器（如 RRF）计算融合分数。
 */
public class RecallResult {

    /** 候选词条 */
    private final SuggestionEntry entry;

    /** 路内排名，从 1 开始 */
    private final int rank;

    public RecallResult(SuggestionEntry entry, int rank) {
        this.entry = entry;
        this.rank = rank;
    }

    public SuggestionEntry getEntry() {
        return entry;
    }

    public int getRank() {
        return rank;
    }

    @Override
    public String toString() {
        return "RecallResult{" +
                "entry=" + entry +
                ", rank=" + rank +
                '}';
    }
}
