package com.ppwx.easysearch.qp.util;

import cn.hutool.core.lang.Filter;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className StrUtil
 * @description 字符工具
 * @date 2024/11/1 14:26
 **/
public final class StrTrimUtil {

    private StrTrimUtil() {

    }

    /**
     * @description 去除特殊字符
     * @author ext.ahs.zhouchzh1@jd.com
     * @date 2024/11/1 14:31
     * @param word 词
     * @param charFilter 字符过滤器
     * @return String
     */
    public static String trim(String word, Filter<Character> charFilter) {
        if (word == null) {
            return null;
        }
        char currentChar;
        final int length = word.length();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            currentChar = word.charAt(i);
            if (charFilter.accept(currentChar)) {
                builder.append(currentChar);
            }
        }
        return builder.toString();
    }

}
