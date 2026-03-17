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

import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * 词图融合分词器：构建 Lattice（词图）并对边上由模型/词典给出的分数做最优路径求解，得到融合分词结果。
 * <p>
 * 将 CRF 与词典两种分词结果视为词图中的边（每条边带置信度/分数），并补充单字边保证连通；
 * 在图上做动态规划求得分和最大的路径，作为最终分词。
 * </p>
 */
public class LatticeFusionTokenizer implements Tokenizer {

    private static final String SOURCE_ATTR = "source";
    private static final String SOURCE_CRF = "crf";
    private static final String SOURCE_DICT = "dict";
    private static final String SOURCE_SINGLE = "single";

    
    private static final double DEFAULT_SINGLE_CHAR_SCORE = 0.3;

    private final Tokenizer crfTokenizer;
    private final Tokenizer dictTokenizer;
    private final double singleCharScore;

    public LatticeFusionTokenizer(Tokenizer crfTokenizer, Tokenizer dictTokenizer) {
        this(crfTokenizer, dictTokenizer, DEFAULT_SINGLE_CHAR_SCORE);
    }

    public LatticeFusionTokenizer(Tokenizer crfTokenizer, Tokenizer dictTokenizer, double singleCharScore) {
        this.crfTokenizer = crfTokenizer;
        this.dictTokenizer = dictTokenizer;
        this.singleCharScore = singleCharScore;
    }

    /**
     * 使用默认 CRF 与空词典；需要词典时请用双参或三参构造传入。
     */
    public LatticeFusionTokenizer() {
        this(new CRFTokenizer(), new DictTokenizer(), DEFAULT_SINGLE_CHAR_SCORE);
    }

    @Override
    public List<Token> tokenize(String text) {
        if (StringUtils.isBlank(text)) {
            return Collections.emptyList();
        }
        List<Token> crfTokens = crfTokenizer.tokenize(text);
        List<Token> dictTokens = dictTokenizer.tokenize(text);

        if (crfTokens.isEmpty() && dictTokens.isEmpty()) {
            return singleCharSegmentation(text);
        }

        List<LatticeEdge> edges = buildLattice(text, crfTokens, dictTokens);
        return bestPath(text, edges);
    }

    private List<Token> singleCharSegmentation(String text) {
        List<Token> result = new ArrayList<>();
        for (int i = 0; i < text.length(); i++) {
            String w = text.substring(i, i + 1);
            result.add(Token.builder()
                    .text(w)
                    .type("n")
                    .startIndex(i)
                    .endIndex(i + 1)
                    .confidence(singleCharScore)
                    .addAttribute(SOURCE_ATTR, SOURCE_SINGLE)
                    .build());
        }
        return result;
    }

    /**
     * 合并 CRF、词典及单字边；同一 (start,end) 只保留分数最高的一条边。
     */
    private List<LatticeEdge> buildLattice(String text, List<Token> crfTokens, List<Token> dictTokens) {
        Map<String, LatticeEdge> bySpan = new HashMap<>();

        for (Token t : crfTokens) {
            addEdge(bySpan, t.getStartIndex(), t.getEndIndex(), t, SOURCE_CRF);
        }
        for (Token t : dictTokens) {
            addEdge(bySpan, t.getStartIndex(), t.getEndIndex(), t, SOURCE_DICT);
        }

        int n = text.length();
        for (int i = 0; i < n; i++) {
            String key = i + "," + (i + 1);
            if (!bySpan.containsKey(key)) {
                Token single = Token.builder()
                        .text(text.substring(i, i + 1))
                        .type("n")
                        .startIndex(i)
                        .endIndex(i + 1)
                        .confidence(singleCharScore)
                        .build();
                bySpan.put(key, new LatticeEdge(i, i + 1, single, singleCharScore, SOURCE_SINGLE));
            }
        }

        return new ArrayList<>(bySpan.values());
    }

    private void addEdge(Map<String, LatticeEdge> bySpan, int start, int end, Token token, String source) {
        String key = start + "," + end;
        double score = token.getConfidence() > 0 ? token.getConfidence() : 1.0;
        LatticeEdge existing = bySpan.get(key);
        if (existing == null || existing.score < score) {
            bySpan.put(key, new LatticeEdge(start, end, token, score, source));
        }
    }

    /**
     * 动态规划求 0 到 n 的最大得分路径，返回路径上的 Token 列表。
     */
    private List<Token> bestPath(String text, List<LatticeEdge> edges) {
        int n = text.length();
        double[] bestScore = new double[n + 1];
        LatticeEdge[] backPtr = new LatticeEdge[n + 1];
        for (int i = 0; i <= n; i++) {
            bestScore[i] = Double.NEGATIVE_INFINITY;
        }
        bestScore[0] = 0;

        for (int j = 1; j <= n; j++) {
            for (LatticeEdge e : edges) {
                if (e.end != j) {
                    continue;
                }
                double cand = bestScore[e.start] + e.score;
                if (cand > bestScore[j]) {
                    bestScore[j] = cand;
                    backPtr[j] = e;
                }
            }
        }

        if (bestScore[n] == Double.NEGATIVE_INFINITY) {
            return singleCharSegmentation(text);
        }

        List<LatticeEdge> path = new ArrayList<>();
        int pos = n;
        while (pos > 0 && backPtr[pos] != null) {
            LatticeEdge e = backPtr[pos];
            path.add(e);
            pos = e.start;
        }
        Collections.reverse(path);

        List<Token> result = new ArrayList<>(path.size());
        for (LatticeEdge e : path) {
            result.add(e.token.toBuilder()
                    .addAttribute(SOURCE_ATTR, e.source)
                    .build());
        }
        result.sort(Comparator.comparingInt(Token::getStartIndex));
        return result;
    }

    private static class LatticeEdge {
        final int start;
        final int end;
        final Token token;
        final double score;
        final String source;

        LatticeEdge(int start, int end, Token token, double score, String source) {
            this.start = start;
            this.end = end;
            this.token = token;
            this.score = score;
            this.source = source;
        }
    }
}
