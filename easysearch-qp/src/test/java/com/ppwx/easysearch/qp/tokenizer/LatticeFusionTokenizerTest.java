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
 * LatticeFusionTokenizer（词图 + 模型打分融合）单元测试。
 */
public class LatticeFusionTokenizerTest {

    private static final String SOURCE_ATTR = "source";

    private DictTokenizer dictTokenizer;
    private LatticeFusionTokenizer fusionWithDict;

    @Before
    public void setUp() {
        String dict = "苹果\tnz\n手机\tn\n苹果手机\tnz\n影石\tnz\n";
        dictTokenizer = DictTokenizer.fromStream(new ByteArrayInputStream(dict.getBytes(StandardCharsets.UTF_8)));
        fusionWithDict = new LatticeFusionTokenizer(new CRFTokenizer(), dictTokenizer);
    }

    @Test
    public void tokenizeBlankReturnsEmpty() {
        LatticeFusionTokenizer tokenizer = new LatticeFusionTokenizer();
        assertTrue(tokenizer.tokenize("").isEmpty());
        assertTrue(tokenizer.tokenize("   ").isEmpty());
        assertTrue(tokenizer.tokenize(null).isEmpty());
    }

    @Test
    public void whenCrfEmptyUsesDictOnlyWithSource() {
        Tokenizer crfEmpty = text -> Collections.emptyList();
        LatticeFusionTokenizer tokenizer = new LatticeFusionTokenizer(crfEmpty, dictTokenizer);
        // 使用「影石手机」：词典切为 [影石][手机]，路径得分 2 > 4*0.3，故取 dict
        List<Token> tokens = tokenizer.tokenize("影石手机");
        assertFalse(tokens.isEmpty());
        for (Token t : tokens) {
            assertEquals("dict", t.getAttribute(SOURCE_ATTR));
        }
    }

    @Test
    public void whenDictEmptyUsesCrfOnlyWithSource() {
        Tokenizer crf = new CRFTokenizer();
        Tokenizer dictEmpty = text -> Collections.emptyList();
        LatticeFusionTokenizer tokenizer = new LatticeFusionTokenizer(crf, dictEmpty);
        List<Token> tokens = tokenizer.tokenize("测试");
        if (tokens.isEmpty()) {
            return;
        }
        for (Token t : tokens) {
            assertEquals("crf", t.getAttribute(SOURCE_ATTR));
        }
    }

    @Test
    public void whenBothEmptyUsesSingleCharWithSourceSingle() {
        Tokenizer crfEmpty = text -> Collections.emptyList();
        Tokenizer dictEmpty = text -> Collections.emptyList();
        LatticeFusionTokenizer tokenizer = new LatticeFusionTokenizer(crfEmpty, dictEmpty);
        List<Token> tokens = tokenizer.tokenize("苹果");
        assertNotNull(tokens);
        assertEquals(2, tokens.size());
        assertEquals("苹", tokens.get(0).getText());
        assertEquals("果", tokens.get(1).getText());
        assertEquals("single", tokens.get(0).getAttribute(SOURCE_ATTR));
        assertEquals("single", tokens.get(1).getAttribute(SOURCE_ATTR));
    }

    @Test
    public void bestPathPicksHigherScoreWhenSameSpan() {
        List<Token> crfTokens = Arrays.asList(
                token("苹果", 0, 2, 0.5),
                token("手机", 2, 4, 0.5)
        );
        List<Token> dictTokens = Arrays.asList(
                token("苹果", 0, 2),  // confidence 1.0
                token("手机", 2, 4)
        );
        Tokenizer mockCrf = text -> crfTokens;
        Tokenizer mockDict = text -> dictTokens;
        LatticeFusionTokenizer tokenizer = new LatticeFusionTokenizer(mockCrf, mockDict);
        List<Token> result = tokenizer.tokenize("苹果手机");
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("苹果", result.get(0).getText());
        assertEquals("手机", result.get(1).getText());
        assertEquals("dict", result.get(0).getAttribute(SOURCE_ATTR));
        assertEquals("dict", result.get(1).getAttribute(SOURCE_ATTR));
    }

