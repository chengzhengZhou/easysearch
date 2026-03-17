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

/**
 *
 * 数据绑定事件调用
 * 目前定义数据准备、加载完毕、异常三个方法
 * 也是作为责任链的调用入口方法
 * 主要实现类为{@link DefaultDataPipeline} 和 {@link AbstractDataHandlerContext}
 *
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