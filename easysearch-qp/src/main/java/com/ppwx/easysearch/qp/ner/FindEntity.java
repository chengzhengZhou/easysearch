package com.ppwx.easysearch.qp.ner;

import cn.hutool.core.lang.DefaultSegment;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className FindEntity
 * @description 实体信息
 * @date 2024/10/9 14:26
 **/
@Deprecated
public class FindEntity extends DefaultSegment<Integer> {

    /**
     * 词条
     */
    private final String term;

    /**
     * 类型
     */
    private final String type;

    private final int weight;

    /**
     * 构造
     * @param term 实体内容
     * @param type 实体类型
     * @param weight 权重
     * @param startIndex 起始位置
     * @param endIndex   结束位置
     */
    public FindEntity(String term, String type, int weight, Integer startIndex, Integer endIndex) {
        super(startIndex, endIndex);
        this.term = term;
        this.type = type;
        this.weight = weight;
    }

    public String getType() {
        return type;
    }

    public String getTerm() {
        return term;
    }

    public int getWeight() {
        return weight;
    }

    @Override
    public String toString() {
        return "FindEntity{" +
                "term='" + term + '\'' +
                ", type='" + type + '\'' +
                ", weight=" + weight +
                ", startIndex=" + startIndex +
                ", endIndex=" + endIndex +
                '}';
    }
}
