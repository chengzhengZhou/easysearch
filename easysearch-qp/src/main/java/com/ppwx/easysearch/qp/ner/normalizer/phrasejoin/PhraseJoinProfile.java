package com.ppwx.easysearch.qp.ner.normalizer.phrasejoin;

import java.util.Set;

/**
 * 词组转移配置：定义某类词组（如型号、存储、焦段）的连接符、ATOM 判定及终止条件，
 * 便于扩展特定类型词组的转移规则。
 */
public interface PhraseJoinProfile {

    /**
     * 允许参与无空格拼接的连接符集合（如 +、-、/、.、_）。
     * 仅当 token 文本在此集合内时才视为 CONNECTOR。
     */
    Set<String> getConnectors();

    /**
     * 判断 token 文本是否属于「主体片段」（字母/数字/混合）。
     */
    boolean isAtom(String tokenText);

    /**
     * 判断是否遇到该 token 即终止当前可合并块（如明显的中文词、停用标点）。
     * 默认不终止；子类可覆盖。
     */
    default boolean isStop(String tokenText) {
        return false;
    }

    /**
     * 是否将括号类 token 与相邻 ATOM 无空格拼接。
     * 默认 true（型号场景通常需要）。
     */
    default boolean isBracketGlued() {
        return true;
    }
}
