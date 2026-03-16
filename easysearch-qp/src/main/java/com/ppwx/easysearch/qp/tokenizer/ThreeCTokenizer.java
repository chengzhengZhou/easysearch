package com.ppwx.easysearch.qp.tokenizer;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 3C领域专业分词器
 * 
 * @author system
 * @date 2024/12/19
 */
public class ThreeCTokenizer implements Tokenizer {
    
    private static final Logger log = LoggerFactory.getLogger(ThreeCTokenizer.class);

    private final Segmentation segmentation;

    // 数字模式
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+(?:\\.\\d+)?");

    // 已知单位集合
    private static final Set<String> KNOWN_UNITS = new HashSet<>(Arrays.asList(
            "GB", "TB", "MB", "KB", "寸", "英寸", "cm", "mm", "kg", "g",
            "mg", "ml", "L", "W", "V", "A", "Hz", "GHz", "MHz", "像素",
            "万像素", "MP", "倍", "倍变焦", "度", "°", "分钟", "小时", "天",
            "年", "月", "周", "日", "毫安", "mAh", "Ah", "毫秒", "秒"
    ));
    
    public ThreeCTokenizer(Segmentation segmentation) {
        this.segmentation = segmentation;
    }
    
    @Override
    public List<Token> tokenize(String text) {
        if (StringUtils.isBlank(text)) {
            log.debug("Input text is blank, returning empty token list");
            return Collections.emptyList();
        }
        
        List<Token> baseTokens = segmentation.segment(text);
        log.debug("HanLP segmentation produced {} tokens", baseTokens.size());

        // 后处理优化
        List<Token> optimizedTokens = optimize(baseTokens);
        log.debug("Final tokenization produced {} tokens", optimizedTokens.size());

        return optimizedTokens;
            
    }

    /**
     * 优化分词结果
     *
     * @param tokens 原始分词结果
     * @return 优化后的分词结果
     */
    public List<Token> optimize(List<Token> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return tokens;
        }

        log.debug("Starting token post-processing for {} tokens", tokens.size());

        // 合并数字单位
        List<Token> merged = mergeNumberUnits(tokens);
        log.debug("After number unit merging: {} tokens", merged.size());

        return merged;
    }

    /**
     * 合并数字和单位
     *
     * @param tokens Token列表
     * @return 合并后的Token列表
     */
    private List<Token> mergeNumberUnits(List<Token> tokens) {
        List<Token> result = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {
            Token current = tokens.get(i);

            if (isNumber(current) && i + 1 < tokens.size()) {
                Token next = tokens.get(i + 1);
                if (isUnit(next)) {
                    // 合并数字和单位
                    Token merged = mergeNumberUnit(current, next);
                    result.add(merged);
                    i++; // 跳过下一个token
                    continue;
                }
            }

            result.add(current);
        }

        return result;
    }

    /**
     * 判断是否为数字
     *
     * @param token Token对象
     * @return 是否为数字
     */
    private boolean isNumber(Token token) {
        return "number".equals(token.getType()) ||
                NUMBER_PATTERN.matcher(token.getText()).matches();
    }

    /**
     * 判断是否为单位
     *
     * @param token Token对象
     * @return 是否为单位
     */
    private boolean isUnit(Token token) {
        return "quantifier".equals(token.getType()) ||
                isKnownUnit(token.getText());
    }

    /**
     * 判断是否为已知单位
     *
     * @param text 文本
     * @return 是否为已知单位
     */
    private boolean isKnownUnit(String text) {
        return KNOWN_UNITS.contains(text);
    }

    /**
     * 合并数字和单位
     *
     * @param number 数字Token
     * @param unit 单位Token
     * @return 合并后的Token
     */
    private Token mergeNumberUnit(Token number, Token unit) {
        return Token.builder()
                .text(number.getText() + unit.getText())
                .type("number_unit")
                .startIndex(number.getStartIndex())
                .endIndex(unit.getEndIndex())
                .confidence(Math.min(number.getConfidence(), unit.getConfidence()))
                .addAttribute("number", number.getText())
                .addAttribute("unit", unit.getText())
                .addAttribute("source", "merged")
                .build();
    }
}
