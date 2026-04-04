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

package com.ppwx.easysearch.qp.support;

/**
 * 查询处理链路中的单个阶段。
 * <p>
 * 每个 Stage 只读写 {@link QueryContext} 的特定部分，并在 trace 中记录执行信息。
 * 通过 {@link Pipeline Pipeline&lt;Stage&gt;} 编排多个 Stage 组成完整的查询处理链路。
 */
public interface Stage {

    /**
     * 阶段名称，用于 trace 记录和日志输出。
     *
     * @return 阶段名称
     */
    String name();

    /**
     * 处理 QueryContext。
     *
     * @param ctx 查询上下文
     */
    void process(QueryContext ctx);
}
