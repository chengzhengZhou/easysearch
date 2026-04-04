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

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * BKTree 数据结构单元测试。
 */
public class BKTreeTest {

    @Test
    public void build_emptyTree() {
        BKTree<String> tree = new BKTree<>(String::length);
        tree.build(Collections.emptyList());
        assertEquals(0, tree.size());
        assertTrue(tree.isEmpty());
    }

    @Test
    public void build_singleItem() {
        BKTree<String> tree = new BKTree<>(String::length);
        tree.build(Collections.singletonList("hello"));
        assertEquals(1, tree.size());
        assertFalse(tree.isEmpty());
    }

    @Test
    public void build_multipleItems() {
        BKTree<String> tree = new BKTree<>(String::length);
        tree.build(Arrays.asList("abc", "abcd", "abcde", "abcdef", "ab"));
        assertEquals(5, tree.size());
    }

    @Test
    public void search_exactMatch() {
        BKTree<String> tree = new BKTree<>((a, b) -> {
            int[][] dp = new int[a.length() + 1][b.length() + 1];
            for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
            for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
            for (int i = 1; i <= a.length(); i++)
                for (int j = 1; j <= b.length(); j++)
                    dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                            dp[i - 1][j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1));
            return dp[a.length()][b.length()];
        });

        tree.build(Arrays.asList("apple", "apply", "ape", "apex", "app"));
        List<String> results = tree.search("apple", 0);
        assertTrue(results.contains("apple"));
        assertEquals(1, results.size());
    }

    @Test
    public void search_editDistance1() {
        BKTree<String> tree = new BKTree<>(EditDistanceCandidateGenerator::levenshteinDistance);
        tree.build(Arrays.asList("apple", "apply", "ape", "apex", "app", "banana"));

        List<String> results = tree.search("apple", 1);
        assertTrue(results.contains("apple"));
        assertTrue(results.contains("apply"));
        assertTrue(results.contains("app"));
        // "ape" 与 "apple" 的编辑距离为 2，不应出现在结果中
        assertFalse(results.contains("ape"));
        // "banana" 差距更远
        assertFalse(results.contains("banana"));
    }

    @Test
    public void search_editDistance2() {
        BKTree<String> tree = new BKTree<>(EditDistanceCandidateGenerator::levenshteinDistance);
        tree.build(Arrays.asList("apple", "apply", "ape", "apex", "app", "banana"));

        List<String> results = tree.search("apple", 2);
        assertTrue(results.contains("apple"));
        assertTrue(results.contains("apply"));
        assertTrue(results.contains("app"));
        assertTrue(results.contains("ape"));
        assertFalse(results.contains("banana"));
    }

    @Test
    public void search_emptyTree() {
        BKTree<String> tree = new BKTree<>(EditDistanceCandidateGenerator::levenshteinDistance);
        List<String> results = tree.search("hello", 2);
        assertTrue(results.isEmpty());
    }

    @Test
    public void search_noMatch() {
        BKTree<String> tree = new BKTree<>(EditDistanceCandidateGenerator::levenshteinDistance);
        tree.build(Arrays.asList("apple", "banana", "cherry"));
        List<String> results = tree.search("xyz", 1);
        assertTrue(results.isEmpty());
    }

    @Test
    public void search_largeDataset() {
        // 验证大数据量下的搜索正确性
        List<String> words = Arrays.asList(
                "手机", "手机壳", "手机膜", "手机套", "手机支架",
                "华为", "华为手机", "华为平板", "华为耳机",
                "苹果", "苹果手机", "苹果笔记本", "苹果手表"
        );
        BKTree<String> tree = new BKTree<>(EditDistanceCandidateGenerator::levenshteinDistance);
        tree.build(words);

        List<String> results = tree.search("手机壳", 1);
        assertTrue(results.contains("手机壳"));
    }

    @Test
    public void dedup_sameItem() {
        // 重复插入相同项应只保留一个
        BKTree<String> tree = new BKTree<>(EditDistanceCandidateGenerator::levenshteinDistance);
        tree.build(Arrays.asList("apple", "apple", "apple"));
        assertEquals(1, tree.size());
    }
}
