package com.ppwx.easysearch.qp.data;

import java.io.IOException;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className MetaSourceConvertor
 * @description 元数据加载、处理、转换
 * @date 2024/10/11 15:01
 **/
public interface MetaSourceConvertor {

    /**
     * @description 数据加载
     * @author ext.ahs.zhouchzh1@jd.com
     * @date 2024/10/11 15:05
     * @return void
     */
    void readData() throws IOException;

}
