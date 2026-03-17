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

package com.ppwx.easysearch.core.metrics;

import java.util.List;

/**
 * @className MetricsRegistry
 * @description Metrics注册器，存在较多的数据度量时可以使用该类进行管理
 **/
public interface MetricsRegistry<E> {

    /**
     * @description 获取一个Metrics，没有则创建一个
     * @param tag 别名
     * @param windowSize 窗口大小
     * @return Metrics
     */
    E metrics(String tag, int windowSize);

    /**
     * @description 移除
     * @param tag 别名
     * @return E
     */
    E remove(String tag);

    /**
     * @description 获取所有Metrix
     * @return List<E>
     */
    List<E> getAll();

    /**
     * @description 移除已创建的Metrix
     * @return void
     */
    void clear();
}
