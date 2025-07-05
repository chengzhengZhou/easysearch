package com.ppwx.easysearch.core.function;

import com.ppwx.easysearch.core.data.Column;

/**
 * 算分接口
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/06 16:34
 * @since 1.0.0
 */
public interface NumberScoreFunction<R> extends Function<R> {

    /**
     * 计算分值
     *
     * @param value
     * @return double
     */
    double score(Number value);

    @Override
    R apply(Column column);
}
