/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: DataPipeline
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/12/8 19:19
 * Description:
 */
package com.ppwx.easysearch.core.pipeline;

import com.ppwx.easysearch.core.data.DataModel;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 * 数据加工管道，典型的责任链模式
 * 该接口类提供了处理器的编排方法
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/08 19:19
 * @since 1.0.0
 */
public interface DataPipeline extends DataBoundInvoker {

    /**
     * Inserts a {@link DataHandler} at the first position of this pipeline.
     *
     * @param name     the name of the handler to insert first
     * @param handler  the handler to insert first
     *
     * @throws IllegalArgumentException
     *         if there's an entry with the same name already in the pipeline
     * @throws NullPointerException
     *         if the specified handler is {@code null}
     */
    DataPipeline addFirst(String name, DataHandler handler);

    /**
     * Inserts a {@link DataHandler} at the first position of this pipeline.
     *
     * @param thread    the {@link AtomicReference<Thread>} which will be used to execute the {@link DataHandler}
     *                 methods
     * @param name     the name of the handler to insert first
     * @param handler  the handler to insert first
     *
     * @throws IllegalArgumentException
     *         if there's an entry with the same name already in the pipeline
     * @throws NullPointerException
     *         if the specified handler is {@code null}
     */
    DataPipeline addFirst(AtomicReference<Thread> thread, String name, DataHandler handler);

    /**
     * Appends a {@link DataHandler} at the last position of this pipeline.
     *
     * @param name     the name of the handler to append
     * @param handler  the handler to append
     *
     * @throws IllegalArgumentException
     *         if there's an entry with the same name already in the pipeline
     * @throws NullPointerException
     *         if the specified handler is {@code null}
     */
    DataPipeline addLast(String name, DataHandler handler);

    /**
     * Appends a {@link DataHandler} at the last position of this pipeline.
     *
     * @param thread    the {@link AtomicReference<Thread>} which will be used to execute the {@link DataHandler}
     *                 methods
     * @param name     the name of the handler to append
     * @param handler  the handler to append
     *
     * @throws IllegalArgumentException
     *         if there's an entry with the same name already in the pipeline
     * @throws NullPointerException
     *         if the specified handler is {@code null}
     */
    DataPipeline addLast(AtomicReference<Thread> thread, String name, DataHandler handler);

    /**
     * Inserts a {@link DataHandler} before an existing handler of this
     * pipeline.
     *
     * @param baseName  the name of the existing handler
     * @param name      the name of the handler to insert before
     * @param handler   the handler to insert before
     *
     * @throws NoSuchElementException
     *         if there's no such entry with the specified {@code baseName}
     * @throws IllegalArgumentException
     *         if there's an entry with the same name already in the pipeline
     * @throws NullPointerException
     *         if the specified baseName or handler is {@code null}
     */
    DataPipeline addBefore(String baseName, String name, DataHandler handler);

    /**
     * Inserts a {@link DataHandler} before an existing handler of this
     * pipeline.
     *
     * @param thread     the {@link AtomicReference<Thread>} which will be used to execute the {@link DataHandler}
     *                  methods
     * @param baseName  the name of the existing handler
     * @param name      the name of the handler to insert before
     * @param handler   the handler to insert before
     *
     * @throws NoSuchElementException
     *         if there's no such entry with the specified {@code baseName}
     * @throws IllegalArgumentException
     *         if there's an entry with the same name already in the pipeline
     * @throws NullPointerException
     *         if the specified baseName or handler is {@code null}
     */
    DataPipeline addBefore(AtomicReference<Thread> thread, String baseName, String name, DataHandler handler);

    /**
     * Inserts a {@link DataHandler} after an existing handler of this
     * pipeline.
     *
     * @param baseName  the name of the existing handler
     * @param name      the name of the handler to insert after
     * @param handler   the handler to insert after
     *
     * @throws NoSuchElementException
     *         if there's no such entry with the specified {@code baseName}
     * @throws IllegalArgumentException
     *         if there's an entry with the same name already in the pipeline
     * @throws NullPointerException
     *         if the specified baseName or handler is {@code null}
     */
    DataPipeline addAfter(String baseName, String name, DataHandler handler);

