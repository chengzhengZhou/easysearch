/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: DataHandlerAdapter
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/12/9 18:44
 * Description:
 */
package com.ppwx.easysearch.core.pipeline;

import io.netty.util.internal.InternalThreadLocalMap;

import java.util.Map;

/**
 *
 * {@link DataHandler} 适配器
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/09 18:44
 * @since 1.0.0
 */
public class DataHandlerAdapter implements DataHandler {

    // Not using volatile because it's used only for a sanity check.
    boolean added;

    /**
     * Throws {@link IllegalStateException} if {@link DataHandlerAdapter#isSharable()} returns {@code true}
     */
    protected void ensureNotSharable() {
        if (isSharable()) {
            throw new IllegalStateException("ChannelHandler " + getClass().getName() + " is not allowed to be shared");
        }
    }

    /**
     * Return {@code true} if the implementation is {@link Sharable} and so can be added
     * to different {@link DataPipeline}s.
     */
    public boolean isSharable() {
        /**
         * Cache the result of {@link Sharable} annotation detection to workaround a condition. We use a
         * {@link ThreadLocal} and {@link WeakHashMap} to eliminate the volatile write/reads. Using different
         * {@link WeakHashMap} instances per {@link Thread} is good enough for us and the number of
         * {@link Thread}s are quite limited anyway.
         *
         * See <a href="https://github.com/netty/netty/issues/2289">#2289</a>.
         */
        Class<?> clazz = getClass();
        Map<Class<?>, Boolean> cache = InternalThreadLocalMap.get().handlerSharableCache();
        Boolean sharable = cache.get(clazz);
        if (sharable == null) {
            sharable = clazz.isAnnotationPresent(Sharable.class);
            cache.put(clazz, sharable);
        }
        return sharable;
    }

    @Override
    public void handlerAdded(DataHandlerContext ctx) throws Exception {
        // NOOP
    }

    @Override
    public void handlerRemoved(DataHandlerContext ctx) throws Exception {
        // NOOP
    }

    @Override
    public void dataPrepare(DataHandlerContext ctx) throws Exception {
        ctx.fireDataPrepare();
    }

    @Override
    public void dataComplete(DataHandlerContext ctx) throws Exception {
        ctx.fireDataComplete();
    }

    @Override
    public void exceptionCaught(DataHandlerContext ctx, Throwable cause) throws Exception {
        ctx.fireExceptionCaught(cause);
    }

}