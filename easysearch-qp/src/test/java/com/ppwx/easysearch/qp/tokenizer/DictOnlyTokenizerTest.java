package com.ppwx.easysearch.qp.tokenizer;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * DictOnlyTokenizer 单元测试：仅输出词典中的词，未命中则丢弃。
 */
public class DictOnlyTokenizerTest {

    private DictOnlyTokenizer tokenizer;

    @Before
    public void setUp() {
        String dict = "苹果\tnz\n手机\tn\n苹果手机\tnz\n";
        tokenizer = DictOnlyTokenizer.fromStream(new ByteArrayInputStream(dict.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void tokenizeNullOrEmptyReturnsEmpty() {
        assertTrue(tokenizer.tokenize(null).isEmpty());
        assertTrue(tokenizer.tokenize("").isEmpty());
    }

    @Test
    public void onlyDictWordsOutput_longestMatch() {
        List<Token> tokens = tokenizer.tokenize("苹果手机");
        assertEquals(1, tokens.size());
        assertEquals("苹果手机", tokens.get(0).getText());
        assertEquals(0, tokens.get(0).getStartIndex());
        assertEquals(4, tokens.get(0).getEndIndex());
    }

    @Test
    public void nonDictCharsDiscarded() {
        List<Token> tokens = tokenizer.tokenize("苹果好手机");
        assertEquals(2, tokens.size());
        assertEquals("苹果", tokens.get(0).getText());
        assertEquals(0, tokens.get(0).getStartIndex());
        assertEquals(2, tokens.get(0).getEndIndex());
        assertEquals("手机", tokens.get(1).getText());
        assertEquals(3, tokens.get(1).getStartIndex());
        assertEquals(5, tokens.get(1).getEndIndex());
    }

    @Test
    public void noDictMatchReturnsEmpty() {
        List<Token> tokens = tokenizer.tokenize("你好世界");
        assertTrue(tokens.isEmpty());
    }

    @Test
    public void emptyDictReturnsEmpty() {
        DictOnlyTokenizer empty = new DictOnlyTokenizer();
        assertTrue(empty.tokenize("苹果").isEmpty());
    }
}
