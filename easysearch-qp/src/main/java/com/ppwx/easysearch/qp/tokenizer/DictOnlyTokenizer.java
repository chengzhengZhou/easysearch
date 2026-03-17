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
 * 仅词典分词器：只输出词典中存在的词，未在词典中的字符不输出任何 token（丢弃）。
 * <p>
 * 采用正向最长匹配，命中则输出一词并前进词长，未命中则仅前进一字符。适合与
 * {@link DictOverrideCompositeTokenizer} 配合：词典只提供需覆盖的区间，其余由 CRF 填缝。
 * </p>
 * <p>
 * 词典格式与 {@link DictTokenizer} 一致，支持工厂方法 {@link #fromPath(String)}、
 * {@link #fromSource(TextLineSource)}、{@link #fromStream(InputStream)} 加载。
 * </p>
 */
public class DictOnlyTokenizer implements Tokenizer {

    private volatile BinTrie<CoreDictionary.Attribute> dictionary;
    private volatile int dictionaryEntryCount;

    /** 默认最大词长（字符数） */
    public static final int DEFAULT_MAX_WORD_LEN = 20;
    private static final String DEFAULT_OOV_POS = "NN";

    public DictOnlyTokenizer() {
        this.dictionary = new BinTrie<>();
        this.dictionaryEntryCount = 0;
    }

    private DictOnlyTokenizer(BinTrie<CoreDictionary.Attribute> dictionary, int entryCount) {
        this.dictionary = dictionary != null ? dictionary : new BinTrie<>();
        this.dictionaryEntryCount = Math.max(entryCount, 0);
    }

    /**
     * 仅词典分词：正向最长匹配，只输出词典中的词，未命中则丢弃（不输出 token）。
     *
     * @param text 输入文本
     * @return 仅包含词典命中的 token 列表，不覆盖全文
     */
    @Override
    public List<Token> tokenize(String text) {
        if (text == null || text.isEmpty() || dictionaryEntryCount == 0) {
            return Collections.emptyList();
        }
        return forwardLongestMatchOnly(text, dictionary);
    }

    /**
     * 正向最长匹配：仅输出词典命中的词，未命中则前进一字符不输出。
     */
    private List<Token> forwardLongestMatchOnly(String text, BinTrie<CoreDictionary.Attribute> dict) {
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
                start += 1;
            }
        }
        return result;
    }

    public void load(String path) throws IOException {
        load(new PathTextLineSource(path));
    }

    public void load(TextLineSource source) throws IOException {
        try (InputStream is = source.openStream()) {
            load(is);
        }
    }

    /**
     * 从输入流加载词典。格式：每行一个词，或 TSV「词\t词性」；空行和 # 开头行忽略。
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

    public static DictOnlyTokenizer fromPath(String path) throws IOException {
        return fromSource(new PathTextLineSource(path));
    }

    public static DictOnlyTokenizer fromSource(TextLineSource source) throws IOException {
        try (InputStream is = source.openStream()) {
            return fromStream(is);
        }
    }

    public static DictOnlyTokenizer fromStream(InputStream inputStream) {
        DictOnlyTokenizer tokenizer = new DictOnlyTokenizer();
        tokenizer.load(inputStream);
        return tokenizer;
    }

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

    public BinTrie<CoreDictionary.Attribute> getDictionary() {
        return dictionary;
    }
}
