/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: DefaultDataPipeline
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/12/8 19:38
 * Description:
 */
package com.ppwx.easysearch.core.pipeline;

import com.ppwx.easysearch.core.common.DataPipelineException;
import com.ppwx.easysearch.core.data.DataModel;
import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 * 数据处理管道实现类
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/08 19:38
 * @since 1.0.0
 */
public class DefaultDataPipeline implements DataPipeline {

    private static final Logger logger = LoggerFactory.getLogger(DefaultDataPipeline.class);

    private static final String HEAD_NAME = generateName0(HeadContext.class);
    private static final String TAIL_NAME = generateName0(TailContext.class);
    private static final FastThreadLocal<Map<Class<?>, String>> nameCaches =
            new FastThreadLocal<Map<Class<?>, String>>() {
                @Override
                protected Map<Class<?>, String> initialValue() {
                    return new WeakHashMap<Class<?>, String>();
                }
            };

    private final DataModel dataModel;

    final HeadContext head;
    final TailContext tail;
    final AtomicReference<Thread> thread;
    final boolean strictMode;

    public DefaultDataPipeline(DataModel dataModel) {
        this(dataModel, false);
    }

    public DefaultDataPipeline(DataModel dataModel, boolean shareMode) {
        this.dataModel = ObjectUtil.checkNotNull(dataModel, "dataModel");
        this.thread = new AtomicReference<>();
        this.strictMode = shareMode;
        /*succeededFuture = new SucceededChannelFuture(channel, null);
        voidPromise =  new VoidChannelPromise(channel, true);*/

        tail = new TailContext(this);
        head = new HeadContext(this);

        head.next = tail;
        tail.prev = head;
    }

    @Override
    public DataPipeline addFirst(String name, DataHandler handler) {
        return addFirst(this.thread, name, handler);
    }

    @Override
    public DataPipeline addFirst(AtomicReference<Thread> thread, String name, DataHandler handler) {
        final AbstractDataHandlerContext newCtx;
        synchronized (this) {
            checkMultiplicity(handler);
            name = filterName(name, handler);

            newCtx = newContext(thread, name, handler);

            addFirst0(newCtx);
        }
        callHandlerAdded0(newCtx);
        return this;
    }

    @Override
    public DataPipeline addLast(String name, DataHandler handler) {
        return addLast(this.thread, name, handler);
    }

    @Override
    public DataPipeline addLast(AtomicReference<Thread> thread, String name, DataHandler handler) {
        final AbstractDataHandlerContext newCtx;
        synchronized (this) {
            checkMultiplicity(handler);
            name = filterName(name, handler);

            newCtx = newContext(thread, name, handler);

            addLast0(newCtx);
        }
        callHandlerAdded0(newCtx);
        return this;
    }

    @Override
    public DataPipeline addBefore(String baseName, String name, DataHandler handler) {
        return addBefore(this.thread, baseName, name, handler);
    }

    @Override
    public DataPipeline addBefore(AtomicReference<Thread> thread, String baseName, String name, DataHandler handler) {
        final AbstractDataHandlerContext newCtx;
        final AbstractDataHandlerContext ctx;
        synchronized (this) {
            checkMultiplicity(handler);
            name = filterName(name, handler);
            ctx = getContextOrDie(baseName);

            newCtx = newContext(thread, name, handler);

            addBefore0(ctx, newCtx);
        }
        callHandlerAdded0(newCtx);
        return this;
    }

    @Override
    public DataPipeline addAfter(String baseName, String name, DataHandler handler) {
        return addAfter(this.thread, baseName, name, handler);
    }

    @Override
    public DataPipeline addAfter(AtomicReference<Thread> thread, String baseName, String name, DataHandler handler) {
        final AbstractDataHandlerContext newCtx;
        final AbstractDataHandlerContext ctx;

        synchronized (this) {
            checkMultiplicity(handler);
            name = filterName(name, handler);
            ctx = getContextOrDie(baseName);

            newCtx = newContext(thread, name, handler);

            addAfter0(ctx, newCtx);
        }
        callHandlerAdded0(newCtx);
        return this;
    }

