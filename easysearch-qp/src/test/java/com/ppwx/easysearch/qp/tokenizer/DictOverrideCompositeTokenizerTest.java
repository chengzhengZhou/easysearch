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

package com.ppwx.easysearch.qp.tokenizer;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * DictOverrideCompositeTokenizer（词典优先覆盖 + CRF 填缝）单元测试。
 */
public class DictOverrideCompositeTokenizerTest {

    private static final String SOURCE_ATTR = "source";

    private DictTokenizer dictTokenizer;
    private DictOverrideCompositeTokenizer compositeWithDict;

    @Before
    public void setUp() {
        String dict = "苹果\tnz\n手机\tn\n苹果手机\tnz\n影石\tnz\n";
        dictTokenizer = DictTokenizer.fromStream(new ByteArrayInputStream(dict.getBytes(StandardCharsets.UTF_8)));
        compositeWithDict = new DictOverrideCompositeTokenizer(new CRFTokenizer(), dictTokenizer);
    }

    @Test
    public void tokenizeBlankReturnsEmpty() {
        DictOverrideCompositeTokenizer tokenizer = new DictOverrideCompositeTokenizer();
        assertTrue(tokenizer.tokenize("").isEmpty());
        assertTrue(tokenizer.tokenize("   ").isEmpty());
        assertTrue(tokenizer.tokenize(null).isEmpty());
    }

    @Test
    public void whenCrfEmptyUsesDictOnlyWithSource() {
        Tokenizer crfEmpty = text -> Collections.emptyList();
        DictOverrideCompositeTokenizer tokenizer = new DictOverrideCompositeTokenizer(crfEmpty, dictTokenizer);
        List<Token> tokens = tokenizer.tokenize("苹果手机");
        assertFalse(tokens.isEmpty());
        for (Token t : tokens) {
            assertEquals("dict", t.getAttribute(SOURCE_ATTR));
        }
    }

    @Test
    public void whenDictEmptyUsesCrfOnlyWithSource() {
        Tokenizer crf = new CRFTokenizer();
        Tokenizer dictEmpty = text -> Collections.emptyList();
        DictOverrideCompositeTokenizer tokenizer = new DictOverrideCompositeTokenizer(crf, dictEmpty);
        List<Token> tokens = tokenizer.tokenize("测试");
        if (tokens.isEmpty()) {
            return;
        }
        for (Token t : tokens) {
            assertEquals("crf", t.getAttribute(SOURCE_ATTR));
        }
    }

    @Test
    public void dictOverridesCrfMultiCharSpan_forceMerge() {
        List<Token> crfBase = Arrays.asList(
                token("iPhone", 0, 6),
                token("15", 6, 8)
        );
        List<Token> dictTokens = Arrays.asList(
                token("iPhone15", 0, 8)
        );
        Tokenizer mockCrf = text -> crfBase;
        Tokenizer mockDict = text -> dictTokens;
        DictOverrideCompositeTokenizer tokenizer = new DictOverrideCompositeTokenizer(mockCrf, mockDict);
        List<Token> result = tokenizer.tokenize("iPhone15");
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("iPhone15", result.get(0).getText());
        assertEquals(0, result.get(0).getStartIndex());
        assertEquals(8, result.get(0).getEndIndex());
        assertEquals("dict", result.get(0).getAttribute(SOURCE_ATTR));
    }

    @Test
    public void dictOverridesCrfMultiCharSpan_forceSplit() {
        List<Token> crfBase = Arrays.asList(
                token("苹果手机", 0, 4)
        );
        List<Token> dictTokens = Arrays.asList(
                token("苹果", 0, 2),
                token("手机", 2, 4)
        );
        Tokenizer mockCrf = text -> crfBase;
        Tokenizer mockDict = text -> dictTokens;
        DictOverrideCompositeTokenizer tokenizer = new DictOverrideCompositeTokenizer(mockCrf, mockDict);
        List<Token> result = tokenizer.tokenize("苹果手机");
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("苹果", result.get(0).getText());
        assertEquals(0, result.get(0).getStartIndex());
        assertEquals(2, result.get(0).getEndIndex());
        assertEquals("dict", result.get(0).getAttribute(SOURCE_ATTR));
        assertEquals("手机", result.get(1).getText());
        assertEquals(2, result.get(1).getStartIndex());
        assertEquals(4, result.get(1).getEndIndex());
        assertEquals("dict", result.get(1).getAttribute(SOURCE_ATTR));
    }

