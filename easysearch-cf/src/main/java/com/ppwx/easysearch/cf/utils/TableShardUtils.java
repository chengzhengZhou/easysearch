/**
 * Copyright (C), 2010-2024, 爱回收
 * FileName: TableShardUtils
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2024/8/5 18:09
 * Description: 分表工具类
 */
package com.ppwx.easysearch.cf.utils;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * 数据库分表工具类
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2024/08/05 18:09
 * @since 1.0.0
 */
public final class TableShardUtils {

    /**
     * 数据分段
     *
     * @param count
     * @param min
     * @param max
     * @param rangeSize 期望的分段大小
     * @return List<BigInteger>
     */
    public static List<BigInteger> splitRange(long count, BigInteger min, BigInteger max, int rangeSize) {
        int shards = (int) ((count % rangeSize > 0) ? count / rangeSize + 1 : count / rangeSize);
        BigInteger offset = max.subtract(min).divide(BigInteger.valueOf(shards));
        if (offset.compareTo(BigInteger.ZERO) == 0) {
            offset = BigInteger.ONE;
        }
        List<BigInteger> ranges = Lists.newArrayListWithCapacity(shards + 1);
        ranges.add(min);
        BigInteger value = min;
        for (int i = 0; i < shards - 1; i++) {
            value = value.add(offset);
            ranges.add(value);
        }
        ranges.add(max);
        return ranges;
    }

    /**
     * 拼接区段sql条件
     *
     * @param rangeList 分段数据
     * @param columnName 字段名
     * @param quote 字段包围的字符
     * @return java.util.List<java.lang.String>
     */
    public static List<String> wrapRange(List<BigInteger> rangeList, String columnName, String quote) {
        int minLen = 2;
        if (null == rangeList || rangeList.size() < minLen) {
            throw new IllegalArgumentException(String.format(
                    "Parameter rangeResult can not be null and its length can not <2. detail:rangeResult=[%s].",
                    StringUtils.join(rangeList, ",")));
        }
        List<String> result = new ArrayList<String>();

        if (minLen == rangeList.size()) {
            result.add(new StringBuilder("(")
                    .append(quote).append(rangeList.get(0)).append(quote).append("<=`").append(columnName)
                    .append("` AND `").append(columnName).append("`<=").append(quote).append(rangeList.get(1)).append(quote)
                    .append(")").toString());
        } else {
            for (int i = 0, len = rangeList.size() - minLen; i < len; i++) {
                result.add(new StringBuilder("(")
                        .append(quote).append(rangeList.get(i)).append(quote).append("<=`").append(columnName)
                        .append("` AND `").append(columnName).append("`<").append(quote).append(rangeList.get(i + 1)).append(quote)
                        .append(")").toString());
            }
            result.add(new StringBuilder("(")
                    .append(quote).append(rangeList.get(rangeList.size() - 2)).append(quote).append("<=`").append(columnName)
                    .append("` AND `").append(columnName).append("`<=").append(quote).append(rangeList.get(rangeList.size() - 1)).append(quote)
                    .append(")").toString());
        }
        return result;
    }

    /**
     * 获取分片值
     *
     * @param key
     * @param tableNums
     * @return int
     */
    public static int getShardNum(String key, int tableNums) {
        return fnvHash(key) % tableNums;
    }

    /**
     * 改进的32位FNV算法1
     *
     * @param data 字符串
     * @return hash结果
     */
    public static int fnvHash(String data) {
        final int p = 16777619;
        int hash = (int) 2166136261L;
        for (int i = 0; i < data.length(); i++) {
            hash = (hash ^ data.charAt(i)) * p;
        }
        hash += hash << 13;
        hash ^= hash >> 7;
        hash += hash << 3;
        hash ^= hash >> 17;
        hash += hash << 5;
        return Math.abs(hash);
    }

}