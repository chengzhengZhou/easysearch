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

import java.util.*;

/**
 * 基于拼音混淆的候选生成器。
 * <p>
 * 将输入片段转为拼音，通过拼音倒排索引召回同音/近音候选词。
 */
public class PinyinCandidateGenerator {

    private final PinyinIndex pinyinIndex;
    private final int maxCandidates;

    public PinyinCandidateGenerator(PinyinIndex pinyinIndex, int maxCandidates) {
        this.pinyinIndex = pinyinIndex;
        this.maxCandidates = maxCandidates;
    }

    /**
     * 为疑似错误片段生成拼音候选列表。
     * <p>
     * 同时执行精确拼音匹配和首字母匹配，合并去重后按词频降序返回。
     *
     * @param term 疑似错误的输入片段
     * @return 候选词列表（按词频降序）
     */
    public List<DictEntry> generate(String term) {
        if (term == null || term.isEmpty()) {
            return Collections.emptyList();
        }

        String termPinyin = PinyinUtil.getPinyin(term, "").toLowerCase();
        String termInitials = PinyinUtil.getPinyinInitials(term).toLowerCase();

        // 精确拼音匹配
        List<DictEntry> exactMatches = pinyinIndex.exactSearch(termPinyin);
        // 首字母匹配（宽泛召回）
        List<DictEntry> initialMatches = pinyinIndex.initialSearch(termInitials);

        // 合并去重（相同词取词频较高者）
        Map<String, DictEntry> merged = new LinkedHashMap<>();
        for (DictEntry entry : exactMatches) {
            merged.put(entry.getWord(), entry);
        }
        for (DictEntry entry : initialMatches) {
            DictEntry existing = merged.get(entry.getWord());
            if (existing == null || entry.getFrequency() > existing.getFrequency()) {
                merged.put(entry.getWord(), entry);
            }
        }

        // 排除与输入完全相同的词
        merged.remove(term);

        // 按词频降序排列，取 Top-N
        List<DictEntry> result = new ArrayList<>(merged.values());
        result.sort((a, b) -> Long.compare(b.getFrequency(), a.getFrequency()));

        if (result.size() > maxCandidates) {
            result = result.subList(0, maxCandidates);
        }

        return result;
    }
}
