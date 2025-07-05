package com.ppwx.easysearch.cf.repository;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.ppwx.easysearch.cf.data.UserRating;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className AbstractPersistentDataRepository
 * @description 模型持久化存储基类
 * @date 2025/2/8 17:54
 **/
public abstract class AbstractPersistentDataRepository implements ModelDataRepository {

    static final String CF = "model_cf";
    static final String UA = "user_action";
    static final String COLON = ":";
    static final String SEAL = ";";
    static final Splitter SEPARATE_1 = Splitter.on(COLON);
    static final Splitter SEPARATE_2 = Splitter.on(SEAL);
    static final int DEFAULT_FETCH_SIZE = 10000;
    /**
     * 2024-08-01 00:00:00的毫秒值
     */
    static final long BASE_TIME = 1722441600000L;

    private int fetchSize = DEFAULT_FETCH_SIZE;

    public void setFetchSize(int fetchSize) {
        this.fetchSize = fetchSize;
    }

    public int getFetchSize() {
        return fetchSize;
    }

    /**
     * 压缩数据
     * {itemId}:{rate}:{time};
     *
     * @param userRatings
     * @return java.lang.String
     */
    protected String compress(Collection<UserRating> userRatings) {
        if (CollectionUtils.isEmpty(userRatings)) {
            return "";
        }
        return userRatings.stream().map(userRating ->
                        new StringBuilder(userRating.getItemId()).append(COLON)
                                .append(userRating.getRate()).append(COLON)
                                .append(userRating.getDatetime() == null ? "" : (userRating.getDatetime().getTime() - BASE_TIME) / 1000))
                .collect(Collectors.joining(SEAL));
    }

    /**
     * 解压数据
     *
     * @param value
     * @return java.util.List<com.ppwx.easysearch.cf.data.UserRating>
     */
    protected List<UserRating> decompress(String value) {
        if (StringUtils.isBlank(value)) {
            return Collections.emptyList();
        }
        Iterator<String> itemIterator = SEPARATE_2.split(value).iterator();
        List<UserRating> list = Lists.newArrayListWithExpectedSize(20);
        String itemId;
        String rate;
        String time;
        while (itemIterator.hasNext()) {
            Iterator<String> valIterator = SEPARATE_1.split(itemIterator.next()).iterator();
            itemId = valIterator.next();
            rate = valIterator.next();
            time = valIterator.next();
            list.add(new UserRating(itemId, Double.valueOf(rate),
                    StringUtils.isBlank(time) ? null : (new Date(Long.parseLong(time) * 1000 + BASE_TIME))));
        }
        return list;
    }
}
