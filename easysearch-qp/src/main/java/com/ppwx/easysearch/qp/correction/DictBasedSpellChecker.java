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

package com.ppwx.easysearch.qp.correction;

import com.hankcs.hanlp.collection.trie.bintrie.BaseNode;
import com.hankcs.hanlp.collection.trie.bintrie.BinTrie;
import com.ppwx.easysearch.qp.source.PathTextLineSource;
import com.ppwx.easysearch.qp.source.TextLineSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 基于词典的拼写纠错引擎。
 * <p>
 * 核心流程：加载词典 → 构建 Trie + 拼音索引 + BK-Tree →
 * 对输入 query 做错误检测 → 候选生成 → 多信号打分 → 输出纠错结果。
 * <p>
 * 线程安全：各索引通过 volatile 引用保证重载时的线程可见性。
 */
public class DictBasedSpellChecker {

    private static final Logger log = LoggerFactory.getLogger(DictBasedSpellChecker.class);

    /** 用于快速判断某词是否在词典中 */
    private volatile BinTrie<Boolean> dictTrie;

    /** 拼音倒排索引 */
    private volatile PinyinIndex pinyinIndex;

    /** BK-Tree 编辑距离索引 */
    private volatile EditDistanceCandidateGenerator editDistanceGenerator;

    /** 候选打分器 */
    private volatile CandidateScorer scorer;

    /** 词典条目列表（保留用于 rebuild） */
    private volatile List<DictEntry> dictionary;

    /** 配置 */
    private final CorrectionConfig config;

    public DictBasedSpellChecker() {
        this(CorrectionConfig.defaults());
    }

    public DictBasedSpellChecker(CorrectionConfig config) {
        this.config = config;
    }

    // ==================== 词典加载 ====================

    /**
     * 从路径加载纠错词典。支持 classpath 资源路径和文件系统路径。
     * <p>
     * 词典格式（TSV）：每行一个词，可选附带词频（tab 分隔）。
     * <pre>
     * # 拼音纠错词典
     * 手机壳	9800
     * 华为	15000
     * </pre>
     *
     * @param path 词典文件路径
     */
    public void load(String path) throws IOException {
        load(new PathTextLineSource(path));
    }

    /**
     * 从统一资源源加载纠错词典。
     */
    public void load(TextLineSource source) throws IOException {
        try (InputStream is = source.openStream()) {
            load(is);
        }
    }

    /**
     * 从输入流加载纠错词典。
     */
    public void load(InputStream inputStream) {
        List<DictEntry> entries = new ArrayList<>();
        long maxFreq = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("\t", -1);
                String word = parts[0].trim();
                if (word.isEmpty()) {
                    continue;
                }
                long freq = 1L;
                if (parts.length >= 2) {
                    try {
                        freq = Long.parseLong(parts[1].trim());
                    } catch (NumberFormatException ignore) {
                        // 使用默认词频 1
                    }
                }
                if (freq > maxFreq) {
                    maxFreq = freq;
                }
                entries.add(new DictEntry(word, null, freq));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load spell correction dictionary", e);
        }

