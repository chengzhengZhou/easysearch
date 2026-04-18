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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * SuggestionEngine 单元测试。
 */
public class SuggestionEngineTest {

    private SuggestionEngine engine;

    @Before
    public void setUp() throws IOException {
        engine = new SuggestionEngine();
        engine.load("suggestion/test_suggestion_dict.txt");
    }

    @Test
    public void isLoaded_trueAfterLoad() {
        assertTrue(engine.isLoaded());
    }

    @Test
    public void isLoaded_falseBeforeLoad() {
        SuggestionEngine empty = new SuggestionEngine();
        assertFalse(empty.isLoaded());
    }

    @Test
    public void prefixSearch_returnsMatchingEntries() {
        List<SuggestionEntry> results = engine.prefixSearch("苹果", 10);
        assertFalse(results.isEmpty());
        for (SuggestionEntry entry : results) {
            assertTrue(entry.getText().startsWith("苹果"));
        }
    }

    @Test
    public void prefixSearch_orderedByWeightDesc() {
        List<SuggestionEntry> results = engine.prefixSearch("苹果", 10);
        for (int i = 1; i < results.size(); i++) {
            assertTrue(results.get(i - 1).getWeight() >= results.get(i).getWeight());
        }
    }

    @Test
    public void prefixSearch_respectsLimit() {
        List<SuggestionEntry> results = engine.prefixSearch("苹果", 2);
        assertTrue(results.size() <= 2);
    }

    @Test
    public void prefixSearch_emptyForNull() {
        assertTrue(engine.prefixSearch(null, 10).isEmpty());
    }

    @Test
    public void prefixSearch_emptyForEmpty() {
        assertTrue(engine.prefixSearch("", 10).isEmpty());
    }

    @Test
    public void prefixSearch_emptyForNoMatch() {
        assertTrue(engine.prefixSearch("不存在的词", 10).isEmpty());
    }

    @Test
    public void pinyinSearch_returnsResults() {
        // "苹果" 的拼音是 "pingguo"
        List<SuggestionEntry> results = engine.pinyinSearch("pingguo", 10);
        assertFalse(results.isEmpty());
    }

    @Test
    public void initialSearch_returnsResults() {
        // "苹果手机" 首字母 "pgsj"
        List<SuggestionEntry> results = engine.initialSearch("pgsj", 10);
        assertFalse(results.isEmpty());
    }

    @Test
    public void loadEntries_programmatic() {
        SuggestionEngine eng = new SuggestionEngine();
        eng.loadEntries(Arrays.asList(
                new SuggestionEntry("测试词A", 100),
                new SuggestionEntry("测试词B", 200)
        ));
        assertTrue(eng.isLoaded());

        List<SuggestionEntry> results = eng.prefixSearch("测试", 10);
        assertEquals(2, results.size());
        assertEquals("测试词B", results.get(0).getText()); // weight 更高排前面
    }

    @Test
    public void getAllEntries_orderedByWeightDesc() {
        List<SuggestionEntry> entries = engine.getAllEntries();
        assertFalse(entries.isEmpty());
        for (int i = 1; i < entries.size(); i++) {
            assertTrue(entries.get(i - 1).getWeight() >= entries.get(i).getWeight());
        }
    }

    @Test
    public void hotReload_replacesData() throws IOException {
        SuggestionEngine eng = new SuggestionEngine();
        eng.loadEntries(Arrays.asList(
                new SuggestionEntry("旧数据", 100)
        ));
        assertEquals(1, eng.prefixSearch("旧", 10).size());

        eng.loadEntries(Arrays.asList(
                new SuggestionEntry("新数据A", 200),
                new SuggestionEntry("新数据B", 300)
        ));
        assertTrue(eng.prefixSearch("旧", 10).isEmpty());
        assertEquals(2, eng.prefixSearch("新", 10).size());
    }
}
