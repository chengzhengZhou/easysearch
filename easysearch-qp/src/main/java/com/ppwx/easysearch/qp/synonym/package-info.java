/**
 * 同义词模块：基于 HanLP BinTrie 的匹配，支持单向/双向、改写与拓展。
 * <ul>
 *   <li>数据格式：简化 txt（源词 \t 方向 \t 目标1,目标2,...）</li>
 *   <li>匹配：SynonymEngine（BinTrie + 最长匹配）</li>
 *   <li>改写：RewriteStrategy（ReplaceFirst / ReplaceAll）</li>
 *   <li>拓展：ExpandStrategy（ExpandOr）</li>
 * </ul>
 *
 * @author ext.ahs.zhouchzh1@jd.com
 * @date 2024/11/1 11:20
 */
package com.ppwx.easysearch.qp.synonym;