/**
 * Copyright (C), 2010-2024, 爱回收
 * FileName: BloomFilterChunk
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2024/1/27 22:32
 * Description: 布隆过滤器块
 */
package com.ppwx.easysearch.core.common.bloom;

import com.ppwx.easysearch.core.util.CurrentTimeUtil;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 *
 * 布隆过滤器块
 * 由多个小的布隆过滤器组成一个块，方便管理
 * 淘汰策略：采用溢出后删除最老的；过有效期的清除；
 *
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2024/01/27 22:32
 * @since 1.0.0
 */
public class BloomFilterChunk implements BloomFilter {
    /**
     * 有效的布隆过滤器
     */
    private List<MetaBloomFilter> bloomFilters;
    /**
     * 被淘汰的过滤器
     */
    private List<MetaBloomFilter> removedFilters;
    /**
     * chunk的size
     */
    private int size;
    /**
     * 过期时间
     */
    private int expireTime;
    /**
     * 时间单位
     */
    private TimeUnit timeUnit;

    /**
     * 构造器，由外部传入布隆过滤器
     *
     * @param size
     * @param expireTime
     * @param timeUnit
     * @param bloomFilters
     */
    public BloomFilterChunk(int size, int expireTime, TimeUnit timeUnit, List<MetaBloomFilter> bloomFilters) {
        if (size < 1) {
            throw new IllegalArgumentException("Bloom filters size can't be smaller than 1");
        }
        if (expireTime < 1) {
            throw new IllegalArgumentException("Bloom filter expire time can't be smaller than 1");
        }
        this.size = size;
        this.expireTime = expireTime;
        this.timeUnit = timeUnit;
        this.bloomFilters = bloomFilters;
        invalidFilter();
    }

    public void tryInit() {
        invalidFilter();
    }

    /**
     * 过滤掉大小溢出的和过期的
     * 本地执行期间，只对溢出后执行淘汰策略
     *
     * @param
     * @return void
     */
    private void invalidFilter() {
        checkNotEmptyFilter();
        // order by time
        bloomFilters.sort(Comparator
                .comparingLong(MetaBloomFilter::getStartTime)
                .thenComparingLong(MetaBloomFilter::getLastTime));

        MetaBloomFilter head = bloomFilters.get(0);
        // size limit
        int bloomSize = bloomFilters.size();
        int diff = bloomSize - this.size;
        if (diff > 0) {
            if (removedFilters == null) {
                removedFilters = new ArrayList<>(2);
            }
            removedFilters.addAll(bloomFilters.subList(0, diff));
            bloomFilters = bloomFilters.subList(diff, bloomSize);
        }

        // time limit
        Iterator<MetaBloomFilter> iterator = bloomFilters.iterator();
        MetaBloomFilter next;
        while (iterator.hasNext()) {
            next = iterator.next();
            if (CurrentTimeUtil.currentTimeMillis() - next.getLastTime() > this.timeUnit.toMillis(this.expireTime)) {
                iterator.remove();
                if (removedFilters == null) {
                    removedFilters = new ArrayList<>(2);
                }
                removedFilters.add(next);
            }
        }

        // at lease one filter
        if (bloomFilters.isEmpty()) {
            bloomFilters.add((MetaBloomFilter) head.clone());
        }
    }


    @Override
    public boolean contains(String s) {
        return bloomFilters.stream().anyMatch(filter -> filter.contains(s));
    }

    @Override
    public int getExpectedInsertions() {
        return bloomFilters.stream().mapToInt(MetaBloomFilter::getExpectedInsertions).sum();
    }

    @Override
    public double getFalseProbability() {
        return bloomFilters.get(0).getFalseProbability();
    }

    @Override
    public int getSize() {
        return bloomFilters.stream().mapToInt(MetaBloomFilter::getSize).sum();
    }

    @Override
    public int getHashIterations() {
        return bloomFilters.get(0).getHashIterations();
    }

    @Override
    public int count() {
        return bloomFilters.stream().mapToInt(MetaBloomFilter::count).sum();
    }

    @Override
    public boolean isFull() {
        return false;
    }

    @Override
    public boolean add(String s) {
        // find first not full node
        Optional<MetaBloomFilter> find = bloomFilters.stream().filter(filter -> !filter.isFull()).findFirst();
        if (find.isPresent()) {
            return find.get().add(s);
        }
        // add new filter
        MetaBloomFilter metaBloomFilter = (MetaBloomFilter) bloomFilters.get(0).clone();
        boolean add = metaBloomFilter.add(s);
        bloomFilters.add(metaBloomFilter);

        if (bloomFilters.size() > this.size) {
            invalidFilter();
        }
        return add;
    }

    public List<MetaBloomFilter> getBloomFilters() {
        return bloomFilters;
    }

    public List<MetaBloomFilter> getRemovedFilters() {
        return removedFilters;
    }

    private void checkNotEmptyFilter() {
        if (bloomFilters == null || bloomFilters.isEmpty()) {
            throw new IllegalArgumentException("empty filters error. you must initialize at least one MetaBloomFilter");
        }
    }
}