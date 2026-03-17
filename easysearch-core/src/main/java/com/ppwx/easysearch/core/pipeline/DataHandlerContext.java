/*
 * Copyright 2026 chengzhengZhou
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ppwx.easysearch.core.pipeline;

import com.ppwx.easysearch.core.data.DataModel;
import io.netty.util.AttributeMap;

/**
 *
 * Pipeline的辅助类
 * 借助该类实现前后处理器的关联，并提供了一些注如参数获取
 *
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