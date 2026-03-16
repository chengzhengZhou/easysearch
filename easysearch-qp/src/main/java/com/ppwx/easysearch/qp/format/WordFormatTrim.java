package com.ppwx.easysearch.qp.format;

import com.ppwx.easysearch.qp.support.CustomStopChar;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className WordFormatTrim
 * @description 剔除前后停顿字符
 * @date 2024/11/1 23:46
 **/
@Deprecated
public class WordFormatTrim implements WordFormat {

    private static final WordFormatTrim INSTANCE = new WordFormatTrim();

    public static WordFormat getInstance() {
        return INSTANCE;
    }
    @Override
    public StringBuilder format(StringBuilder original) {
        int len = original.length();
        int st = 0;

        while ((st < len) && (CustomStopChar.isStopChar(original.charAt(st)))) {
            st++;
        }
        while ((st < len) && (CustomStopChar.isStopChar(original.charAt(len - 1)))) {
            len--;
        }
        return ((st > 0) || (len < original.length())) ? new StringBuilder(original.substring(st, len)) : original;
    }
}