    @Override
    public DataPipeline addFirst(DataHandler... handlers) {
        return addFirst(this.thread, handlers);
    }

    @Override
    public DataPipeline addFirst(AtomicReference<Thread> thread, DataHandler... handlers) {
        ObjectUtil.checkNotNull(handlers, "handlers");
        if (handlers.length == 0 || handlers[0] == null) {
            return this;
        }

        int size;
        for (size = 1; size < handlers.length; size ++) {
            if (handlers[size] == null) {
                break;
            }
        }

        for (int i = size - 1; i >= 0; i --) {
            DataHandler h = handlers[i];
            addFirst(null, h);
        }

        return this;
    }

    @Override
    public DataPipeline addLast(DataHandler... handlers) {
        return addLast(this.thread, handlers);
    }

    @Override
    public DataPipeline addLast(AtomicReference<Thread> thread, DataHandler... handlers) {
        ObjectUtil.checkNotNull(handlers, "handlers");

        for (DataHandler h: handlers) {
            if (h == null) {
                break;
            }
            addLast(null, h);
        }

        return this;
    }

    @Override
    public DataPipeline remove(DataHandler handler) {
        remove(getContextOrDie(handler));
        return this;
    }

    @Override
    public DataHandler remove(String name) {
        return remove(getContextOrDie(name)).handler();
    }

    @Override
    public <T extends DataHandler> T remove(Class<T> handlerType) {
        return (T) remove(getContextOrDie(handlerType)).handler();
    }

    @Override
    public DataHandler removeFirst() {
        if (head.next == tail) {
            throw new NoSuchElementException();
        }
        return remove(head.next).handler();
    }

    @Override
    public DataHandler removeLast() {
        if (head.next == tail) {
            throw new NoSuchElementException();
        }
        return remove(tail.prev).handler();
    }

    @Override
    public DataPipeline replace(DataHandler oldHandler, String newName, DataHandler newHandler) {
        replace(getContextOrDie(oldHandler), newName, newHandler);
        return this;
    }

    @Override
    public DataHandler replace(String oldName, String newName, DataHandler newHandler) {
        return replace(getContextOrDie(oldName), newName, newHandler);
    }

    @Override
    public <T extends DataHandler> T replace(Class<T> oldHandlerType, String newName, DataHandler newHandler) {
        return (T) replace(getContextOrDie(oldHandlerType), newName, newHandler);
    }

    @Override
    public DataHandler first() {
        DataHandlerContext first = firstContext();
        if (first == null) {
            return null;
        }
        return first.handler();
    }

    @Override
    public DataHandlerContext firstContext() {
        AbstractDataHandlerContext first = head.next;
        if (first == tail) {
            return null;
        }
        return head.next;
    }

    @Override
    public DataHandler last() {
        AbstractDataHandlerContext last = tail.prev;
        if (last == head) {
            return null;
        }
        return last.handler();
    }

    @Override
    public DataHandlerContext lastContext() {
        AbstractDataHandlerContext last = tail.prev;
        if (last == head) {
            return null;
        }
        return last;
    }

    @Override
    public DataHandler get(String name) {
        DataHandlerContext ctx = context(name);
        if (ctx == null) {
            return null;
        } else {
            return ctx.handler();
        }
    }

    @Override
    public <T extends DataHandler> T get(Class<T> handlerType) {
        DataHandlerContext ctx = context(handlerType);
        if (ctx == null) {
            return null;
        } else {
            return (T) ctx.handler();
        }
    }

    @Override
    public DataHandlerContext context(DataHandler handler) {
        ObjectUtil.checkNotNull(handler, "handler");

        AbstractDataHandlerContext ctx = head.next;
        for (;;) {

            if (ctx == null) {
                return null;
            }

            if (ctx.handler() == handler) {
                return ctx;
            }

            ctx = ctx.next;
        }
    }

    @Override
    public DataHandlerContext context(String name) {
        return context0(ObjectUtil.checkNotNull(name, "name"));
    }

