/*
 * Copyright 2025-2026 �品万象(ppwx.com).
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
package com.ppwx.easysearch.qp.source;

import java.io.IOException;

/**
 * 可热加载资源的统一抽象。
 * <p>
 * 所有依赖外部词表/规则的引擎均实现此接口，方便后台管理系统
 * 统一触发动态更新，无需感知各引擎的具体实现细节。
 * <p>
 * 典型实现包括：
 * <ul>
 *   <li>干预引擎（TermInterventionEngine, SentenceInterventionEngine）</li>
 *   <li>同义词引擎（SynonymEngine）</li>
 *   <li>纠错引擎（DictBasedSpellChecker）</li>
 *   <li>分词器（DictTokenizer, DictOnlyTokenizer）</li>
 *   <li>实体识别器（DictEntityRecognizer）</li>
 * </ul>
 *
 * @author easysearch
 * @since 1.0
 */
public interface Reloadable {

    /**
     * 引擎唯一标识名称（用于注册和管理后台展示）。
     *
     * @return 引擎名称，不能为 null
     */
    String name();

    /**
     * 从文件路径加载资源。
     * <p>
     * 实现应支持热更新，即在运行时可多次调用以更新资源。
     *
     * @param path 资源文件路径
     * @throws IOException 如果读取资源失败
     */
    void load(String path) throws IOException;

    /**
     * 从统一资源源加载资源。
     * <p>
     * 支持多种来源（文件、数据库、配置中心等），便于后台动态更新。
     *
     * @param source 资源数据源
     * @throws IOException 如果读取资源失败
     */
    void load(TextLineSource source) throws IOException;

    /**
     * 判断是否已加载资源。
     *
     * @return 如果已加载有效资源返回 true，否则返回 false
     */
    boolean isLoaded();

    /**
     * 获取上次成功加载资源的时间戳。
     *
     * @return 上次加载成功的时间戳（毫秒），未加载返回 -1
     */
    long lastLoadedTime();
}
