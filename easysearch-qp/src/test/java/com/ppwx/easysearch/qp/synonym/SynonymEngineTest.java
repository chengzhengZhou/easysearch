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

package com.ppwx.easysearch.qp.synonym;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.*;

/**
 * SynonymEngine 单元测试。
 */
public class SynonymEngineTest {

    private SynonymEngine engine;

    @Before
    public void setUp() throws IOException {
        engine = new SynonymEngine();
        engine.load("synonym/synonym.txt");
    }

    @Test
    public void match_emptyWhenQueryIsNull() {
        assertTrue(engine.match(null).isEmpty());
    }

    @Test
    public void match_emptyWhenQueryIsEmpty() {
        assertTrue(engine.match("").isEmpty());
    }

    @Test
    public void match_emptyWhenNotLoaded() throws IOException {
        SynonymEngine empty = new SynonymEngine();
        assertTrue(empty.match("苹果").isEmpty());
    }

    @Test
    public void match_returnsMatchForUnidirectional() {
        List<SynonymMatch> matches = engine.match("苹果 15");
        assertEquals(1, matches.size());
        assertEquals(0, matches.get(0).getStartIndex());
        assertEquals(4, matches.get(0).getEndIndex());
        assertEquals("苹果 15", matches.get(0).getSource());
        assertEquals("iphone 15", matches.get(0).getAttribute().getFirstTarget());
        assertEquals(SynonymType.UNIDIRECTIONAL, matches.get(0).getAttribute().getType());
    }

    @Test
    public void match_returnsMatchForBidirectional() {
        List<SynonymMatch> matches = engine.match("手环");
        assertEquals(1, matches.size());
        assertEquals("手表", matches.get(0).getAttribute().getFirstTarget());

        List<SynonymMatch> reverse = engine.match("手表");
        assertEquals(1, reverse.size());
        assertEquals("手环", reverse.get(0).getAttribute().getFirstTarget());
    }

    @Test
    public void match_multipleMatchesLongestFirst() {
        List<SynonymMatch> matches = engine.match("我要苹果 16和手环");
        assertTrue(matches.size() >= 2);
        assertEquals("苹果 16", matches.get(0).getSource());
        assertEquals("手环", matches.get(1).getSource());
    }

    @Test
    public void match_emptyWhenNoMatch() {
        assertTrue(engine.match("没有同义词").isEmpty());
    }

    @Test
    public void load_fromInputStream() {
        String txt = "测试词\t=>\t替换词\n";
        engine.load(new ByteArrayInputStream(txt.getBytes(StandardCharsets.UTF_8)));
        List<SynonymMatch> matches = engine.match("测试词");
        assertEquals(1, matches.size());
        assertEquals("替换词", matches.get(0).getAttribute().getFirstTarget());
    }

    @Test
    public void load_ignoresCommentAndEmptyLine() {
        String txt = "# comment\n\n手环\tSYM\t手表\n";
        engine.load(new ByteArrayInputStream(txt.getBytes(StandardCharsets.UTF_8)));
        List<SynonymMatch> matches = engine.match("手环");
        assertEquals(1, matches.size());
    }

    @Test
    public void isLoaded_trueAfterLoad() throws IOException {
        SynonymEngine e = new SynonymEngine();
        assertFalse(e.isLoaded());
        e.load("synonym/synonym.txt");
        assertTrue(e.isLoaded());
    }
}
