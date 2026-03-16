package com.ppwx.easysearch.qp.tokenizer;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 组合分词器：词典优先覆盖 + CRF 填缝。
 * <p>
 * 以 CRF 为主分词，但词典可强干预：在词典命中的区间内以词典切分为准，其余区间用 CRF 结果填充。
 * 支持通过干预词典快速修正 CRF 切分（强制合并、强制切分、强制替换）。
 * </p>
 *
 * @see CRFCompositeTokenizer 仅对连续单字做词典补充，干预能力弱
 */
public class DictOverrideCompositeTokenizer implements Tokenizer {

    private static final String SOURCE_ATTR = "source";
    private static final String SOURCE_CRF = "crf";
    private static final String SOURCE_DICT = "dict";

    private final Tokenizer crfTokenizer;
    private final Tokenizer dictTokenizer;

    public DictOverrideCompositeTokenizer(Tokenizer crfTokenizer, Tokenizer dictTokenizer) {
        this.crfTokenizer = crfTokenizer;
        this.dictTokenizer = dictTokenizer;
    }

    /**
     * 使用默认的 CRF 与空词典 Dict 分词器实例。
     * 需要词典时请使用 {@link DictTokenizer#fromPath(String)} 等工厂方法创建 Dict 实例后传入双参构造。
     */
    public DictOverrideCompositeTokenizer() {
        this(new CRFTokenizer(), new DictTokenizer());
    }

    /**
     * 分词：词典优先覆盖 + CRF 填缝。
     * 先取词典在全文上的最长匹配得到不重叠的干预区间，再在未被词典覆盖的区间用 CRF 结果填充。
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

        if (dictTokens.isEmpty()) {
            return markSource(crfTokens, SOURCE_CRF);
        }
        if (crfTokens.isEmpty()) {
            return markSource(normalizeDictSpans(dictTokens), SOURCE_DICT);
        }

        return mergeDictOverrideCrfFill(text, crfTokens, dictTokens);
    }

    private static List<Token> markSource(List<Token> tokens, String source) {
        List<Token> result = new ArrayList<>(tokens.size());
        for (Token t : tokens) {
            result.add(t.toBuilder().addAttribute(SOURCE_ATTR, source).build());
        }
        return result;
    }

    /**
     * 将词典结果规范为不重叠区间：同一起点取最长匹配，再从左到右去重叠。
     */
    private List<Token> normalizeDictSpans(List<Token> dictTokens) {
        if (dictTokens.isEmpty()) {
            return dictTokens;
        }
        List<Token> sorted = new ArrayList<>(dictTokens);
        sorted.sort(Comparator.comparingInt(Token::getStartIndex));

        Map<Integer, Token> longestAtStart = new LinkedHashMap<>();
        for (Token t : sorted) {
            int s = t.getStartIndex();
            int len = t.getEndIndex() - t.getStartIndex();
            Token existing = longestAtStart.get(s);
            if (existing == null || (existing.getEndIndex() - existing.getStartIndex()) < len) {
                longestAtStart.put(s, t);
            }
        }

        List<Token> nonOverlapping = new ArrayList<>();
        for (Token t : longestAtStart.values()) {
            if (nonOverlapping.isEmpty() || t.getStartIndex() >= nonOverlapping.get(nonOverlapping.size() - 1).getEndIndex()) {
                nonOverlapping.add(t);
            }
        }
        return nonOverlapping;
    }

    /**
     * 词典优先覆盖 + CRF 填缝：先得到不重叠的词典区间，再在缝隙 [gStart, gEnd) 中用 CRF token 填充（与缝隙求交）。
     */
    private List<Token> mergeDictOverrideCrfFill(String text, List<Token> crfTokens, List<Token> dictTokens) {
        List<Token> dictSpans = normalizeDictSpans(dictTokens);
        List<Token> crfSorted = new ArrayList<>(crfTokens);
        crfSorted.sort(Comparator.comparingInt(Token::getStartIndex));

        List<Token> result = new ArrayList<>();

        for (Token d : dictSpans) {
            result.add(d.toBuilder().addAttribute(SOURCE_ATTR, SOURCE_DICT).build());
        }

        int len = text.length();
        int gStart = 0;
        for (Token d : dictSpans) {
            int gEnd = d.getStartIndex();
            if (gStart < gEnd) {
                fillGapWithCrf(text, result, crfSorted, gStart, gEnd);
            }
            gStart = d.getEndIndex();
        }
        if (gStart < len) {
            fillGapWithCrf(text, result, crfSorted, gStart, len);
        }

        result.sort(Comparator.comparingInt(Token::getStartIndex));
        return result;
    }

    /** 在缝隙 [gStart, gEnd) 内用 CRF token 与缝隙的交集填充。 */
    private void fillGapWithCrf(String text, List<Token> result, List<Token> crfSorted, int gStart, int gEnd) {
        for (Token c : crfSorted) {
            int cs = c.getStartIndex();
            int ce = c.getEndIndex();
            if (ce <= gStart || cs >= gEnd) {
                continue;
            }
            int segStart = Math.max(cs, gStart);
            int segEnd = Math.min(ce, gEnd);
            if (segStart >= segEnd) {
                continue;
            }
            String segText = text.substring(segStart, segEnd);
            Token seg = new Token(segText, c.getType(), segStart, segEnd, c.getConfidence());
            result.add(seg.toBuilder().addAttribute(SOURCE_ATTR, SOURCE_CRF).build());
        }
    }
}
