/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: ScoreDetail
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/7/6 15:01
 * Description: 算分明细
 */
package com.ppwx.easysearch.core.common;

/**
 *
 * 算分明细信息
 * 当前先简单记录各因子的分值进行输出即可
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/07/06 15:01
 * @since 1.0.0
 */
public class ScoreDetail {
    /** 描述 */
    private String desc;
    /** 权重 */
    private Double weight;
    /** 分值 */
    private Double score;

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    @Override
    public String toString() {
        return "ScoreDetail{" +
                "desc='" + desc + '\'' +
                ", weight=" + weight +
                ", score=" + score +
                '}';
    }
}