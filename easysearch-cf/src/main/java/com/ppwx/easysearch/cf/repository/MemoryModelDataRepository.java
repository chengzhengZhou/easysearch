/**
 * Copyright (C), 2010-2024, 爱回收
 * FileName: MemoryModelDataRepository
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2024/7/28 11:43
 * Description: 基于内存存储实现
 */
package com.ppwx.easysearch.cf.repository;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.ppwx.easysearch.cf.data.CFSimilarity;
import com.ppwx.easysearch.cf.data.UserRating;
import com.ppwx.easysearch.core.common.DataException;

import java.math.BigDecimal;
import java.util.*;

/**
 *
 * 基于内存存储实现
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2024/07/28 11:43
 * @since 1.0.0
 */
public class MemoryModelDataRepository implements ModelDataRepository {

    private final Map<String, List<UserRating>> userRatingMap;

    private final Map<String, Double> scoreMap;

    public MemoryModelDataRepository() {
        this.userRatingMap = Maps.newHashMap();
        this.scoreMap = Maps.newHashMap();
    }

    @Override
    public Collection<UserRating> getUserRating(String userId) {
        if (userRatingMap.containsKey(userId)) {
            return userRatingMap.get(userId);
        }
        return Collections.emptyList();
    }

    @Override
    public CFSimilarity getCFScoreV2() {
        return new CFSimilarity(scoreMap);
    }

    @Override
    public CFSimilarity getCFScoreV2(Date beginTime, Date endTime) {
        throw new DataException("not support option");
    }

    @Override
    public CFSimilarity getCFScore(Collection<String> items) {
        throw new DataException("not support option");
    }

    @Override
    public CFSimilarity getSameItemCFScore() {
        throw new DataException("not support option");
    }


    @Override
    public void saveUserRating(String userId, List<UserRating> ratings) {
        userRatingMap.put(userId, ratings);
    }

    @Override
    public void saveUserRating(Multimap<String, UserRating> userMap) {
        for (String user : userMap.keySet()) {
            userRatingMap.put(user, new ArrayList<>(userMap.get(user)));
        }
    }

    @Override
    public void saveCFScore(CFSimilarity similarity) {
        Map<String, Double> scores = similarity.getScores();
        Double old;
        for (String k : scores.keySet()) {
            old = scoreMap.get(k);
            if (old == null) {
                scoreMap.put(k, scores.get(k));
            } else {
                scoreMap.put(k, BigDecimal.valueOf(old + scores.get(k)).doubleValue());
            }
        }
    }

    @Override
    public void cleanSimilarity(CleanStrategy strategy) {
        scoreMap.clear();
    }

}