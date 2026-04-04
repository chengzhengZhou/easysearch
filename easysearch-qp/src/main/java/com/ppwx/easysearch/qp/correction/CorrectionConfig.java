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
 * 纠错配置项，包含阈值、权重和候选数限制等参数。
 * <p>
 * 所有字段均可通过 Builder 或 setter 调整，支持链式调用。
 */
public class CorrectionConfig {

    /** 编辑距离候选的最大编辑距离 */
    private int maxEditDistance = 2;

    /** 拼音候选最大召回数量 */
    private int maxPinyinCandidates = 10;

    /** 编辑距离候选最大召回数量 */
    private int maxEditCandidates = 10;

    /** 自动纠正的高置信度阈值，高于此值直接替换 */
    private double highThreshold = 0.8;

    /** 建议纠正的低置信度阈值，低于此值不做纠正 */
    private double lowThreshold = 0.6;

    /** 拼音相似度在总分中的权重 */
    private double pinyinWeight = 0.4;

    /** 编辑距离相似度在总分中的权重 */
    private double editDistanceWeight = 0.4;

    /** 词频在总分中的权重 */
    private double frequencyWeight = 0.2;

    public CorrectionConfig() {
    }

    public static CorrectionConfig defaults() {
        return new CorrectionConfig();
    }

    // --- Getters and Setters ---

    public int getMaxEditDistance() {
        return maxEditDistance;
    }

    public CorrectionConfig setMaxEditDistance(int maxEditDistance) {
        this.maxEditDistance = maxEditDistance;
        return this;
    }

    public int getMaxPinyinCandidates() {
        return maxPinyinCandidates;
    }

    public CorrectionConfig setMaxPinyinCandidates(int maxPinyinCandidates) {
        this.maxPinyinCandidates = maxPinyinCandidates;
        return this;
    }

    public int getMaxEditCandidates() {
        return maxEditCandidates;
    }

    public CorrectionConfig setMaxEditCandidates(int maxEditCandidates) {
        this.maxEditCandidates = maxEditCandidates;
        return this;
    }

    public double getHighThreshold() {
        return highThreshold;
    }

    public CorrectionConfig setHighThreshold(double highThreshold) {
        this.highThreshold = highThreshold;
        return this;
    }

    public double getLowThreshold() {
        return lowThreshold;
    }

    public CorrectionConfig setLowThreshold(double lowThreshold) {
        this.lowThreshold = lowThreshold;
        return this;
    }

    public double getPinyinWeight() {
        return pinyinWeight;
    }

    public CorrectionConfig setPinyinWeight(double pinyinWeight) {
        this.pinyinWeight = pinyinWeight;
        return this;
    }

    public double getEditDistanceWeight() {
        return editDistanceWeight;
    }

    public CorrectionConfig setEditDistanceWeight(double editDistanceWeight) {
        this.editDistanceWeight = editDistanceWeight;
        return this;
    }

    public double getFrequencyWeight() {
        return frequencyWeight;
    }

    public CorrectionConfig setFrequencyWeight(double frequencyWeight) {
        this.frequencyWeight = frequencyWeight;
        return this;
    }
}
