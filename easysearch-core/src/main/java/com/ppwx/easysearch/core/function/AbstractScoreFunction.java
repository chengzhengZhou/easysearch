/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: AbstractNormalizer
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/7/4 19:36
 * Description: 归一化函数
 */
package com.ppwx.easysearch.core.function;

import com.ppwx.easysearch.core.data.Column;
import com.ppwx.easysearch.core.data.element.DateColumn;
import com.ppwx.easysearch.core.data.element.DoubleColumn;
import com.ppwx.easysearch.core.data.element.DynamicColumn;
import com.ppwx.easysearch.core.data.element.LongColumn;

import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * 抽象实现
 * 通用性算法是基于当前数据集进行的，该抽象类主要实现一些通用的计算方法
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/26 19:36
 * @since 1.0.0
 */
public abstract class AbstractScoreFunction implements NumberScoreFunction<Double> {
    /**
     * 需要处理的数据
     */
    protected List<Column> columns;
    /**
     * 需要缓存的数据
     */
    private ConcurrentHashMap<String, Double> valueCache;

    /**
     * 无参构造
     *
     * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
     * @param
     * @date 2023/7/6 23:43
     * @return
     */
    public AbstractScoreFunction() {
        this(null);
    }

    /**
     * 构造器
     *
     * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
     * @param columns
     * @date 2023/7/4 19:46
     * @return
     */
    public AbstractScoreFunction(List<Column> columns) {
        this.columns = columns;
        this.valueCache = new ConcurrentHashMap<>(8);
    }
    /**
     * 最小值
     *
     * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
     * @param 
     * @date 2023/7/4 20:08
     * @return int
     */
    public final double min() {
        return valueCache.computeIfAbsent("min", k -> {
            checkNotNull();

            OptionalDouble min = columns.stream().filter(Objects::nonNull).mapToDouble(Column::asLong).min();
            return min.orElse(0.0);
        });
    }
    /**
     * 最大值
     *
     * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
     * @param
     * @date 2023/7/4 20:08
     * @return int
     */
    public final double max() {
        return valueCache.computeIfAbsent("max", k -> {
            checkNotNull();

            OptionalDouble max = columns.stream().filter(Objects::nonNull).mapToDouble(Column::asLong).max();
            return max.orElse(0.0);
        });
    }
    /**
     * 计算平均值
     *
     * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
     * @param
     * @date 2023/7/4 20:14
     * @return double
     */
    public final double mean() {
        return valueCache.computeIfAbsent("mean", k -> {
            checkNotNull();

            OptionalDouble average = columns.stream().filter(Objects::nonNull).mapToDouble(Column::asLong).average();
            return average.orElse(0.0);
        });
    }
    /**
     * 计算标准差
     *
     * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
     * @param
     * @date 2023/7/4 20:18
     * @return double
     */
    public final double standardDeviation() {
        return valueCache.computeIfAbsent("standardDeviation", k -> {
            checkNotNull();

            double sum = 0.0;
            double mean = mean();

            for (Column col : columns) {
                sum += Math.pow(col.asDouble() - mean, 2);
            }

            double variance = sum / columns.size();
            return Math.sqrt(variance);
        });
    }

    private void checkNotNull() {
        if (columns == null) {
            throw new IllegalArgumentException("Columns must not be null");
        }
    }

    @Override
    public Double apply(Column column) {
        if (column == null) {
            return 0.0;
        }
        if (!(column instanceof DateColumn) && !(column instanceof DoubleColumn) &&
                !(column instanceof LongColumn) && !(column instanceof DynamicColumn)) {
            throw new IllegalArgumentException("Not support field type.");
        }
        return score(column.asDouble());
    }
}