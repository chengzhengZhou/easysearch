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

import com.google.common.collect.Lists;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className InMemoryMetricsRegistry
 * @description 基于内存的实例注册器
 * @date 2024/11/28 17:48
 **/
public class InMemoryMetricsRegistry implements MetricsRegistry<Metrics> {

    private static final String NAME_MUST_NOT_BE_NULL = "Name must not be null";
    private static final String TAGS_MUST_NOT_BE_NULL = "Tags must not be null";

    protected final Map<String, Metrics> registryTags;

    public InMemoryMetricsRegistry(Map<String, Metrics> tags) {
        this.registryTags = Objects.requireNonNull(tags, TAGS_MUST_NOT_BE_NULL);
    }

    protected Metrics computeIfAbsent(String name, Supplier<Metrics> supplier) {
        return registryTags.computeIfAbsent(Objects.requireNonNull(name, NAME_MUST_NOT_BE_NULL), k -> supplier.get());
    }

    public Metrics metrics(String tag, int windowSize) {
        return computeIfAbsent(tag, () -> newMetrics(windowSize));
    }

    private Metrics newMetrics(int windowSize) {
        return new SlidingTimeWindowMetrics(windowSize, Clock.systemUTC());
    }

    @Override
    public Metrics remove(String tag) {
        return registryTags.remove(tag);
    }

    @Override
    public List<Metrics> getAll() {
        return Lists.newLinkedList(registryTags.values());
    }

    @Override
    public void clear() {
        registryTags.clear();
    }

}
