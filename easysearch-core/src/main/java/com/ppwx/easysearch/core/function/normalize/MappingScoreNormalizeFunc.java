/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: MappingScoreNormalization
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/7/6 10:52
 * Description: 数据映射分值归一
 */
package com.ppwx.easysearch.core.function.normalize;

import com.ppwx.easysearch.core.data.Column;
import com.ppwx.easysearch.core.function.TextScoreFunction;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * 数据映射分值归一
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/07/06 10:52
 * @since 1.0.0
 */
public class MappingScoreNormalizeFunc implements TextScoreFunction {
    /** 分值映射表 */
    private Map<String, Double> mapping;
    /** 未映射时的默认值 */
    private double defaultScore;
    /**
     * 构造器
     *
     * @param mapping
     * @param defaultScore
     * @return
     * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
     * @date 2023/7/4 19:46
     */
    public MappingScoreNormalizeFunc(Map<String, Double> mapping, double defaultScore) {
        assert mapping != null;
        this.mapping = new HashMap<>(mapping);
        this.defaultScore = defaultScore;
    }

    @Override
    public double score(String value) {
        return mapping.containsKey(value) ? mapping.get(value) : defaultScore;
    }

    @Override
    public Double apply(Column column) {
        return score(column.asString());
    }
}