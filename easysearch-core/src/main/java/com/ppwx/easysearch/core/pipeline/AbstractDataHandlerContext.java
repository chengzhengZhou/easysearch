/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: AbstractDataHandlerContext
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/12/8 19:45
 * Description:
 */
package com.ppwx.easysearch.core.pipeline;

import com.ppwx.easysearch.core.common.DataPipelineException;
import com.ppwx.easysearch.core.data.DataModel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.internal.ThrowableUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 * 维护了前后处理器，通过fireXXX方法实现向前推进
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/08 19:45
 * @since 1.0.0
 */
public abstract class AbstractDataHandlerContext implements DataHandlerContext {

    private static final Logger logger = LoggerFactory.getLogger(AbstractDataHandlerContext.class);

    /**
     * {@link DataHandler#handlerAdded(DataHandlerContext)} is about to be called.
     */
    private static final int ADD_PENDING = 1;
    /**
     * {@link DataHandler#handlerAdded(DataHandlerContext)} was called.
     */
    private static final int ADD_COMPLETE = 2;
    /**
     * {@link DataHandler#handlerRemoved(DataHandlerContext)} was called.
     */
    private static final int REMOVE_COMPLETE = 3;
    /**
     * Neither {@link DataHandler#handlerAdded(DataHandlerContext)}
     * nor {@link DataHandler#handlerRemoved(DataHandlerContext)} was called.
     */
    private static final int INIT = 0;

    volatile AbstractDataHandlerContext next;
    volatile AbstractDataHandlerContext prev;

    private final DefaultDataPipeline pipeline;
    private final String name;
    private AtomicReference<Thread> thread;

    private static final AtomicIntegerFieldUpdater<AbstractDataHandlerContext> HANDLER_STATE_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(AbstractDataHandlerContext.class, "handlerState");

    private volatile int handlerState = INIT;

