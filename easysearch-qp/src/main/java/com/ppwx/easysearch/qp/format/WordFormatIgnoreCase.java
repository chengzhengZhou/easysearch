package com.ppwx.easysearch.qp.format;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className WordFormatIgnoreCase
 * @description 忽略大小写
 * @date 2024/11/1 19:43
 **/
public class WordFormatIgnoreCase implements WordFormat {

    private static final WordFormatIgnoreCase INSTANCE = new WordFormatIgnoreCase();

    public static WordFormat getInstance() {
        return INSTANCE;
    }

    @Override
    public StringBuilder format(StringBuilder original) {
        for (int i = 0; i < original.length(); i++) {
            original.setCharAt(i, Character.toLowerCase(original.charAt(i)));
        }
        return original;
    }
}
