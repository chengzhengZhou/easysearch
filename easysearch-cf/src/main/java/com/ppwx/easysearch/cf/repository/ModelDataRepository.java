/**
 * Copyright (C), 2010-2024, 爱回收
 * FileName: ModelDataRepository
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2024/7/27 19:47
 * Description: 模型数据仓储
 */
package com.ppwx.easysearch.cf.repository;

import com.google.common.collect.Multimap;
import com.ppwx.easysearch.cf.data.CFSimilarity;
import com.ppwx.easysearch.cf.data.UserRating;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 *
 * 模型数据仓储
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2024/07/27 19:47
 * @since 1.0.0
 */
public interface ModelDataRepository {

    /**
     * 获取用户评分
     *
     * @param userId
     * @return java.util.List<com.ppwx.easysearch.cf.data.UserRating>
     */
    Collection<UserRating> getUserRating(String userId);

    /**
     * 获取CF分值信息
     *
     * @param
     * @return com.ppwx.easysearch.cf.data.CFSimilarity
     */
    CFSimilarity getCFScoreV2();

    /**
     * 根据时间窗口获取期间变更的CF分值信息
     *
     * @param beginTime
     * @param endTime
     * @return com.ppwx.easysearch.cf.data.CFSimilarity
     */
    CFSimilarity getCFScoreV2(Date beginTime, Date endTime);

    /**
     * 获取任意包含集合中元素的CF分值信息
     *
     * @param items
     * @return com.ppwx.easysearch.cf.data.CFSimilarity
     */
    CFSimilarity getCFScore(Collection<String> items);

    /**
     * 获取自身的CF分值
     *
     * @return com.ppwx.easysearch.cf.data.CFSimilarity
     */
    CFSimilarity getSameItemCFScore();
    /**
     * 保存用户行为
     *
     * @param userId
     * @param ratings
     * @return void
     */
    void saveUserRating(String userId, List<UserRating> ratings);

    /**
     * 保存用户行为
     *
     * @param userMap
     * @return void
     */
    void saveUserRating(Multimap<String, UserRating> userMap);

    /**
     * 保存CF分值信息
     *
     * @param similarity
     * @return void
     */
    void saveCFScore(CFSimilarity similarity);

    /**
     * @description 清理相似矩阵
     * @author ext.ahs.zhouchzh1@jd.com
     * @date 2024/10/23 19:25
     * @param strategy
     * @return void
     */
    void cleanSimilarity(CleanStrategy strategy);
}