    @Override
    public DataHandlerContext context(Class<? extends DataHandler> handlerType) {
        ObjectUtil.checkNotNull(handlerType, "handlerType");

        AbstractDataHandlerContext ctx = head.next;
        for (;;) {
            if (ctx == null) {
                return null;
            }
            if (handlerType.isAssignableFrom(ctx.handler().getClass())) {
                return ctx;
            }
            ctx = ctx.next;
        }
    }

    @Override
    public DataModel dataModel() {
        return this.dataModel;
    }

    @Override
    public List<String> names() {
        List<String> list = new ArrayList<String>();
        AbstractDataHandlerContext ctx = head.next;
        for (;;) {
            if (ctx == null) {
                return list;
            }
            list.add(ctx.name());
            ctx = ctx.next;
        }
    }

    @Override
    public Map<String, DataHandler> toMap() {
        Map<String, DataHandler> map = new LinkedHashMap<String, DataHandler>();
        AbstractDataHandlerContext ctx = head.next;
        for (;;) {
            if (ctx == tail) {
                return map;
            }
            map.put(ctx.name(), ctx.handler());
            ctx = ctx.next;
        }
    }

    @Override
    public DataBoundInvoker fireDataPrepare() {
        if (strictMode) {
            thread.compareAndSet(null, Thread.currentThread());
        }
        AbstractDataHandlerContext.invokeDataPrepared(head);
        return this;
    }

    @Override
    public DataBoundInvoker fireDataComplete() {
        if (strictMode) {
            thread.compareAndSet(null, Thread.currentThread());
        }
        AbstractDataHandlerContext.invokeDataComplete(head);
        return this;
    }

    @Override
    public DataBoundInvoker fireExceptionCaught(Throwable cause) {
        AbstractDataHandlerContext.invokeExceptionCaught(head, cause);
        return this;
    }

    private void addFirst0(AbstractDataHandlerContext newCtx) {
        AbstractDataHandlerContext nextCtx = head.next;
        newCtx.prev = head;
        newCtx.next = nextCtx;
        head.next = newCtx;
        nextCtx.prev = newCtx;
    }

    private void addLast0(AbstractDataHandlerContext newCtx) {
        AbstractDataHandlerContext prev = tail.prev;
        newCtx.prev = prev;
        newCtx.next = tail;
        prev.next = newCtx;
        tail.prev = newCtx;
    }

    private static void addBefore0(AbstractDataHandlerContext ctx, AbstractDataHandlerContext newCtx) {
        newCtx.prev = ctx.prev;
        newCtx.next = ctx;
        ctx.prev.next = newCtx;
        ctx.prev = newCtx;
    }

    private static void addAfter0(AbstractDataHandlerContext ctx, AbstractDataHandlerContext newCtx) {
        newCtx.prev = ctx;
        newCtx.next = ctx.next;
        ctx.next.prev = newCtx;
        ctx.next = newCtx;
    }

    private void callHandlerAdded0(AbstractDataHandlerContext ctx) {
        try {
            ctx.callHandlerAdded();
        } catch (Throwable t) {
            boolean removed = false;
            try {
                atomicRemoveFromHandlerList(ctx);
                ctx.callHandlerRemoved();
                removed = true;
            } catch (Throwable t2) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Failed to remove a handler: " + ctx.name(), t2);
                }
            }

