package com.ppwx.easysearch.qp.format;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @interface WordFormat
 * @description 单词格式化
 * @date 2024/11/1 19:40
 **/
public interface WordFormat {

    /**
     * @description 字符串格式化
     * @author ext.ahs.zhouchzh1@jd.com
     * @date 2024/11/1 19:42
     * @param original
     * @return StringBuilder
     */
    StringBuilder format(final StringBuilder original);

}