        buildIndex(entries, maxFreq);
    }

    /**
     * 直接从词条列表构建索引（用于测试或程序化加载）。
     *
     * @param entries 词条列表
     */
    public void loadEntries(List<DictEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            log.warn("Empty dictionary provided, spell correction will be disabled");
            return;
        }
        long maxFreq = 0;
        for (DictEntry entry : entries) {
            if (entry.getFrequency() > maxFreq) {
                maxFreq = entry.getFrequency();
            }
        }
        buildIndex(entries, maxFreq);
    }

    private void buildIndex(List<DictEntry> entries, long maxFreq) {
        // 1. 构建 BinTrie（用于词典覆盖率检测）
        BinTrie<Boolean> newTrie = new BinTrie<>();
        for (DictEntry entry : entries) {
            newTrie.put(entry.getWord(), Boolean.TRUE);
        }

        // 2. 构建拼音倒排索引
        PinyinIndex newPinyinIndex = new PinyinIndex();
        newPinyinIndex.build(entries);

        // 3. 构建 BK-Tree 编辑距离索引
        EditDistanceCandidateGenerator newEditGen = new EditDistanceCandidateGenerator(
                config.getMaxEditCandidates(), config.getMaxEditDistance());
        newEditGen.buildIndex(entries);

        // 4. 初始化打分器
        CandidateScorer newScorer = new CandidateScorer(config);
        newScorer.setMaxFrequency(maxFreq);

        // 5. 创建拼音候选生成器
        // (延迟创建，在 check 方法中使用)

        // 原子替换所有引用
        this.dictTrie = newTrie;
        this.pinyinIndex = newPinyinIndex;
        this.editDistanceGenerator = newEditGen;
        this.scorer = newScorer;
        this.dictionary = Collections.unmodifiableList(new ArrayList<>(entries));

        log.info("Spell correction dictionary loaded, entries: {}", entries.size());
    }

    // ==================== 纠错检查 ====================

    /**
     * 对输入 query 执行拼写纠错检查。
     * <p>
     * 流程：错误检测 → 候选生成（拼音 + 编辑距离） → 多信号打分 → 置信度判断
     *
     * @param query 输入 query
     * @return 纠错结果
     */
    public CorrectionResult check(String query) {
        if (query == null || query.isEmpty() || dictTrie == null) {
            return CorrectionResult.noCorrection(query);
        }

        // 错误检测：将 query 做最长匹配切分，找出未被词典覆盖的片段
        List<Span> errorSpans = detectErrors(query);
        if (errorSpans.isEmpty()) {
            return CorrectionResult.noCorrection(query);
        }

        // 对每个疑似错误片段生成候选并打分
        List<Correction> corrections = new ArrayList<>();
        StringBuilder correctedQuery = new StringBuilder(query);

        // 从后往前替换，避免位置偏移
        // 先排序（按起始位置降序）
        errorSpans.sort((a, b) -> Integer.compare(b.start, a.start));

        for (Span span : errorSpans) {
            String term = query.substring(span.start, span.end);
            Candidate best = findBestCandidate(term);

            if (best != null && best.getTotalScore() >= config.getLowThreshold()) {
                boolean autoCorrect = best.getTotalScore() >= config.getHighThreshold();
                List<Candidate> allCandidates = generateAndScoreCandidates(term);

                corrections.add(new Correction(
                        term, best.getText(),
                        span.start, span.end,
                        best.getTotalScore(),
                        allCandidates
                ));

                if (autoCorrect) {
                    correctedQuery.replace(span.start, span.end, best.getText());
                }
            }
        }

        if (corrections.isEmpty()) {
            return CorrectionResult.noCorrection(query);
        }

        // 按起始位置升序排列 corrections
        corrections.sort((a, b) -> Integer.compare(a.getStartIndex(), b.getStartIndex()));

        // 计算整体置信度（取所有纠正置信度的平均值）
        double avgConfidence = corrections.stream()
                .mapToDouble(Correction::getConfidence)
                .average()
                .orElse(0.0);

        boolean anyAutoCorrect = corrections.stream()
                .anyMatch(c -> c.getConfidence() >= config.getHighThreshold());

        return new CorrectionResult(
                query,
                anyAutoCorrect ? correctedQuery.toString() : query,
                avgConfidence,
                corrections,
                anyAutoCorrect
        );
    }

    /**
     * 错误检测：通过 Trie 最长匹配找出 query 中未被词典覆盖的连续片段。
     */
    private List<Span> detectErrors(String query) {
        BinTrie<Boolean> trie = this.dictTrie;
        if (trie == null) {
            return Collections.emptyList();
        }

        List<Span> errors = new ArrayList<>();
        int len = query.length();
        int pos = 0;

        while (pos < len) {
            char ch = query.charAt(pos);
            // 跳过空格和标点等非汉字字符
            if (Character.isWhitespace(ch) || !isChineseOrCommon(ch)) {
                pos++;
                continue;
            }

            // 尝试最长匹配
            int matchedEnd = -1;
            int tryLen = Math.min(8, len - pos); // 限制最大匹配长度
            for (int l = tryLen; l >= 1; l--) {
                String word = query.substring(pos, pos + l);
                BaseNode<Boolean> node = trie.transition(word, 0);
                if (node != null && node.getValue() != null) {
                    matchedEnd = pos + l;
                    break;
                }
            }

            if (matchedEnd > pos) {
                // 匹配到词典中的词，跳过
                pos = matchedEnd;
            } else {
                // 未匹配，记录未被覆盖的连续片段
                int errorStart = pos;
                pos++;
                // 继续收集连续未匹配的汉字
                while (pos < len && isChineseOrCommon(query.charAt(pos))) {
                    // 再次尝试匹配
                    int newMatchedEnd = -1;
                    tryLen = Math.min(8, len - pos);
                    for (int l = tryLen; l >= 1; l--) {
                        String word = query.substring(pos, pos + l);
                        BaseNode<Boolean> node = trie.transition(word, 0);
                        if (node != null && node.getValue() != null) {
                            newMatchedEnd = pos + l;
                            break;
                        }
                    }
                    if (newMatchedEnd > pos) {
                        break; // 找到匹配，终止错误片段
                    }
                    pos++;
                }
                errors.add(new Span(errorStart, pos));
            }
        }

        return errors;
    }

    /**
     * 判断字符是否为汉字或常见的搜索字符（字母、数字）。
     */
    private boolean isChineseOrCommon(char ch) {
        return (ch >= '\u4E00' && ch <= '\u9FA5')
                || (ch >= 'a' && ch <= 'z')
                || (ch >= 'A' && ch <= 'Z')
                || (ch >= '0' && ch <= '9');
    }

    /**
     * 为输入片段生成所有候选并打分排序，返回 Top-K。
     */
    private List<Candidate> generateAndScoreCandidates(String term) {
        CandidateScorer currentScorer = this.scorer;
        if (currentScorer == null) {
            return Collections.emptyList();
        }

        PinyinIndex currentPinyinIndex = this.pinyinIndex;
        EditDistanceCandidateGenerator currentEditGen = this.editDistanceGenerator;

        // 生成拼音候选
        List<DictEntry> pinyinCandidates = Collections.emptyList();
        if (currentPinyinIndex != null) {
            PinyinCandidateGenerator pinyinGen = new PinyinCandidateGenerator(
                    currentPinyinIndex, config.getMaxPinyinCandidates());
            pinyinCandidates = pinyinGen.generate(term);
        }

        // 生成编辑距离候选
        List<DictEntry> editCandidates = Collections.emptyList();
        if (currentEditGen != null) {
            editCandidates = currentEditGen.generate(term);
        }

        // 合并去重
        Set<String> seen = new HashSet<>();
        List<DictEntry> allEntries = new ArrayList<>();
        for (DictEntry e : pinyinCandidates) {
            if (seen.add(e.getWord())) {
                allEntries.add(e);
            }
        }
        for (DictEntry e : editCandidates) {
            if (seen.add(e.getWord())) {
                allEntries.add(e);
            }
        }

        // 打分排序
        List<Candidate> scored = new ArrayList<>();
        for (DictEntry entry : allEntries) {
            scored.add(currentScorer.score(term, entry));
        }
        Collections.sort(scored);

        return scored;
    }

    /**
     * 找到最佳候选（得分最高的）。
     */
    private Candidate findBestCandidate(String term) {
        List<Candidate> candidates = generateAndScoreCandidates(term);
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    public boolean isLoaded() {
        return dictTrie != null;
    }

    /**
     * 位置区间。
     */
    private static class Span {
        final int start;
        final int end;

        Span(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }
}
