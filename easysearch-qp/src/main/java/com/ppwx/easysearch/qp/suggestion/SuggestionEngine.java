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

import com.hankcs.hanlp.collection.trie.bintrie.BinTrie;
import com.ppwx.easysearch.qp.source.AbstractReloadableEngine;
import com.ppwx.easysearch.qp.util.PinyinUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 联想词引擎：负责词表加载和三套 Trie 索引构建。
 * <p>
 * 继承 {@link AbstractReloadableEngine}，支持通过 {@code TextLineSource} 热加载。
 * 在 {@link #doLoad(InputStream)} 中解析 TSV 词表并构建三棵 BinTrie：
 * <ul>
 *   <li>汉字前缀 Trie（原始文本 → SuggestionEntry）</li>
 *   <li>全拼前缀 Trie（拼音无调 → SuggestionEntry 列表）</li>
 *   <li>首字母前缀 Trie（拼音首字母 → SuggestionEntry 列表）</li>
 * </ul>
 * 词表格式（TSV）：{@code query文本 \t 权重(可选) \t 类目标签(可选)}
 */
public class SuggestionEngine extends AbstractReloadableEngine {

    public static final String ENGINE_NAME = "suggestion";

    /** 汉字前缀索引：text → SuggestionEntry */
    private volatile BinTrie<SuggestionEntry> prefixTrie;

    /** 全拼前缀索引：pinyin → List<SuggestionEntry> */
    private volatile BinTrie<List<SuggestionEntry>> pinyinTrie;

    /** 首字母前缀索引：initials → List<SuggestionEntry> */
    private volatile BinTrie<List<SuggestionEntry>> initialsTrie;

    /** 所有词条（保留用于前缀搜索遍历） */
    private volatile List<SuggestionEntry> allEntries;

    public SuggestionEngine() {
        super(ENGINE_NAME);
    }

    @Override
    protected void doLoad(InputStream is) throws IOException {
        List<SuggestionEntry> entries = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                SuggestionEntry entry = parseLine(line);
                if (entry != null) {
                    entries.add(entry);
                }
            }
        }

        buildIndex(entries);
    }

    @Override
    protected boolean checkLoaded() {
        return prefixTrie != null;
    }

    /**
     * 直接从词条列表构建索引（用于测试或程序化加载）。
     *
     * @param entries 词条列表
     */
    public void loadEntries(List<SuggestionEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            log.warn("Empty suggestion dictionary provided");
            return;
        }
        buildIndex(entries);
    }

    private void buildIndex(List<SuggestionEntry> entries) {
        // 1. 构建汉字前缀 Trie
        BinTrie<SuggestionEntry> newPrefixTrie = new BinTrie<>();
        for (SuggestionEntry entry : entries) {
            newPrefixTrie.put(entry.getText(), entry);
        }

        // 2. 构建全拼 Trie 和首字母 Trie
        // 由于拼音可能有多个 entry 映射到同一个拼音，使用 Map 先聚合再批量写入 Trie
        Map<String, List<SuggestionEntry>> pinyinMap = new HashMap<>();
        Map<String, List<SuggestionEntry>> initialsMap = new HashMap<>();

        for (SuggestionEntry entry : entries) {
            String text = entry.getText();

            // 全拼索引
            String pinyin = PinyinUtil.getPinyin(text, "");
            if (pinyin != null && !pinyin.isEmpty()) {
                String pinyinKey = pinyin.toLowerCase();
                pinyinMap.computeIfAbsent(pinyinKey, k -> new ArrayList<>()).add(entry);
            }

            // 首字母索引
            String initials = PinyinUtil.getPinyinInitials(text);
            if (initials != null && !initials.isEmpty()) {
                String initialsKey = initials.toLowerCase();
                initialsMap.computeIfAbsent(initialsKey, k -> new ArrayList<>()).add(entry);
            }
        }

        BinTrie<List<SuggestionEntry>> newPinyinTrie = new BinTrie<>();
        for (Map.Entry<String, List<SuggestionEntry>> e : pinyinMap.entrySet()) {
            newPinyinTrie.put(e.getKey(), e.getValue());
        }

        BinTrie<List<SuggestionEntry>> newInitialsTrie = new BinTrie<>();
        for (Map.Entry<String, List<SuggestionEntry>> e : initialsMap.entrySet()) {
            newInitialsTrie.put(e.getKey(), e.getValue());
        }

        // 按 weight 降序排序的副本
        List<SuggestionEntry> sortedEntries = new ArrayList<>(entries);
        sortedEntries.sort((a, b) -> Long.compare(b.getWeight(), a.getWeight()));

        // 原子替换所有引用
        this.prefixTrie = newPrefixTrie;
        this.pinyinTrie = newPinyinTrie;
        this.initialsTrie = newInitialsTrie;
        this.allEntries = Collections.unmodifiableList(sortedEntries);

        log.info("Suggestion dictionary loaded, entries: {}", entries.size());
    }

    /**
     * 汉字前缀搜索：返回以 prefix 开头的候选词条，按 weight 降序排列。
     *
     * @param prefix 汉字前缀
     * @param limit  最大返回条数
     * @return 匹配的词条列表
     */
    public List<SuggestionEntry> prefixSearch(String prefix, int limit) {
        if (prefix == null || prefix.isEmpty()) {
            return Collections.emptyList();
        }
        List<SuggestionEntry> snapshot = this.allEntries;
        if (snapshot == null) {
            return Collections.emptyList();
        }

        // 用 BinTrie 做精确命中检查 + 遍历所有条目过滤前缀
        // 对于联想词场景（通常 < 10 万条），线性过滤 + 堆排序性能可接受
        PriorityQueue<SuggestionEntry> heap = new PriorityQueue<>(
                Math.min(limit + 1, 100),
                Comparator.comparingLong(SuggestionEntry::getWeight)
        );

        for (SuggestionEntry entry : snapshot) {
            if (entry.getText().startsWith(prefix)) {
                heap.offer(entry);
                if (heap.size() > limit) {
                    heap.poll();
                }
            }
        }

        List<SuggestionEntry> result = new ArrayList<>(heap.size());
        while (!heap.isEmpty()) {
            result.add(heap.poll());
        }
        // 反转为降序
        Collections.reverse(result);
        return result;
    }

    /**
     * 全拼前缀搜索：返回拼音以 pinyinPrefix 开头的候选词条，按 weight 降序排列。
     *
     * @param pinyinPrefix 拼音前缀（小写）
     * @param limit        最大返回条数
     * @return 匹配的词条列表
     */
    public List<SuggestionEntry> pinyinSearch(String pinyinPrefix, int limit) {
        if (pinyinPrefix == null || pinyinPrefix.isEmpty()) {
            return Collections.emptyList();
        }
        BinTrie<List<SuggestionEntry>> trie = this.pinyinTrie;
        if (trie == null) {
            return Collections.emptyList();
        }

        return searchByPinyinTrie(trie, pinyinPrefix.toLowerCase(), limit);
    }

    /**
     * 首字母前缀搜索：返回拼音首字母以 initialsPrefix 开头的候选词条。
     *
     * @param initialsPrefix 首字母前缀（小写）
     * @param limit          最大返回条数
     * @return 匹配的词条列表
     */
    public List<SuggestionEntry> initialSearch(String initialsPrefix, int limit) {
        if (initialsPrefix == null || initialsPrefix.isEmpty()) {
            return Collections.emptyList();
        }
        BinTrie<List<SuggestionEntry>> trie = this.initialsTrie;
        if (trie == null) {
            return Collections.emptyList();
        }

        return searchByPinyinTrie(trie, initialsPrefix.toLowerCase(), limit);
    }

    /**
     * 在拼音/首字母 Trie 中做前缀搜索。
     * <p>
     * 由于 BinTrie 的 entrySet() 返回所有条目，遍历过滤以 prefix 开头的 key，
     * 收集对应的 entry 列表，用堆排序保留 Top-N。
     */
    private List<SuggestionEntry> searchByPinyinTrie(
            BinTrie<List<SuggestionEntry>> trie, String prefix, int limit) {

        PriorityQueue<SuggestionEntry> heap = new PriorityQueue<>(
                Math.min(limit + 1, 100),
                Comparator.comparingLong(SuggestionEntry::getWeight)
        );

        Set<String> seen = new HashSet<>();

        for (Map.Entry<String, List<SuggestionEntry>> e : trie.entrySet()) {
            if (e.getKey().startsWith(prefix)) {
                for (SuggestionEntry entry : e.getValue()) {
                    if (seen.add(entry.getText())) {
                        heap.offer(entry);
                        if (heap.size() > limit) {
                            heap.poll();
                        }
                    }
                }
            }
        }

        List<SuggestionEntry> result = new ArrayList<>(heap.size());
        while (!heap.isEmpty()) {
            result.add(heap.poll());
        }
        Collections.reverse(result);
        return result;
    }

    /**
     * 获取所有词条（按 weight 降序）。
     */
    public List<SuggestionEntry> getAllEntries() {
        List<SuggestionEntry> snapshot = this.allEntries;
        return snapshot != null ? snapshot : Collections.<SuggestionEntry>emptyList();
    }

    private static SuggestionEntry parseLine(String line) {
        String[] parts = line.split("\t", -1);
        if (parts.length < 1) {
            return null;
        }
        String text = parts[0].trim();
        if (text.isEmpty()) {
            return null;
        }

        long weight = 1L;
        if (parts.length >= 2) {
            try {
                weight = Long.parseLong(parts[1].trim());
            } catch (NumberFormatException ignore) {
                // 使用默认权重
            }
        }

        String category = null;
        if (parts.length >= 3) {
            String cat = parts[2].trim();
            if (!cat.isEmpty()) {
                category = cat;
            }
        }

        return new SuggestionEntry(text, weight, category);
    }
}
