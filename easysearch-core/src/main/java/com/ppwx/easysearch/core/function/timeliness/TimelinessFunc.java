/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: TimelinessFunc
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/12/26 15:22
 * Description: 时效分，用于衡量文档的新旧程度
 */
package com.ppwx.easysearch.core.function.timeliness;

import com.ppwx.easysearch.core.data.Column;
import com.ppwx.easysearch.core.function.AbstractScoreFunction;
import com.ppwx.easysearch.core.function.NumberScoreFunction;
import com.ppwx.easysearch.core.function.normalize.FiniteNormalizeFunc;
import com.ppwx.easysearch.core.function.normalize.GaussNormalizeFunc;
import com.ppwx.easysearch.core.util.CurrentTimeUtil;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 *
 * 时效分，用于衡量文档的新旧程度
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/26 15:22
 * @since 1.0.0
 */
public class TimelinessFunc extends AbstractScoreFunction {

    private long currSeconds;
    /**
     * 归一化
     */
    private NumberScoreFunction finiteNormalize;

    public TimelinessFunc() {
        super(Collections.emptyList());
    }

    public TimelinessFunc(List<Column> columns) {
        super(columns);
        currSeconds = CurrentTimeUtil.currentTimeSeconds();
    }

    /**
     * 要评估的字段，类型必须为数值，单位为秒
     * 值域为[0,1]，值越大表示时效性越好。若大于当前时间则返回0
     *
     * @param value
     * @return java.lang.Float
     */
    @Override
    public double score(Number value) {
        if (value == null || value.longValue() > currSeconds) {
            return 0.0D;
        }
        if (finiteNormalize == null) {
            if (min() == 0.0) {
                finiteNormalize = new FiniteNormalizeFunc(currSeconds, currSeconds - TimeUnit.DAYS.toSeconds(365));
            } else {
                finiteNormalize = new GaussNormalizeFunc(currSeconds, standardDeviation());
            }
        }
        return finiteNormalize.score(value);
    }

    @Override
    public Double apply(Column column) {
        if (column == null) {
            return 0.0;
        }
        return score(column.asLong());
    }
}