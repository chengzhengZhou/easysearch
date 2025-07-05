package com.ppwx.easysearch.core.function;

import com.ppwx.easysearch.core.data.Column;

/**
 * 功能函数接口
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/06 16:34
 * @since 1.0.0
 */
public interface Function<R> {

    /**
     * apply
     *
     * @param column
     * @return double
     */
    R apply(Column column);

}
