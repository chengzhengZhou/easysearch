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
 * PinyinRecallChannel 单元测试。
 */
public class PinyinRecallChannelTest {

    private PinyinRecallChannel channel;

    @Before
    public void setUp() throws IOException {
        SuggestionEngine engine = new SuggestionEngine();
        engine.load("suggestion/test_suggestion_dict.txt");
        channel = new PinyinRecallChannel(engine);
    }

    @Test
    public void name_returnsPinyin() {
        assertEquals("pinyin", channel.name());
    }

    @Test
    public void recall_latinInput_pinyinMatch() {
        // "pingguo" 应该匹配 "苹果" 开头的词
        List<RecallResult> results = channel.recall("pingguo", 10);
        assertFalse(results.isEmpty());
    }

    @Test
    public void recall_latinInput_initialsMatch() {
        // "pgsj" 是 "苹果手机" 的首字母
        List<RecallResult> results = channel.recall("pgsj", 10);
        assertFalse(results.isEmpty());
    }

    @Test
    public void recall_chineseInput_convertsToPinyin() {
        // 汉字输入"苹果"会被转为拼音搜索
        List<RecallResult> results = channel.recall("苹果", 10);
        assertFalse(results.isEmpty());
    }

    @Test
    public void recall_noDuplicates() {
        List<RecallResult> results = channel.recall("pg", 20);
        long distinctCount = results.stream()
                .map(r -> r.getEntry().getText())
                .distinct()
                .count();
        assertEquals(results.size(), distinctCount);
    }

    @Test
    public void recall_respectsLimit() {
        List<RecallResult> results = channel.recall("pingguo", 2);
        assertTrue(results.size() <= 2);
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
    public void detectInputType_chinese() {
        assertEquals(PinyinRecallChannel.InputType.CHINESE,
                PinyinRecallChannel.detectInputType("苹果手机"));
    }

    @Test
    public void detectInputType_latin() {
        assertEquals(PinyinRecallChannel.InputType.LATIN,
                PinyinRecallChannel.detectInputType("pingguo"));
    }

    @Test
    public void detectInputType_mixed() {
        assertEquals(PinyinRecallChannel.InputType.MIXED,
                PinyinRecallChannel.detectInputType("苹果abc"));
    }
}
