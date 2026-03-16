package com.ppwx.easysearch.qp.format;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className WordFormatNone
 * @description 无处理
 * @date 2024/11/1 19:42
 **/
public class WordFormatNone implements WordFormat {

    private static final WordFormat INSTANCE = new WordFormatNone();

    public static WordFormat getInstance() {
        return INSTANCE;
    }

    @Override
    public StringBuilder format(StringBuilder original) {
        return original;
    }
}
