/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: SumDoubleArray
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/7/4 21:13
 * Description: 数组计算辅助类
 */
package com.ppwx.easysearch.core.common;

import com.ppwx.easysearch.core.common.enums.JoinModeEnum;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * 数组计算辅助类
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/07/04 21:13
 * @since 1.0.0
 */
public class ScoreJoiner {
    /**
     * Zero score
     */
    public static final Double ZERO = 0.0;
    /**
     * 当前数组
     */
    private double[] array;
    /**
     * 权重
     */
    private Double weight;
    /**
     * 叠加的数组
     */
    private List<ScoreJoiner> sumDoubleArrays;
    /**
     * 算分明细
     */
    private ScoreDetail[] detailArray;
    /**
     * 评分组合关系，暂时只支持加和乘
     */
    private JoinModeEnum joinMode;
    /**
     * 父级
     */
    private ScoreJoiner parent;

    ScoreJoiner() {
    }

    /**
     * 添加数组
     *
     * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
     * @param scoreJoiner
     * @date 2023/7/5 10:58
     * @return com.aihuishou.szc2c.c2c.linli.business.service.util.score.SumDoubleArray
     */
    public synchronized ScoreJoiner join(ScoreJoiner scoreJoiner) {
        if (sumDoubleArrays == null) {
            sumDoubleArrays = new ArrayList<>();
        }
        sumDoubleArrays.add(scoreJoiner);
        scoreJoiner.setParent(this);
        return this;
    }
    /**
     * 叠加两个数组
     *
     * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
     * @param 
     * @date 2023/7/4 21:36
     * @return double[]
     */
    public double[] evaluate() {
        double[] resultArr;
        if (weight == null) {
            resultArr = Arrays.copyOf(array, array.length);
        } else {
            resultArr = Arrays.stream(array)
                    .map(v -> v * weight)
                    .toArray();
        }

        if (sumDoubleArrays != null) {
            sumDoubleArrays.forEach(sumDoubleArray -> {
                double[] nextArr = sumDoubleArray.evaluate();
                int minLen = Math.min(resultArr.length, nextArr.length);
                for (int i = 0; i < minLen; i++) {
                    if (joinMode == JoinModeEnum.MULTIPLY) {
                        if (Double.compare(resultArr[i], 0.0) == 0 || Double.compare(nextArr[i], 0.0) == 0) {
                            resultArr[i] = Math.max(resultArr[i], nextArr[i]);
                        } else {
                            resultArr[i] = resultArr[i] * nextArr[i];
                        }
                    } else {
                        resultArr[i] = resultArr[i] + nextArr[i];
                    }
                }
            });
        }
        return resultArr;
    }

    /**
     * 获取特定明细
     *
     * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
     * @param i
     * @date 2023/7/7 0:42
     * @return java.util.List<com.aihuishou.szc2c.c2c.linli.business.service.util.score.ScoreDetail>
     */
    public List<ScoreDetail> details(int i) {
        List<ScoreDetail> list = new ArrayList<>();
        if (detailArray[i] != null) {
            list.add(detailArray[i]);
        }
        if (sumDoubleArrays != null) {
            sumDoubleArrays.forEach(sumDoubleArray -> list.addAll(sumDoubleArray.details(i)));
        }
        return list;
    }

    /**
     * 构建一个数据叠加器
     *
     * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
     * @param array
     * @param weight
     * @date 2023/7/4 21:27
     * @return com.aihuishou.szc2c.c2c.linli.business.service.util.score.SumDoubleArray
     */
    public static ScoreJoiner of(double[] array, Double weight) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException("Array must not be empty or null");
        }
        ScoreJoiner sumDoubleArray = new ScoreJoiner();
        sumDoubleArray.array = array;
        sumDoubleArray.weight = weight;
        sumDoubleArray.detailArray = new ScoreDetail[array.length];
        sumDoubleArray.joinMode = JoinModeEnum.SUM;
        return sumDoubleArray;
    }
    /**
     * 构建一个初始容器
     *
     * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
     * @param initSize
     * @date 2023/7/5 18:42
     * @return com.aihuishou.szc2c.c2c.linli.business.service.util.score.SumDoubleArray
     */
    public static ScoreJoiner ofEmpty(int initSize) {
        double[] arr = new double[initSize];
        for (int i = 0; i < initSize; i++) {
            arr[i] = 0;
        }
        ScoreJoiner sumDoubleArray = new ScoreJoiner();
        sumDoubleArray.array = arr;
        sumDoubleArray.detailArray = new ScoreDetail[initSize];
        sumDoubleArray.joinMode = JoinModeEnum.SUM;
        return sumDoubleArray;
    }
    /**
     * 设置算分结果
     *
     * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
     * @param i
     * @param detail
     * @date 2023/7/6 15:56
     * @return void
     */
    public void setScoreDetail(int i, ScoreDetail detail) {
        this.array[i] = detail.getScore();
        this.detailArray[i] = detail;
    }

    double[] getArray() {
        return array;
    }

    ScoreDetail[] getDetailArray() {
        return detailArray;
    }

    /**
     * 获取得分
     *
     * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
     * @param i
     * @date 2023/7/6 17:09
     * @return double
     */
    public double getScore(int i) {
        return this.array[i];
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public void setJoinMode(JoinModeEnum joinMode) {
        this.joinMode = joinMode;
    }

    public void setParent(ScoreJoiner parent) {
        this.parent = parent;
    }

    public ScoreJoiner getParent() {
        return parent;
    }

    public ScoreJoiner getRoot() {
        if (this.parent != null) {
            return this.parent.getRoot();
        }
        return this;
    }
}