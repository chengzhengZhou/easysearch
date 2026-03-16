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
 * SpaceSegmentTokenizer 单元测试。
 * 验证按空格分片、逐段分词及位置映射逻辑。
 */
public class SpaceSegmentTokenizerTest {

    private Tokenizer compositeWithDict;

    @Before
    public void setUp() {
        String dict = "苹果\tnz\n手机\tn\n苹果手机\tnz\n真我\tnz\nGT5\tnz\nPro\tnz\n";
        DictTokenizer dictTokenizer = DictTokenizer.fromStream(new ByteArrayInputStream(dict.getBytes(StandardCharsets.UTF_8)));
        compositeWithDict = new CRFCompositeTokenizer(new CRFTokenizer(), dictTokenizer);
    }

    @Test
    public void tokenizeBlankReturnsEmpty() {
        SpaceSegmentTokenizer tokenizer = new SpaceSegmentTokenizer(compositeWithDict);
        assertTrue(tokenizer.tokenize("").isEmpty());
        assertTrue(tokenizer.tokenize("   ").isEmpty());
        assertTrue(tokenizer.tokenize(null).isEmpty());
    }

    @Test
    public void tokenizeNoSpaceSameAsDelegate() {
        Tokenizer delegate = compositeWithDict;
        SpaceSegmentTokenizer tokenizer = new SpaceSegmentTokenizer(delegate);
        String text = "苹果手机";
        List<Token> expected = delegate.tokenize(text);
        List<Token> actual = tokenizer.tokenize(text);
        assertNotNull(actual);
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i).getText(), actual.get(i).getText());
            assertEquals(expected.get(i).getStartIndex(), actual.get(i).getStartIndex());
            assertEquals(expected.get(i).getEndIndex(), actual.get(i).getEndIndex());
        }
    }

    @Test
    public void tokenizeWithSpacesMapsIndicesCorrectly() {
        Tokenizer mockDelegate = text -> Collections.singletonList(
                Token.builder().text(text).type("n").startIndex(0).endIndex(text.length()).confidence(1.0).build()
        );
        SpaceSegmentTokenizer tokenizer = new SpaceSegmentTokenizer(mockDelegate);

        List<Token> tokens = tokenizer.tokenize("真我 GT5 Pro");
        assertNotNull(tokens);
        assertEquals(3, tokens.size());

        assertEquals("真我", tokens.get(0).getText());
        assertEquals(0, tokens.get(0).getStartIndex());
        assertEquals(2, tokens.get(0).getEndIndex());

        assertEquals("GT5", tokens.get(1).getText());
        assertEquals(3, tokens.get(1).getStartIndex());  // 空格后
        assertEquals(6, tokens.get(1).getEndIndex());

        assertEquals("Pro", tokens.get(2).getText());
        assertEquals(7, tokens.get(2).getStartIndex());
        assertEquals(10, tokens.get(2).getEndIndex());
    }

    @Test
    public void tokenizeWithMultipleSpaces() {
        Tokenizer mockDelegate = text -> Collections.singletonList(
                Token.builder().text(text).type("n").startIndex(0).endIndex(text.length()).confidence(1.0).build()
        );
        SpaceSegmentTokenizer tokenizer = new SpaceSegmentTokenizer(mockDelegate);

        List<Token> tokens = tokenizer.tokenize("A  B   C");
        assertNotNull(tokens);
        assertEquals(3, tokens.size());
        assertEquals("A", tokens.get(0).getText());
        assertEquals(0, tokens.get(0).getStartIndex());
        assertEquals(1, tokens.get(0).getEndIndex());
        assertEquals("B", tokens.get(1).getText());
        assertEquals(3, tokens.get(1).getStartIndex());
        assertEquals(4, tokens.get(1).getEndIndex());
        assertEquals("C", tokens.get(2).getText());
        assertEquals(7, tokens.get(2).getStartIndex());
        assertEquals(8, tokens.get(2).getEndIndex());
    }

    @Test
    public void tokenizeWithDelegateReturningMultipleTokens() {
        Tokenizer mockDelegate = text -> {
            if ("苹果手机".equals(text)) {
                return Arrays.asList(
                        Token.builder().text("苹果").type("n").startIndex(0).endIndex(2).confidence(1.0).build(),
                        Token.builder().text("手机").type("n").startIndex(2).endIndex(4).confidence(1.0).build()
                );
            }
            return Collections.singletonList(
                    Token.builder().text(text).type("n").startIndex(0).endIndex(text.length()).confidence(1.0).build()
            );
        };
        SpaceSegmentTokenizer tokenizer = new SpaceSegmentTokenizer(mockDelegate);

        List<Token> tokens = tokenizer.tokenize("苹果手机 256GB");
        assertNotNull(tokens);
        assertEquals(3, tokens.size());
        assertEquals("苹果", tokens.get(0).getText());
        assertEquals(0, tokens.get(0).getStartIndex());
        assertEquals(2, tokens.get(0).getEndIndex());
        assertEquals("手机", tokens.get(1).getText());
        assertEquals(2, tokens.get(1).getStartIndex());
        assertEquals(4, tokens.get(1).getEndIndex());
        assertEquals("256GB", tokens.get(2).getText());
        assertEquals(5, tokens.get(2).getStartIndex());
        assertEquals(10, tokens.get(2).getEndIndex());
    }

    @Test
    public void preservesDelegateAttributes() {
        Tokenizer mockDelegate = text -> Collections.singletonList(
                Token.builder()
                        .text(text)
                        .type("nz")
                        .startIndex(0)
                        .endIndex(text.length())
                        .confidence(0.9)
                        .addAttribute("source", "dict")
                        .build()
        );
        SpaceSegmentTokenizer tokenizer = new SpaceSegmentTokenizer(mockDelegate);
        List<Token> tokens = tokenizer.tokenize("华为 手机");
        assertNotNull(tokens);
        assertEquals(2, tokens.size());
        assertEquals("dict", tokens.get(0).getAttribute("source"));
        assertEquals("dict", tokens.get(1).getAttribute("source"));
        assertEquals(0.9, tokens.get(0).getConfidence(), 1e-9);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNullDelegate() {
        new SpaceSegmentTokenizer(null);
    }

    @Test
    public void integrationWithCompositeTokenizer() {
        SpaceSegmentTokenizer tokenizer = new SpaceSegmentTokenizer(compositeWithDict);
        List<Token> tokens = tokenizer.tokenize("真我 GT5 Pro");
        assertNotNull(tokens);
        assertFalse(tokens.isEmpty());
        int lastEnd = 0;
        for (Token t : tokens) {
            assertTrue(t.getStartIndex() >= lastEnd);
            lastEnd = t.getEndIndex();
            assertNotNull(t.getText());
        }
        assertEquals(10, lastEnd);  // "真我 GT5 Pro" 长度 10（含空格）
    }
}
