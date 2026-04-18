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

import java.util.*;
import java.util.stream.Collectors;

/**
 * RRF（Reciprocal Rank Fusion）融合实现。
 * <p>
 * 公式：{@code score(d) = SUM( 1 / (k + rank_r(d)) )}
 * <p>
 * 其中 k 为平滑常数（默认 60），rank_r(d) 是候选 d 在第 r 路召回中的排名（从 1 开始）。
 * <p>
 * RRF 的优势：
 * <ul>
 *   <li>不需要分数归一化</li>
 *   <li>不需要设路间权重</li>
 *   <li>天然鼓励多路共识（同一候选被多路召回时 RRF 分累加）</li>
 *   <li>对异常分数鲁棒</li>
 * </ul>
 */
public class RRFSuggestionMerger implements SuggestionMerger {

    private final int k;

    /**
     * 使用默认平滑常数 k=60 创建 RRF 融合器。
     */
    public RRFSuggestionMerger() {
        this(60);
    }

    /**
     * 使用指定平滑常数创建 RRF 融合器。
     *
     * @param k 平滑常数，通常取 60
     */
    public RRFSuggestionMerger(int k) {
        this.k = Math.max(1, k);
    }

    @Override
    public List<Suggestion> merge(Map<String, List<RecallResult>> channelResults, int limit) {
        if (channelResults == null || channelResults.isEmpty()) {
            return Collections.emptyList();
        }

        // 聚合：text → (rrfScore, sources)
        Map<String, RRFAccumulator> accumulators = new LinkedHashMap<>();

        for (Map.Entry<String, List<RecallResult>> channelEntry : channelResults.entrySet()) {
            String channelName = channelEntry.getKey();
            List<RecallResult> results = channelEntry.getValue();
            if (results == null) {
                continue;
            }

            for (RecallResult result : results) {
                String text = result.getEntry().getText();
                double rrfScore = 1.0 / (k + result.getRank());

                accumulators
                        .computeIfAbsent(text, t -> new RRFAccumulator(result.getEntry()))
                        .addScore(rrfScore, channelName);
            }
        }

        // 构建 Suggestion 列表并按 RRF 分降序排序
        return accumulators.values().stream()
                .map(RRFAccumulator::toSuggestion)
                .sorted()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * RRF 分数累加器。
     */
    private static class RRFAccumulator {
        private final SuggestionEntry entry;
        private double totalScore;
        private final Set<String> sources;

        RRFAccumulator(SuggestionEntry entry) {
            this.entry = entry;
            this.totalScore = 0;
            this.sources = new LinkedHashSet<>();
        }

        void addScore(double score, String channel) {
            this.totalScore += score;
            this.sources.add(channel);
        }

        Suggestion toSuggestion() {
            return new Suggestion(entry.getText(), totalScore, new LinkedHashSet<>(sources));
        }
    }
}
