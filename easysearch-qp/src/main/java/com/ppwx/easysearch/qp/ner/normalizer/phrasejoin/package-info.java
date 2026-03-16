/**
 * 词组转移状态机：在分词阶段可能将词拆得过细（如 16+ → 16、+）时，将误拆分的 token 重新合并。
 * <p>
 * 扩展方式：实现 {@link com.ppwx.easysearch.qp.ner.normalizer.phrasejoin.PhraseJoinProfile} 定义连接符集合与 ATOM 判定，
 * 用同一套 {@link com.ppwx.easysearch.qp.ner.normalizer.phrasejoin.PhraseJoinEngine} 驱动不同实体类型（型号、存储、焦段等）。
 * </p>
 */
package com.ppwx.easysearch.qp.ner.normalizer.phrasejoin;
