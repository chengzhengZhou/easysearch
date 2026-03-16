package com.ppwx.easysearch.qp.intervention;

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
 * 干预词：基于词表的改写引擎。
 * <p>
 * 格式约定（制表符分隔）：
 * 源词 \t 目标词 \t 优先级
 * - 源词：在 query 中匹配的片段
 * - 目标词：用于替换的文本
 * - 优先级：可选，默认为 0；同一源词多条规则时，仅保留优先级最高的一条
 */
public class TermInterventionEngine {

    public static final int DEFAULT_MAX_WORD_LEN = 20;

    private volatile BinTrie<TermAttribute> trie;
    private final int maxWordLen;

    public TermInterventionEngine() {
        this(DEFAULT_MAX_WORD_LEN);
    }

    public TermInterventionEngine(int maxWordLen) {
        this.maxWordLen = Math.max(1, maxWordLen);
    }

    /**
     * 从路径加载干预词表。支持 classpath 资源路径和文件系统路径。
     */
    public void load(String path) throws IOException {
        load(new PathTextLineSource(path));
    }

    /**
     * 从统一资源源加载干预词表（如文件、数据库等）。
     */
    public void load(TextLineSource source) throws IOException {
        try (InputStream is = source.openStream()) {
            load(is);
        }
    }

    /**
     * 从输入流加载。格式：源词 \t 目标词 \t 优先级
     */
    public void load(InputStream inputStream) {
        BinTrie<TermAttribute> newTrie = new BinTrie<>();
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
            throw new RuntimeException("Failed to load term intervention dictionary", e);
        }
        this.trie = newTrie;
    }

    private static void parseAndPut(BinTrie<TermAttribute> trie, String line) {
        String[] parts = line.split("\t", -1);
        if (parts.length < 2) {
            return;
        }
        String source = parts[0].trim();
        String target = parts[1].trim();
        if (source.isEmpty() || target.isEmpty()) {
            return;
        }

        int priority = 0;
        if (parts.length >= 3 && StringUtils.isNotBlank(parts[2])) {
            try {
                priority = Integer.parseInt(parts[2].trim());
            } catch (NumberFormatException ignore) {
                // 使用默认优先级 0
            }
        }

        TermAttribute attr = new TermAttribute(target, priority);
        TermAttribute existing = trie.get(source);
        if (existing == null || priority > existing.getPriority()) {
            trie.put(source, attr);
        }
    }

    /**
     * 在 query 上做最长匹配，返回不重叠的匹配列表（按起始位置有序）。
     */
    public List<TermMatch> match(String query) {
        if (query == null || query.isEmpty()) {
            return Collections.emptyList();
        }
        BinTrie<TermAttribute> dict = trie;
        if (dict == null) {
            return Collections.emptyList();
        }

        List<TermMatch> result = new ArrayList<>();
        int len = query.length();
        int start = 0;

        while (start < len) {
            int tryLen = Math.min(maxWordLen, len - start);
            int end = -1;
            String matchedWord = null;
            TermAttribute attr = null;

            for (int l = tryLen; l >= 1; l--) {
                String word = query.substring(start, start + l);
                BaseNode<TermAttribute> node = dict.transition(word, 0);
                if (node != null && node.getValue() != null) {
                    end = start + l;
                    matchedWord = word;
                    attr = node.getValue();
                    break;
                }
            }

            if (end > start && matchedWord != null && attr != null) {
                result.add(new TermMatch(start, end, matchedWord, attr));
                start = end;
            } else {
                start += 1;
            }
        }

        return result;
    }

    /**
     * 使用词表对 query 进行改写：替换所有命中的干预词。
     */
    public String rewrite(String query) {
        if (query == null) {
            return null;
        }
        List<TermMatch> matches = match(query);
        if (matches.isEmpty()) {
            return query;
        }

        StringBuilder sb = new StringBuilder(query.length());
        int cursor = 0;
        for (TermMatch match : matches) {
            if (match.getStartIndex() > cursor) {
                sb.append(query, cursor, match.getStartIndex());
            }
            sb.append(match.getAttribute().getTarget());
            cursor = match.getEndIndex();
        }
        if (cursor < query.length()) {
            sb.append(query, cursor, query.length());
        }
        return sb.toString();
    }

    /** 当前是否已加载词表 */
    public boolean isLoaded() {
        return trie != null;
    }

    /**
     * 词表条目属性：目标词 + 优先级。
     */
    static class TermAttribute {
        private final String target;
        private final int priority;

        TermAttribute(String target, int priority) {
            this.target = target;
            this.priority = priority;
        }

        String getTarget() {
            return target;
        }

        int getPriority() {
            return priority;
        }
    }

    /**
     * 匹配结果：在 query 中的 span 及对应的属性。
     */
    static class TermMatch {
        private final int startIndex;
        private final int endIndex;
        private final String source;
        private final TermAttribute attribute;

        TermMatch(int startIndex, int endIndex, String source, TermAttribute attribute) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.source = source;
            this.attribute = attribute;
        }

        int getStartIndex() {
            return startIndex;
        }

        int getEndIndex() {
            return endIndex;
        }

        String getSource() {
            return source;
        }

        TermAttribute getAttribute() {
            return attribute;
        }
    }
}

