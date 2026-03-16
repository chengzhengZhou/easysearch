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
 * DualPathTokenizer 单元测试。
 * 验证双路分词、冲突舍弃及合并逻辑。
 */
public class DualPathTokenizerTest {

    private DualPathTokenizer defaultTokenizer;

    @Before
    public void setUp() {
        String dict = "苹果\tnz\n手机\tn\n苹果手机\tnz\n真我\tnz\nGT5\tnz\nPro\tnz\niPhone\tnz\n16\tnz\n";
        DictTokenizer dictTokenizer = DictTokenizer.fromStream(new ByteArrayInputStream(dict.getBytes(StandardCharsets.UTF_8)));
        Tokenizer composite = new CRFCompositeTokenizer(new CRFTokenizer(), dictTokenizer);
        defaultTokenizer = new DualPathTokenizer(composite);
    }

    @Test
    public void tokenizeBlankReturnsEmpty() {
        DualPathTokenizer tokenizer = defaultTokenizer;
        assertTrue(tokenizer.tokenize("").isEmpty());
        assertTrue(tokenizer.tokenize("   ").isEmpty());
        assertTrue(tokenizer.tokenize(null).isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNullDelegate() {
        new DualPathTokenizer(null);
    }

    @Test
    public void noSpaceSameAsSinglePath() {
        DictTokenizer dictTokenizer = DictTokenizer.fromStream(new ByteArrayInputStream("苹果\tnz\n手机\tn\n苹果手机\tnz\n真我\tnz\nGT5\tnz\nPro\tnz\n".getBytes(StandardCharsets.UTF_8)));
        Tokenizer delegate = new CRFCompositeTokenizer(new CRFTokenizer(), dictTokenizer);
        DualPathTokenizer dual = new DualPathTokenizer(delegate);
        SpaceSegmentTokenizer spaceOnly = new SpaceSegmentTokenizer(delegate);
        String text = "苹果手机";
        List<Token> dualTokens = dual.tokenize(text);
        List<Token> spaceTokens = spaceOnly.tokenize(text);
        assertNotNull(dualTokens);
        assertNotNull(spaceTokens);
        assertEquals(spaceTokens.size(), dualTokens.size());
        for (int i = 0; i < spaceTokens.size(); i++) {
            assertEquals(spaceTokens.get(i).getText(), dualTokens.get(i).getText());
            assertEquals(spaceTokens.get(i).getStartIndex(), dualTokens.get(i).getStartIndex());
            assertEquals(spaceTokens.get(i).getEndIndex(), dualTokens.get(i).getEndIndex());
        }
    }

    @Test
    public void pathACrossingBoundaryIsDiscarded() {
        // Path A 返回跨空格的 token "真我GT5"，Path B 按空格得 ["真我","GT5"]
        Tokenizer mockDelegate = text -> {
            if (text.contains(" ")) {
                return Collections.emptyList();
            }
            if ("真我GT5".equals(text)) {
                return Collections.singletonList(
                        Token.builder().text("真我GT5").type("n").startIndex(0).endIndex(4).confidence(1.0).build()
                );
            }
            if ("真我".equals(text)) {
                return Collections.singletonList(token(text, 0, text.length()));
            }
            if ("GT5".equals(text)) {
                return Collections.singletonList(token(text, 0, text.length()));
            }
            return Collections.emptyList();
        };
        DualPathTokenizer tokenizer = new DualPathTokenizer(mockDelegate);
        List<Token> tokens = tokenizer.tokenize("真我 GT5");
        assertNotNull(tokens);
        assertEquals(2, tokens.size());
        assertEquals("真我", tokens.get(0).getText());
        assertEquals(0, tokens.get(0).getStartIndex());
        assertEquals(2, tokens.get(0).getEndIndex());
        assertEquals("GT5", tokens.get(1).getText());
        assertEquals(3, tokens.get(1).getStartIndex());
        assertEquals(6, tokens.get(1).getEndIndex());
    }

    @Test
    public void pathAFinerWithinSegmentIsUsed() {
        Tokenizer mockDelegate = text -> {
            if (text.contains(" ")) {
                return Collections.emptyList();
            }
            if ("iPhone16".equals(text)) {
                return Arrays.asList(
                        Token.builder().text("iPhone").type("n").startIndex(0).endIndex(6).confidence(1.0).build(),
                        Token.builder().text("16").type("n").startIndex(6).endIndex(8).confidence(1.0).build()
                );
            }
            if ("iPhone".equals(text) || "16".equals(text)) {
                return Collections.singletonList(token(text, 0, text.length()));
            }
            return Collections.emptyList();
        };
        DualPathTokenizer tokenizer = new DualPathTokenizer(mockDelegate);
        List<Token> tokens = tokenizer.tokenize("iPhone16");
        assertNotNull(tokens);
        assertEquals(2, tokens.size());
        assertEquals("iPhone", tokens.get(0).getText());
        assertEquals(0, tokens.get(0).getStartIndex());
        assertEquals(6, tokens.get(0).getEndIndex());
        assertEquals("16", tokens.get(1).getText());
        assertEquals(6, tokens.get(1).getStartIndex());
        assertEquals(8, tokens.get(1).getEndIndex());
    }

    @Test
    public void pathBFinerWithinSegmentIsUsed() {
        Tokenizer mockDelegate = text -> {
            if (text.contains(" ")) {
                return Collections.emptyList();
            }
            if ("GT5Pro".equals(text)) {
                return Collections.singletonList(
                        Token.builder().text("GT5Pro").type("n").startIndex(0).endIndex(6).confidence(1.0).build()
                );
            }
            if ("GT5".equals(text)) {
                return Arrays.asList(
                        Token.builder().text("GT").type("n").startIndex(0).endIndex(2).confidence(1.0).build(),
                        Token.builder().text("5").type("n").startIndex(2).endIndex(3).confidence(1.0).build()
                );
            }
            if ("Pro".equals(text)) {
                return Collections.singletonList(token(text, 0, text.length()));
            }
            return Collections.emptyList();
        };
        DualPathTokenizer tokenizer = new DualPathTokenizer(mockDelegate);
        List<Token> tokens = tokenizer.tokenize("GT5 Pro");
        assertNotNull(tokens);
        assertEquals(3, tokens.size());
        assertEquals("GT", tokens.get(0).getText());
        assertEquals(0, tokens.get(0).getStartIndex());
        assertEquals(2, tokens.get(0).getEndIndex());
        assertEquals("5", tokens.get(1).getText());
        assertEquals(2, tokens.get(1).getStartIndex());
        assertEquals(3, tokens.get(1).getEndIndex());
        assertEquals("Pro", tokens.get(2).getText());
        assertEquals(4, tokens.get(2).getStartIndex());
        assertEquals(7, tokens.get(2).getEndIndex());
    }

    @Test
    public void indicesAreCorrectForOriginalText() {
        Tokenizer mockDelegate = text -> Collections.singletonList(
                Token.builder().text(text).type("n").startIndex(0).endIndex(text.length()).confidence(1.0).build()
        );
        DualPathTokenizer tokenizer = new DualPathTokenizer(mockDelegate);
        List<Token> tokens = tokenizer.tokenize("A B C");
        assertNotNull(tokens);
        assertEquals(3, tokens.size());
        assertEquals("A", tokens.get(0).getText());
        assertEquals(0, tokens.get(0).getStartIndex());
        assertEquals(1, tokens.get(0).getEndIndex());
        assertEquals("B", tokens.get(1).getText());
        assertEquals(2, tokens.get(1).getStartIndex());
        assertEquals(3, tokens.get(1).getEndIndex());
        assertEquals("C", tokens.get(2).getText());
        assertEquals(4, tokens.get(2).getStartIndex());
        assertEquals(5, tokens.get(2).getEndIndex());
    }

    @Test
    public void integrationWithCompositeTokenizer() {
        List<Token> tokens = defaultTokenizer.tokenize("真我 GT5 Pro");
        assertNotNull(tokens);
        assertFalse(tokens.isEmpty());
        int lastEnd = 0;
        for (Token t : tokens) {
            assertTrue(t.getStartIndex() >= lastEnd);
            lastEnd = t.getEndIndex();
            assertNotNull(t.getText());
        }
        assertEquals(10, lastEnd);
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
