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

import com.ppwx.easysearch.qp.source.TextLineSource;

import java.io.IOException;
import java.util.*;

/**
 * 联想词服务门面：封装多路召回 + 融合 + 截断的完整流程。
 * <p>
 * 通过 {@link Builder} 模式构建，支持自定义 {@link RecallChannel} 列表和 {@link SuggestionMerger} 实现。
 * <p>
 * 使用示例：
 * <pre>{@code
 * SuggestionEngine engine = new SuggestionEngine();
 * engine.load("suggestion/dict.txt");
 *
 * SuggestionService service = SuggestionService.builder()
 *     .withEngine(engine)
 *     .withConfig(SuggestionConfig.defaults().setTopK(8))
 *     .build();
 *
 * List<Suggestion> suggestions = service.suggest("苹果手");
 * }</pre>
 */
public class SuggestionService {

    private final SuggestionEngine engine;
    private final List<RecallChannel> channels;
    private final SuggestionMerger merger;
    private final SuggestionConfig config;

    private SuggestionService(SuggestionEngine engine, List<RecallChannel> channels,
                              SuggestionMerger merger, SuggestionConfig config) {
        this.engine = engine;
        this.channels = channels;
        this.merger = merger;
        this.config = config;
    }

    /**
     * 获取联想词建议。
     *
     * @param prefix 用户当前输入的前缀
     * @return 排序后的联想词列表
     */
    public List<Suggestion> suggest(String prefix) {
        return suggest(prefix, config.getTopK());
    }

    /**
     * 获取联想词建议。
     *
     * @param prefix 用户当前输入的前缀
     * @param limit  最大返回条数
     * @return 排序后的联想词列表
     */
    public List<Suggestion> suggest(String prefix, int limit) {
        if (prefix == null || prefix.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 多路召回
        Map<String, List<RecallResult>> channelResults = new LinkedHashMap<>();
        for (RecallChannel channel : channels) {
            int channelLimit = getChannelLimit(channel);
            List<RecallResult> results = channel.recall(prefix, channelLimit);
            if (results != null && !results.isEmpty()) {
                channelResults.put(channel.name(), results);
            }
        }

        if (channelResults.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 融合排序
        List<Suggestion> suggestions = merger.merge(channelResults, limit);

        // 3. 设置高亮信息
        for (Suggestion suggestion : suggestions) {
            suggestion.setHighlight(buildHighlight(prefix, suggestion.getText()));
        }

        return suggestions;
    }

    /**
     * 重新加载联想词表。
     *
     * @param path 资源文件路径
     * @throws IOException 如果加载失败
     */
    public void reload(String path) throws IOException {
        engine.load(path);
    }

    /**
     * 重新加载联想词表。
     *
     * @param source 资源数据源
     * @throws IOException 如果加载失败
     */
    public void reload(TextLineSource source) throws IOException {
        engine.load(source);
    }

    /**
     * 获取底层引擎。
     */
    public SuggestionEngine getEngine() {
        return engine;
    }

    /**
     * 获取当前配置。
     */
    public SuggestionConfig getConfig() {
        return config;
    }

    /**
     * 获取所有召回通道。
     */
    public List<RecallChannel> getChannels() {
        return Collections.unmodifiableList(channels);
    }

    private int getChannelLimit(RecallChannel channel) {
        String name = channel.name();
        if (PrefixRecallChannel.CHANNEL_NAME.equals(name)) {
            return config.getMaxPrefixCandidates();
        } else if (PinyinRecallChannel.CHANNEL_NAME.equals(name)) {
            return config.getMaxPinyinCandidates();
        } else if (InvertedIndexRecallChannel.CHANNEL_NAME.equals(name)) {
            return config.getMaxInvertedCandidates();
        }
        return config.getMaxPrefixCandidates();
    }

    /**
     * 构建高亮信息：标记用户已输入的前缀部分。
     * <p>
     * 格式：{@code <em>前缀</em>剩余部分}
     */
    private static String buildHighlight(String prefix, String text) {
        if (text == null) {
            return null;
        }
        if (text.startsWith(prefix)) {
            return "<em>" + prefix + "</em>" + text.substring(prefix.length());
        }
        return text;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * SuggestionService 构建器。
     */
    public static class Builder {

        private SuggestionEngine engine;
        private SuggestionConfig config;
        private SuggestionMerger merger;
        private final List<RecallChannel> extraChannels = new ArrayList<>();

        /**
         * 设置联想词引擎（必选）。
         */
        public Builder withEngine(SuggestionEngine engine) {
            this.engine = engine;
            return this;
        }

        /**
         * 设置配置项（可选，默认使用 {@link SuggestionConfig#defaults()}）。
         */
        public Builder withConfig(SuggestionConfig config) {
            this.config = config;
            return this;
        }

        /**
         * 设置融合策略（可选，默认使用 {@link RRFSuggestionMerger}）。
         */
        public Builder withMerger(SuggestionMerger merger) {
            this.merger = merger;
            return this;
        }

        /**
         * 添加自定义召回通道（如倒排索引召回、语义召回等）。
         */
        public Builder addChannel(RecallChannel channel) {
            this.extraChannels.add(channel);
            return this;
        }

        /**
         * 构建 SuggestionService。
         *
         * @throws IllegalArgumentException 如果未设置 engine
         */
        public SuggestionService build() {
            if (engine == null) {
                throw new IllegalArgumentException("SuggestionEngine must not be null");
            }

            SuggestionConfig cfg = config != null ? config : SuggestionConfig.defaults();
            SuggestionMerger mgr = merger != null ? merger : new RRFSuggestionMerger(cfg.getRrfK());

            // 构建默认召回通道列表
            List<RecallChannel> channels = new ArrayList<>();
            if (cfg.isEnablePrefix()) {
                channels.add(new PrefixRecallChannel(engine));
            }
            if (cfg.isEnablePinyin()) {
                channels.add(new PinyinRecallChannel(engine));
            }

            // 添加自定义通道
            channels.addAll(extraChannels);

            return new SuggestionService(engine, channels, mgr, cfg);
        }
    }
}
