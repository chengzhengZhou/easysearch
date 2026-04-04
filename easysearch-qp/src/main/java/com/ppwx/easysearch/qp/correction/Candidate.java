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

/**
 * 纠错候选词，包含候选文本和各信号的分值明细。
 */
public class Candidate implements Comparable<Candidate> {

    private final String text;
    private final double pinyinScore;
    private final double editDistanceScore;
    private final double frequencyScore;
    private final double totalScore;

    public Candidate(String text, double pinyinScore, double editDistanceScore,
                     double frequencyScore, double totalScore) {
        this.text = text;
        this.pinyinScore = pinyinScore;
        this.editDistanceScore = editDistanceScore;
        this.frequencyScore = frequencyScore;
        this.totalScore = totalScore;
    }

    public Candidate(String text, double totalScore) {
        this(text, 0.0, 0.0, 0.0, totalScore);
    }

    public String getText() {
        return text;
    }

    public double getPinyinScore() {
        return pinyinScore;
    }

    public double getEditDistanceScore() {
        return editDistanceScore;
    }

    public double getFrequencyScore() {
        return frequencyScore;
    }

    public double getTotalScore() {
        return totalScore;
    }

    @Override
    public int compareTo(Candidate other) {
        // 按总分降序排列
        int cmp = Double.compare(other.totalScore, this.totalScore);
        if (cmp != 0) {
            return cmp;
        }
        return this.text.compareTo(other.text);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Candidate candidate = (Candidate) o;
        return text.equals(candidate.text);
    }

    @Override
    public int hashCode() {
        return text.hashCode();
    }

    @Override
    public String toString() {
        return "Candidate{text='" + text + "', totalScore=" + totalScore + "}";
    }
}
