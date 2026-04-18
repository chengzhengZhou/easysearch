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

import org.junit.Before;
import org.junit.Test;

import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.*;

/**
 * PrefixTrie 单元测试。
 */
public class PrefixTrieTest {

    private PrefixTrie<SuggestionEntry> trie;

    /** 按 weight 升序比较（小顶堆使用，堆顶为最小值） */
    private static final Comparator<SuggestionEntry> WEIGHT_ASC =
            Comparator.comparingLong(SuggestionEntry::getWeight);

    @Before
    public void setUp() {
        trie = new PrefixTrie<>();
        trie.put("苹果", new SuggestionEntry("苹果", 500));
        trie.put("苹果手机", new SuggestionEntry("苹果手机", 1000));
        trie.put("苹果电脑", new SuggestionEntry("苹果电脑", 800));
        trie.put("苹果耳机", new SuggestionEntry("苹果耳机", 300));
        trie.put("华为手机", new SuggestionEntry("华为手机", 900));
        trie.put("华为平板", new SuggestionEntry("华为平板", 600));
        trie.put("小米", new SuggestionEntry("小米", 400));
    }

    // ========== put / get 基础操作 ==========

    @Test
    public void get_existingKey_returnsValue() {
        SuggestionEntry entry = trie.get("苹果手机");
        assertNotNull(entry);
        assertEquals("苹果手机", entry.getText());
        assertEquals(1000, entry.getWeight());
    }

    @Test
    public void get_nonExistingKey_returnsNull() {
        assertNull(trie.get("不存在"));
    }

    @Test
    public void get_partialKey_returnsNull() {
        // "苹" 不是一个完整的 key
        assertNull(trie.get("苹"));
    }

    @Test
    public void put_overwriteExistingKey() {
        trie.put("苹果", new SuggestionEntry("苹果", 999));
        assertEquals(999, trie.get("苹果").getWeight());
        assertEquals(7, trie.size()); // 总数不变
    }

    @Test
    public void put_nullKey_ignored() {
        trie.put(null, new SuggestionEntry("test", 1));
        assertEquals(7, trie.size());
    }

    @Test
    public void put_emptyKey_ignored() {
        trie.put("", new SuggestionEntry("test", 1));
        assertEquals(7, trie.size());
    }

    @Test
    public void put_nullValue_ignored() {
        trie.put("test", null);
        assertEquals(7, trie.size());
    }

    // ========== size ==========

    @Test
    public void size_correctCount() {
        assertEquals(7, trie.size());
    }

    @Test
    public void size_emptyTrie() {
        PrefixTrie<String> empty = new PrefixTrie<>();
        assertEquals(0, empty.size());
    }

    // ========== prefixSearch ==========

    @Test
    public void prefixSearch_returnsMatchingEntries() {
        List<SuggestionEntry> results = trie.prefixSearch("苹果", 10, WEIGHT_ASC);
        assertEquals(4, results.size());
        for (SuggestionEntry entry : results) {
            assertTrue(entry.getText().startsWith("苹果"));
        }
    }

    @Test
    public void prefixSearch_orderedByWeightDesc() {
        List<SuggestionEntry> results = trie.prefixSearch("苹果", 10, WEIGHT_ASC);
        for (int i = 1; i < results.size(); i++) {
            assertTrue(results.get(i - 1).getWeight() >= results.get(i).getWeight());
        }
    }

    @Test
    public void prefixSearch_respectsLimit() {
        List<SuggestionEntry> results = trie.prefixSearch("苹果", 2, WEIGHT_ASC);
        assertEquals(2, results.size());
        // 应返回 weight 最高的 2 条：苹果手机(1000) 和 苹果电脑(800)
        assertEquals("苹果手机", results.get(0).getText());
        assertEquals("苹果电脑", results.get(1).getText());
    }

    @Test
    public void prefixSearch_singleCharPrefix() {
        List<SuggestionEntry> results = trie.prefixSearch("华", 10, WEIGHT_ASC);
        assertEquals(2, results.size());
        for (SuggestionEntry entry : results) {
            assertTrue(entry.getText().startsWith("华"));
        }
    }

    @Test
    public void prefixSearch_exactMatch() {
        List<SuggestionEntry> results = trie.prefixSearch("小米", 10, WEIGHT_ASC);
        assertEquals(1, results.size());
        assertEquals("小米", results.get(0).getText());
    }

    @Test
    public void prefixSearch_noMatch() {
        List<SuggestionEntry> results = trie.prefixSearch("三星", 10, WEIGHT_ASC);
        assertTrue(results.isEmpty());
    }

    @Test
    public void prefixSearch_nullPrefix() {
        assertTrue(trie.prefixSearch(null, 10, WEIGHT_ASC).isEmpty());
    }

    @Test
    public void prefixSearch_emptyPrefix() {
        assertTrue(trie.prefixSearch("", 10, WEIGHT_ASC).isEmpty());
    }

    @Test
    public void prefixSearch_zeroLimit() {
        assertTrue(trie.prefixSearch("苹果", 0, WEIGHT_ASC).isEmpty());
    }

    @Test
    public void prefixSearch_emptyTrie() {
        PrefixTrie<SuggestionEntry> empty = new PrefixTrie<>();
        assertTrue(empty.prefixSearch("苹果", 10, WEIGHT_ASC).isEmpty());
    }

    // ========== collectAllValues ==========

    @Test
    public void collectAllValues_returnsAll() {
        List<SuggestionEntry> all = trie.collectAllValues();
        assertEquals(7, all.size());
    }

    @Test
    public void collectAllValues_emptyTrie() {
        PrefixTrie<String> empty = new PrefixTrie<>();
        assertTrue(empty.collectAllValues().isEmpty());
    }

    // ========== 拼音 Trie 场景（V = List） ==========

    @Test
    public void prefixSearch_withListValues() {
        // 模拟拼音 Trie：一个拼音 key 对应多个 entry
        PrefixTrie<List<SuggestionEntry>> pinyinTrie = new PrefixTrie<>();

        List<SuggestionEntry> pgEntries = java.util.Arrays.asList(
                new SuggestionEntry("苹果", 500),
                new SuggestionEntry("苹果手机", 1000)
        );
        pinyinTrie.put("pingguo", pgEntries);

        List<SuggestionEntry> pgsjEntries = java.util.Arrays.asList(
                new SuggestionEntry("苹果手机", 1000)
        );
        pinyinTrie.put("pingguoshouji", pgsjEntries);

        // 前缀搜索 "pingguo" 应匹配 "pingguo" 和 "pingguoshouji"
        // 这里 comparator 不太适用于 List 类型，主要验证结构正确性
        List<List<SuggestionEntry>> results = pinyinTrie.prefixSearch(
                "pingguo", 10, (a, b) -> 0);
        assertEquals(2, results.size());
    }
}
