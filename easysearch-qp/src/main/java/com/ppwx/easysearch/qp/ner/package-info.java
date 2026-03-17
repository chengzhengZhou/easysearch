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
 * 命名实体识别（Name entity recognition 简称NER），指从文本中抽取有意义的实体，如人名、地址、专有名词
 * 主要方式可分为规则和机器学习
 * 1、基于规则的方法：
 * 使用预定义的规则和词典来匹配文本中的实体。
 * 依赖于语言学专家构造的规则模板，通过模式和字符串匹配来识别实体。
 * 优点是准确度高，缺点是可移植性差，需要大量的人工参与，且难以适应新情况。
 * 2、机器学习（深度学习、迁移学习、端到端学习等）
 */
package com.ppwx.easysearch.qp.ner;