    /**
     * Inserts a {@link DataHandler} after an existing handler of this
     * pipeline.
     *
     * @param thread     the {@link AtomicReference<Thread>} which will be used to execute the {@link DataHandler}
     *                  methods
     * @param baseName  the name of the existing handler
     * @param name      the name of the handler to insert after
     * @param handler   the handler to insert after
     *
     * @throws NoSuchElementException
     *         if there's no such entry with the specified {@code baseName}
     * @throws IllegalArgumentException
     *         if there's an entry with the same name already in the pipeline
     * @throws NullPointerException
     *         if the specified baseName or handler is {@code null}
     */
    DataPipeline addAfter(AtomicReference<Thread> thread, String baseName, String name, DataHandler handler);

    /**
     * Inserts {@link DataHandler}s at the first position of this pipeline.
     *
     * @param handlers  the handlers to insert first
     *
     */
    DataPipeline addFirst(DataHandler... handlers);

    /**
     * Inserts {@link DataHandler}s at the first position of this pipeline.
     *
     * @param thread     the {@link AtomicReference<Thread>} which will be used to execute the {@link DataHandler}s
     *                  methods.
     * @param handlers  the handlers to insert first
     *
     */
    DataPipeline addFirst(AtomicReference<Thread> thread, DataHandler... handlers);

    /**
     * Inserts {@link DataHandler}s at the last position of this pipeline.
     *
     * @param handlers  the handlers to insert last
     *
     */
    DataPipeline addLast(DataHandler... handlers);

    /**
     * Inserts {@link DataHandler}s at the last position of this pipeline.
     *
     * @param thread     the {@link AtomicReference<Thread>} which will be used to execute the {@link DataHandler}s
     *                  methods.
     * @param handlers  the handlers to insert last
     *
     */
    DataPipeline addLast(AtomicReference<Thread> thread, DataHandler... handlers);

    /**
     * Removes the specified {@link DataHandler} from this pipeline.
     *
     * @param  handler          the {@link DataHandler} to remove
     *
     * @throws NoSuchElementException
     *         if there's no such handler in this pipeline
     * @throws NullPointerException
     *         if the specified handler is {@code null}
     */
    DataPipeline remove(DataHandler handler);

    /**
     * Removes the {@link DataHandler} with the specified name from this pipeline.
     *
     * @param  name             the name under which the {@link DataHandler} was stored.
     *
     * @return the removed handler
     *
     * @throws NoSuchElementException
     *         if there's no such handler with the specified name in this pipeline
     * @throws NullPointerException
     *         if the specified name is {@code null}
     */
    DataHandler remove(String name);

    /**
     * Removes the {@link DataHandler} of the specified type from this pipeline.
     *
     * @param <T>           the type of the handler
     * @param handlerType   the type of the handler
     *
     * @return the removed handler
     *
     * @throws NoSuchElementException
     *         if there's no such handler of the specified type in this pipeline
     * @throws NullPointerException
     *         if the specified handler type is {@code null}
     */
    <T extends DataHandler> T remove(Class<T> handlerType);

    /**
     * Removes the first {@link DataHandler} in this pipeline.
     *
     * @return the removed handler
     *
     * @throws NoSuchElementException
     *         if this pipeline is empty
     */
    DataHandler removeFirst();

    /**
     * Removes the last {@link DataHandler} in this pipeline.
     *
     * @return the removed handler
     *
     * @throws NoSuchElementException
     *         if this pipeline is empty
     */
    DataHandler removeLast();

    /**
     * Replaces the specified {@link DataHandler} with a new handler in this pipeline.
     *
     * @param  oldHandler    the {@link DataHandler} to be replaced
     * @param  newName       the name under which the replacement should be added
     * @param  newHandler    the {@link DataHandler} which is used as replacement
     *
     * @return itself

     * @throws NoSuchElementException
     *         if the specified old handler does not exist in this pipeline
     * @throws IllegalArgumentException
     *         if a handler with the specified new name already exists in this
     *         pipeline, except for the handler to be replaced
     * @throws NullPointerException
     *         if the specified old handler or new handler is
     *         {@code null}
     */
    DataPipeline replace(DataHandler oldHandler, String newName, DataHandler newHandler);

