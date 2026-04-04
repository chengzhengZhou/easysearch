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

import java.util.Collections;
import java.util.List;

/**
 * 纠错结果，包含原始 query、纠正后 query、置信度和各处纠正详情。
 * <p>
 * 不可变对象，线程安全。
 */
public class CorrectionResult {

    private final String originalQuery;
    private final String correctedQuery;
    private final double confidence;
    private final List<Correction> corrections;
    private final boolean autoCorrect;

    public CorrectionResult(String originalQuery, String correctedQuery,
                            double confidence, List<Correction> corrections,
                            boolean autoCorrect) {
        this.originalQuery = originalQuery;
        this.correctedQuery = correctedQuery;
        this.confidence = confidence;
        this.corrections = corrections != null
                ? Collections.unmodifiableList(corrections)
                : Collections.emptyList();
        this.autoCorrect = autoCorrect;
    }

    /**
     * 创建无纠正的结果（query 本身无拼写错误）。
     */
    public static CorrectionResult noCorrection(String query) {
        return new CorrectionResult(query, query, 0.0,
                Collections.emptyList(), false);
    }

    /** 原始 query */
    public String getOriginalQuery() {
        return originalQuery;
    }

    /** 纠错后 query（可能与原始相同） */
    public String getCorrectedQuery() {
        return correctedQuery;
    }

    /** 整体置信度 [0, 1] */
    public double getConfidence() {
        return confidence;
    }

    /** 各片段纠正详情 */
    public List<Correction> getCorrections() {
        return corrections;
    }

    /** 是否执行了自动纠正（vs 仅建议） */
    public boolean isAutoCorrect() {
        return autoCorrect;
    }

    /** 是否有任何纠正 */
    public boolean hasCorrections() {
        return !corrections.isEmpty();
    }

    @Override
    public String toString() {
        return "CorrectionResult{original='" + originalQuery
                + "', corrected='" + correctedQuery
                + "', confidence=" + confidence
                + ", autoCorrect=" + autoCorrect
                + ", corrections=" + corrections + "}";
    }
}
