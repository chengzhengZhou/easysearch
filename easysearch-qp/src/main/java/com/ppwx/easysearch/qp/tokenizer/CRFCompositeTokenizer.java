package com.ppwx.easysearch.qp.tokenizer;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 组合分词器：CRF 基座 + Dict 补充。
 * <p>
 * CRF 结果作为主分词序列；在 CRF 的「连续单字」片段上，若词典存在覆盖该区间的词，则用词典结果替换该片段，
 * 从而在保持 CRF 泛化能力的同时，用词典快速扩展领域词。
 * </p>
 */
public class CRFCompositeTokenizer implements Tokenizer {

    private static final String SOURCE_ATTR = "source";
    private static final String SOURCE_CRF = "crf";
    private static final String SOURCE_DICT = "dict";

    private final Tokenizer crfTokenizer;
    private final Tokenizer dictTokenizer;

    public CRFCompositeTokenizer(Tokenizer crfTokenizer, Tokenizer dictTokenizer) {
        this.crfTokenizer = crfTokenizer;
        this.dictTokenizer = dictTokenizer;
    }

    /**
     * 使用默认的 CRF 与空词典 Dict 分词器实例。
     * 需要词典时请使用 {@link DictTokenizer#fromPath(String)} 等工厂方法创建 Dict 实例后传入双参构造。
     */
    public CRFCompositeTokenizer() {
        this(new CRFTokenizer(), new DictTokenizer());
    }

    /**
     * 分词：CRF 基座 + Dict 补充。
     * 先取 CRF 结果；对 CRF 中连续单字组成的区间，若 Dict 有恰好覆盖该区间的词则用 Dict 替换。
     *
     * @param text 输入文本
     * @return 分词结果（无重叠、覆盖全文），每个 Token 带 attributes["source"] = "crf" 或 "dict"
     */
    @Override
    public List<Token> tokenize(String text) {
        if (StringUtils.isBlank(text)) {
            return Collections.emptyList();
        }
        List<Token> crfTokens = crfTokenizer.tokenize(text);
        List<Token> dictTokens = dictTokenizer.tokenize(text);

        if (crfTokens.isEmpty()) {
            return markSource(dictTokens, SOURCE_DICT);
        }
        if (dictTokens.isEmpty()) {
            return markSource(crfTokens, SOURCE_CRF);
        }

        return mergeCrfBaseDictSupplement(crfTokens, dictTokens);
    }

    private static List<Token> markSource(List<Token> tokens, String source) {
        List<Token> result = new ArrayList<>(tokens.size());
        for (Token t : tokens) {
            result.add(t.toBuilder().addAttribute(SOURCE_ATTR, source).build());
        }
        return result;
    }

    /**
     * CRF 基座 + Dict 补充：以 CRF 为基座；对任意连续单字片段 [spanStart, spanEnd)，
     * 若 Dict 中存在若干 token 恰好无重叠、无空隙覆盖该区间，则用该 Dict 序列替换，否则保留 CRF 单字。
     */
    private List<Token> mergeCrfBaseDictSupplement(List<Token> crfTokens, List<Token> dictTokens) {
        List<Token> base = new ArrayList<>(crfTokens);
        base.sort(Comparator.comparingInt(Token::getStartIndex));

        List<Token> dictSorted = new ArrayList<>(dictTokens);
        dictSorted.sort(Comparator.comparingInt(Token::getStartIndex));

        List<Token> result = new ArrayList<>();
        int i = 0;
        while (i < base.size()) {
            Token t = base.get(i);
            int len = t.getEndIndex() - t.getStartIndex();
            if (len > 1) {
                result.add(t.toBuilder().addAttribute(SOURCE_ATTR, SOURCE_CRF).build());
                i++;
                continue;
            }
            int runStart = i;
            while (i < base.size() && (base.get(i).getEndIndex() - base.get(i).getStartIndex() == 1)) {
                i++;
            }
            int spanStart = base.get(runStart).getStartIndex();
            int spanEnd = base.get(i - 1).getEndIndex();
            List<Token> cover = findExactCover(dictSorted, spanStart, spanEnd);
            if (cover != null) {
                for (Token d : cover) {
                    result.add(d.toBuilder().addAttribute(SOURCE_ATTR, SOURCE_DICT).build());
                }
            } else {
                for (int k = runStart; k < i; k++) {
                    result.add(base.get(k).toBuilder().addAttribute(SOURCE_ATTR, SOURCE_CRF).build());
                }
            }
        }
        result.sort(Comparator.comparingInt(Token::getStartIndex));
        return result;
    }

    /**
     * 从已按 startIndex 排序的 dictTokens 中，找出完全落在 [spanStart, spanEnd) 内且无重叠、无空隙覆盖该区间的子序列；
     * 若存在则返回该子序列，否则返回 null。
     */
    private List<Token> findExactCover(List<Token> dictSorted, int spanStart, int spanEnd) {
        List<Token> candidates = new ArrayList<>();
        for (Token t : dictSorted) {
            if (t.getStartIndex() >= spanEnd) {
                break;
            }
            if (t.getEndIndex() <= spanStart) {
                continue;
            }
            if (t.getStartIndex() >= spanStart && t.getEndIndex() <= spanEnd) {
                candidates.add(t);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        List<Token> cover = new ArrayList<>();
        int pos = spanStart;
        for (Token t : candidates) {
            if (t.getStartIndex() != pos) {
                return null;
            }
            cover.add(t);
            pos = t.getEndIndex();
            if (pos >= spanEnd) {
                break;
            }
        }
        return pos == spanEnd ? cover : null;
    }
}
