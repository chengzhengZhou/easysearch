/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: DataHandler
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/12/8 19:26
 * Description:
 */
package com.ppwx.easysearch.core.pipeline;

import java.lang.annotation.*;

/**
 *
 *
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/08 19:26
 * @since 1.0.0
 */
public interface DataHandler {

    void handlerAdded(DataHandlerContext ctx) throws Exception;

    void handlerRemoved(DataHandlerContext ctx) throws Exception;

    void dataPrepare(DataHandlerContext ctx) throws Exception;

    void dataComplete(DataHandlerContext ctx) throws Exception;

    void exceptionCaught(DataHandlerContext ctx, Throwable cause) throws Exception;

    /**
     * Indicates that the same instance of the annotated {@link DataHandler}
     * can be added to one or more {@link DataPipeline}s multiple times
     * without a race condition.
     * <p>
     * If this annotation is not specified, you have to create a new handler
     * instance every time you add it to a pipeline because it has unshared
     * state such as member variables.
     * <p>
     * This annotation is provided for documentation purpose, just like
     * <a href="http://www.javaconcurrencyinpractice.com/annotations/doc/">the JCIP annotations</a>.
     */
    @Inherited
    @Documented
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Sharable {
        // no value
    }
}