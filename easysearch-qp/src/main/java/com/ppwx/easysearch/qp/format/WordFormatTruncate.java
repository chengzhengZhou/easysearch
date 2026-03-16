package com.ppwx.easysearch.qp.format;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className WordFormatTruncate
 * @description 截断
 * @date 2024/11/1 19:47
 **/
public class WordFormatTruncate implements WordFormat {

    private static final WordFormatTruncate INSTANCE = new WordFormatTruncate(60);

    public static WordFormat getInstance() {
        return INSTANCE;
    }

    private final int maxLen;

    public WordFormatTruncate(int maxLen) {
        this.maxLen = maxLen;
    }

    @Override
    public StringBuilder format(StringBuilder original) {
        if (original.length() > maxLen) {
            original.setLength(maxLen);
        }
        return original;
    }
}
