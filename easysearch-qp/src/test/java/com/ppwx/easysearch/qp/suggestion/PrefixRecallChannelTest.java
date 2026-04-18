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
import java.util.List;

import static org.junit.Assert.*;

/**
 * PrefixRecallChannel 单元测试。
 */
public class PrefixRecallChannelTest {

    private PrefixRecallChannel channel;

    @Before
    public void setUp() throws IOException {
        SuggestionEngine engine = new SuggestionEngine();
        engine.load("suggestion/test_suggestion_dict.txt");
        channel = new PrefixRecallChannel(engine);
    }

    @Test
    public void name_returnsPrefix() {
        assertEquals("prefix", channel.name());
    }

    @Test
    public void recall_returnsMatchingResults() {
        List<RecallResult> results = channel.recall("苹果", 10);
        assertFalse(results.isEmpty());
        for (RecallResult r : results) {
            assertTrue(r.getEntry().getText().startsWith("苹果"));
        }
    }

    @Test
    public void recall_rankStartsFromOne() {
        List<RecallResult> results = channel.recall("苹果", 10);
        assertFalse(results.isEmpty());
        assertEquals(1, results.get(0).getRank());
        for (int i = 0; i < results.size(); i++) {
            assertEquals(i + 1, results.get(i).getRank());
        }
    }

    @Test
    public void recall_respectsLimit() {
        List<RecallResult> results = channel.recall("苹果", 2);
        assertTrue(results.size() <= 2);
    }

    @Test
    public void recall_orderedByWeightDesc() {
        List<RecallResult> results = channel.recall("苹果", 10);
        for (int i = 1; i < results.size(); i++) {
            assertTrue(results.get(i - 1).getEntry().getWeight() >= results.get(i).getEntry().getWeight());
        }
    }

    @Test
    public void recall_emptyForNull() {
        assertTrue(channel.recall(null, 10).isEmpty());
    }

    @Test
    public void recall_emptyForEmpty() {
        assertTrue(channel.recall("", 10).isEmpty());
    }

    @Test
    public void recall_emptyForNoMatch() {
        assertTrue(channel.recall("不存在的前缀", 10).isEmpty());
    }

    @Test
    public void recall_emptyWhenEngineNotLoaded() {
        SuggestionEngine emptyEngine = new SuggestionEngine();
        PrefixRecallChannel emptyChannel = new PrefixRecallChannel(emptyEngine);
        assertTrue(emptyChannel.recall("苹果", 10).isEmpty());
    }
}
