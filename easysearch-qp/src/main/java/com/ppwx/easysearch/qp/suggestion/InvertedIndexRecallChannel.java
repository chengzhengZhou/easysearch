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

package com.ppwx.easysearch.qp.suggestion;

import java.util.Collections;
import java.util.List;

/**
 * 倒排索引召回通道：仅定义接口，供外部检索引擎（如 Elasticsearch）实现对接。
 * <p>
 * 倒排索引召回支持任意位置、任意顺序的 Term 匹配，通常由独立的检索引擎承载。
 * 此类提供默认的空实现，外部系统继承后覆写 {@link #recall(String, int)} 方法即可接入。
 * <p>
 * 扩展示例：
 * <pre>{@code
 * public class EsSuggestionRecallChannel extends InvertedIndexRecallChannel {
 *
 *     private final ElasticsearchClient client;
 *     private final String indexName;
 *
 *     public EsSuggestionRecallChannel(ElasticsearchClient client, String indexName) {
 *         this.client = client;
 *         this.indexName = indexName;
 *     }
 *
 *     @Override
 *     public List<RecallResult> recall(String prefix, int limit) {
 *         // 调用 ES completion suggester 或 term query
 *         // 将结果转换为 RecallResult 列表返回
 *     }
 * }
 * }</pre>
 */
public class InvertedIndexRecallChannel implements RecallChannel {

    public static final String CHANNEL_NAME = "inverted";

    @Override
    public String name() {
        return CHANNEL_NAME;
    }

    /**
     * 默认返回空列表。子类应覆写此方法实现倒排索引召回。
     *
     * @param prefix 用户当前输入
     * @param limit  最大返回条数
     * @return 召回结果列表
     */
    @Override
    public List<RecallResult> recall(String prefix, int limit) {
        return Collections.emptyList();
    }
}
