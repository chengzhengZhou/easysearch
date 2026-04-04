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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于编辑距离的候选生成器。
 * <p>
 * 利用 BK-Tree 在词典中查找与输入片段编辑距离 ≤ maxDistance 的候选词。
 */
public class EditDistanceCandidateGenerator {

    private static final Logger log = LoggerFactory.getLogger(EditDistanceCandidateGenerator.class);

    /** BK-Tree 编辑距离索引 */
    private volatile BKTree<DictEntry> bkTree;

    private final int maxCandidates;
    private final int maxEditDistance;

    public EditDistanceCandidateGenerator(int maxCandidates, int maxEditDistance) {
        this.maxCandidates = maxCandidates;
        this.maxEditDistance = maxEditDistance;
    }

    /**
     * 构建 BK-Tree 索引。
     *
     * @param entries 词典条目列表
     */
    public void buildIndex(List<DictEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            this.bkTree = null;
            return;
        }

        BKTree<DictEntry> tree = new BKTree<>((a, b) ->
                levenshteinDistance(a.getWord(), b.getWord()));
        tree.build(entries);
        this.bkTree = tree;

        log.info("BK-Tree 索引构建完成，词条数: {}", entries.size());
    }

    /**
     * 为疑似错误片段生成编辑距离候选列表。
     *
     * @param term 疑似错误的输入片段
     * @return 候选词条列表（按词频降序）
     */
    public List<DictEntry> generate(String term) {
        if (term == null || term.isEmpty() || bkTree == null) {
            return Collections.emptyList();
        }

        // 构造查询用的 DictEntry（词频不影响距离计算）
        DictEntry query = new DictEntry(term, null, 0);
        List<DictEntry> matches = bkTree.search(query, maxEditDistance);

        // 排除与输入完全相同的词
        matches = matches.stream()
                .filter(e -> !e.getWord().equals(term))
                .collect(Collectors.toList());

        // 按词频降序排列，取 Top-N
        matches.sort((a, b) -> Long.compare(b.getFrequency(), a.getFrequency()));

        if (matches.size() > maxCandidates) {
            matches = matches.subList(0, maxCandidates);
        }

        return matches;
    }

    /**
     * 计算两个字符串的 Levenshtein 编辑距离。
     * 使用动态规划，空间优化为 O(min(m,n))。
     *
     * @param a 字符串 a
     * @param b 字符串 b
     * @return 编辑距离
     */
    static int levenshteinDistance(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";
        if (a.equals(b)) return 0;

        int m = a.length();
        int n = b.length();

        // 确保 n <= m 以减少空间使用
        if (n > m) {
            String tmp = a;
            a = b;
            b = tmp;
            m = a.length();
            n = b.length();
        }

        int[] prev = new int[n + 1];
        int[] curr = new int[n + 1];

        for (int j = 0; j <= n; j++) {
            prev[j] = j;
        }

        for (int i = 1; i <= m; i++) {
            curr[0] = i;
            for (int j = 1; j <= n; j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                curr[j] = Math.min(
                        Math.min(curr[j - 1] + 1, prev[j] + 1),
                        prev[j - 1] + cost
                );
            }
            int[] swap = prev;
            prev = curr;
            curr = swap;
        }

        return prev[n];
    }
}
