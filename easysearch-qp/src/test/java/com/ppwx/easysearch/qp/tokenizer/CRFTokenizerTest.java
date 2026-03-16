package com.ppwx.easysearch.qp.tokenizer;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * CRFTokenizer 单元测试。
 * 空/null 不依赖模型；有模型时校验分词结果结构。
 */
public class CRFTokenizerTest {

    @Test
    public void tokenizeNullOrEmptyReturnsEmpty() {
        CRFTokenizer tokenizer = new CRFTokenizer();
        assertTrue(tokenizer.tokenize(null).isEmpty());
        assertTrue(tokenizer.tokenize("").isEmpty());
        assertTrue(tokenizer.tokenize("   ").isEmpty());
    }

    @Test
    public void tokenizeWhenModelAvailableReturnsValidTokens() {
        CRFTokenizer tokenizer = new CRFTokenizer();
        List<Token> tokens = tokenizer.tokenize("华为平板M5");
        // 模型可能不存在则返回空；存在则应有词且含偏移与词性
        if (tokens.isEmpty()) {
            return;
        }
        assertFalse(tokens.isEmpty());
        int lastEnd = 0;
        for (Token t : tokens) {
            assertNotNull(t.getText());
            assertFalse(t.getText().isEmpty());
            assertNotNull(t.getType());
            assertTrue(t.getStartIndex() >= 0);
            assertTrue(t.getEndIndex() > t.getStartIndex());
            assertTrue(t.getStartIndex() >= lastEnd);
            lastEnd = t.getEndIndex();
        }
        assertEquals("华为平板M5".length(), lastEnd);
    }
}
