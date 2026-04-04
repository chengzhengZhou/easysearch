/*
 * Copyright 2025-2026 品万象(ppwx.com).
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 可热加载引擎的注册中心。
 * <p>
 * 后台管理系统通过此注册中心统一触发资源更新，无需感知各引擎的具体实现细节。
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 初始化时注册引擎
 * ReloadableRegistry registry = new ReloadableRegistry();
 * registry.register(termInterventionEngine);
 * registry.register(synonymEngine);
 * registry.register(spellChecker);
 *
 * // 后台 API 触发单个引擎更新
 * registry.reload("termIntervention", new PathTextLineSource("/new/path"));
 *
 * // 批量更新多个引擎
 * Map<String, TextLineSource> sources = new HashMap<>();
 * sources.put("termIntervention", new PathTextLineSource("/path1"));
 * sources.put("synonym", new PathTextLineSource("/path2"));
 * Map<String, Exception> failures = registry.reloadAll(sources);
 *
 * // 查看所有引擎状态
 * Map<String, EngineStatus> status = registry.getStatus();
 * }</pre>
 *
 * @author easysearch
 * @since 1.0
 */
public class ReloadableRegistry {

    private static final Logger log = LoggerFactory.getLogger(ReloadableRegistry.class);

    private final Map<String, Reloadable> engines = new ConcurrentHashMap<>();

    /**
     * 注册一个可热加载的引擎。
     *
     * @param engine 引擎实例，不能为 null，其 name() 不能为 null
     * @throws NullPointerException 如果 engine 或其 name() 为 null
     */
    public void register(Reloadable engine) {
        Objects.requireNonNull(engine, "engine must not be null");
        Objects.requireNonNull(engine.name(), "engine name must not be null");
        engines.put(engine.name(), engine);
        log.info("Registered reloadable engine: {}", engine.name());
    }

    /**
     * 注销一个引擎。
     *
     * @param name 引擎名称
     */
    public void unregister(String name) {
        Reloadable removed = engines.remove(name);
        if (removed != null) {
            log.info("Unregistered reloadable engine: {}", name);
        }
    }

    /**
     * 根据名称获取引擎。
     *
     * @param name 引擎名称
     * @return 引擎实例，未找到返回 null
     */
    public Reloadable getEngine(String name) {
        return engines.get(name);
    }

    /**
     * 根据名称和类型获取引擎。
     *
     * @param name 引擎名称
     * @param type 引擎类型
     * @param <T>  引擎类型
     * @return 引擎实例，未找到或类型不匹配返回 null
     */
    @SuppressWarnings("unchecked")
    public <T extends Reloadable> T getEngine(String name, Class<T> type) {
        Reloadable engine = engines.get(name);
        if (engine != null && type.isInstance(engine)) {
            return (T) engine;
        }
        return null;
    }

    /**
     * 按名称重新加载某个引擎的资源。
     *
     * @param name   引擎名称
     * @param source 资源数据源
     * @throws IOException              如果加载失败
     * @throws IllegalArgumentException 如果指定名称的引擎不存在
     */
    public void reload(String name, TextLineSource source) throws IOException {
        Reloadable engine = engines.get(name);
        if (engine == null) {
            throw new IllegalArgumentException("No engine registered with name: " + name);
        }
        log.info("Reloading engine: {}", name);
        engine.load(source);
    }

    /**
     * 按名称重新加载某个引擎的资源（从路径）。
     *
     * @param name 引擎名称
     * @param path 资源文件路径
     * @throws IOException              如果加载失败
     * @throws IllegalArgumentException 如果指定名称的引擎不存在
     */
    public void reload(String name, String path) throws IOException {
        reload(name, new PathTextLineSource(path));
    }

    /**
     * 批量重新加载多个引擎。
     * <p>
     * 即使部分引擎加载失败，也会继续尝试加载其他引擎。
     *
     * @param sources 引擎名称到资源源的映射
     * @return 失败的引擎名称及其异常，如果全部成功返回空 Map
     */
    public Map<String, Exception> reloadAll(Map<String, TextLineSource> sources) {
        Map<String, Exception> failures = new LinkedHashMap<>();
        for (Map.Entry<String, TextLineSource> entry : sources.entrySet()) {
            try {
                reload(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                log.error("Failed to reload engine: {}", entry.getKey(), e);
                failures.put(entry.getKey(), e);
            }
        }
        return failures;
    }

    /**
     * 获取所有已注册引擎的状态。
     *
     * @return 引擎名称到状态的映射
     */
    public Map<String, EngineStatus> getStatus() {
        Map<String, EngineStatus> result = new LinkedHashMap<>();
        for (Map.Entry<String, Reloadable> entry : engines.entrySet()) {
            Reloadable e = entry.getValue();
            String version = null;
            if (e instanceof AbstractReloadableEngine) {
                version = ((AbstractReloadableEngine) e).getVersion();
            }
            result.put(entry.getKey(), new EngineStatus(
                    e.name(), e.isLoaded(), e.lastLoadedTime(), version
            ));
        }
        return result;
    }

    /**
     * 获取所有已注册的引擎名称。
     *
     * @return 引擎名称集合（不可变）
     */
    public Collection<String> listEngines() {
        return Collections.unmodifiableSet(engines.keySet());
    }

    /**
     * 判断是否存在指定名称的引擎。
     *
     * @param name 引擎名称
     * @return 如果存在返回 true
     */
    public boolean hasEngine(String name) {
        return engines.containsKey(name);
    }

    /**
     * 获取已注册引擎数量。
     *
     * @return 引擎数量
     */
    public int size() {
        return engines.size();
    }

    /**
     * 引擎状态快照。
     * <p>
     * 包含引擎的名称、加载状态、最后加载时间和版本号。
     */
    public static class EngineStatus {
        private final String name;
        private final boolean loaded;
        private final long lastLoadedTime;
        private final String version;

        public EngineStatus(String name, boolean loaded, long lastLoadedTime, String version) {
            this.name = name;
            this.loaded = loaded;
            this.lastLoadedTime = lastLoadedTime;
            this.version = version;
        }

        /**
         * 获取引擎名称。
         */
        public String getName() {
            return name;
        }

        /**
         * 判断引擎是否已加载资源。
         */
        public boolean isLoaded() {
            return loaded;
        }

        /**
         * 获取最后加载时间（毫秒）。
         *
         * @return 时间戳，未加载返回 -1
         */
        public long getLastLoadedTime() {
            return lastLoadedTime;
        }

        /**
         * 获取资源版本号。
         *
         * @return 版本号，未设置返回 null
         */
        public String getVersion() {
            return version;
        }

        @Override
        public String toString() {
            return "EngineStatus{" +
                    "name='" + name + '\'' +
                    ", loaded=" + loaded +
                    ", lastLoadedTime=" + lastLoadedTime +
                    ", version='" + version + '\'' +
                    '}';
        }
    }
}