            if (removed) {
                fireExceptionCaught(new DataPipelineException(
                        ctx.handler().getClass().getName() +
                                ".handlerAdded() has thrown an exception; removed.", t));
            } else {
                fireExceptionCaught(new DataPipelineException(
                        ctx.handler().getClass().getName() +
                                ".handlerAdded() has thrown an exception; also failed to remove.", t));
            }
        }
    }

    private AbstractDataHandlerContext getContextOrDie(String name) {
        AbstractDataHandlerContext ctx = (AbstractDataHandlerContext) context(name);
        if (ctx == null) {
            throw new NoSuchElementException(name);
        } else {
            return ctx;
        }
    }

    private AbstractDataHandlerContext getContextOrDie(DataHandler handler) {
        AbstractDataHandlerContext ctx = (AbstractDataHandlerContext) context(handler);
        if (ctx == null) {
            throw new NoSuchElementException(handler.getClass().getName());
        } else {
            return ctx;
        }
    }

    private AbstractDataHandlerContext getContextOrDie(Class<? extends DataHandler> handlerType) {
        AbstractDataHandlerContext ctx = (AbstractDataHandlerContext) context(handlerType);
        if (ctx == null) {
            throw new NoSuchElementException(handlerType.getName());
        } else {
            return ctx;
        }
    }

    private AbstractDataHandlerContext remove(final AbstractDataHandlerContext ctx) {
        assert ctx != head && ctx != tail;

        synchronized (this) {
            atomicRemoveFromHandlerList(ctx);
        }
        callHandlerRemoved0(ctx);
        return ctx;
    }

    /**
     * Method is synchronized to make the handler removal from the double linked list atomic.
     */
    private synchronized void atomicRemoveFromHandlerList(AbstractDataHandlerContext ctx) {
        AbstractDataHandlerContext prev = ctx.prev;
        AbstractDataHandlerContext next = ctx.next;
        prev.next = next;
        next.prev = prev;
    }

    private String filterName(String name, DataHandler handler) {
        if (name == null) {
            return generateName(handler);
        }
        checkDuplicateName(name);
        return name;
    }

    private String generateName(DataHandler handler) {
        Map<Class<?>, String> cache = nameCaches.get();
        Class<?> handlerType = handler.getClass();
        String name = cache.get(handlerType);
        if (name == null) {
            name = generateName0(handlerType);
            cache.put(handlerType, name);
        }

        // It's not very likely for a user to put more than one handler of the same type, but make sure to avoid
        // any name conflicts.  Note that we don't cache the names generated here.
        if (context0(name) != null) {
            String baseName = name.substring(0, name.length() - 1); // Strip the trailing '0'.
            for (int i = 1;; i ++) {
                String newName = baseName + i;
                if (context0(newName) == null) {
                    name = newName;
                    break;
                }
            }
        }
        return name;
    }

    private void callHandlerRemoved0(final AbstractDataHandlerContext ctx) {
        // Notify the complete removal.
        try {
            ctx.callHandlerRemoved();
        } catch (Throwable t) {
            fireExceptionCaught(new DataPipelineException(
                    ctx.handler().getClass().getName() + ".handlerRemoved() has thrown an exception.", t));
        }
    }

    private static String generateName0(Class<?> handlerType) {
        return StringUtil.simpleClassName(handlerType) + "#0";
    }

    private AbstractDataHandlerContext context0(String name) {
        AbstractDataHandlerContext context = head.next;
        while (context != tail) {
            if (context.name().equals(name)) {
                return context;
            }
            context = context.next;
        }
        return null;
    }

    private AbstractDataHandlerContext newContext(AtomicReference<Thread> thread, String name, DataHandler handler) {
        return new DefaultDataHandlerContext(this, thread, name, handler);
    }

    private void checkDuplicateName(String name) {
        if (context0(name) != null) {
            throw new IllegalArgumentException("Duplicate handler name: " + name);
        }
    }

    private static void checkMultiplicity(DataHandler handler) {
        if (handler instanceof DataHandlerAdapter) {
            DataHandlerAdapter h = (DataHandlerAdapter) handler;
            if (!h.isSharable() && h.added) {
                throw new DataPipelineException(
                        h.getClass().getName() +
                                " is not a @Sharable handler, so can't be added or removed multiple times.");
            }
            h.added = true;
        }
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

    final class HeadContext extends AbstractDataHandlerContext implements DataBoundInvoker {

        HeadContext(DefaultDataPipeline pipeline) {
            super(pipeline, thread, HEAD_NAME, HeadContext.class);
            setAddComplete();
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

        @Override
        public DataHandler handler() {
            return this;
        }
    }

    final class TailContext extends AbstractDataHandlerContext implements DataBoundInvoker {

        TailContext(DefaultDataPipeline pipeline) {
            super(pipeline, thread, TAIL_NAME, TailContext.class);
            setAddComplete();
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
        public DataHandler handler() {
            return this;
        }
    }
}