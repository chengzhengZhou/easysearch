/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: DefaultDataHandlerContext
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/12/8 19:18
 * Description:
 */
package com.ppwx.easysearch.core.pipeline;

import java.util.concurrent.atomic.AtomicReference;

/**
 *
 * 绑定处理器
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/08 19:18
 * @since 1.0.0
 */
public class DefaultDataHandlerContext extends AbstractDataHandlerContext {

    private final DataHandler handler;

    DefaultDataHandlerContext(
            DefaultDataPipeline pipeline, AtomicReference<Thread> thread, String name, DataHandler handler) {
        super(pipeline, thread, name, handler.getClass());
        this.handler = handler;
    }

    @Override
    public DataHandler handler() {
        return this.handler;
    }
}