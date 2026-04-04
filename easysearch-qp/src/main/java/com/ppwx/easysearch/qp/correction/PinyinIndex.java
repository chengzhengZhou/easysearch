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
 * 拼音倒排索引：将词典中每个词转为拼音，建立 拼音 → 词条列表 的映射。
 * <p>
 * 支持精确拼音匹配和拼音首字母匹配两种查询模式。
 */
public class PinyinIndex {

    /** 拼音（无调，各字拼音直接拼接） → 词条列表 */
    private final Map<String, List<DictEntry>> exactIndex = new HashMap<>();

    /** 拼音首字母 → 词条列表 */
    private final Map<String, List<DictEntry>> initialIndex = new HashMap<>();

    /**
     * 构建拼音倒排索引。
     *
     * @param entries 词典条目列表
     */
    public void build(List<DictEntry> entries) {
        exactIndex.clear();
        initialIndex.clear();

        for (DictEntry entry : entries) {
            String word = entry.getWord();
            // 如果词条自带拼音则直接使用，否则通过 PinyinUtil 转换
            String pinyin = (entry.getPinyin() != null && !entry.getPinyin().isEmpty())
                    ? entry.getPinyin()
                    : PinyinUtil.getPinyin(word, "");

            // 精确拼音索引
            addToIndex(exactIndex, pinyin, entry);

            // 首字母索引
            String initials = PinyinUtil.getPinyinInitials(word);
            if (initials != null && !initials.isEmpty()) {
                addToIndex(initialIndex, initials.toLowerCase(), entry);
            }
        }
    }

    private void addToIndex(Map<String, List<DictEntry>> index, String key, DictEntry entry) {
        if (key == null || key.isEmpty()) {
            return;
        }
        String normalizedKey = key.toLowerCase();
        index.computeIfAbsent(normalizedKey, k -> new ArrayList<>()).add(entry);
    }

    /**
     * 精确拼音匹配：查询拼音完全相同的词条。
     *
     * @param pinyin 无调拼音字符串
     * @return 匹配的词条列表，不为 null
     */
    public List<DictEntry> exactSearch(String pinyin) {
        if (pinyin == null || pinyin.isEmpty()) {
            return Collections.emptyList();
        }
        List<DictEntry> result = exactIndex.get(pinyin.toLowerCase());
        return result != null ? result : Collections.emptyList();
    }

    /**
     * 拼音首字母匹配：查询首字母相同的词条。
     *
     * @param initials 拼音首字母字符串
     * @return 匹配的词条列表，不为 null
     */
    public List<DictEntry> initialSearch(String initials) {
        if (initials == null || initials.isEmpty()) {
            return Collections.emptyList();
        }
        List<DictEntry> result = initialIndex.get(initials.toLowerCase());
        return result != null ? result : Collections.emptyList();
    }

    /**
     * 查询某个词是否存在于精确拼音索引中（即词典中是否有该词）。
     *
     * @param word 待查词
     * @return true 如果词典中存在
     */
    public boolean containsWord(String word) {
        String pinyin = PinyinUtil.getPinyin(word, "");
        List<DictEntry> entries = exactIndex.get(pinyin.toLowerCase());
        if (entries == null) {
            return false;
        }
        for (DictEntry entry : entries) {
            if (entry.getWord().equals(word)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取精确索引大小。
     */
    public int exactSize() {
        return exactIndex.size();
    }

    /**
     * 获取首字母索引大小。
     */
    public int initialSize() {
        return initialIndex.size();
    }
}
