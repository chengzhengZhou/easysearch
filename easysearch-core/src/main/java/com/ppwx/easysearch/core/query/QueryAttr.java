package com.ppwx.easysearch.core.query;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className QueryAttr
 * @description 查询条件
 * @date 2024/10/14 19:19
 **/
public abstract class QueryAttr<R> {

    /**
     * @description 获取属性值
     * @author ext.ahs.zhouchzh1@jd.com
     * @date 2024/10/14 19:21
     * @return R
     */
    public abstract R getValue();

}
