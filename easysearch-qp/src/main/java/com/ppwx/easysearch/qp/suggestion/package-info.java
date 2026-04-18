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
 * 搜索联想词（Search Suggestion / Autocomplete）模块。
 * <p>
 * 采用多路召回 + 可插拔融合的架构，为用户在搜索框输入时实时推荐候选查询词。
 *
 * <h3>架构概览</h3>
 * <pre>
 *   用户输入 prefix
 *       ├── PrefixRecallChannel   （汉字前缀 Trie 匹配）
 *       ├── PinyinRecallChannel   （全拼 + 首字母 Trie 匹配）
 *       ├── InvertedIndexRecallChannel （倒排索引 Term 匹配 — 仅接口定义）
 *       └── （自定义召回通道）
 *               ↓
 *         SuggestionMerger（RRF 融合 / 加权求和 / LTR）
 *               ↓
 *         去重 + 截断 Top-K → List&lt;Suggestion&gt;
 * </pre>
 *
 * <h3>核心类</h3>
 * <ul>
 *   <li>{@link com.ppwx.easysearch.qp.suggestion.SuggestionService} — 门面，编排完整流程</li>
 *   <li>{@link com.ppwx.easysearch.qp.suggestion.SuggestionEngine} — 引擎，词表加载与索引构建</li>
 *   <li>{@link com.ppwx.easysearch.qp.suggestion.RecallChannel} — 召回通道接口</li>
 *   <li>{@link com.ppwx.easysearch.qp.suggestion.SuggestionMerger} — 融合策略接口</li>
 *   <li>{@link com.ppwx.easysearch.qp.suggestion.RRFSuggestionMerger} — RRF 融合默认实现</li>
 * </ul>
 *
 * <h3>扩展方式</h3>
 * <ul>
 *   <li>新增召回路径：实现 {@link com.ppwx.easysearch.qp.suggestion.RecallChannel} 接口</li>
 *   <li>替换融合策略：实现 {@link com.ppwx.easysearch.qp.suggestion.SuggestionMerger} 接口</li>
 *   <li>接入检索引擎：继承 {@link com.ppwx.easysearch.qp.suggestion.InvertedIndexRecallChannel}</li>
 * </ul>
 *
 * @since 1.0
 */
package com.ppwx.easysearch.qp.suggestion;
