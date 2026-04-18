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

import com.ppwx.easysearch.qp.util.PinyinUtil;

import java.util.*;

/**
 * 拼音召回通道：支持全拼前缀匹配和首字母前缀匹配。
 * <p>
 * 自动判断输入类型：
 * <ul>
 *   <li>汉字输入 → 转拼音后走全拼 Trie</li>
 *   <li>纯拉丁字母输入 → 同时走全拼 Trie 和首字母 Trie，合并去重</li>
 *   <li>混合输入 → 走全拼 Trie</li>
 * </ul>
 */
public class PinyinRecallChannel implements RecallChannel {

    public static final String CHANNEL_NAME = "pinyin";

    private final SuggestionEngine engine;

    public PinyinRecallChannel(SuggestionEngine engine) {
        this.engine = engine;
    }

    @Override
    public String name() {
        return CHANNEL_NAME;
    }

    @Override
    public List<RecallResult> recall(String prefix, int limit) {
        if (prefix == null || prefix.isEmpty() || !engine.isLoaded()) {
            return Collections.emptyList();
        }

        InputType inputType = detectInputType(prefix);
        Set<String> seen = new LinkedHashSet<>();
        List<SuggestionEntry> merged = new ArrayList<>();

        switch (inputType) {
            case CHINESE:
                // 汉字输入：转为拼音，走全拼 Trie
                String pinyin = PinyinUtil.getPinyin(prefix, "");
                if (pinyin != null && !pinyin.isEmpty()) {
                    collectEntries(engine.pinyinSearch(pinyin.toLowerCase(), limit), seen, merged);
                }
                break;

            case LATIN:
                // 纯拉丁字母：同时走全拼和首字母
                String lowerPrefix = prefix.toLowerCase();
                collectEntries(engine.pinyinSearch(lowerPrefix, limit), seen, merged);
                collectEntries(engine.initialSearch(lowerPrefix, limit), seen, merged);
                break;

            case MIXED:
            default:
                // 混合输入：转全拼匹配
                String mixedPinyin = PinyinUtil.getPinyin(prefix, "");
                if (mixedPinyin != null && !mixedPinyin.isEmpty()) {
                    collectEntries(engine.pinyinSearch(mixedPinyin.toLowerCase(), limit), seen, merged);
                }
                break;
        }

        // 按 weight 降序排序后截断
        merged.sort((a, b) -> Long.compare(b.getWeight(), a.getWeight()));
        if (merged.size() > limit) {
            merged = merged.subList(0, limit);
        }

        // 包装为 RecallResult
        List<RecallResult> results = new ArrayList<>(merged.size());
        for (int i = 0; i < merged.size(); i++) {
            results.add(new RecallResult(merged.get(i), i + 1));
        }
        return results;
    }

    private void collectEntries(List<SuggestionEntry> entries, Set<String> seen,
                                List<SuggestionEntry> merged) {
        for (SuggestionEntry entry : entries) {
            if (seen.add(entry.getText())) {
                merged.add(entry);
            }
        }
    }

    /**
     * 检测输入类型。
     */
    static InputType detectInputType(String input) {
        boolean hasChinese = false;
        boolean hasLatin = false;

        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (ch >= '\u4E00' && ch <= '\u9FA5') {
                hasChinese = true;
            } else if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
                hasLatin = true;
            }
        }

        if (hasChinese && hasLatin) {
            return InputType.MIXED;
        } else if (hasChinese) {
            return InputType.CHINESE;
        } else if (hasLatin) {
            return InputType.LATIN;
        }
        // 数字或其他字符，按 LATIN 处理
        return InputType.LATIN;
    }

    enum InputType {
        CHINESE,
        LATIN,
        MIXED
    }
}
