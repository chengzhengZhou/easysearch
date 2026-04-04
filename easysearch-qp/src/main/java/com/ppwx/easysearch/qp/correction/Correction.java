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
 * 单处纠错信息，记录 query 中某一片段的纠正详情。
 */
public class Correction {

    private final String original;
    private final String corrected;
    private final int startIndex;
    private final int endIndex;
    private final double confidence;
    private final List<Candidate> alternatives;

    public Correction(String original, String corrected, int startIndex, int endIndex,
                      double confidence, List<Candidate> alternatives) {
        this.original = original;
        this.corrected = corrected;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.confidence = confidence;
        this.alternatives = alternatives != null
                ? Collections.unmodifiableList(alternatives)
                : Collections.emptyList();
    }

    /** 被纠正的原始片段 */
    public String getOriginal() {
        return original;
    }

    /** 纠正后的词 */
    public String getCorrected() {
        return corrected;
    }

    /** 原始片段在 query 中的起始位置（含） */
    public int getStartIndex() {
        return startIndex;
    }

    /** 原始片段在 query 中的结束位置（不含） */
    public int getEndIndex() {
        return endIndex;
    }

    /** 该处纠正的置信度 [0, 1] */
    public double getConfidence() {
        return confidence;
    }

    /** 候选词列表（降序），包含被选中的纠正词和其余备选 */
    public List<Candidate> getAlternatives() {
        return alternatives;
    }

    @Override
    public String toString() {
        return "Correction{'" + original + "' -> '" + corrected
                + "' [" + startIndex + "," + endIndex + ")"
                + ", confidence=" + confidence + "}";
    }
}
