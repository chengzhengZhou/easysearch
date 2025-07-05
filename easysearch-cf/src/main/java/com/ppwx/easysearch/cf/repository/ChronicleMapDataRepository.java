package com.ppwx.easysearch.cf.repository;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.ppwx.easysearch.cf.data.CFSimilarity;
import com.ppwx.easysearch.cf.data.UserRating;
import com.ppwx.easysearch.core.common.DataException;
import net.openhft.chronicle.map.ChronicleMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className ChronicleMapDataRepository
 * @description 基于Chronicle Map构建
 * @date 2025/2/14 9:51
 **/
public class ChronicleMapDataRepository extends AbstractPersistentDataRepository {

    private static final Logger LOG = LoggerFactory.getLogger(ChronicleMapDataRepository.class);

    private final ChronicleMap<String, String> userRatingMap;

    private final ChronicleMap<String, Double> scoreMap;

    public ChronicleMapDataRepository(long matrixSize, long userSize) {
        // 创建Chronicle Map实例
        scoreMap = ChronicleMap
                .of(String.class, Double.class)
                .name("scoreMap")
                .entries(matrixSize)
                .averageKeySize(20)
                .create();
        userRatingMap = ChronicleMap
                .of(String.class, String.class)
                .name("userRatingMap")
                .entries(userSize)
                .averageValueSize(320)
                .averageKeySize(10)
                .create();
    }

    @Override
    public Collection<UserRating> getUserRating(String userId) {
        String val = userRatingMap.get(userId);
        if (val != null) {
            return decompress(val);
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
        Map<String, Double> scores = Maps.newHashMap();
        scoreMap.forEach((k, v) -> {
            String[] ids = CFSimilarity.getBiItemId(k);
            if (items.contains(ids[0]) || items.contains(ids[1])) {
                scores.put(k, v);
            }
        });
        return new CFSimilarity(scores);
    }

    @Override
    public CFSimilarity getSameItemCFScore() {
        Map<String, Double> scores = Maps.newHashMap();
        scoreMap.forEach((k, v) -> {
            String[] ids = CFSimilarity.getBiItemId(k);
            if (StringUtils.equals(ids[0], ids[1])) {
                scores.put(k, v);
            }
        });
        return new CFSimilarity(scores);
    }

    @Override
    public void saveUserRating(String userId, List<UserRating> ratings) {
        userRatingMap.put(userId, compress(ratings));
    }

    @Override
    public void saveUserRating(Multimap<String, UserRating> userMap) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        int count = 0;
        for (String user : userMap.keySet()) {
            saveUserRating(user, new ArrayList<>(userMap.get(user)));
            count++;
        }
        stopWatch.stop();
        LOG.info("finish saveUserRating total:{}, elapse:{}ms", count, stopWatch.getTotalTimeMillis());
    }

    @Override
    public void saveCFScore(CFSimilarity similarity) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        AtomicInteger count = new AtomicInteger(0);
        Map<String, Double> scores = similarity.getScores();
        scores.forEach((k, v) -> {
            Double old = scoreMap.get(k);
            if (old == null) {
                scoreMap.put(k, scores.get(k));
            } else {
                scoreMap.put(k, BigDecimal.valueOf(old + scores.get(k)).doubleValue());
            }
            count.incrementAndGet();
        });
        stopWatch.stop();
        LOG.info("finish saveCFScore total:{}, elapse:{}ms", count.get(), stopWatch.getTotalTimeMillis());
    }

    @Override
    public void cleanSimilarity(CleanStrategy strategy) {
        userRatingMap.clear();
        userRatingMap.close();
        scoreMap.clear();
        scoreMap.close();
    }
}
