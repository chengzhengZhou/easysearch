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

package com.ppwx.easysearch.qp.ner.recognizer;

import com.hankcs.hanlp.collection.trie.bintrie.BaseNode;
import com.hankcs.hanlp.collection.trie.bintrie.BinTrie;
import com.ppwx.easysearch.qp.ner.Entity;
import com.ppwx.easysearch.qp.ner.EntityRecognizer;
import com.ppwx.easysearch.qp.ner.EntityType;
import com.ppwx.easysearch.qp.source.PathTextLineSource;
import com.ppwx.easysearch.qp.source.TextLineSource;
import com.ppwx.easysearch.qp.tokenizer.Token;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 基于 HanLP BinTrie 的自定义词典实体识别器
 * 以词为维度进行状态转移，支持多词实体与最长匹配。
 * <p>
 * 词典按实例隔离，请使用工厂方法 {@link #fromPath(String)}、{@link #fromSource(TextLineSource)}、
 * {@link #fromStream(InputStream)} 加载资源并创建实例。
 * </p>
 */
public class DictEntityRecognizer implements EntityRecognizer {

    private volatile BinTrie<DictEntityLine> dictionary;

    /**
     * 无参构造：使用空词典。需要词典时请使用工厂方法创建实例。
     */
    public DictEntityRecognizer() {
        this.dictionary = new BinTrie<>();
    }

    /**
     * 内部构造：由工厂方法传入已加载的词典。
     */
    private DictEntityRecognizer(BinTrie<DictEntityLine> dictionary) {
        this.dictionary = dictionary != null ? dictionary : new BinTrie<>();
    }

    /**
     * 识别实体
     *
     * @param originText 原始文本
     * @param tokens     分词结果
     * @return 识别到的实体
     */
    @Override
    public Collection<Entity> extractEntities(String originText, List<Token> tokens) {
        if (originText == null || tokens == null || tokens.isEmpty()) {
            return Collections.emptyList();
        }
        BinTrie<DictEntityLine> dict = this.dictionary;

        String[] wordNet = new String[tokens.size()];
        for (int i = 0; i < tokens.size(); i++) {
            wordNet[i] = tokens.get(i).getText();
        }

        List<Entity> entities = new ArrayList<>();
        int length = wordNet.length;

        for (int i = 0; i < length; i++) {
            if (wordNet[i] == null || wordNet[i].isEmpty()) {
                continue;
            }
            BaseNode<DictEntityLine> state = dict.transition(wordNet[i], 0);
            if (state == null) {
                continue;
            }

            int end = i + 1;
            DictEntityLine value = state.getValue();
            for (int to = i + 1; to < length; to++) {
                if (wordNet[to] == null || wordNet[to].isEmpty()) {
                    continue;
                }
                state = state.transition(wordNet[to], 0);
                if (state == null) {
                    break;
                }
                if (state.getValue() != null) {
                    value = state.getValue();
                    end = to + 1;
                }
            }

            if (value != null) {
                Token startToken = tokens.get(i);
                Token endToken = tokens.get(end - 1);
                int startOffset = startToken.getStartIndex();
                int endOffset = endToken.getEndIndex();
                String entityValue = originText.substring(
                        Math.min(startOffset, originText.length()),
                        Math.min(endOffset, originText.length()));

                Entity entity = new Entity(
                        entityValue,
                        value.getType(),
                        value.getNormalizedValue() != null ? value.getNormalizedValue() : entityValue,
                        1.0,
                        startOffset,
                        endOffset
                );
                entity.setAttachment(value);
                if (value.getId() != null) {
                    entity.setId(value.getId());
                }
                entities.add(entity);
                i = end - 1;
            }
        }
        return entities;
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
     * 从输入流加载词典并创建识别器实例。
     * 支持格式：
     * 1. JSONL：每行一个 JSON 对象，如 {"entity":"iPhone 15 Pro","type":"MODEL","attributes":{...}}
     * 2. TSV：entity\ttype[\\tnormalizedValue]
     *
     * @param inputStream 输入流
     */
    public void load(InputStream inputStream) {
        BinTrie<DictEntityLine> trie = new BinTrie<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                parseAndPut(trie, line);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load custom dictionary", e);
        }
        this.dictionary = trie;
    }

    // --------------- 工厂方法：从资源加载并创建实例 ---------------

    /**
     * 从路径加载词典并创建识别器实例。支持 classpath 资源路径和文件系统路径。
     *
     * @param path 资源路径（classpath 或文件路径）
     * @return 已加载该词典的 DictEntityRecognizer 实例
     */
    public static DictEntityRecognizer fromPath(String path) throws IOException {
        return fromSource(new PathTextLineSource(path));
    }

    /**
     * 从统一资源源加载词典并创建识别器实例（如文件、数据库等）。
     *
     * @param source 文本行资源
     * @return 已加载该词典的 DictEntityRecognizer 实例
     */
    public static DictEntityRecognizer fromSource(TextLineSource source) throws IOException {
        try (InputStream is = source.openStream()) {
            return fromStream(is);
        }
    }

    /**
     * 从输入流加载词典并创建识别器实例。
     * 支持格式：
     * 1. JSONL：每行一个 JSON 对象，如 {"entity":"iPhone 15 Pro","type":"MODEL","attributes":{...}}
     * 2. TSV：entity\ttype[\\tnormalizedValue]
     *
     * @param inputStream 输入流
     * @return 已加载该词典的 DictEntityRecognizer 实例
     */
    public static DictEntityRecognizer fromStream(InputStream inputStream) {
        DictEntityRecognizer dictEntityRecognizer = new DictEntityRecognizer();
        dictEntityRecognizer.load(inputStream);
        return dictEntityRecognizer;
    }

    private static void parseAndPut(BinTrie<DictEntityLine> trie, String line) {
        line = line.trim();
        if (line.isEmpty()) {
            return;
        }
        if (line.startsWith("{")) {
            parseJsonLine(trie, line);
        } else {
            parseTsvLine(trie, line);
        }
    }

    private static void parseJsonLine(BinTrie<DictEntityLine> trie, String line) {
        DictEntityLineParser.parseLine(line).ifPresent(record -> {
            putEntity(trie, record.getEntity(), record);
            if (record.getAliases() != null) {
                for (String alias : record.getAliases()) {
                    if (StringUtils.isNotBlank(alias)) {
                        putEntity(trie, alias, record);
                    }
                }
            }
        });
    }

    private static void parseTsvLine(BinTrie<DictEntityLine> trie, String line) {
        String[] parts = line.split("\t");
        if (parts.length < 2) {
            return;
        }
        String entity = parts[0].trim();
        String typeStr = parts[1].trim();
        if (entity.isEmpty() || typeStr.isEmpty()) {
            return;
        }
        EntityType type = EntityType.getByName(typeStr);
        BaseDictEntityLine attr = new BaseDictEntityLine();
        attr.setEntity(entity);
        attr.setType(type);
        if (parts.length >= 3) {
            attr.setNormalizedValue(parts[2].trim());
        } else {
            attr.setNormalizedValue(entity);
        }
        putEntity(trie, entity, attr);
    }

    private static void putEntity(BinTrie<DictEntityLine> trie, String entity, DictEntityLine attr) {
        String key = normalizeKey(entity);
        if (key != null && !key.isEmpty()) {
            trie.put(key, attr);
        }
    }

    /**
     * 标准化词典 key：去除空格，用于与分词结果拼接后的字符序列匹配
     */
    private static String normalizeKey(String entity) {
        if (entity == null) {
            return null;
        }
        return entity.replaceAll("\\s+", "");
    }

    /**
     * 返回当前实例的词典（用于测试或调试）
     */
    public BinTrie<DictEntityLine> getDictionary() {
        return dictionary;
    }

}
