package com.ppwx.easysearch.qp.eval;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 分词、词性、实体识别评估
 *
 * <p>评估维度：
 * <ul>
 *   <li>分词（CWS）：基于字符边界（使用归一化偏移）比较 gold / pred 的分词边界集合</li>
 *   <li>词性（POS）：在同样的字符边界上比较 (span, POS) 是否一致</li>
 *   <li>实体（NER）：基于实体 span + 类型的严格匹配</li>
 * </ul>
 *
 * <p>输入源：
 * <ol>
 *   <li>原文本：每行一个文本</li>
 *   <li>gold 标注文件：人工标注（分词 + 词性 + 实体），格式见下</li>
 *   <li>模型输出文件：模型预测（分词 + 词性 + 实体），格式与 gold 相同</li>
 *   <li>全量词典：一行一个词，用于 IV/OOV 分析（可选）</li>
 * </ol>
 *
 * <p>标注格式：
 * <ol>
 *   <li>词语之间由空格分隔</li>
 *   <li>普通词：{@code 词语/词性}，例如：{@code 苹果/NN}</li>
 *   <li>复合词组成的实体用 {@code [多个词]/实体} 括起来，例如：
 *   {@code [iphone/NR /VV 16/NR]/MODEL}</li>
 * </ol>
 *
 * <p>评估指标：
 * <ul>
 *   <li>P（Precision）：TP / (TP + FP)</li>
 *   <li>R（Recall）：TP / (TP + FN)</li>
 *   <li>F1：2 * P * R / (P + R)</li>
 * </ul>
 *
 * <p>注意：为了让 gold / pred 使用同一套“字符坐标”，本实现对原始文本和 token 统一做
 * 归一化（去除所有空白并转小写），然后在归一化串上计算偏移，这样可以在不依赖具体空格形态的
 * 情况下比较分词与实体边界。
 */
public class CWSEvaluation {

    /**
     * 单个 token 信息：表面词、词性、在归一化串上的起止偏移，以及是否在词典中（IV/OOV 分析用）
     */
    static class Token {
        final String surface;
        final String pos;
        final int start;  // 在归一化串上的起始偏移（包含）
        final int end;    // 在归一化串上的结束偏移（不包含）
        final boolean inVocab;

        Token(String surface, String pos, int start, int end, boolean inVocab) {
            this.surface = surface;
            this.pos = pos;
            this.start = start;
            this.end = end;
            this.inVocab = inVocab;
        }
    }

    /**
     * 实体 span：基于 token 序号 + 归一化偏移 + 实体类型。
     */
    static class EntitySpan {
        final int startTokenIndex;
        final int endTokenIndex;
        final int start;  // 在归一化串上的起始偏移（包含）
        final int end;    // 在归一化串上的结束偏移（不包含）
        final String type;
        final boolean inVocab; // 内部所有 token 是否都在词典中

        EntitySpan(int startTokenIndex, int endTokenIndex, int start, int end, String type, boolean inVocab) {
            this.startTokenIndex = startTokenIndex;
            this.endTokenIndex = endTokenIndex;
            this.start = start;
            this.end = end;
            this.type = type;
            this.inVocab = inVocab;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EntitySpan)) return false;
            EntitySpan that = (EntitySpan) o;
            return start == that.start
                    && end == that.end
                    && Objects.equals(type, that.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(start, end, type);
        }