    @Test
    public void crfFillsGapsBetweenDictSpans() {
        List<Token> crfBase = Arrays.asList(
                token("前", 0, 1),
                token("苹果", 1, 3),
                token("后", 3, 4)
        );
        List<Token> dictTokens = Arrays.asList(
                token("苹果", 1, 3)
        );
        Tokenizer mockCrf = text -> crfBase;
        Tokenizer mockDict = text -> dictTokens;
        DictOverrideCompositeTokenizer tokenizer = new DictOverrideCompositeTokenizer(mockCrf, mockDict);
        List<Token> result = tokenizer.tokenize("前苹果后");
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("前", result.get(0).getText());
        assertEquals(0, result.get(0).getStartIndex());
        assertEquals(1, result.get(0).getEndIndex());
        assertEquals("crf", result.get(0).getAttribute(SOURCE_ATTR));
        assertEquals("苹果", result.get(1).getText());
        assertEquals(1, result.get(1).getStartIndex());
        assertEquals(3, result.get(1).getEndIndex());
        assertEquals("dict", result.get(1).getAttribute(SOURCE_ATTR));
        assertEquals("后", result.get(2).getText());
        assertEquals(3, result.get(2).getStartIndex());
        assertEquals(4, result.get(2).getEndIndex());
        assertEquals("crf", result.get(2).getAttribute(SOURCE_ATTR));
    }

    @Test
    public void normalizeDictLongestAtStartAndNonOverlapping() {
        List<Token> crfBase = Arrays.asList(
                token("苹", 0, 1),
                token("果", 1, 2),
                token("公", 2, 3),
                token("司", 3, 4)
        );
        List<Token> dictTokens = Arrays.asList(
                token("苹果", 0, 2),
                token("苹果公司", 0, 4),
                token("公司", 2, 4)
        );
        Tokenizer mockCrf = text -> crfBase;
        Tokenizer mockDict = text -> dictTokens;
        DictOverrideCompositeTokenizer tokenizer = new DictOverrideCompositeTokenizer(mockCrf, mockDict);
        List<Token> result = tokenizer.tokenize("苹果公司");
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("苹果公司", result.get(0).getText());
        assertEquals(0, result.get(0).getStartIndex());
        assertEquals(4, result.get(0).getEndIndex());
        assertEquals("dict", result.get(0).getAttribute(SOURCE_ATTR));
    }

    @Test
    public void resultCoversFullTextNoOverlapSorted() {
        List<Token> crfBase = Arrays.asList(
                token("前", 0, 1),
                token("苹果", 1, 3),
                token("后", 3, 4)
        );
        List<Token> dictTokens = Arrays.asList(
                token("苹果", 1, 3)
        );
        Tokenizer mockCrf = text -> crfBase;
        Tokenizer mockDict = text -> dictTokens;
        DictOverrideCompositeTokenizer tokenizer = new DictOverrideCompositeTokenizer(mockCrf, mockDict);
        String text = "前苹果后";
        List<Token> result = tokenizer.tokenize(text);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        int lastEnd = 0;
        for (Token t : result) {
            assertTrue("startIndex should be >= lastEnd", t.getStartIndex() >= lastEnd);
            lastEnd = t.getEndIndex();
            assertNotNull(t.getAttribute(SOURCE_ATTR));
            assertTrue("crf".equals(t.getAttribute(SOURCE_ATTR)) || "dict".equals(t.getAttribute(SOURCE_ATTR)));
        }
        assertEquals("Result should cover full text", text.length(), lastEnd);
    }

    @Test
    public void integrationWithRealTokenizers() {
        List<Token> tokens = compositeWithDict.tokenize("影石手机");
        assertNotNull(tokens);
        assertFalse(tokens.isEmpty());
        int lastEnd = 0;
        for (Token t : tokens) {
            assertTrue(t.getStartIndex() >= lastEnd);
            lastEnd = t.getEndIndex();
            assertNotNull(t.getAttribute(SOURCE_ATTR));
            assertTrue("crf".equals(t.getAttribute(SOURCE_ATTR)) || "dict".equals(t.getAttribute(SOURCE_ATTR)));
        }
        assertEquals(4, lastEnd);
    }

    private static Token token(String text, int start, int end) {
        return Token.builder()
                .text(text)
                .type("n")
                .startIndex(start)
                .endIndex(end)
                .confidence(1.0)
                .build();
    }
}
