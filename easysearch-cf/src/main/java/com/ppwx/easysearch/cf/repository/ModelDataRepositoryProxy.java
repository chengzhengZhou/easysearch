package com.ppwx.easysearch.cf.repository;

import com.google.common.collect.Multimap;
import com.ppwx.easysearch.cf.data.CFSimilarity;
import com.ppwx.easysearch.cf.data.UserRating;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className CacheESModelDataRepository
 * @description 组合使用多个数据源，这里按用户偏好和相似矩阵进行拆分
 * @date 2025/2/11 13:41
 **/
public class ModelDataRepositoryProxy implements ModelDataRepository {
    /**
     * 用户偏好数据
     */
    private final ModelDataRepository userDataRepository;
    /**
     * 评分矩阵
     */
    private final ModelDataRepository cfDataRepository;

    public ModelDataRepositoryProxy(ModelDataRepository userDataRepository, ModelDataRepository cfDataRepository) {
        this.userDataRepository = userDataRepository;
        this.cfDataRepository = cfDataRepository;
    }

    @Override
    public Collection<UserRating> getUserRating(String userId) {
        return userDataRepository.getUserRating(userId);
    }

    @Override
    public CFSimilarity getCFScoreV2() {
        return cfDataRepository.getCFScoreV2();
    }

    @Override
    public CFSimilarity getCFScoreV2(Date beginTime, Date endTime) {
        return cfDataRepository.getCFScoreV2(beginTime, endTime);
    }

    @Override
    public CFSimilarity getCFScore(Collection<String> items) {
        return cfDataRepository.getCFScore(items);
    }

    @Override
    public CFSimilarity getSameItemCFScore() {
        return cfDataRepository.getSameItemCFScore();
    }

    @Override
    public void saveUserRating(String userId, List<UserRating> ratings) {
        userDataRepository.saveUserRating(userId, ratings);
    }

    @Override
    public void saveUserRating(Multimap<String, UserRating> userMap) {
        userDataRepository.saveUserRating(userMap);
    }

    @Override
    public void saveCFScore(CFSimilarity similarity) {
        cfDataRepository.saveCFScore(similarity);
    }

    @Override
    public void cleanSimilarity(CleanStrategy strategy) {
        userDataRepository.cleanSimilarity(strategy);
        if (userDataRepository.getClass().equals(cfDataRepository.getClass())) {
            cfDataRepository.cleanSimilarity(strategy);
        }
    }
}
