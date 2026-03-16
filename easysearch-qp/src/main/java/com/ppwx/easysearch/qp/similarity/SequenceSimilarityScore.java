package com.ppwx.easysearch.qp.similarity;

import cn.hutool.core.lang.Filter;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @interface SequenceSimilarityScore
 * @description 字符序列相似度
 * @date 2024/10/12 15:31
 **/
public interface SequenceSimilarityScore<R> {

    /**
     * @description 以字符为细腻度的相似度计算
     * @author ext.ahs.zhouchzh1@jd.com
     * @date 2024/10/12 14:33
     * @param var1
     * @param var2
     * @param charFilter 字符过滤
     * @return R
     */
    R apply(CharSequence var1, CharSequence var2, Filter<Character> charFilter);

}
