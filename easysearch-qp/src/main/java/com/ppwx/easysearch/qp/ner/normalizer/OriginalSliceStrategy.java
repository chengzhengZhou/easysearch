package com.ppwx.easysearch.qp.ner.normalizer;

import com.ppwx.easysearch.qp.tokenizer.Token;

import java.util.List;

/**
 * 使用原文切片的策略：按首尾 token 的 offset 在 originText 上截取，保持与原文一致，便于与索引匹配。
 */
public class OriginalSliceStrategy implements TokenSpanToValueStrategy {

    @Override
    public String toValue(String originText, List<Token> tokens, int startTokenIdx, int endTokenIdx) {
        if (originText == null || tokens == null || startTokenIdx < 0 || endTokenIdx > tokens.size() || startTokenIdx >= endTokenIdx) {
            return "";
        }
        Token startToken = tokens.get(startTokenIdx);
        Token endToken = tokens.get(endTokenIdx - 1);
        int start = Math.min(startToken.getStartIndex(), originText.length());
        int end = Math.min(endToken.getEndIndex(), originText.length());
        if (start >= end) {
            return "";
        }
        return originText.substring(start, end);
    }
}