        public String getType() {
            return type;
        }
    }

    /**
     * 一行（一个查询）的标注信息：token 列表 + 实体列表。
     */
    static class SentenceAnnotation {
        final List<Token> tokens = new ArrayList<>();
        final List<EntitySpan> entities = new ArrayList<>();
        final String normalizedRaw; // 归一化后的原始文本，用于简单一致性检查

        SentenceAnnotation(String normalizedRaw) {
            this.normalizedRaw = normalizedRaw;
        }
    }

    /**
     * 评估计数器：TP、FP、FN。
     */
    static class Metrics {
        long tp = 0;
        long fp = 0;
        long fn = 0;

        void addTP(long v) { tp += v; }
        void addFP(long v) { fp += v; }
        void addFN(long v) { fn += v; }

        void merge(Metrics other) {
            if (other == null) return;
            tp += other.tp;
            fp += other.fp;
            fn += other.fn;
        }

        double precision() {
            long denom = tp + fp;
            return denom == 0 ? 0.0 : (double) tp / denom;
        }

        double recall() {
            long denom = tp + fn;
            return denom == 0 ? 0.0 : (double) tp / denom;
        }

        double f1() {
            double p = precision();
            double r = recall();
            if (p + r == 0) return 0.0;
            return 2 * p * r / (p + r);
        }

        @Override
        public String toString() {
            return String.format("P=%.4f, R=%.4f, F1=%.4f (TP=%d, FP=%d, FN=%d)",
                    precision(), recall(), f1(), tp, fp, fn);
        }
    }

    /**
     * 命令行入口示例（可根据需要调整文件路径或改为单元测试调用）。
     *
     * args:
     * 0: 原文本文件路径
     * 1: gold 标注文件路径
     * 2: 模型预测标注文件路径
     * 3: 词典文件路径（可选，没有则不做 OOV/IV 分析）
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println("Usage: <rawPath> <goldPath> <predPath> [<dictPath>]");
            System.exit(1);
        }
        Path rawPath = Paths.get(args[0]);
        Path goldPath = Paths.get(args[1]);
        Path predPath = Paths.get(args[2]);
        Path dictPath = args.length >= 4 ? Paths.get(args[3]) : null;

        EvaluationResult result = evaluate(rawPath, goldPath, predPath, dictPath);
        result.prettyPrint();
    }

    /**
     * 对给定的三个文件进行整体评估。
     */
    public static EvaluationResult evaluate(Path rawPath,
                                            Path goldPath,
                                            Path predPath,
                                            Path dictPath) throws IOException {
        Set<String> vocab = dictPath != null ? loadVocab(dictPath) : null;

        Metrics cwsMetrics = new Metrics();
        Metrics posMetrics = new Metrics();
        Metrics nerMetrics = new Metrics();

        // 实体分类型指标
        Map<String, Metrics> nerByType = new HashMap<>();

        // 可选：CWS OOV/IV 统计
        Metrics cwsIv = new Metrics();
        Metrics cwsOov = new Metrics();

        // 可选：NER OOV/IV 统计（按实体内部 token 是否全部在词典中来区分）
        Metrics nerIv = new Metrics();
        Metrics nerOov = new Metrics();

        try (BufferedReader rawReader = Files.newBufferedReader(rawPath, StandardCharsets.UTF_8);
             BufferedReader goldReader = Files.newBufferedReader(goldPath, StandardCharsets.UTF_8);
             BufferedReader predReader = Files.newBufferedReader(predPath, StandardCharsets.UTF_8)) {

            String rawLine;
            int lineIdx = 0;
            while ((rawLine = rawReader.readLine()) != null) {
                String goldLine = goldReader.readLine();
                String predLine = predReader.readLine();

                if (goldLine == null || predLine == null) {
                    System.err.println("Warning: gold/pred 文件在第 " + lineIdx + " 行提前结束，停止评估。");
                    break;
                }

                SentenceAnnotation gold = parseAnnotatedLine(goldLine, rawLine, vocab);
                SentenceAnnotation pred = parseAnnotatedLine(predLine, rawLine, vocab);

                // 简单一致性检查（可选）：归一化后的原文本长度与 token 归一化拼接长度差异过大时发出警告
                if (!gold.normalizedRaw.equals(pred.normalizedRaw)) {
                    System.err.println("Warning: 第 " + lineIdx + " 行 gold/pred 归一化原文不一致，可能有严重对齐问题。");
                }

                // 分词评估（CWS）
                evaluateCws(gold, pred, cwsMetrics, cwsIv, cwsOov);

                // 词性评估（POS）
                evaluatePos(gold, pred, posMetrics);

                // 实体评估（NER）
                evaluateNer(gold, pred, nerMetrics, nerByType, nerIv, nerOov);

                lineIdx++;
            }
        }

        EvaluationResult result = new EvaluationResult();
        result.cws = cwsMetrics;
        result.pos = posMetrics;
        result.ner = nerMetrics;
        result.nerByType = nerByType;
        result.cwsIv = cwsIv;
        result.cwsOov = cwsOov;
        result.nerIv = nerIv;
        result.nerOov = nerOov;
        return result;
    }

    /**
     * 整体评估结果封装，方便后续扩展输出。
     */
    public static class EvaluationResult {
        public Metrics cws;
        public Metrics pos;
        public Metrics ner;
        public Map<String, Metrics> nerByType;
        public Metrics cwsIv;
        public Metrics cwsOov;
        public Metrics nerIv;
        public Metrics nerOov;

        public void prettyPrint() {
            System.out.println("=== CWS (分词) ===");
            System.out.println(cws);

            System.out.println("\n=== POS (词性) ===");
            System.out.println(pos);

            System.out.println("\n=== NER (实体，micro) ===");
            System.out.println(ner);

            if (nerByType != null && !nerByType.isEmpty()) {
                System.out.println("\n=== NER 按实体类型 ===");
                for (Map.Entry<String, Metrics> e : nerByType.entrySet()) {
                    System.out.println(e.getKey() + " -> " + e.getValue());
                }
            }

            if (cwsIv != null && cwsOov != null) {
                System.out.println("\n=== CWS IV/OOV ===");
                System.out.println("IV  : " + cwsIv);
                System.out.println("OOV : " + cwsOov);
            }

            if (nerIv != null && nerOov != null) {
                System.out.println("\n=== NER IV/OOV (按实体内部 token 是否全部在词典中) ===");
                System.out.println("IV  : " + nerIv);
                System.out.println("OOV : " + nerOov);
            }
        }
    }

    /**
     * 从词典文件加载词表，做简单归一化（去空白 + 小写）。
     */
    static Set<String> loadVocab(Path dictPath) throws IOException {
        Set<String> vocab = new HashSet<>();
        try (BufferedReader reader = Files.newBufferedReader(dictPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String norm = normalize(line);
                if (!norm.isEmpty()) {
                    vocab.add(norm);
                }
            }
        }
        return vocab;
    }

    /**
     * 对一行标注进行解析，生成 token / entity 信息。
     *
     * @param annotatedLine 标注行（分词 + 词性 + 实体）
     * @param rawLine       原始文本行
     * @param vocab         归一化后的词典集合，可为 null
     */
    static SentenceAnnotation parseAnnotatedLine(String annotatedLine,
                                                         String rawLine,
                                                         Set<String> vocab) {
        String normalizedRaw = normalize(rawLine);
        SentenceAnnotation sent = new SentenceAnnotation(normalizedRaw);

        if (annotatedLine == null || annotatedLine.trim().isEmpty()) {
            return sent;
        }

        Pattern pattern = Pattern.compile("(\\[(([^\\s\\]]+/[0-9a-zA-Z]+)\\s+)+?([^\\s\\]]+/[0-9a-zA-Z]+)]/?[0-9a-zA-Z]+)|([^\\s]+/[0-9a-zA-Z]+)");
        Matcher matcher = pattern.matcher(annotatedLine);
        int currentOffset = 0; // 在归一化串上的偏移

        while (matcher.find()) {
            String item = matcher.group();
            if (item.isEmpty()) {
                continue;
            }

            // 复合实体：[...]/TYPE
            if (item.startsWith("[") && item.contains("]/")) {
                int idx = item.lastIndexOf("]/");
                if (idx <= 0 || idx + 2 >= item.length()) {
                    continue;
                }
                String inner = item.substring(1, idx); // 去掉前导 '[' 和末尾 ']' 之前的部分
                String entityType = item.substring(idx + 2).trim();

                String[] innerTokens = inner.split("\\s");
                int entityStartTokenIndex = sent.tokens.size();
                boolean entityInVocab = true;

                for (String innerItem : innerTokens) {
                    String t = innerItem.trim();
                    if (t.isEmpty()) continue;
                    Token token = createToken(t, currentOffset, vocab);
                    if (token == null) continue;
                    sent.tokens.add(token);
                    currentOffset = token.end;
                    if (!token.inVocab) {
                        entityInVocab = false;
                    }
                }

                int entityEndTokenIndex = sent.tokens.size() - 1;
                if (entityEndTokenIndex >= entityStartTokenIndex) {
                    Token first = sent.tokens.get(entityStartTokenIndex);
                    Token last = sent.tokens.get(entityEndTokenIndex);
                    EntitySpan span = new EntitySpan(
                            entityStartTokenIndex,
                            entityEndTokenIndex,
                            first.start,
                            last.end,
                            entityType,
                            entityInVocab
                    );
                    sent.entities.add(span);
                }
            } else {
                // 普通 token：词语/词性
                Token token = createToken(item, currentOffset, vocab);
                if (token == null) continue;
                sent.tokens.add(token);
                currentOffset = token.end;
            }
        }

        return sent;
    }

    /**
     * 解析单个 token（词语/词性），并根据当前偏移计算其在归一化串中的起止位置。
     */
    private static Token createToken(String item,
                                     int currentOffset,
                                     Set<String> vocab) {
        int slashIdx = item.lastIndexOf('/');
        if (slashIdx <= 0 || slashIdx == item.length() - 1) {
            // 非法格式，跳过
            return null;
        }
        String word = item.substring(0, slashIdx);
        String pos = item.substring(slashIdx + 1);
        String norm = normalize(word);
        int start = currentOffset;
        int end = currentOffset + norm.length();

        boolean inVocab = false;
        if (vocab != null && !norm.isEmpty()) {
            inVocab = vocab.contains(norm);
        }

        return new Token(word, pos, start, end, inVocab);
    }

    /**
     * 分词评估：对比 gold/pred 的边界集合，并根据词典划分 IV/OOV。
     */
    private static void evaluateCws(SentenceAnnotation gold,
                                    SentenceAnnotation pred,
                                    Metrics overall,
                                    Metrics ivMetrics,
                                    Metrics oovMetrics) {
        // 边界集合：start:end
        Set<String> goldBoundaries = new HashSet<>();
        for (Token t : gold.tokens) {
            goldBoundaries.add(boundaryKey(t.start, t.end));
        }

        Set<String> predBoundaries = new HashSet<>();
        for (Token t : pred.tokens) {
            predBoundaries.add(boundaryKey(t.start, t.end));
        }

        // overall
        long tp = 0;
        for (String b : predBoundaries) {
            if (goldBoundaries.contains(b)) {
                tp++;
            }
        }
        long fp = predBoundaries.size() - tp;
        long fn = goldBoundaries.size() - tp;
        overall.addTP(tp);
        overall.addFP(fp);
        overall.addFN(fn);

        // IV/OOV：以 gold token 为准
        if (ivMetrics != null && oovMetrics != null) {
            for (Token t : gold.tokens) {
                String key = boundaryKey(t.start, t.end);
                boolean hit = predBoundaries.contains(key);
                Metrics m = t.inVocab ? ivMetrics : oovMetrics;
                if (hit) {
                    m.addTP(1);
                } else {
                    m.addFN(1);
                }
            }

            // 再从 pred 角度补充 FP
            for (Token t : pred.tokens) {
                String key = boundaryKey(t.start, t.end);
                if (!goldBoundaries.contains(key)) {
                    Metrics m = t.inVocab ? ivMetrics : oovMetrics;
                    m.addFP(1);
                }
            }
        }
    }

    /**
     * 词性评估：在完全相同的分词边界上比较 (span, POS) 是否一致。
     */
    private static void evaluatePos(SentenceAnnotation gold,
                                    SentenceAnnotation pred,
                                    Metrics posMetrics) {
        Map<String, String> goldMap = new HashMap<>(); // boundary -> POS
        for (Token t : gold.tokens) {
            goldMap.put(boundaryKey(t.start, t.end), t.pos);
        }

        Map<String, String> predMap = new HashMap<>();
        for (Token t : pred.tokens) {
            predMap.put(boundaryKey(t.start, t.end), t.pos);
        }

        long tp = 0;
        long fp = 0;
        long fn = 0;

        // 遍历 pred：预测到的 (boundary, pos)
        for (Map.Entry<String, String> e : predMap.entrySet()) {
            String boundary = e.getKey();
            String predPos = e.getValue();
            String goldPos = goldMap.get(boundary);
            if (goldPos != null && goldPos.equals(predPos)) {
                tp++;
            } else {
                fp++;
            }
        }

        // gold 中存在但 pred 中缺失或 POS 不同的，都算 FN
        for (Map.Entry<String, String> e : goldMap.entrySet()) {
            String boundary = e.getKey();
            String goldPos = e.getValue();
            String predPos = predMap.get(boundary);
            if (predPos == null || !goldPos.equals(predPos)) {
                fn++;
            }
        }

        posMetrics.addTP(tp);
        posMetrics.addFP(fp);
        posMetrics.addFN(fn);
    }

    /**
     * 实体评估：严格匹配 (start, end, type)；并按实体类型 + IV/OOV 统计。
     */
    private static void evaluateNer(SentenceAnnotation gold,
                                    SentenceAnnotation pred,
                                    Metrics overall,
                                    Map<String, Metrics> byType,
                                    Metrics ivMetrics,
                                    Metrics oovMetrics) {
        Set<EntitySpan> goldSet = new HashSet<>(gold.entities);
        Set<EntitySpan> predSet = new HashSet<>(pred.entities);

        // 整体 TP
        long tp = 0;
        for (EntitySpan p : predSet) {
            if (goldSet.contains(p)) {
                tp++;
            }
        }
        long fp = predSet.size() - tp;
        long fn = goldSet.size() - tp;
        overall.addTP(tp);
        overall.addFP(fp);
        overall.addFN(fn);

        // 按类型 & IV/OOV
        if (byType != null || (ivMetrics != null && oovMetrics != null)) {
            // 先构建 map 便于判断 TP/FP/FN
            Map<EntitySpan, EntitySpan> goldMap = new HashMap<>();
            for (EntitySpan g : goldSet) {
                goldMap.put(g, g);
            }

            Map<EntitySpan, EntitySpan> predMap = new HashMap<>();
            for (EntitySpan p : predSet) {
                predMap.put(p, p);
            }

            // 处理预测实体（TP/FP）
            for (EntitySpan p : predSet) {
                EntitySpan g = goldMap.get(p);
                boolean isTp = g != null;

                // 按类型
                if (byType != null) {
                    Metrics m = byType.computeIfAbsent(p.type, k -> new Metrics());
                    if (isTp) {
                        m.addTP(1);
                    } else {
                        m.addFP(1);
                    }
                }

                // IV/OOV：以 gold 实体为准，如果 TP，则看 gold 的 inVocab；
                // 对 FP，则只能用 pred 的 inVocab 信息。
                if (ivMetrics != null && oovMetrics != null) {
                    if (isTp) {
                        Metrics m = (g.inVocab ? ivMetrics : oovMetrics);
                        m.addTP(1);
                    } else {
                        Metrics m = (p.inVocab ? ivMetrics : oovMetrics);
                        m.addFP(1);
                    }
                }
            }

            // 处理 FN：gold 中有但 pred 中没有的实体
            for (EntitySpan g : goldSet) {
                if (!predSet.contains(g)) {
                    if (byType != null) {
                        Metrics m = byType.computeIfAbsent(g.type, k -> new Metrics());
                        m.addFN(1);
                    }
                    if (ivMetrics != null && oovMetrics != null) {
                        Metrics m = (g.inVocab ? ivMetrics : oovMetrics);
                        m.addFN(1);
                    }
                }
            }
        }
    }

    private static String boundaryKey(int start, int end) {
        return start + ":" + end;
    }

    /**
     * 归一化：去除所有空白（空格、Tab 等）并转为小写。
     */
    static String normalize(String s) {
        if (s == null) return "";
        // 去掉所有 Unicode 空白，然后转小写
        return s.replaceAll("\\s+", "").toLowerCase();
    }

}
