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

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * RRFSuggestionMerger 单元测试。
 */
public class RRFSuggestionMergerTest {

    private final RRFSuggestionMerger merger = new RRFSuggestionMerger(60);

    @Test
    public void merge_singleChannel() {
        Map<String, List<RecallResult>> channelResults = new LinkedHashMap<>();
        channelResults.put("prefix", Arrays.asList(
                new RecallResult(new SuggestionEntry("苹果手机", 50000), 1),
                new RecallResult(new SuggestionEntry("苹果手机壳", 12000), 2)
        ));

        List<Suggestion> result = merger.merge(channelResults, 10);
        assertEquals(2, result.size());
        assertEquals("苹果手机", result.get(0).getText());
        assertEquals("苹果手机壳", result.get(1).getText());
        assertTrue(result.get(0).getScore() > result.get(1).getScore());
    }

    @Test
    public void merge_multiChannel_duplicateBoost() {
        Map<String, List<RecallResult>> channelResults = new LinkedHashMap<>();
        // 前缀路：苹果手机(rank1), 苹果手机壳(rank2)
        channelResults.put("prefix", Arrays.asList(
                new RecallResult(new SuggestionEntry("苹果手机", 50000), 1),
                new RecallResult(new SuggestionEntry("苹果手机壳", 12000), 2)
        ));
        // 拼音路：苹果手机壳(rank1), 苹果手机(rank2)
        channelResults.put("pinyin", Arrays.asList(
                new RecallResult(new SuggestionEntry("苹果手机壳", 12000), 1),
                new RecallResult(new SuggestionEntry("苹果手机", 50000), 2)
        ));

        List<Suggestion> result = merger.merge(channelResults, 10);
        assertEquals(2, result.size());

        // 两者都被两路召回，RRF 分数相近但 "苹果手机" 在两路中的 rank 之和更小
        // prefix: 苹果手机=1/(60+1), 苹果手机壳=1/(60+2)
        // pinyin: 苹果手机壳=1/(60+1), 苹果手机=1/(60+2)
        // 苹果手机 total = 1/61 + 1/62
        // 苹果手机壳 total = 1/62 + 1/61
        // 两者相同，排序可能交替，但都应该被命中两路
        for (Suggestion s : result) {
            assertEquals(2, s.getSources().size());
        }
    }

    @Test
    public void merge_multiHitBetterThanSingleHit() {
        Map<String, List<RecallResult>> channelResults = new LinkedHashMap<>();
        // 前缀路：A rank1, B rank2
        List<RecallResult> prefixResults = new ArrayList<>();
        prefixResults.add(new RecallResult(new SuggestionEntry("A词条", 100), 1));
        prefixResults.add(new RecallResult(new SuggestionEntry("B词条", 80), 2));
        channelResults.put("prefix", prefixResults);
        // 拼音路：B rank1（B 被两路召回，A 只被一路）
        channelResults.put("pinyin", Arrays.asList(
                new RecallResult(new SuggestionEntry("B词条", 80), 1)
        ));

        List<Suggestion> result = merger.merge(channelResults, 10);
        // B 被两路召回，A 只被一路召回
        // B: 1/(60+2) + 1/(60+1) ≈ 0.0161 + 0.0164 = 0.0325
        // A: 1/(60+1) ≈ 0.0164
        assertEquals("B词条", result.get(0).getText());
        assertEquals(2, result.get(0).getSources().size());
    }

    @Test
    public void merge_respectsLimit() {
        Map<String, List<RecallResult>> channelResults = new LinkedHashMap<>();
        List<RecallResult> results = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            results.add(new RecallResult(new SuggestionEntry("词条" + i, 100 - i), i + 1));
        }
        channelResults.put("prefix", results);

        List<Suggestion> merged = merger.merge(channelResults, 5);
        assertEquals(5, merged.size());
    }

    @Test
    public void merge_emptyInput() {
        assertTrue(merger.merge(null, 10).isEmpty());
        assertTrue(merger.merge(Collections.<String, List<RecallResult>>emptyMap(), 10).isEmpty());
    }

    @Test
    public void merge_emptyChannelResults() {
        Map<String, List<RecallResult>> channelResults = new LinkedHashMap<>();
        channelResults.put("prefix", Collections.<RecallResult>emptyList());

        List<Suggestion> result = merger.merge(channelResults, 10);
        assertTrue(result.isEmpty());
    }

    @Test
    public void merge_customK() {
        RRFSuggestionMerger customMerger = new RRFSuggestionMerger(1);
        Map<String, List<RecallResult>> channelResults = new LinkedHashMap<>();
        channelResults.put("prefix", Arrays.asList(
                new RecallResult(new SuggestionEntry("A", 100), 1),
                new RecallResult(new SuggestionEntry("B", 80), 2)
        ));

        List<Suggestion> result = customMerger.merge(channelResults, 10);
        assertEquals(2, result.size());
        // k=1: A=1/(1+1)=0.5, B=1/(1+2)=0.333
        assertEquals(0.5, result.get(0).getScore(), 0.001);
    }
}
