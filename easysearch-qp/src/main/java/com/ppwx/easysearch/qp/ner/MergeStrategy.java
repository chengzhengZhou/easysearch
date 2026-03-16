package com.ppwx.easysearch.qp.ner;

/**
 * 多识别器结果合并策略
 * <ul>
 *   <li>DICT_FIRST：词典优先，冲突时保留词典识别结果</li>
 *   <li>CRF_FIRST：CRF优先，冲突时保留CRF模型识别结果</li>
 * </ul>
 */
public enum MergeStrategy {
    /**
     * 词典优先（默认）
     * 适合新词多、词典更新频繁的场景，词典能快速响应特殊词或临时实体
     */
    DICT_FIRST,

    /**
     * CRF 优先
     * 适合词典质量不稳定或模型泛化能力更强的场景
     */
    CRF_FIRST
}
