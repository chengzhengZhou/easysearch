package com.ppwx.easysearch.core.metrics;

import java.util.List;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className MetricsRegistry
 * @description Metrics注册器，存在较多的数据度量时可以使用该类进行管理
 * @date 2024/11/28 17:43
 **/
public interface MetricsRegistry<E> {

    /**
     * @description 获取一个Metrics，没有则创建一个
     * @author ext.ahs.zhouchzh1@jd.com
     * @date 2024/11/28 19:00
     * @param tag 别名
     * @param windowSize 窗口大小
     * @return Metrics
     */
    E metrics(String tag, int windowSize);

    /**
     * @description 移除
     * @author ext.ahs.zhouchzh1@jd.com
     * @date 2024/12/3 11:32
     * @param tag 别名
     * @return E
     */
    E remove(String tag);

    /**
     * @description 获取所有Metrix
     * @author ext.ahs.zhouchzh1@jd.com
     * @date 2024/12/3 11:32
     * @return List<E>
     */
    List<E> getAll();

    /**
     * @description 移除已创建的Metrix
     * @author ext.ahs.zhouchzh1@jd.com
     * @date 2024/12/3 11:34
     * @return void
     */
    void clear();
}
