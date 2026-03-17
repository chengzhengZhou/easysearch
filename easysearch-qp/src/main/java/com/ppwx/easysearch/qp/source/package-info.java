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
 * 通用文本行资源加载：{@link com.ppwx.easysearch.qp.source.TextLineSource} 抽象与实现，
 * {@link com.ppwx.easysearch.qp.source.CompositeTextLineSource} 支持多源合并；
 * 供同义词、分词器、实体识别器等从文件、classpath 或数据库等统一加载外部数据。
 */
package com.ppwx.easysearch.qp.source;