    AbstractDataHandlerContext(DefaultDataPipeline pipeline, AtomicReference<Thread> thread, String name,
                               Class<? extends DataHandler> handlerClass) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("name can not be null");
        }
        this.thread = thread;
        this.name = name;
        this.pipeline = pipeline;
    }

    private void invokeDataPrepared() {
        if (invokeHandler()) {
            try {
                final DataHandler handler = handler();
                final DefaultDataPipeline.HeadContext headContext = pipeline.head;
                if (handler == headContext) {
                    headContext.dataPrepare(this);
                } else {
                    handler.dataPrepare(this);
                }
            } catch (Throwable t) {
                invokeExceptionCaught(t);
            }
        } else {
            fireDataPrepare();
        }
    }

    private void invokeDateComplete() {
        if (invokeHandler()) {
            try {
                final DataHandler handler = handler();
                final DefaultDataPipeline.HeadContext headContext = pipeline.head;
                if (handler == headContext) {
                    headContext.dataComplete(this);
                } else {
                    handler.dataComplete(this);
                }
            } catch (Throwable t) {
                invokeExceptionCaught(t);
            }
        } else {
            fireDataComplete();
        }
    }

    /**
     * Makes best possible effort to detect if {@link DataHandler#handlerAdded(DataHandlerContext)} was called
     * yet. If not return {@code false} and if called or could not detect return {@code true}.
     *
     * If this method returns {@code false} we will not invoke the {@link DataHandler} but just forward the event.
     * This is needed as {@link DefaultDataPipeline} may already put the {@link DataHandler} in the linked-list
     * but not called {@link DataHandler#handlerAdded(DataHandlerContext)}.
     */
    private boolean invokeHandler() {
        // Store in local variable to reduce volatile reads.
        int handlerState = this.handlerState;
        return handlerState == ADD_COMPLETE || handlerState == ADD_PENDING;
    }

    final boolean setAddComplete() {
        for (;;) {
            int oldState = handlerState;
            if (oldState == REMOVE_COMPLETE) {
                return false;
            }
            // Ensure we never update when the handlerState is REMOVE_COMPLETE already.
            // oldState is usually ADD_PENDING but can also be REMOVE_COMPLETE when an EventExecutor is used that is not
            // exposing ordering guarantees.
            if (HANDLER_STATE_UPDATER.compareAndSet(this, oldState, ADD_COMPLETE)) {
                return true;
            }
        }
    }

    final void setRemoved() {
        handlerState = REMOVE_COMPLETE;
    }

    private void invokeExceptionCaught(final Throwable cause) {
        if (invokeHandler()) {
            try {
                handler().exceptionCaught(this, cause);
            } catch (Throwable error) {
                if (logger.isDebugEnabled()) {
                    logger.debug(
                            "An exception {}" +
                                    "was thrown by a user handler's exceptionCaught() " +
                                    "method while handling the following exception:",
                            ThrowableUtil.stackTraceToString(error), cause);
                } else if (logger.isWarnEnabled()) {
                    logger.warn(
                            "An exception '{}' [enable DEBUG level for full stacktrace] " +
                                    "was thrown by a user handler's exceptionCaught() " +
                                    "method while handling the following exception:", error, cause);
                }
            }
        } else {
            fireExceptionCaught(cause);
        }
    }

    static void invokeDataPrepared(AbstractDataHandlerContext next) {
        inCurrInvokerAndThrow(next.thread.get());
        next.invokeDataPrepared();
    }

    static void invokeDataComplete(AbstractDataHandlerContext next) {
        inCurrInvokerAndThrow(next.thread.get());
        next.invokeDateComplete();
    }

    static void invokeExceptionCaught(final AbstractDataHandlerContext next, final Throwable cause) {
        inCurrInvokerAndThrow(next.thread.get());
        next.invokeExceptionCaught(cause);
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public DataBoundInvoker fireDataPrepare() {
        invokeDataPrepared(this.next);
        return this;
    }

    @Override
    public DataBoundInvoker fireDataComplete() {
        invokeDataComplete(this.next);
        return this;
    }

    @Override
    public DataBoundInvoker fireExceptionCaught(Throwable cause) {
        invokeExceptionCaught(this.next, cause);
        return this;
    }

    @Override
    public void handlerAdded(DataHandlerContext ctx) throws Exception {

    }

    @Override
    public void handlerRemoved(DataHandlerContext ctx) throws Exception {

    }

    @Override
    public void dataPrepare(DataHandlerContext ctx) throws Exception {

    }

    @Override
    public void dataComplete(DataHandlerContext ctx) throws Exception {

    }

    @Override
    public void exceptionCaught(DataHandlerContext ctx, Throwable cause) throws Exception {

    }

    @Override
    public DataModel dataModel() {
        return pipeline.dataModel();
    }

    @Override
    public DataPipeline pipeline() {
        return this.pipeline;
    }

    @Override
    public <T> Attribute<T> attr(AttributeKey<T> key) {
        return this.dataModel().attr(key);
    }

    @Override
    public <T> boolean hasAttr(AttributeKey<T> key) {
        return this.dataModel().hasAttr(key);
    }

    static void inCurrInvokerAndThrow(Thread thread) {
        if (thread != null && thread != Thread.currentThread()) {
            throw new DataPipelineException("Multi threads execute.");
        }
    }

    final void callHandlerAdded() throws Exception {
        // We must call setAddComplete before calling handlerAdded. Otherwise if the handlerAdded method generates
        // any pipeline events ctx.handler() will miss them because the state will not allow it.
        if (setAddComplete()) {
            handler().handlerAdded(this);
        }
    }

    final void callHandlerRemoved() throws Exception {
        try {
            // Only call handlerRemoved(...) if we called handlerAdded(...) before.
            if (handlerState == ADD_COMPLETE) {
                handler().handlerRemoved(this);
            }
        } finally {
            // Mark the handler as removed in any case.
            setRemoved();
        }
    }
}