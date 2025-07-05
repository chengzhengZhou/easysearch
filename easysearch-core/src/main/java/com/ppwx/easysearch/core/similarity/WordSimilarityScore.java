package com.ppwx.easysearch.core.similarity;

import java.util.Collection;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className WordSimilarityScore
 * @description 词相似度
 * @date 2024/10/10 14:30
 **/
public interface WordSimilarityScore<R> {

    /**
     * @description 以词为细腻度的相似度计算
     * @author ext.ahs.zhouchzh1@jd.com
     * @date 2024/10/12 14:32
     * @param var1
     * @param var2
     * @return R
     */
    R apply(Collection<String> var1, Collection<String> var2);

}
