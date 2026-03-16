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
 * CRFCompositeTokenizer（CRF 基座 + Dict 补充）单元测试。
 */
public class CompositeTokenizerTest {

    private static final String SOURCE_ATTR = "source";

    private DictTokenizer dictTokenizer;
    private CRFCompositeTokenizer compositeWithDict;

    @Before
    public void setUp() {
        String dict = "苹果\tnz\n手机\tn\n苹果手机\tnz\n影石\tnz\n";
        dictTokenizer = DictTokenizer.fromStream(new ByteArrayInputStream(dict.getBytes(StandardCharsets.UTF_8)));
        compositeWithDict = new CRFCompositeTokenizer(new CRFTokenizer(), dictTokenizer);
    }

    @Test
    public void tokenizeBlankReturnsEmpty() {
        CRFCompositeTokenizer tokenizer = new CRFCompositeTokenizer();
        assertTrue(tokenizer.tokenize("").isEmpty());
        assertTrue(tokenizer.tokenize("   ").isEmpty());
        assertTrue(tokenizer.tokenize(null).isEmpty());
    }

    @Test
    public void whenCrfEmptyUsesDictOnlyWithSource() {
        Tokenizer crfEmpty = text -> Collections.emptyList();
        CRFCompositeTokenizer tokenizer = new CRFCompositeTokenizer(crfEmpty, dictTokenizer);
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
        CRFCompositeTokenizer tokenizer = new CRFCompositeTokenizer(crf, dictEmpty);
        List<Token> tokens = tokenizer.tokenize("测试");
        if (tokens.isEmpty()) {
            return;
        }
        for (Token t : tokens) {
            assertEquals("crf", t.getAttribute(SOURCE_ATTR));
        }
    }

    @Test
    public void mergeReplacesSingleCharRunWithDictWhenSpanMatches() {
        // CRF 基座：三个单字 "苹""果""手"
        List<Token> crfBase = Arrays.asList(
                token("苹", 0, 1),
                token("果", 1, 2),
                token("手", 2, 3),
                token("机", 3, 4)
        );
        // Dict 有 "苹果" [0,2) 和 "手机" [2,4)
        List<Token> dictTokens = Arrays.asList(
                token("苹果", 0, 2),
                token("手机", 2, 4)
        );
        Tokenizer mockCrf = text -> crfBase;
        Tokenizer mockDict = text -> dictTokens;
        CRFCompositeTokenizer tokenizer = new CRFCompositeTokenizer(mockCrf, mockDict);
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
    public void mergeKeepsCrfMultiCharAndReplacesSingleCharRuns() {
        // CRF: "华为" [0,2] + "手" [2,3] + "机" [3,4]
        List<Token> crfBase = Arrays.asList(
                token("华为", 0, 2),
                token("手", 2, 3),
                token("机", 3, 4)
        );
        List<Token> dictTokens = Arrays.asList(
                token("华为", 0, 2),
                token("手机", 2, 4)
        );
        Tokenizer mockCrf = text -> crfBase;
        Tokenizer mockDict = text -> dictTokens;
        CRFCompositeTokenizer tokenizer = new CRFCompositeTokenizer(mockCrf, mockDict);
        List<Token> result = tokenizer.tokenize("华为手机");
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("华为", result.get(0).getText());
        assertEquals("crf", result.get(0).getAttribute(SOURCE_ATTR));
        assertEquals("手机", result.get(1).getText());
        assertEquals("dict", result.get(1).getAttribute(SOURCE_ATTR));
    }

    @Test
    public void integrationWithRealTokenizers() {
        List<Token> tokens = compositeWithDict.tokenize("影石手机");
        System.out.println(tokens);
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
