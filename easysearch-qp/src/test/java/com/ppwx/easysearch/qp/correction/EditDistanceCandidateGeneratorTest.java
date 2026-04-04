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
import java.util.List;

import static org.junit.Assert.*;

/**
 * EditDistanceCandidateGenerator 单元测试。
 */
public class EditDistanceCandidateGeneratorTest {

    @Test
    public void levenshteinDistance_sameString() {
        assertEquals(0, EditDistanceCandidateGenerator.levenshteinDistance("abc", "abc"));
    }

    @Test
    public void levenshteinDistance_emptyStrings() {
        assertEquals(0, EditDistanceCandidateGenerator.levenshteinDistance("", ""));
    }

    @Test
    public void levenshteinDistance_oneEmpty() {
        assertEquals(3, EditDistanceCandidateGenerator.levenshteinDistance("", "abc"));
        assertEquals(3, EditDistanceCandidateGenerator.levenshteinDistance("abc", ""));
    }

    @Test
    public void levenshteinDistance_insertion() {
        assertEquals(1, EditDistanceCandidateGenerator.levenshteinDistance("abc", "abcd"));
    }

    @Test
    public void levenshteinDistance_deletion() {
        assertEquals(1, EditDistanceCandidateGenerator.levenshteinDistance("abcd", "abc"));
    }

    @Test
    public void levenshteinDistance_substitution() {
        assertEquals(1, EditDistanceCandidateGenerator.levenshteinDistance("abc", "axc"));
    }

    @Test
    public void levenshteinDistance_complex() {
        assertEquals(3, EditDistanceCandidateGenerator.levenshteinDistance("kitten", "sitting"));
    }

    @Test
    public void levenshteinDistance_chinese() {
        // 中文字符每个算一个编辑距离
        assertEquals(1, EditDistanceCandidateGenerator.levenshteinDistance("手机", "平机"));
        assertEquals(2, EditDistanceCandidateGenerator.levenshteinDistance("手机壳", "手机膜"));
    }

    @Test
    public void levenshteinDistance_nullSafe() {
        assertEquals(0, EditDistanceCandidateGenerator.levenshteinDistance(null, null));
        assertEquals(3, EditDistanceCandidateGenerator.levenshteinDistance(null, "abc"));
        assertEquals(3, EditDistanceCandidateGenerator.levenshteinDistance("abc", null));
    }

    @Test
    public void generate_noIndex() {
        EditDistanceCandidateGenerator gen = new EditDistanceCandidateGenerator(10, 2);
        List<DictEntry> results = gen.generate("手机壳");
        assertTrue(results.isEmpty());
    }

    @Test
    public void generate_editDistance1() {
        List<DictEntry> entries = Arrays.asList(
                new DictEntry("手机壳", null, 1000),
                new DictEntry("手机膜", null, 800),
                new DictEntry("充电宝", null, 500)
        );

        EditDistanceCandidateGenerator gen = new EditDistanceCandidateGenerator(10, 1);
        gen.buildIndex(entries);

        List<DictEntry> results = gen.generate("手机壳");
        // "手机膜" 与 "手机壳" 编辑距离为 1，应被召回
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(e -> "手机膜".equals(e.getWord())));
        // "充电宝" 编辑距离太远，不应被召回
        assertFalse(results.stream().anyMatch(e -> "充电宝".equals(e.getWord())));
        // 输入本身不应出现在结果中
        assertFalse(results.stream().anyMatch(e -> "手机壳".equals(e.getWord())));
    }

    @Test
    public void generate_maxCandidates() {
        List<DictEntry> entries = Arrays.asList(
                new DictEntry("abc", null, 1),
                new DictEntry("abd", null, 2),
                new DictEntry("abe", null, 3),
                new DictEntry("abf", null, 4),
                new DictEntry("abg", null, 5)
        );

        EditDistanceCandidateGenerator gen = new EditDistanceCandidateGenerator(2, 2);
        gen.buildIndex(entries);

        List<DictEntry> results = gen.generate("abc");
        assertTrue(results.size() <= 2);
    }

    @Test
    public void generate_emptyTerm() {
        List<DictEntry> entries = Arrays.asList(
                new DictEntry("手机壳", null, 1000)
        );
        EditDistanceCandidateGenerator gen = new EditDistanceCandidateGenerator(10, 2);
        gen.buildIndex(entries);
        assertTrue(gen.generate("").isEmpty());
        assertTrue(gen.generate(null).isEmpty());
    }

    @Test
    public void buildIndex_nullEntries() {
        EditDistanceCandidateGenerator gen = new EditDistanceCandidateGenerator(10, 2);
        gen.buildIndex(null);
        assertTrue(gen.generate("手机").isEmpty());
    }

    @Test
    public void generate_sortedByFrequency() {
        List<DictEntry> entries = Arrays.asList(
                new DictEntry("手机膜", null, 100),  // 低频
                new DictEntry("手机套", null, 1000)   // 高频
        );

        EditDistanceCandidateGenerator gen = new EditDistanceCandidateGenerator(10, 1);
        gen.buildIndex(entries);

        List<DictEntry> results = gen.generate("手机壳");
        if (results.size() >= 2) {
            // 应按词频降序
            assertTrue(results.get(0).getFrequency() >= results.get(1).getFrequency());
        }
    }
}
