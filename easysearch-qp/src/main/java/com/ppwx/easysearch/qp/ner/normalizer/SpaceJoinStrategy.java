package com.ppwx.easysearch.qp.ner.normalizer;

import com.ppwx.easysearch.qp.tokenizer.Token;

import java.util.List;

/**
 * 使用空格拼接各 token 文本的策略（原 CRF 默认行为）。
 */
public class SpaceJoinStrategy implements TokenSpanToValueStrategy {

    @Override
    public String toValue(String originText, List<Token> tokens, int startTokenIdx, int endTokenIdx) {
        if (tokens == null || startTokenIdx < 0 || endTokenIdx > tokens.size() || startTokenIdx >= endTokenIdx) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = startTokenIdx; i < endTokenIdx; i++) {
            String text = tokens.get(i).getText();
            if (text != null) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(text);
            }
        }
        return sb.toString();
    }
}
