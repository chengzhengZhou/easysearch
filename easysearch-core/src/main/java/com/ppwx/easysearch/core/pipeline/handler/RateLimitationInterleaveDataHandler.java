/**
 * Copyright (C), 2010-2024, 爱回收
 * FileName: RateLimitationInterleaveDataHandler
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2024/7/1 15:50
 * Description: 控制出向比率
 */
package com.ppwx.easysearch.core.pipeline.handler;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.ppwx.easysearch.core.data.Column;

import java.util.*;

/**
 *
 * 控制露出比率（不可共享）
 * 
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2024/07/01 15:50
 * @since 1.0.0
 */
public class RateLimitationInterleaveDataHandler extends SlidingWindowInterleaveDataHandler {

    /**
     * 限制比例
     */
    private double limitRate;
    /**
     * 打散比较字段
     */
    private String field;

    public void setLimitRate(double limitRate) {
        this.limitRate = limitRate;
    }

    public void setField(String field) {
        this.field = field;
    }

    @Override
    protected void process(int from, int l, int r, List<Map<String, Column>> items) {
        // avoid out of array index
        if (r + 1 >= items.size()) {
            return;
        }
        // 下标集合
        Multimap<String, Integer> indexMap = HashMultimap.create();
        // 计数桶
        Map<String, Integer> bulk = Maps.newHashMap();

        String key;
        for(int i = l; i <= r; i++) {
            Map<String, Column> map = items.get(i);
            key = map.get(field).asString();
            indexMap.put(key, i);
            bulk.compute(key, (k, v) -> v == null ? 1 : v + 1);
        }
        int limitSize = (int) (getDefaultWindowSize() * limitRate);
        for (String field : bulk.keySet()) {
            if (indexMap.get(field).size() > limitSize) {
                // should interleave items
                int count = indexMap.get(field).size() - limitSize;
                List<Integer> list = new ArrayList<>(indexMap.get(field));
                Collections.shuffle(list);
                list = list.subList(0, count);
                // find next
                boolean find;
                int idx = r + 1;
                for (int replaceIdx : list) {
                    for(find = false ; idx < items.size(); ++idx) {
                        if (!this.isSimilar(items.get(replaceIdx), items.get(idx), bulk, limitSize)) {
                            this.swap(idx, replaceIdx, items);
                            find = true;
                            break;
                        }
                    }

                    if (!find) {
                        break;
                    }
                }

            }
        }
    }

    /**
     * 判断两标的物是否相似
     *
     * @param origin
     * @param target
     * @param bulk
     * @param limitSize
     * @return boolean
     */
    protected boolean isSimilar(Map<String, Column> origin, Map<String, Column> target, Map<String, Integer> bulk, int limitSize) {
        Object originVal = origin.get(field).getRawData();
        Object targetVal = target.get(field).getRawData();
        boolean equals = Objects.equals(originVal, targetVal);
        int count = bulk.getOrDefault(targetVal.toString(), 0);
        if (!equals && count < limitSize) {
            bulk.computeIfPresent(targetVal.toString(), (k, v) -> v + 1);
            return false;
        }
        return true;
    }

}