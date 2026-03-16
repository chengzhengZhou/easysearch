package com.ppwx.easysearch.qp.ner.normalizer;

import com.ppwx.easysearch.qp.tokenizer.Token;

import java.util.List;

/**
 * 从 token 片段得到用于归一化的候选字符串的策略。
 * 用于 CRF 等多 token 实体识别场景，可按实体类型选用不同策略，避免单一空格拼接破坏原文格式导致召回过宽。
 */
public interface TokenSpanToValueStrategy {

    /**
     * 根据原文与 token 区间生成候选字符串（供后续按类型归一化使用）。
     *
     * @param originText     原始查询文本
     * @param tokens         token 列表
     * @param startTokenIdx  实体起始 token 下标（含）
     * @param endTokenIdx    实体结束 token 下标（不含）
     * @return 候选字符串，不应为 null
     */
    String toValue(String originText, List<Token> tokens, int startTokenIdx, int endTokenIdx);
}
