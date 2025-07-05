/**
 * Copyright (C), 2010-2024, 爱回收
 * FileName: RankService
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2024/8/9 11:26
 * Description: 排序服务
 */
package com.ppwx.easysearch.cf.rank;

import com.ppwx.easysearch.cf.data.UserRating;
import net.librec.recommender.item.ItemEntry;

import java.util.Collection;

/**
 *
 * 排序服务
 * 通过模型预测用户对商品的评好，然后进行排序
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2024/08/09 11:26
 * @since 1.0.0
 */
public interface RankService {

    /**
     * 获取用户频分
     *
     * @param userId
     * @return java.util.Collection<com.ppwx.easysearch.cf.data.UserRating>
     */
    Collection<UserRating> getUA(String userId);

    /**
     * 推荐商品
     *
     * @param userId
     * @param topK
     * @return java.util.Collection<net.librec.recommender.item.ItemEntry<java.lang.String,java.lang.Double>>
     */
    Collection<ItemEntry<String, Double>> rank(String userId, int topK);

    /**
     * 推荐商品
     *
     * @param userId
     * @param minScore 限定最小分值
     * @param topK
     * @return java.util.Collection<net.librec.recommender.item.ItemEntry<java.lang.String,java.lang.Double>>
     */
    Collection<ItemEntry<String, Double>> rank(String userId, double minScore, int topK);

    /**
     * 推荐商品
     *
     * @param ua 用户的评分数据
     * @param minScore
     * @param topK
     * @return java.util.Collection<net.librec.recommender.item.ItemEntry<java.lang.String,java.lang.Double>>
     */
    Collection<ItemEntry<String, Double>> rank(Collection<UserRating> ua, double minScore, int topK);

}