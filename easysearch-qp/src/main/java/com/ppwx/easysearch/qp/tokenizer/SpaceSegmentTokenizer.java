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
 * 按空格分片的分词包装器。
 * <p>
 * 将用户查询按空白字符切分为多个片段，对每个片段单独调用底层分词器，最后合并结果并保持正确的原始位置信息。
 * 用于解决：用户输入含空格（如「真我 GT5 Pro」），而词典与 CRF 语料多为无空格，若直接分词会导致识别效果下降；
 * 同时，空格作为用户意图边界，有助于新词（如新机型）的正确切分，避免合并后分词错误导致召回不足。
 * </p>
 *
 * @see Tokenizer
 * @see CRFCompositeTokenizer
 */
public class SpaceSegmentTokenizer implements Tokenizer {

    /** 匹配非空白字符序列，用于按空格分片 */
    private static final Pattern NON_WHITESPACE = Pattern.compile("\\S+");

    private final Tokenizer delegate;

    private final CRFPOSTagger crfposTagger;

    /**
     * 使用指定的底层分词器构造。
     *
     * @param delegate 底层分词器（如 CRFCompositeTokenizer），对每个无空格的片段进行分词
     */
    public SpaceSegmentTokenizer(Tokenizer delegate) {
        this(delegate, null);
    }

    public SpaceSegmentTokenizer(Tokenizer delegate, CRFPOSTagger crfposTagger) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate tokenizer must not be null");
        }
        this.delegate = delegate;
        this.crfposTagger = crfposTagger;
    }

    /**
     * 使用默认的 CRFCompositeTokenizer 构造。
     */
    public SpaceSegmentTokenizer() {
        this(new CRFCompositeTokenizer());
    }

    /**
     * 分词：先按空白切分为片段，再对每个片段分别分词，最后合并并保持原始位置。
     *
     * @param text 输入文本（可含空格）
     * @return 分词结果，每个 Token 的 startIndex/endIndex 为原始文本中的位置
     */
    @Override
    public List<Token> tokenize(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        if (StringUtils.isBlank(text)) {
            return Collections.emptyList();
        }

        List<Token> result = new ArrayList<>();
        Matcher matcher = NON_WHITESPACE.matcher(text);

        while (matcher.find()) {
            String segment = matcher.group();
            int segmentStart = matcher.start();
            int segmentEnd = matcher.end();

            List<Token> segmentTokens = delegate.tokenize(segment);
            for (Token t : segmentTokens) {
                int origStart = segmentStart + t.getStartIndex();
                int origEnd = segmentStart + t.getEndIndex();
                // 边界检查，避免越界
                if (origEnd > segmentEnd) {
                    origEnd = segmentEnd;
                }
                if (origStart >= origEnd) {
                    continue;
                }
                result.add(remapToken(t, origStart, origEnd));
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

    /**
     * 将 Token 的起止位置重映射到原始文本，并确保 text 与原始文本一致。
     */
    private Token remapToken(Token token, int origStart, int origEnd) {
        return token.toBuilder()
                .startIndex(origStart)
                .endIndex(origEnd)
                .build();
    }

    /**
     * 返回底层分词器（用于测试或扩展）。
     */
    public Tokenizer getDelegate() {
        return delegate;
    }
}
