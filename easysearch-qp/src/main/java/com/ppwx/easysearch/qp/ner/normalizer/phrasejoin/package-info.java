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
 * 词组转移状态机：在分词阶段可能将词拆得过细（如 16+ → 16、+）时，将误拆分的 token 重新合并。
 * <p>
 * 扩展方式：实现 {@link com.ppwx.easysearch.qp.ner.normalizer.phrasejoin.PhraseJoinProfile} 定义连接符集合与 ATOM 判定，
 * 用同一套 {@link com.ppwx.easysearch.qp.ner.normalizer.phrasejoin.PhraseJoinEngine} 驱动不同实体类型（型号、存储、焦段等）。
 * </p>
 */
package com.ppwx.easysearch.qp.ner.normalizer.phrasejoin;