    @Test
    public void bestPathPrefersHigherScoreSpan() {
        List<Token> crfTokens = Arrays.asList(
                token("苹果手机", 0, 4, 0.8)
        );
        List<Token> dictTokens = Arrays.asList(
                token("苹果", 0, 2, 1.0),
                token("手机", 2, 4, 1.0)
        );
        Tokenizer mockCrf = text -> crfTokens;
        Tokenizer mockDict = text -> dictTokens;
        LatticeFusionTokenizer tokenizer = new LatticeFusionTokenizer(mockCrf, mockDict);
        List<Token> result = tokenizer.tokenize("苹果手机");
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("苹果", result.get(0).getText());
        assertEquals("手机", result.get(1).getText());
        assertEquals("dict", result.get(0).getAttribute(SOURCE_ATTR));
        assertEquals("dict", result.get(1).getAttribute(SOURCE_ATTR));
    }

    @Test
    public void bestPathPrefersLongerWordWhenScoreHigher() {
        List<Token> crfTokens = Arrays.asList(
                token("苹", 0, 1, 0.3),
                token("果", 1, 2, 0.3),
                token("手", 2, 3, 0.3),
                token("机", 3, 4, 0.3)
        );
        // 成词分数需高于 4*0.3=1.2，最优路径才选一条成词
        List<Token> dictTokens = Arrays.asList(
                token("苹果手机", 0, 4, 1.5)
        );
        Tokenizer mockCrf = text -> crfTokens;
        Tokenizer mockDict = text -> dictTokens;
        LatticeFusionTokenizer tokenizer = new LatticeFusionTokenizer(mockCrf, mockDict);
        List<Token> result = tokenizer.tokenize("苹果手机");
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("苹果手机", result.get(0).getText());
        assertEquals(0, result.get(0).getStartIndex());
        assertEquals(4, result.get(0).getEndIndex());
        assertEquals("dict", result.get(0).getAttribute(SOURCE_ATTR));
    }

    @Test
    public void resultCoversFullTextNoOverlapSorted() {
        List<Token> crfTokens = Arrays.asList(
                token("前", 0, 1, 0.8),
                token("苹果", 1, 3, 0.8),
                token("后", 3, 4, 0.8)
        );
        List<Token> dictTokens = Arrays.asList(
                token("苹果", 1, 3, 1.0)
        );
        Tokenizer mockCrf = text -> crfTokens;
        Tokenizer mockDict = text -> dictTokens;
        LatticeFusionTokenizer tokenizer = new LatticeFusionTokenizer(mockCrf, mockDict);
        String text = "前苹果后";
        List<Token> result = tokenizer.tokenize(text);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        int lastEnd = 0;
        for (Token t : result) {
            assertTrue(t.getStartIndex() >= lastEnd);
            lastEnd = t.getEndIndex();
            assertNotNull(t.getAttribute(SOURCE_ATTR));
        }
        assertEquals(text.length(), lastEnd);
    }

    @Test
    public void integrationWithRealTokenizers() {
        List<Token> tokens = fusionWithDict.tokenize("影石手机");
        assertNotNull(tokens);
        assertFalse(tokens.isEmpty());
        int lastEnd = 0;
        for (Token t : tokens) {
            assertTrue(t.getStartIndex() >= lastEnd);
            lastEnd = t.getEndIndex();
            assertNotNull(t.getAttribute(SOURCE_ATTR));
        }
        assertEquals(4, lastEnd);
    }

    private static Token token(String text, int start, int end) {
        return token(text, start, end, 1.0);
    }

    private static Token token(String text, int start, int end, double confidence) {
        return Token.builder()
                .text(text)
                .type("n")
                .startIndex(start)
                .endIndex(end)
                .confidence(confidence)
                .build();
    }
}
