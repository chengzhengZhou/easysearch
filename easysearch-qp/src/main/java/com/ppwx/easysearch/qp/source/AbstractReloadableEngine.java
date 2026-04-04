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
import java.io.InputStream;

/**
 * 可热加载引擎的抽象基类。
 * <p>
 * 统一处理 {@code load(String)} → {@code load(TextLineSource)} → {@code doLoad(InputStream)}
 * 的委托链，并记录加载时间戳和版本信息。
 * <p>
 * 子类只需实现：
 * <ul>
 *   <li>{@link #doLoad(InputStream)} — 从 InputStream 解析并构建内部数据结构</li>
 *   <li>{@link #checkLoaded()} — 判断引擎是否已经加载了有效数据</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>{@code
 * public class MyEngine extends AbstractReloadableEngine {
 *     private volatile MyData data;
 *
 *     public MyEngine() {
 *         super("myEngine");
 *     }
 *
 *     @Override
 *     protected void doLoad(InputStream is) throws IOException {
 *         MyData newData = parseFromStream(is);
 *         this.data = newData; // 原子替换，保证线程安全
 *     }
 *
 *     @Override
 *     protected boolean checkLoaded() {
 *         return data != null;
 *     }
 * }
 * }</pre>
 *
 * @author easysearch
 * @since 1.0
 */
public abstract class AbstractReloadableEngine implements Reloadable {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final String engineName;
    private volatile long lastLoadedTimestamp = -1;
    private volatile String version;

    /**
     * 创建可热加载引擎实例。
     *
     * @param engineName 引擎唯一标识名称，用于注册和后台展示
     */
    protected AbstractReloadableEngine(String engineName) {
        this.engineName = engineName;
    }

    @Override
    public final String name() {
        return engineName;
    }

    /**
     * 从路径加载资源。
     * <p>
     * 委托给 {@link #load(TextLineSource)}，使用 {@link PathTextLineSource} 封装路径。
     *
     * @param path 资源文件路径（支持 classpath 和文件系统路径）
     * @throws IOException 如果读取资源失败
     */
    @Override
    public final void load(String path) throws IOException {
        load(new PathTextLineSource(path));
    }

    /**
     * 从统一资源源加载资源。
     * <p>
     * 打开资源流后委托给 {@link #doLoad(InputStream)}，加载成功后记录时间戳。
     *
     * @param source 资源数据源
     * @throws IOException 如果读取资源失败
     */
    @Override
    public final void load(TextLineSource source) throws IOException {
        try (InputStream is = source.openStream()) {
            doLoad(is);
            this.lastLoadedTimestamp = System.currentTimeMillis();
            log.info("[{}] resource loaded successfully", engineName);
        }
    }

    @Override
    public final boolean isLoaded() {
        return checkLoaded();
    }

    @Override
    public final long lastLoadedTime() {
        return lastLoadedTimestamp;
    }

    /**
     * 获取资源版本号。
     * <p>
     * 版本号由外部设置，用于管理后台追踪资源版本。
     *
     * @return 资源版本号，未设置返回 null
     */
    public String getVersion() {
        return version;
    }

    /**
     * 设置资源版本号。
     *
     * @param version 资源版本号
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * 子类实现：从 InputStream 解析数据并构建内部索引/数据结构。
     * <p>
     * 实现需保证线程安全，通常采用 volatile 引用原子替换的方式：
     * 构建完整的新数据结构后，一次性替换引用。
     * <p>
     * 注意：不需要关闭 InputStream，由基类负责关闭。
     *
     * @param is 资源输入流
     * @throws IOException 如果解析失败
     */
    protected abstract void doLoad(InputStream is) throws IOException;

    /**
     * 子类实现：判断引擎是否有有效数据。
     * <p>
     * 典型实现是检查内部数据结构是否为 null。
     *
     * @return 如果已加载有效数据返回 true，否则返回 false
     */
    protected abstract boolean checkLoaded();
}
