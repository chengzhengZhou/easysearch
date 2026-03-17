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

package com.ppwx.easysearch.qp.tokenizer;

import com.hankcs.hanlp.collection.trie.bintrie.BaseNode;
import com.hankcs.hanlp.collection.trie.bintrie.BinTrie;
import com.hankcs.hanlp.corpus.tag.Nature;
import com.hankcs.hanlp.dictionary.CoreDictionary;
import com.ppwx.easysearch.qp.source.PathTextLineSource;
import com.ppwx.easysearch.qp.source.TextLineSource;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 词典分词器：基于 BinTrie 前缀树，从词典文件加载，支持双向最长匹配。
 * <p>
 * 词典按实例隔离，请使用工厂方法 {@link #fromPath(String)}、{@link #fromSource(TextLineSource)}、
 * {@link #fromStream(InputStream)} 加载资源并创建实例。
 * </p>
 */
public class DictTokenizer implements Tokenizer {

    private volatile BinTrie<CoreDictionary.Attribute> dictionary;
    /** 词典条目数，0 表示空词典（无参构造或加载了空文件），用于快速判断是否返回空结果 */
    private volatile int dictionaryEntryCount;

    /** 默认最大词长（字符数） */
    public static final int DEFAULT_MAX_WORD_LEN = 20;
    /** 未登录词默认词性 */
    private static final String DEFAULT_OOV_POS = "NN";

    /**
     * 无参构造：使用空词典。需要词典时请使用工厂方法创建实例。
     */
    public DictTokenizer() {
        this.dictionary = new BinTrie<>();
        this.dictionaryEntryCount = 0;
    }

    /**
     * 内部构造：由工厂方法传入已加载的词典及条目数。
     */
    private DictTokenizer(BinTrie<CoreDictionary.Attribute> dictionary, int entryCount) {
        this.dictionary = dictionary != null ? dictionary : new BinTrie<>();
        this.dictionaryEntryCount = Math.max(entryCount, 0);
    }

    /**
     * 词典分词：双向最长匹配。
     * 先做正向、反向最长匹配，再按「词数少者优先」选取结果；词数相同时取正向结果。
     *
     * @param text 输入文本
     * @return 分词结果
     */
    @Override
    public List<Token> tokenize(String text) {
        if (text == null) {
            return Collections.emptyList();
        }
        if (text.isEmpty()) {
            return Collections.emptyList();
        }
        if (dictionaryEntryCount == 0) {
            return Collections.emptyList();
        }

        List<Token> forward = forwardLongestMatch(text, dictionary);
        List<Token> backward = backwardLongestMatch(text, dictionary);

        if (backward.size() < forward.size()) {
            return backward;
        }
        return forward;
    }

    /**
     * 正向最长匹配：从左到右扫描，每次取当前起点的最长词。
     */
    private List<Token> forwardLongestMatch(String text, BinTrie<CoreDictionary.Attribute> dict) {
        List<Token> result = new ArrayList<>();
        int len = text.length();
        int maxLen = Math.min(DEFAULT_MAX_WORD_LEN, len);
        int start = 0;

        while (start < len) {
            int end = -1;
            CoreDictionary.Attribute attr = null;
            int tryLen = Math.min(maxLen, len - start);
            for (int l = tryLen; l >= 1; l--) {
                String word = text.substring(start, start + l);
                BaseNode<CoreDictionary.Attribute> node = dict.transition(word, 0);
                if (node != null && node.getValue() != null) {
                    end = start + l;
                    attr = node.getValue();
                    break;
                }
            }
            if (end > start && attr != null) {
                String typeStr = getNatureString(attr);
                result.add(Token.builder()
                        .text(text.substring(start, end))
                        .type(typeStr)
                        .startIndex(start)
                        .endIndex(end)
                        .confidence(1.0)
                        .build());
                start = end;
            } else {
                result.add(Token.builder()
                        .text(text.substring(start, start + 1))
                        .type(DEFAULT_OOV_POS)
                        .startIndex(start)
                        .endIndex(start + 1)
                        .confidence(0.5)
                        .build());
                start += 1;
            }
        }
        return result;
    }

    /**
     * 反向最长匹配：从右到左扫描，每次取当前终点的最长词（即从右向左延伸的最长词）。
     */
    private List<Token> backwardLongestMatch(String text, BinTrie<CoreDictionary.Attribute> dict) {
        List<Token> result = new ArrayList<>();
        int len = text.length();
        int maxLen = Math.min(DEFAULT_MAX_WORD_LEN, len);
        int pos = len;

        while (pos > 0) {
            int start = -1;
            CoreDictionary.Attribute attr = null;
            int tryLen = Math.min(maxLen, pos);
            for (int l = tryLen; l >= 1; l--) {
                int s = pos - l;
                String word = text.substring(s, pos);
                BaseNode<CoreDictionary.Attribute> node = dict.transition(word, 0);
                if (node != null && node.getValue() != null) {
                    start = s;
                    attr = node.getValue();
                    break;
                }
            }
            if (start >= 0 && attr != null) {
                String typeStr = getNatureString(attr);
                result.add(0, Token.builder()
                        .text(text.substring(start, pos))
                        .type(typeStr)
                        .startIndex(start)
                        .endIndex(pos)
                        .confidence(1.0)
                        .build());
                pos = start;
            } else {
                result.add(0, Token.builder()
                        .text(text.substring(pos - 1, pos))
                        .type(DEFAULT_OOV_POS)
                        .startIndex(pos - 1)
                        .endIndex(pos)
                        .confidence(0.5)
                        .build());
                pos -= 1;
            }
        }
        return result;
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
     * 从输入流加载词典并创建分词器实例。
     * 格式：每行一个词，或 TSV「词\t词性」；空行和 # 开头行忽略。
     *
     * @param inputStream 输入流（UTF-8）
     * @return 已加载该词典的 DictTokenizer 实例
     */
    public void load(InputStream inputStream) {
        BinTrie<CoreDictionary.Attribute> trie = new BinTrie<>();
        int entryCount = 0;
        try (InputStreamReader isr = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            java.io.BufferedReader reader = new java.io.BufferedReader(isr);
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (parseAndPut(trie, line)) {
                    entryCount++;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load custom dictionary", e);
        }
        this.dictionary = trie;
        this.dictionaryEntryCount = entryCount;
    }

    // --------------- 工厂方法：从资源加载并创建实例 ---------------

    /**
     * 从路径加载词典并创建分词器实例。支持 classpath 资源路径和文件系统路径。
     *
     * @param path 资源路径（classpath 或文件路径）
     * @return 已加载该词典的 DictTokenizer 实例
     */
    public static DictTokenizer fromPath(String path) throws IOException {
        return fromSource(new PathTextLineSource(path));
    }

    /**
     * 从统一资源源加载词典并创建分词器实例（如文件、数据库等）。
     *
     * @param source 文本行资源
     * @return 已加载该词典的 DictTokenizer 实例
     */
    public static DictTokenizer fromSource(TextLineSource source) throws IOException {
        try (InputStream is = source.openStream()) {
            return fromStream(is);
        }
    }

    /**
     * 从输入流加载词典并创建分词器实例。
     * 格式：每行一个词，或 TSV「词\t词性」；空行和 # 开头行忽略。
     *
     * @param inputStream 输入流（UTF-8）
     * @return 已加载该词典的 DictTokenizer 实例
     */
    public static DictTokenizer fromStream(InputStream inputStream) {
        DictTokenizer dictTokenizer = new DictTokenizer();
        dictTokenizer.load(inputStream);
        return dictTokenizer;
    }

    /** @return true 表示成功放入一条 */
    private static boolean parseAndPut(BinTrie<CoreDictionary.Attribute> trie, String line) {
        if (StringUtils.isBlank(line)) {
            return false;
        }
        String[] parts = line.split("\t");
        String word = parts[0].trim();
        if (word.isEmpty()) {
            return false;
        }
        String pos = parts.length >= 2 ? parts[1].trim() : DEFAULT_OOV_POS;
        if (pos.isEmpty()) {
            pos = DEFAULT_OOV_POS;
        }
        Nature nature = Nature.create(pos);
        trie.put(word, new CoreDictionary.Attribute(nature));
        return true;
    }

    /**
     * 从 CoreDictionary.Attribute 取得词性字符串
     */
    private static String getNatureString(CoreDictionary.Attribute attr) {
        if (attr == null) {
            return DEFAULT_OOV_POS;
        }
        try {
            return attr.nature != null ? attr.nature[0].toString() : DEFAULT_OOV_POS;
        } catch (Exception e) {
            return DEFAULT_OOV_POS;
        }
    }

    /**
     * 返回当前实例的词典（用于测试或调试）。
     */
    public BinTrie<CoreDictionary.Attribute> getDictionary() {
        return dictionary;
    }
}
