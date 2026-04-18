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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 前缀召回通道：基于汉字前缀 Trie 进行前缀匹配召回。
 * <p>
 * 持有 {@link SuggestionEngine} 引用，通过汉字前缀匹配召回候选词条，
 * 按 weight 降序排列后包装为 {@link RecallResult} 返回。
 */
public class PrefixRecallChannel implements RecallChannel {

    public static final String CHANNEL_NAME = "prefix";

    private final SuggestionEngine engine;

    public PrefixRecallChannel(SuggestionEngine engine) {
        this.engine = engine;
    }

    @Override
    public String name() {
        return CHANNEL_NAME;
    }

    @Override
    public List<RecallResult> recall(String prefix, int limit) {
        if (prefix == null || prefix.isEmpty() || !engine.isLoaded()) {
            return Collections.emptyList();
        }

        List<SuggestionEntry> entries = engine.prefixSearch(prefix, limit);
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }

        List<RecallResult> results = new ArrayList<>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            results.add(new RecallResult(entries.get(i), i + 1));
        }
        return results;
    }
}
