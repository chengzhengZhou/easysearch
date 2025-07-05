package com.ppwx.easysearch.core.data.model;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @interface ICache
 * @description todo
 * @date 2024/11/28 17:27
 **/
public interface ICache <T, R> {

    /**
     * @description 获取缓存数据
     * @author ext.ahs.zhouchzh1@jd.com
     * @date 2024/11/28 17:29
     * @param key
     * @return R
     */
    R getIfPresent(T key);

    /**
     * @description 添加缓存数据
     * @author ext.ahs.zhouchzh1@jd.com
     * @date 2024/11/28 17:29
     * @param key
     * @param value
     * @return void
     */
    void put(T key, R value);

}
