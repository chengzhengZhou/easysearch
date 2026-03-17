package com.ppwx.easysearch.qp.ner.normalizer.phrasejoin;

import com.ppwx.easysearch.qp.tokenizer.Token;

import java.util.*;

/**
 * 词组转移状态机引擎：根据 Profile 对 token 序列做转移，输出补偿合并后的字符串。
 * 支持通过不同 Profile 扩展特定类型词组（型号、存储、焦段等）的转移规则。
 * <p>
 * SPACE 与 OTHER 会终止当前可合并块且不追加到输出；前导非 ATOM 被跳过直至遇到第一个 ATOM。
 * </p>
 */
public final class PhraseJoinEngine {

    private static final Set<String> LEFT_BRACKETS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("(", "（", "[")));
    private static final Set<String> RIGHT_BRACKETS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(")", "）", "]")));

    /**
     * 在实体 span 内从左到右跑 FSM，返回合并后的候选串；若无法合并则返回空串（由调用方回退）。
     *
     * @param originText    原始查询文本（当前未使用，保留供后续扩展）
     * @param tokens        token 列表
     * @param startTokenIdx 实体起始 token 下标（含）
     * @param endTokenIdx   实体结束 token 下标（不含）
     * @param profile       词组配置
     * @return 合并结果，可能为空
     */
    public String run(String originText, List<Token> tokens, int startTokenIdx, int endTokenIdx,
                      PhraseJoinProfile profile) {
        if (originText == null || tokens == null || profile == null) {
            return "";
        }
        if (startTokenIdx < 0 || endTokenIdx > tokens.size() || startTokenIdx >= endTokenIdx) {
            return "";
        }
        if (startTokenIdx + 1 == endTokenIdx) {
            String single = tokens.get(startTokenIdx).getText();
            return single != null ? single : "";
        }

        StringBuilder out = new StringBuilder();
        PhraseJoinState state = PhraseJoinState.START;
        Set<String> connectors = profile.getConnectors();

        outer:
        for (int i = startTokenIdx; i < endTokenIdx; i++) {
            Token token = tokens.get(i);
            String text = token.getText();
            if (text == null) {
                text = "";
            }
            TokenClass tc = classify(profile, connectors, text);

            if (profile.isStop(text)) {
                break;
            }

            switch (state) {
                case START:
                    if (tc == TokenClass.ATOM) {
                        out.append(text);
                        state = PhraseJoinState.AFTER_ATOM;
                    }
                    break;

                case AFTER_ATOM:
                    if (tc == TokenClass.CONNECTOR && connectors.contains(text)) {
                        TokenClass next = peekClass(tokens, i + 1, endTokenIdx, profile, connectors);
                        if (next == TokenClass.ATOM) {
                            out.append(text);
                            state = PhraseJoinState.AFTER_CONNECTOR;
                        } else {
                            out.append(text);
                            break outer;
                        }
                    } else if (tc == TokenClass.ATOM) {
                        out.append(' ').append(text);
                        state = PhraseJoinState.AFTER_ATOM;
                    } else if ((tc == TokenClass.LP || tc == TokenClass.RP) && profile.isBracketGlued()) {
                        out.append(text);
                        state = PhraseJoinState.AFTER_ATOM;
                    } else {
                        break outer;
                    }
                    break;

                case AFTER_CONNECTOR:
                    if (tc == TokenClass.ATOM) {
                        out.append(text);
                        state = PhraseJoinState.AFTER_ATOM;
                    } else {
                        break outer;
                    }
                    break;

                case END:
                    break;
            }
        }

        return out.toString().trim();
    }

    private static TokenClass classify(PhraseJoinProfile profile, Set<String> connectors, String text) {
        if (text == null) {
            return TokenClass.OTHER;
        }
        if (text.isEmpty()) {
            return TokenClass.OTHER;
        }
        if (text.trim().isEmpty()) {
            return TokenClass.SPACE;
        }
        if (LEFT_BRACKETS.contains(text)) {
            return TokenClass.LP;
        }
        if (RIGHT_BRACKETS.contains(text)) {
            return TokenClass.RP;
        }
        if (connectors.contains(text)) {
            return TokenClass.CONNECTOR;
        }
        if (profile.isAtom(text)) {
            return TokenClass.ATOM;
        }
        return TokenClass.OTHER;
    }

    private static TokenClass peekClass(List<Token> tokens, int index, int endTokenIdx,
                                        PhraseJoinProfile profile, Set<String> connectors) {
        if (index >= endTokenIdx) {
            return TokenClass.OTHER;
        }
        Token t = tokens.get(index);
        String text = t != null ? t.getText() : null;
        return classify(profile, connectors, text != null ? text : "");
    }
}
