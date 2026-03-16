package com.ppwx.easysearch.qp.synonym;

/**
 * 同义词方向类型。
 * <ul>
 *   <li>UNIDIRECTIONAL：单向，仅 source → targets 参与匹配与改写/拓展</li>
 *   <li>BIDIRECTIONAL：双向，任一侧命中都可替换/拓展为另一侧</li>
 * </ul>
 */
public enum SynonymType {
    /** 单向：仅 源词 → 目标 参与 */
    UNIDIRECTIONAL,
    /** 双向：等价，任一侧命中都可替换/拓展为另一侧 */
    BIDIRECTIONAL
}
