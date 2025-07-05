/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: DataHandlerContext
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/12/8 19:17
 * Description:
 */
package com.ppwx.easysearch.core.pipeline;

import com.ppwx.easysearch.core.data.DataModel;
import io.netty.util.AttributeMap;

/**
 *
 * Pipeline的辅助类
 * 借助该类实现前后处理器的关联，并提供了一些注如参数获取
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/08 19:17
 * @since 1.0.0
 */
public interface DataHandlerContext extends AttributeMap, DataBoundInvoker {

    /**
     * The unique name of the {@link DataHandlerContext}.The name was used when then {@link DataHandler}
     * was added to the {@link DataPipeline}. This name can also be used to access the registered
     * {@link DataHandler} from the {@link DataPipeline}.
     */
    String name();

    /**
     * The {@link DataHandler} that is bound this {@link DataHandlerContext}.
     */
    DataHandler handler();

    /**
     * The {@link DataModel} that is bound this {@link DataHandlerContext}
     */
    DataModel dataModel();

    /**
     * Return the assigned {@link DataPipeline}
     */
    DataPipeline pipeline();
}