    /**
     * Replaces the {@link DataHandler} of the specified name with a new handler in this pipeline.
     *
     * @param  oldName       the name of the {@link DataHandler} to be replaced
     * @param  newName       the name under which the replacement should be added
     * @param  newHandler    the {@link DataHandler} which is used as replacement
     *
     * @return the removed handler
     *
     * @throws NoSuchElementException
     *         if the handler with the specified old name does not exist in this pipeline
     * @throws IllegalArgumentException
     *         if a handler with the specified new name already exists in this
     *         pipeline, except for the handler to be replaced
     * @throws NullPointerException
     *         if the specified old handler or new handler is
     *         {@code null}
     */
    DataHandler replace(String oldName, String newName, DataHandler newHandler);

    /**
     * Replaces the {@link DataHandler} of the specified type with a new handler in this pipeline.
     *
     * @param  oldHandlerType   the type of the handler to be removed
     * @param  newName          the name under which the replacement should be added
     * @param  newHandler       the {@link DataHandler} which is used as replacement
     *
     * @return the removed handler
     *
     * @throws NoSuchElementException
     *         if the handler of the specified old handler type does not exist
     *         in this pipeline
     * @throws IllegalArgumentException
     *         if a handler with the specified new name already exists in this
     *         pipeline, except for the handler to be replaced
     * @throws NullPointerException
     *         if the specified old handler or new handler is
     *         {@code null}
     */
    <T extends DataHandler> T replace(Class<T> oldHandlerType, String newName,
                                         DataHandler newHandler);

    /**
     * Returns the first {@link DataHandler} in this pipeline.
     *
     * @return the first handler.  {@code null} if this pipeline is empty.
     */
    DataHandler first();

    /**
     * Returns the context of the first {@link DataHandler} in this pipeline.
     *
     * @return the context of the first handler.  {@code null} if this pipeline is empty.
     */
    DataHandlerContext firstContext();

    /**
     * Returns the last {@link DataHandler} in this pipeline.
     *
     * @return the last handler.  {@code null} if this pipeline is empty.
     */
    DataHandler last();

    /**
     * Returns the context of the last {@link DataHandler} in this pipeline.
     *
     * @return the context of the last handler.  {@code null} if this pipeline is empty.
     */
    DataHandlerContext lastContext();

    /**
     * Returns the {@link DataHandler} with the specified name in this
     * pipeline.
     *
     * @return the handler with the specified name.
     *         {@code null} if there's no such handler in this pipeline.
     */
    DataHandler get(String name);

    /**
     * Returns the {@link DataHandler} of the specified type in this
     * pipeline.
     *
     * @return the handler of the specified handler type.
     *         {@code null} if there's no such handler in this pipeline.
     */
    <T extends DataHandler> T get(Class<T> handlerType);

    /**
     * Returns the context object of the specified {@link DataHandler} in
     * this pipeline.
     *
     * @return the context object of the specified handler.
     *         {@code null} if there's no such handler in this pipeline.
     */
    DataHandlerContext context(DataHandler handler);

    /**
     * Returns the context object of the {@link DataHandler} with the
     * specified name in this pipeline.
     *
     * @return the context object of the handler with the specified name.
     *         {@code null} if there's no such handler in this pipeline.
     */
    DataHandlerContext context(String name);

    /**
     * Returns the context object of the {@link DataHandler} of the
     * specified type in this pipeline.
     *
     * @return the context object of the handler of the specified type.
     *         {@code null} if there's no such handler in this pipeline.
     */
    DataHandlerContext context(Class<? extends DataHandler> handlerType);

    /**
     * Returns the {@link DataModel} that this pipeline is attached to.
     *
     * @return the channel. {@code null} if this pipeline is not attached yet.
     */
    DataModel dataModel();

    /**
     * Returns the {@link List} of the handler names.
     */
    List<String> names();

    /**
     * Converts this pipeline into an ordered {@link Map} whose keys are
     * handler names and whose values are handlers.
     */
    Map<String, DataHandler> toMap();

    @Override
    DataBoundInvoker fireDataPrepare();

    @Override
    DataBoundInvoker fireDataComplete();

    @Override
    DataBoundInvoker fireExceptionCaught(Throwable cause);
}