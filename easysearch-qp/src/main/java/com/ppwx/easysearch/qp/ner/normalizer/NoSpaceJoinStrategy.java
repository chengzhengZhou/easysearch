package com.ppwx.easysearch.qp.ner.normalizer;

import com.ppwx.easysearch.qp.tokenizer.Token;

import java.util.List;

/**
 * 无空格拼接各 token 文本的策略，适用于索引侧为紧凑写法的场景。
 */
public class NoSpaceJoinStrategy implements TokenSpanToValueStrategy {

    @Override
    public String toValue(String originText, List<Token> tokens, int startTokenIdx, int endTokenIdx) {
        if (tokens == null || startTokenIdx < 0 || endTokenIdx > tokens.size() || startTokenIdx >= endTokenIdx) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = startTokenIdx; i < endTokenIdx; i++) {
            String text = tokens.get(i).getText();
            if (text != null) {
                sb.append(text);
            }
        }
        return sb.toString();
    }
}
