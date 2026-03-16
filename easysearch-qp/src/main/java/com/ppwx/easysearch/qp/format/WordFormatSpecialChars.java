package com.ppwx.easysearch.qp.format;

import java.util.regex.Pattern;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className WordFormatSpecialChars
 * @description 特殊字符清理
 * @date 2024/11/1 19:47
 **/
public class WordFormatSpecialChars implements WordFormat{

    // 表情符号
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[\\x{1F600}-\\x{1F64F}\\x{1F300}-\\x{1F5FF}\\x{1F680}-\\x{1F6FF}\\x{1F1E0}-\\x{1F1FF}]");

    @Override
    public StringBuilder format(StringBuilder original) {
        if (original.length() <= 0) {
            return original;
        }
        // 清理特殊字符
        String normalized = SPECIAL_CHARS_PATTERN.matcher(original).replaceAll("");
        // 清理多余空格
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return new StringBuilder(normalized);
    }
}
