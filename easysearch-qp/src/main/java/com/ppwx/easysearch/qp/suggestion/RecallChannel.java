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

import java.util.List;

/**
 * 召回通道接口：每路召回实现此接口。
 * <p>
 * 所有召回通道（前缀、拼音、倒排索引等）均实现此接口，
 * 由 {@link SuggestionService} 统一编排调用。
 */
public interface RecallChannel {

    /**
     * 通道名称，用于融合时的 channel 标识和 trace 记录。
     *
     * @return 通道名称，不能为 null
     */
    String name();

    /**
     * 从候选池中召回与 prefix 匹配的候选，按路内排序返回 Top-limit。
     *
     * @param prefix 用户当前输入的前缀
     * @param limit  最大返回条数
     * @return 召回结果列表（已按路内排序），不为 null
     */
    List<RecallResult> recall(String prefix, int limit);
}
