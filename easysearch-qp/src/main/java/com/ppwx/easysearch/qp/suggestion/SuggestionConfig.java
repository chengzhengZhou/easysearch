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
 * 联想词配置项。
 * <p>
 * 包含返回条数、RRF 平滑参数、各路召回开关和最大召回数等参数。
 * 支持链式 setter 调用。
 */
public class SuggestionConfig {

    /** 最终返回的联想词条数 */
    private int topK = 10;

    /** RRF 融合的平滑常数 k（公式：1/(k+rank)） */
    private int rrfK = 60;

    /** 前缀召回路最大候选数 */
    private int maxPrefixCandidates = 50;

    /** 拼音召回路最大候选数 */
    private int maxPinyinCandidates = 30;

    /** 倒排索引召回路最大候选数 */
    private int maxInvertedCandidates = 30;

    /** 是否启用前缀召回 */
    private boolean enablePrefix = true;

    /** 是否启用拼音召回 */
    private boolean enablePinyin = true;

    /** 是否启用倒排索引召回 */
    private boolean enableInvertedIndex = false;

    public SuggestionConfig() {
    }

    public static SuggestionConfig defaults() {
        return new SuggestionConfig();
    }

    // --- Getters and chainable Setters ---

    public int getTopK() {
        return topK;
    }

    public SuggestionConfig setTopK(int topK) {
        this.topK = topK;
        return this;
    }

    public int getRrfK() {
        return rrfK;
    }

    public SuggestionConfig setRrfK(int rrfK) {
        this.rrfK = rrfK;
        return this;
    }

    public int getMaxPrefixCandidates() {
        return maxPrefixCandidates;
    }

    public SuggestionConfig setMaxPrefixCandidates(int maxPrefixCandidates) {
        this.maxPrefixCandidates = maxPrefixCandidates;
        return this;
    }

    public int getMaxPinyinCandidates() {
        return maxPinyinCandidates;
    }

    public SuggestionConfig setMaxPinyinCandidates(int maxPinyinCandidates) {
        this.maxPinyinCandidates = maxPinyinCandidates;
        return this;
    }

    public int getMaxInvertedCandidates() {
        return maxInvertedCandidates;
    }

    public SuggestionConfig setMaxInvertedCandidates(int maxInvertedCandidates) {
        this.maxInvertedCandidates = maxInvertedCandidates;
        return this;
    }

    public boolean isEnablePrefix() {
        return enablePrefix;
    }

    public SuggestionConfig setEnablePrefix(boolean enablePrefix) {
        this.enablePrefix = enablePrefix;
        return this;
    }

    public boolean isEnablePinyin() {
        return enablePinyin;
    }

    public SuggestionConfig setEnablePinyin(boolean enablePinyin) {
        this.enablePinyin = enablePinyin;
        return this;
    }

    public boolean isEnableInvertedIndex() {
        return enableInvertedIndex;
    }

    public SuggestionConfig setEnableInvertedIndex(boolean enableInvertedIndex) {
        this.enableInvertedIndex = enableInvertedIndex;
        return this;
    }
}
