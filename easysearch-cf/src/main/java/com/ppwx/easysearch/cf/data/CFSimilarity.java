/**
 * Copyright (C), 2010-2024, 爱回收
 * FileName: CFSimilarity
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2024/7/28 11:32
 * Description: cf分值信息
 */
package com.ppwx.easysearch.cf.data;

import com.google.common.base.Splitter;
import com.google.common.collect.BiMap;
import com.google.common.collect.Maps;
import net.librec.math.structure.SymmMatrix;

import java.util.Iterator;
import java.util.Map;

/**
 *
 * cf分值信息
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2024/07/28 11:32
 * @since 1.0.0
 */
public class CFSimilarity {

    /**
     * 评分数据
     */
    private final Map<String, Double> scores;

    public CFSimilarity() {
        this.scores = Maps.newHashMapWithExpectedSize(1024);
    }

    public CFSimilarity(Map<String, Double> scores) {
        this.scores = scores;
    }

    /**
     * 按ASCII高位在前
     *
     * @param one
     * @param other
     * @return java.lang.String
     */
    public String join(String one, String other) {
        if (one.compareTo(other) >= 0) {
            return one + "-" + other;
        } else {
            return other + "-" + one;
        }
    }

    /**
     * 设置值
     *
     * @param one
     * @param other
     * @param score
     * @return void
     */
    public void setScore(String one, String other, double score) {
        this.scores.put(join(one, other), score);
    }

    /**
     * 数据拷贝到矩阵中
     *
     * @param items
     * @param matrix
     * @return void
     */
    public void copyTo(BiMap<String, Integer> items, SymmMatrix matrix) {
        copyTo(items, matrix, false);
    }

    /**
     * 数据拷贝到矩阵中
     *
     * @param items
     * @param matrix
     * @param ignoreNew 是否忽略新出现的标的物 true-忽略
     * @return void
     */
    public void copyTo(BiMap<String, Integer> items, SymmMatrix matrix, boolean ignoreNew) {
        Splitter splitter = Splitter.on("-");
        scores.forEach((k,v) -> {
            Iterator<String> iterator = splitter.split(k).iterator();
            String item = iterator.next();
            int row;
            int column;
            if (items.containsKey(item)) {
                row = items.get(item);
            } else {
                if (ignoreNew) {
                    return;
                }
                row = items.size();
            }
            items.put(item, row);

            item = iterator.next();
            if (items.containsKey(item)) {
                column = items.get(item);
            } else {
                if (ignoreNew) {
                    return;
                }
                column = items.size();
            }
            items.put(item, column);

            matrix.set(row, column, v);
        });
    }

    /**
     * 从评分矩阵中构建
     *
     * @param items
     * @param matrix
     * @return com.ppwx.easysearch.cf.data.CFSimilarity
     */
    public static CFSimilarity from(BiMap<String, Integer> items, SymmMatrix matrix) {
        BiMap<Integer, String> inverse = items.inverse();
        CFSimilarity cfSimilarity = new CFSimilarity();
        int dim = matrix.getDim();
        for (int i = 0; i < dim; i++) {
            for (int j = i; j < dim; j++) {
                if (matrix.get(i, j) != 0) {
                    cfSimilarity.setScore(inverse.get(i), inverse.get(j), matrix.get(i, j));
                }
            }
        }
        return cfSimilarity;
    }

    public static String[] getBiItemId(String uid) {
        return uid.split("-");
    }

    public Map<String, Double> getScores() {
        return scores;
    }
}