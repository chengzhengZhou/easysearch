/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: DataBoundInvoker
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/12/8 19:21
 * Description:
 */
package com.ppwx.easysearch.core.pipeline;

/**
 *
 * 数据绑定事件调用
 * 目前定义数据准备、加载完毕、异常三个方法
 * 也是作为责任链的调用入口方法
 * 主要实现类为{@link DefaultDataPipeline} 和 {@link AbstractDataHandlerContext}
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/08 19:21
 * @since 1.0.0
 */
public interface DataBoundInvoker extends DataHandler {

    /**
     * 数据准备
     *
     * @param
     * @return com.ppwx.easysearch.core.pipeline.DataBoundInvoker
     */
    DataBoundInvoker fireDataPrepare();

    /**
     * 数据处理完毕
     *
     * @param
     * @return com.ppwx.easysearch.core.pipeline.DataBoundInvoker
     */
    DataBoundInvoker fireDataComplete();

    /**
     * 异常捕获处理
     *
     * @param cause
     * @return com.ppwx.easysearch.core.pipeline.DataBoundInvoker
     */
    DataBoundInvoker fireExceptionCaught(Throwable cause);
}