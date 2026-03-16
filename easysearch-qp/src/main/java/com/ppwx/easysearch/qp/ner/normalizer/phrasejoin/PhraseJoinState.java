package com.ppwx.easysearch.qp.ner.normalizer.phrasejoin;

/**
 * 词组转移状态机的状态。
 */
public enum PhraseJoinState {
    /** 尚未输出或上一可合并块已结束 */
    START,
    /** 刚输出到 ATOM 结尾 */
    AFTER_ATOM,
    /** 刚输出 CONNECTOR，下一 token 应为 ATOM */
    AFTER_CONNECTOR,
    /** 终止，不再消费 token */
    END
}
