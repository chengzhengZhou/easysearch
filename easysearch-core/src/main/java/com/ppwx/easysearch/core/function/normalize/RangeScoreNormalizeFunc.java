/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: RangeScoreNormalizeFunc
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/7/6 10:51
 * Description: 数据段评分归一
 */
package com.ppwx.easysearch.core.function.normalize;

import com.ppwx.easysearch.core.data.Column;
import com.ppwx.easysearch.core.function.NumberScoreFunction;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * 数据段评分归一
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/07/06 10:51
 * @since 1.0.0
 */
public class RangeScoreNormalizeFunc implements NumberScoreFunction<Double> {
    /** 分值映射表 */
    private Map<Double, Double> mapping;
    /** 数据范围段 */
    private List<Double> rangeArr;
    /** 溢出分段的分值 */
    private double overflowScore;
    /**
     * 构造器
     *
     * @param mapping
     * @param overflowScore 超出最大范围的分值
     * @return
     * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
     * @date 2023/7/4 19:46
     */
    public RangeScoreNormalizeFunc(Map<Double, Double> mapping, double overflowScore) {
        assert mapping != null;
        parseMapping(mapping);
        this.overflowScore = overflowScore;
    }

    /**
     * 将数据分段映射,去头含尾
     * 示例：1km内 +0.9分；3km内 +0.8分；5km内 +0.6分；10km内+0.4分；10km及以上 +0.2分
     * 将被解析为[1,3,5,10]，映射段为：1-1km内，3-3km内，5-5km内，10-10km内,Double.max-10km以上
     *
     * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
     * @param mapping
     * @date 2023/7/6 11:08
     * @return void
     */
    private void parseMapping(Map<Double, Double> mapping) {
        List<Double> rangeArr = mapping.keySet().stream().collect(Collectors.toList());
        Collections.sort(rangeArr);
        this.rangeArr = rangeArr;
        this.mapping = new HashMap<>(mapping);
    }

    @Override
    public double score(Number value) {
        Optional<Double> find = rangeArr.stream().filter(v -> Double.compare(value.doubleValue(), v) <= 0).findFirst();
        if (find.isPresent()) {
            return mapping.get(find.get());
        }
        return overflowScore;
    }

    @Override
    public Double apply(Column column) {
        return score(column.asDouble());
    }
}