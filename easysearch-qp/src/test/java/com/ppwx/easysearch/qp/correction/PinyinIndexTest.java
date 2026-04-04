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
 * PinyinIndex 拼音倒排索引单元测试。
 */
public class PinyinIndexTest {

    @Test
    public void build_emptyEntries() {
        PinyinIndex index = new PinyinIndex();
        index.build(Arrays.asList());
        assertEquals(0, index.exactSize());
        assertEquals(0, index.initialSize());
    }

    @Test
    public void build_exactIndex() {
        List<DictEntry> entries = Arrays.asList(
                new DictEntry("手机壳", null, 100),
                new DictEntry("华为", null, 200)
        );
        PinyinIndex index = new PinyinIndex();
        index.build(entries);

        // "手机壳" 的拼音是 "shoujike"（无分隔符）
        List<DictEntry> results = index.exactSearch("shoujike");
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(e -> "手机壳".equals(e.getWord())));
    }

    @Test
    public void build_initialIndex() {
        List<DictEntry> entries = Arrays.asList(
                new DictEntry("手机壳", null, 100),
                new DictEntry("手机膜", null, 200)
        );
        PinyinIndex index = new PinyinIndex();
        index.build(entries);

        // "手机壳" 首字母是 "sjk"
        List<DictEntry> results = index.initialSearch("sjk");
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(e -> "手机壳".equals(e.getWord())));
    }

    @Test
    public void exactSearch_noMatch() {
        List<DictEntry> entries = Arrays.asList(
                new DictEntry("手机壳", null, 100)
        );
        PinyinIndex index = new PinyinIndex();
        index.build(entries);

        List<DictEntry> results = index.exactSearch("zzzzzzzzz");
        assertTrue(results.isEmpty());
    }

    @Test
    public void exactSearch_caseInsensitive() {
        List<DictEntry> entries = Arrays.asList(
                new DictEntry("华为", null, 200)
        );
        PinyinIndex index = new PinyinIndex();
        index.build(entries);

        // 大小写不敏感
        String pinyin = index.exactSearch("huawei").isEmpty()
                ? "" : index.exactSearch("huawei").get(0).getPinyin();
        if (pinyin != null && !pinyin.isEmpty()) {
            List<DictEntry> upper = index.exactSearch(pinyin.toUpperCase());
            assertFalse(upper.isEmpty());
        }
    }

    @Test
    public void exactSearch_null() {
        PinyinIndex index = new PinyinIndex();
        index.build(Arrays.asList(new DictEntry("测试", null, 1)));
        assertTrue(index.exactSearch(null).isEmpty());
        assertTrue(index.exactSearch("").isEmpty());
    }

    @Test
    public void initialSearch_null() {
        PinyinIndex index = new PinyinIndex();
        index.build(Arrays.asList(new DictEntry("测试", null, 1)));
        assertTrue(index.initialSearch(null).isEmpty());
        assertTrue(index.initialSearch("").isEmpty());
    }

    @Test
    public void containsWord_existing() {
        List<DictEntry> entries = Arrays.asList(
                new DictEntry("手机壳", null, 100)
        );
        PinyinIndex index = new PinyinIndex();
        index.build(entries);
        assertTrue(index.containsWord("手机壳"));
    }

    @Test
    public void containsWord_notExisting() {
        List<DictEntry> entries = Arrays.asList(
                new DictEntry("手机壳", null, 100)
        );
        PinyinIndex index = new PinyinIndex();
        index.build(entries);
        assertFalse(index.containsWord("充电宝"));
    }

    @Test
    public void build_withPinyinPreSet() {
        // 词条已预设有拼音
        List<DictEntry> entries = Arrays.asList(
                new DictEntry("手机壳", "shoujike", 100)
        );
        PinyinIndex index = new PinyinIndex();
        index.build(entries);

        List<DictEntry> results = index.exactSearch("shoujike");
        assertFalse(results.isEmpty());
        assertEquals("shoujike", results.get(0).getPinyin());
    }
}
