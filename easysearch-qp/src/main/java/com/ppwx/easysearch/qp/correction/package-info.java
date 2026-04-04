/*
 * Copyright 2026 chengzhengZhou
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * 搜索 query 拼写纠错模块。
 *
 * <p>提供基于拼音混淆 + 编辑距离的多信号融合纠错能力，包含：
 * <ul>
 *   <li>错误检测：词典覆盖率检测 + 拼音异常检测</li>
 *   <li>候选生成：拼音倒排索引召回 + BK-Tree 编辑距离近邻搜索</li>
 *   <li>多信号排序：拼音相似度 + 编辑距离 + 词频加权打分</li>
 *   <li>置信度分级：auto-correct / suggest-only / no-correction</li>
 * </ul>
 *
 * <p>对外入口为 {@link com.ppwx.easysearch.qp.correction.SpellCorrectionEngine}。
 */
package com.ppwx.easysearch.qp.correction;
