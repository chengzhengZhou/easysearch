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

import com.ppwx.easysearch.qp.util.PinyinUtil;

/**
 * 多信号融合候选打分器。
 * <p>
 * 对每个候选词综合计算拼音相似度、编辑距离相似度和词频得分，加权求和得到总分。
 * <p>
 * 公式：score = w1 * pinyinSim + w2 * editSim + w3 * freqSim
 */
public class CandidateScorer {

    private final double pinyinWeight;
    private final double editDistanceWeight;
    private final double frequencyWeight;

    /** 词频归一化基准值（词典中最高词频），用于将词频归一化到 [0, 1] */
    private long maxFrequency;

    public CandidateScorer(CorrectionConfig config) {
        this.pinyinWeight = config.getPinyinWeight();
        this.editDistanceWeight = config.getEditDistanceWeight();
        this.frequencyWeight = config.getFrequencyWeight();
        this.maxFrequency = 1L;
    }

    /**
     * 更新词频基准值（在词典加载完成后调用）。
     *
     * @param maxFrequency 词典中最高词频
     */
    public void setMaxFrequency(long maxFrequency) {
        this.maxFrequency = Math.max(maxFrequency, 1L);
    }

    /**
     * 对候选词条计算综合得分。
     *
     * @param originalTerm 原始（疑似错误的）输入片段
     * @param entry        词典中的候选词条
     * @return 打分后的 Candidate 对象
     */
    public Candidate score(String originalTerm, DictEntry entry) {
        String candidateWord = entry.getWord();

        double pinyinScore = computePinyinSimilarity(originalTerm, candidateWord);
        double editScore = computeEditDistanceSimilarity(originalTerm, candidateWord);
        double freqScore = computeFrequencyScore(entry.getFrequency());

        double totalScore = pinyinWeight * pinyinScore
                + editDistanceWeight * editScore
                + frequencyWeight * freqScore;

        return new Candidate(candidateWord, pinyinScore, editScore, freqScore, totalScore);
    }

    /**
     * 计算拼音相似度。
     * <p>
     * 将两个词分别转为拼音，然后计算拼音字符串的编辑距离相似度。
     *
     * @param term 原始词
     * @param candidate 候选词
     * @return 拼音相似度 [0, 1]
     */
    double computePinyinSimilarity(String term, String candidate) {
        if (term == null || candidate == null) {
            return 0.0;
        }
        if (term.equals(candidate)) {
            return 1.0;
        }

        String termPinyin = PinyinUtil.getPinyin(term, "");
        String candPinyin = PinyinUtil.getPinyin(candidate, "");

        if (termPinyin.equalsIgnoreCase(candPinyin)) {
            return 1.0;
        }

        int distance = EditDistanceCandidateGenerator.levenshteinDistance(
                termPinyin.toLowerCase(), candPinyin.toLowerCase());
        int maxLen = Math.max(termPinyin.length(), candPinyin.length());
        if (maxLen == 0) {
            return 1.0;
        }

        return 1.0 - (double) distance / maxLen;
    }

    /**
     * 计算字符编辑距离相似度。
     *
     * @param term 原始词
     * @param candidate 候选词
     * @return 编辑距离相似度 [0, 1]
     */
    double computeEditDistanceSimilarity(String term, String candidate) {
        if (term == null || candidate == null) {
            return 0.0;
        }
        if (term.equals(candidate)) {
            return 1.0;
        }

        int distance = EditDistanceCandidateGenerator.levenshteinDistance(term, candidate);
        int maxLen = Math.max(term.length(), candidate.length());
        if (maxLen == 0) {
            return 1.0;
        }

        return 1.0 - (double) distance / maxLen;
    }

    /**
     * 计算词频得分（归一化到 [0, 1]）。
     * <p>
     * 使用对数平滑以避免高频词垄断得分。
     *
     * @param frequency 候选词词频
     * @return 归一化词频得分 [0, 1]
     */
    double computeFrequencyScore(long frequency) {
        if (frequency <= 0) {
            return 0.0;
        }
        // 对数平滑：log(1 + freq) / log(1 + maxFreq)
        double normalized = Math.log(1.0 + frequency) / Math.log(1.0 + maxFrequency);
        return Math.min(1.0, Math.max(0.0, normalized));
    }
}
