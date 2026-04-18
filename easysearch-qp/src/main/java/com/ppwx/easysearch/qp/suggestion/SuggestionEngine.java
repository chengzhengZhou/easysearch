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
 * 在 {@link #doLoad(InputStream)} 中解析 TSV 词表并构建三棵 {@link PrefixTrie}：
 * <ul>
 *   <li>汉字前缀 Trie（原始文本 → SuggestionEntry）</li>
 *   <li>全拼前缀 Trie（拼音无调 → SuggestionEntry 列表）</li>
 *   <li>首字母前缀 Trie（拼音首字母 → SuggestionEntry 列表）</li>
 * </ul>
 * 词表格式（TSV）：{@code query文本 \t 权重(可选) \t 类目标签(可选)}
 * <p>
 * 使用自定义 {@link PrefixTrie} 替代 HanLP BinTrie，原生支持前缀子树 DFS 遍历，
 * 前缀搜索复杂度从 O(N) 降低到 O(prefix.length + 匹配数)。
 */
public class SuggestionEngine extends AbstractReloadableEngine {

    public static final String ENGINE_NAME = "suggestion";

    /** 按 weight 升序比较器（小顶堆使用，堆顶为最小值） */
    private static final Comparator<SuggestionEntry> WEIGHT_ASC =
            Comparator.comparingLong(SuggestionEntry::getWeight);

    /** 汉字前缀索引：text → SuggestionEntry */
    private volatile PrefixTrie<SuggestionEntry> prefixTrie;

    /** 全拼前缀索引：pinyin → List<SuggestionEntry> */
    private volatile PrefixTrie<List<SuggestionEntry>> pinyinTrie;

    /** 首字母前缀索引：initials → List<SuggestionEntry> */
    private volatile PrefixTrie<List<SuggestionEntry>> initialsTrie;

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
        PrefixTrie<SuggestionEntry> newPrefixTrie = new PrefixTrie<>();
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

        PrefixTrie<List<SuggestionEntry>> newPinyinTrie = new PrefixTrie<>();
        for (Map.Entry<String, List<SuggestionEntry>> e : pinyinMap.entrySet()) {
            newPinyinTrie.put(e.getKey(), e.getValue());
        }

        PrefixTrie<List<SuggestionEntry>> newInitialsTrie = new PrefixTrie<>();
        for (Map.Entry<String, List<SuggestionEntry>> e : initialsMap.entrySet()) {
            newInitialsTrie.put(e.getKey(), e.getValue());
        }

        // 3. 构建 maxWeight 索引，启用剪枝优化（方案一 + 方案二）
        newPrefixTrie.buildMaxWeight(SuggestionEntry::getWeight);
        // 拼音 Trie 的值是 List<SuggestionEntry>，取列表中最大权重
        newPinyinTrie.buildMaxWeight(list -> list.stream()
                .mapToLong(SuggestionEntry::getWeight)
                .max()
                .orElse(Long.MIN_VALUE));
        newInitialsTrie.buildMaxWeight(list -> list.stream()
                .mapToLong(SuggestionEntry::getWeight)
                .max()
                .orElse(Long.MIN_VALUE));

        // 原子替换所有引用
        this.prefixTrie = newPrefixTrie;
        this.pinyinTrie = newPinyinTrie;
        this.initialsTrie = newInitialsTrie;

        log.info("Suggestion dictionary loaded, entries: {}", entries.size());
    }

    /**
     * 汉字前缀搜索：返回以 prefix 开头的候选词条，按 weight 降序排列。
     * <p>
     * 使用 {@link PrefixTrie#prefixSearch} 通过 DFS 遍历前缀子树 + 小顶堆保留 Top-N，
     * 复杂度 O(prefix.length + M·log(limit))，M 为匹配数。
     *
     * @param prefix 汉字前缀
     * @param limit  最大返回条数
     * @return 匹配的词条列表（按 weight 降序）
     */
    public List<SuggestionEntry> prefixSearch(String prefix, int limit) {
        if (prefix == null || prefix.isEmpty()) {
            return Collections.emptyList();
        }
        PrefixTrie<SuggestionEntry> trie = this.prefixTrie;
        if (trie == null) {
            return Collections.emptyList();
        }
        return trie.prefixSearch(prefix, limit, WEIGHT_ASC);
    }

    /**
     * 全拼前缀搜索：返回拼音以 pinyinPrefix 开头的候选词条，按 weight 降序排列。
     * <p>
     * 使用 {@link PrefixTrie#prefixSearch} 通过 DFS 遍历前缀子树获取匹配的拼音 key，
     * 再展开 List 并用小顶堆保留 Top-N。
     *
     * @param pinyinPrefix 拼音前缀（小写）
     * @param limit        最大返回条数
     * @return 匹配的词条列表
     */
    public List<SuggestionEntry> pinyinSearch(String pinyinPrefix, int limit) {
        if (pinyinPrefix == null || pinyinPrefix.isEmpty()) {
            return Collections.emptyList();
        }
        PrefixTrie<List<SuggestionEntry>> trie = this.pinyinTrie;
        if (trie == null) {
            return Collections.emptyList();
        }

        return searchByPinyinTrie(trie, pinyinPrefix.toLowerCase(), limit);
    }

    /**
     * 首字母前缀搜索：返回拼音首字母以 initialsPrefix 开头的候选词条。
     * <p>
     * 使用 {@link PrefixTrie#prefixSearch} 通过 DFS 遍历前缀子树获取匹配的首字母 key，
     * 再展开 List 并用小顶堆保留 Top-N。
     *
     * @param initialsPrefix 首字母前缀（小写）
     * @param limit          最大返回条数
     * @return 匹配的词条列表
     */
    public List<SuggestionEntry> initialSearch(String initialsPrefix, int limit) {
        if (initialsPrefix == null || initialsPrefix.isEmpty()) {
            return Collections.emptyList();
        }
        PrefixTrie<List<SuggestionEntry>> trie = this.initialsTrie;
        if (trie == null) {
            return Collections.emptyList();
        }

        return searchByPinyinTrie(trie, initialsPrefix.toLowerCase(), limit);
    }

    /**
     * 在拼音/首字母 Trie 中做前缀搜索（方案三优化版）。
     * <p>
     * 使用 {@link PrefixTrie#prefixVisitWithPrune} 直接在 DFS 遍历中处理，
     * 避免创建中间 List，并支持通过 maxWeight 剪枝。
     * 复杂度从 O(prefix.length + M·log(limit)) 降低到 O(prefix.length + K·log(limit))，K ≪ M。
     */
    private List<SuggestionEntry> searchByPinyinTrie(
            PrefixTrie<List<SuggestionEntry>> trie, String prefix, int limit) {

        // 使用小顶堆保留 Top-N
        PriorityQueue<SuggestionEntry> heap = new PriorityQueue<>(
                Math.min(limit + 1, 100), WEIGHT_ASC);
        Set<String> seen = new HashSet<>();

        // 方案三：使用 prefixVisitWithPrune 直接遍历，避免中间 List 并支持剪枝
        PrefixTrie.PruneContext context = new PrefixTrie.PruneContext() {
            @Override
            public long getThreshold() {
                if (heap.isEmpty()) {
                    return Long.MIN_VALUE;
                }
                return heap.peek().getWeight();
            }

            @Override
            public boolean isFull() {
                return heap.size() >= limit;
            }
        };

        trie.prefixVisitWithPrune(prefix, entryList -> {
            for (SuggestionEntry entry : entryList) {
                // 单条 entry 级别的剪枝：堆已满且当前 entry 权重 <= 堆顶时跳过
                if (heap.size() >= limit && entry.getWeight() <= heap.peek().getWeight()) {
                    continue;
                }
                if (seen.add(entry.getText())) {
                    heap.offer(entry);
                    if (heap.size() > limit) {
                        heap.poll();
                    }
                }
            }
        }, context);

        List<SuggestionEntry> result = new ArrayList<>(heap.size());
        while (!heap.isEmpty()) {
            result.add(heap.poll());
        }
        Collections.reverse(result);
        return result;
    }

    /**
     * 获取所有词条（按 weight 降序）。
     * <p>
     * 从 prefixTrie 收集全部值后排序返回。该方法非热路径，仅用于调试或统计。
     */
    public List<SuggestionEntry> getAllEntries() {
        PrefixTrie<SuggestionEntry> trie = this.prefixTrie;
        if (trie == null) {
            return Collections.emptyList();
        }
        List<SuggestionEntry> all = trie.collectAllValues();
        all.sort((a, b) -> Long.compare(b.getWeight(), a.getWeight()));
        return Collections.unmodifiableList(all);
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
