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

package com.ppwx.easysearch.qp.correction;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * CandidateScorer 多信号打分器单元测试。
 */
public class CandidateScorerTest {

    @Test
    public void computeEditDistanceSimilarity_sameWord() {
        CorrectionConfig config = CorrectionConfig.defaults();
        CandidateScorer scorer = new CandidateScorer(config);
        assertEquals(1.0, scorer.computeEditDistanceSimilarity("手机壳", "手机壳"), 0.001);
    }

    @Test
    public void computeEditDistanceSimilarity_edit1() {
        CorrectionConfig config = CorrectionConfig.defaults();
        CandidateScorer scorer = new CandidateScorer(config);
        // "手机壳" vs "手机膜"：编辑距离 1，maxLen 3 → 1 - 1/3 = 0.667
        double sim = scorer.computeEditDistanceSimilarity("手机壳", "手机膜");
        assertTrue(sim > 0.6 && sim < 0.7);
    }

    @Test
    public void computeEditDistanceSimilarity_completelyDifferent() {
        CorrectionConfig config = CorrectionConfig.defaults();
        CandidateScorer scorer = new CandidateScorer(config);
        double sim = scorer.computeEditDistanceSimilarity("abc", "xyz");
        assertEquals(0.0, sim, 0.001);
    }

    @Test
    public void computeEditDistanceSimilarity_nullSafe() {
        CorrectionConfig config = CorrectionConfig.defaults();
        CandidateScorer scorer = new CandidateScorer(config);
        assertEquals(0.0, scorer.computeEditDistanceSimilarity(null, "abc"), 0.001);
        assertEquals(0.0, scorer.computeEditDistanceSimilarity("abc", null), 0.001);
    }

    @Test
    public void computePinyinSimilarity_samePinyin() {
        CorrectionConfig config = CorrectionConfig.defaults();
        CandidateScorer scorer = new CandidateScorer(config);
        // 拼音相同的词
        double sim = scorer.computePinyinSimilarity("手机壳", "手机壳");
        assertEquals(1.0, sim, 0.001);
    }

    @Test
    public void computePinyinSimilarity_differentPinyin() {
        CorrectionConfig config = CorrectionConfig.defaults();
        CandidateScorer scorer = new CandidateScorer(config);
        double sim = scorer.computePinyinSimilarity("手机壳", "充电宝");
        assertTrue(sim < 1.0);
    }

    @Test
    public void computeFrequencyScore_zeroFrequency() {
        CorrectionConfig config = CorrectionConfig.defaults();
        CandidateScorer scorer = new CandidateScorer(config);
        scorer.setMaxFrequency(10000);
        assertEquals(0.0, scorer.computeFrequencyScore(0), 0.001);
    }

    @Test
    public void computeFrequencyScore_maxFrequency() {
        CorrectionConfig config = CorrectionConfig.defaults();
        CandidateScorer scorer = new CandidateScorer(config);
        scorer.setMaxFrequency(10000);
        assertEquals(1.0, scorer.computeFrequencyScore(10000), 0.001);
    }

    @Test
    public void computeFrequencyScore_halfFrequency() {
        CorrectionConfig config = CorrectionConfig.defaults();
        CandidateScorer scorer = new CandidateScorer(config);
        scorer.setMaxFrequency(10000);
        // 使用对数平滑，所以 5000 的得分不是简单 0.5
        double score = scorer.computeFrequencyScore(5000);
        assertTrue(score > 0.0 && score < 1.0);
    }

    @Test
    public void score_candidate() {
        CorrectionConfig config = CorrectionConfig.defaults();
        CandidateScorer scorer = new CandidateScorer(config);
        scorer.setMaxFrequency(10000);

        DictEntry entry = new DictEntry("手机膜", null, 8500);
        Candidate candidate = scorer.score("手机壳", entry);

        assertEquals("手机膜", candidate.getText());
        assertTrue(candidate.getTotalScore() > 0.0);
        assertTrue(candidate.getPinyinScore() >= 0.0);
        assertTrue(candidate.getEditDistanceScore() >= 0.0);
        assertTrue(candidate.getFrequencyScore() >= 0.0);
    }

    @Test
    public void score_highFrequencyCandidateRanksHigher() {
        CorrectionConfig config = new CorrectionConfig()
                .setPinyinWeight(0.0)
                .setEditDistanceWeight(0.0)
                .setFrequencyWeight(1.0);
        CandidateScorer scorer = new CandidateScorer(config);
        scorer.setMaxFrequency(10000);

        DictEntry lowFreq = new DictEntry("手机膜", null, 100);
        DictEntry highFreq = new DictEntry("手机套", null, 9000);

        // 编辑距离相同，纯看词频
        Candidate lowCandidate = scorer.score("手机壳", lowFreq);
        Candidate highCandidate = scorer.score("手机壳", highFreq);

        assertTrue(highCandidate.getTotalScore() > lowCandidate.getTotalScore());
    }

    @Test
    public void candidate_comparable() {
        Candidate high = new Candidate("a", 0.9);
        Candidate low = new Candidate("b", 0.5);
        assertTrue(high.compareTo(low) < 0); // 降序，高分在前
    }

    @Test
    public void candidate_equalsAndHashCode() {
        Candidate c1 = new Candidate("手机膜", 0.8, 0.7, 0.9, 0.8);
        Candidate c2 = new Candidate("手机膜", 0.5, 0.5, 0.5, 0.5);
        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
    }
}
