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

package com.ppwx.easysearch.qp.synonym;

import com.hankcs.hanlp.collection.trie.bintrie.BaseNode;
import com.hankcs.hanlp.collection.trie.bintrie.BinTrie;
import com.ppwx.easysearch.qp.source.PathTextLineSource;
import com.ppwx.easysearch.qp.source.TextLineSource;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 同义词引擎：基于 HanLP BinTrie 的匹配结构，从简化 txt 加载，支持最长匹配。
 * <p>
 * txt 格式：源词 \t 方向(=>|<=|SYM) \t 目标1,目标2,... ；空行和 # 开头行忽略。
 */
public class SynonymEngine {

    public static final int DEFAULT_MAX_WORD_LEN = 20;

    private volatile BinTrie<SynonymAttribute> trie;
    private final int maxWordLen;

    public SynonymEngine() {
        this(DEFAULT_MAX_WORD_LEN);
    }

    public SynonymEngine(int maxWordLen) {
        this.maxWordLen = Math.max(1, maxWordLen);
    }

    /**
     * 从路径加载同义词表。支持 classpath 资源路径和文件系统路径。
     */
    public void load(String path) throws IOException {
        load(new PathTextLineSource(path));
    }

    /**
     * 从统一资源源加载同义词表（如文件、数据库等）。
     */
    public void load(TextLineSource source) throws IOException {
        try (InputStream is = source.openStream()) {
            load(is);
        }
    }

    /**
     * 从输入流加载。格式：源词 \t 方向 \t 目标1,目标2,...
     */
    public void load(InputStream inputStream) {
        BinTrie<SynonymAttribute> newTrie = new BinTrie<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                parseAndPut(newTrie, line);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load synonym dictionary", e);
        }
        this.trie = newTrie;
    }

    private static void parseAndPut(BinTrie<SynonymAttribute> trie, String line) {
        String[] parts = line.split("\t", -1);
        if (parts.length < 3) {
            return;
        }
        String source = parts[0].trim();
        String direction = parts[1].trim();
        String targetsStr = parts[2].trim();
        if (source.isEmpty()) {
            return;
        }

        SynonymType type;
        switch (direction) {
            case "SYM":
                type = SynonymType.BIDIRECTIONAL;
                break;
            case "=>":
            case "<=":
            default:
                type = SynonymType.UNIDIRECTIONAL;
                break;
        }

        List<String> targets = splitTargets(targetsStr);
        SynonymAttribute attr = new SynonymAttribute(targets, type);
        trie.put(source, attr);

        if (type == SynonymType.BIDIRECTIONAL && !targets.isEmpty()) {
            for (String t : targets) {
                if (StringUtils.isNotBlank(t) && !t.equals(source)) {
                    trie.put(t.trim(), new SynonymAttribute(Collections.singletonList(source), type));
                }
            }
        }
    }

    private static List<String> splitTargets(String targetsStr) {
        if (StringUtils.isBlank(targetsStr)) {
            return Collections.emptyList();
        }
        String[] arr = targetsStr.split(",");
        List<String> list = new ArrayList<>(arr.length);
        for (String s : arr) {
            String t = s.trim();
            if (!t.isEmpty()) {
                list.add(t);
            }
        }
        return list;
    }

    /**
     * 在 query 上做最长匹配，返回不重叠的匹配列表（按起始位置有序）。
     */
    public List<SynonymMatch> match(String query) {
        if (query == null || query.isEmpty()) {
            return Collections.emptyList();
        }
        BinTrie<SynonymAttribute> dict = trie;
        if (dict == null) {
            return Collections.emptyList();
        }

        List<SynonymMatch> result = new ArrayList<>();
        int len = query.length();
        int start = 0;

        while (start < len) {
            int tryLen = Math.min(maxWordLen, len - start);
            int end = -1;
            String matchedWord = null;
            SynonymAttribute attr = null;

            for (int l = tryLen; l >= 1; l--) {
                String word = query.substring(start, start + l);
                BaseNode<SynonymAttribute> node = dict.transition(word, 0);
                if (node != null && node.getValue() != null) {
                    end = start + l;
                    matchedWord = word;
                    attr = node.getValue();
                    break;
                }
            }

            if (end > start && matchedWord != null && attr != null) {
                result.add(new SynonymMatch(start, end, matchedWord, attr));
                start = end;
            } else {
                start += 1;
            }
        }

        return result;
    }

    /** 当前是否已加载词表 */
    public boolean isLoaded() {
        return trie != null;
    }
}
