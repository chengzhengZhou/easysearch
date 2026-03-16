package com.ppwx.easysearch.qp.tokenizer;

import com.hankcs.hanlp.model.crf.CRFPOSTagger;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 双路分词器：Path A（去空格）+ Path B（按空格分片），合并时优先采用 Path B 的边界。
 * <p>
 * Path A：去除空格后整体分词，有利于词典匹配（如「iPhone15Pro」）；
 * Path B：按空格分片后逐段分词，尊重用户意图边界，有利于新词切分（如「真我 GT5 Pro」）。
 * </p>
 * <p>
 * 合并策略：
 * <ul>
 *   <li>以 Path B 的片段边界为准；</li>
 *   <li>Path A 中跨越 Path B 片段边界的 token 予以舍弃；</li>
 *   <li>在每个片段内，若 Path A 有更细粒度切分且不与 Path B 重叠，则采用 Path A 的切分。</li>
 * </ul>
 * </p>
 *
 * @see Tokenizer
 * @see SpaceSegmentTokenizer
 * @see CRFCompositeTokenizer
 */
public class DualPathTokenizer implements Tokenizer {

    private static final Pattern NON_WHITESPACE = Pattern.compile("\\S+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final Tokenizer delegate;

    private final CRFPOSTagger crfposTagger;

    /**
     * 使用指定的底层分词器构造。
     *
     * @param delegate 底层分词器（如 CRFCompositeTokenizer）
     */
    public DualPathTokenizer(Tokenizer delegate) {
        this(delegate, null);
    }

    public DualPathTokenizer(Tokenizer delegate, CRFPOSTagger crfposTagger) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate tokenizer must not be null");
        }
        this.delegate = delegate;
        this.crfposTagger = crfposTagger;
    }

    /**
     * 使用默认的 CRFCompositeTokenizer 构造。
     */
    public DualPathTokenizer() {
        this(new CRFCompositeTokenizer());
    }

    @Override
    public List<Token> tokenize(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        if (StringUtils.isBlank(text)) {
            return Collections.emptyList();
        }

        List<Segment> segments = extractSegments(text);
        if (segments.isEmpty()) {
            return Collections.emptyList();
        }

        List<Token> pathB = tokenizePathB(text, segments);
        List<Token> pathA = tokenizePathA(text, segments);
        List<Token> pathAFiltered = filterConflicting(pathA, segments);

        return merge(pathB, pathAFiltered, segments);
    }

    /** 片段：[start, end) 在原文中的起止位置 */
    private static class Segment {
        final int start;
        final int end;
        final String text;

        Segment(int start, int end, String text) {
            this.start = start;
            this.end = end;
            this.text = text;
        }
    }

    private List<Segment> extractSegments(String text) {
        List<Segment> segments = new ArrayList<>();
        Matcher m = NON_WHITESPACE.matcher(text);
        while (m.find()) {
            segments.add(new Segment(m.start(), m.end(), m.group()));
        }
        return segments;
    }

    /** Path B：按空格分片，逐段分词 */
    private List<Token> tokenizePathB(String text, List<Segment> segments) {
        List<Token> result = new ArrayList<>();
        for (Segment seg : segments) {
            List<Token> tokens = delegate.tokenize(seg.text);
            for (Token t : tokens) {
                int origStart = seg.start + t.getStartIndex();
                int origEnd = seg.start + t.getEndIndex();
                if (origEnd > seg.end) {
                    origEnd = seg.end;
                }
                if (origStart < origEnd) {
                    result.add(t.toBuilder().startIndex(origStart).endIndex(origEnd).build());
                }
            }
        }
        result.sort(Comparator.comparingInt(Token::getStartIndex));
        // reTag
        if (crfposTagger != null) {
            String[] tags = crfposTagger.tag(result.stream().map(Token::getText).toArray(String[]::new));
            for (int i = 0; i < result.size(); i++) {
                Token t = result.get(i);
                t.setType(tags[i]);
            }
        }
        return result;
    }

    /** Path A：去空格后整体分词，并将索引映射回原文 */
    private List<Token> tokenizePathA(String text, List<Segment> segments) {
        String noSpace = WHITESPACE.matcher(text).replaceAll("");
        if (noSpace.isEmpty()) {
            return Collections.emptyList();
        }

        int[] collapsedToOriginal = buildCollapsedToOriginalMapping(text);
        List<Token> rawTokens = delegate.tokenize(noSpace);
        List<Token> result = new ArrayList<>();

        for (Token t : rawTokens) {
            int cStart = t.getStartIndex();
            int cEnd = t.getEndIndex();
            if (cStart >= collapsedToOriginal.length || cEnd > collapsedToOriginal.length || cStart >= cEnd) {
                continue;
            }
            int origStart = collapsedToOriginal[cStart];
            int origEnd = (cEnd > 0 && cEnd - 1 < collapsedToOriginal.length)
                    ? collapsedToOriginal[cEnd - 1] + 1
                    : origStart;
            if (origEnd <= origStart) {
                continue;
            }
            result.add(t.toBuilder().startIndex(origStart).endIndex(origEnd).build());
        }
        result.sort(Comparator.comparingInt(Token::getStartIndex));
        return result;
    }

    /** 建立：去空格后的下标 -> 原文下标 的映射 */
    private int[] buildCollapsedToOriginalMapping(String text) {
        List<Integer> mapping = new ArrayList<>();
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isWhitespace(text.charAt(i))) {
                mapping.add(i);
            }
        }
        int[] arr = new int[mapping.size()];
        for (int i = 0; i < mapping.size(); i++) {
            arr[i] = mapping.get(i);
        }
        return arr;
    }

    /** 舍弃 Path A 中跨越 Path B 片段边界的 token */
    private List<Token> filterConflicting(List<Token> pathA, List<Segment> segments) {
        if (pathA.isEmpty()) {
            return pathA;
        }
        List<Token> result = new ArrayList<>();
        for (Token t : pathA) {
            int start = t.getStartIndex();
            int end = t.getEndIndex();
            if (!crossesSegmentBoundary(start, end, segments)) {
                result.add(t);
            }
        }
        return result;
    }

    private boolean crossesSegmentBoundary(int start, int end, List<Segment> segments) {
        for (int i = 0; i < segments.size() - 1; i++) {
            int boundary = segments.get(i).end;
            if (start < boundary && end > boundary) {
                return true;
            }
        }
        return false;
    }

    /**
     * 合并：以 Path B 为基，每个片段内若 Path A 有更细粒度且不重叠的切分则采用 Path A。
     */
    private List<Token> merge(List<Token> pathB, List<Token> pathAFiltered, List<Segment> segments) {
        List<Token> result = new ArrayList<>();

        for (Segment seg : segments) {
            List<Token> bInSeg = tokensInRange(pathB, seg.start, seg.end);
            List<Token> aInSeg = tokensInRange(pathAFiltered, seg.start, seg.end);

            List<Token> chosen = chooseFiner(bInSeg, aInSeg);
            result.addAll(chosen);
        }

        result.sort(Comparator.comparingInt(Token::getStartIndex));
        return result;
    }

    private List<Token> tokensInRange(List<Token> tokens, int start, int end) {
        List<Token> out = new ArrayList<>();
        for (Token t : tokens) {
            if (t.getStartIndex() >= start && t.getEndIndex() <= end) {
                out.add(t);
            }
        }
        return out;
    }

    /** 在每个片段内选择更细粒度的切分：Path A 更细时采用 Path A，否则采用 Path B（优先 Path B） */
    private List<Token> chooseFiner(List<Token> pathB, List<Token> pathA) {
        if (pathA.isEmpty()) {
            return pathB;
        }
        if (pathB.isEmpty()) {
            return pathA;
        }
        return pathA.size() > pathB.size() ? pathA : pathB;
    }

    public Tokenizer getDelegate() {
        return delegate;
    }
}
