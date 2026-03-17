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

package com.ppwx.easysearch.qp.support;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @className DefaultPipeline
 * @description 默认实现
 **/
public class DefaultPipeline<T> implements Pipeline<T> {

    /**
     * 创建一个内部的链表
     */
    private LinkedList<T> list = new LinkedList<>();

    @Override
    public Pipeline<T> addLast(T t) {
        list.addLast(t);
        return this;
    }

    @Override
    public Pipeline<T> addFirst(T t) {
        list.addFirst(t);
        return this;
    }

    @Override
    public Pipeline<T> set(int index, T t) {
        list.set(index, t);
        return this;
    }

    @Override
    public Pipeline<T> removeLast() {
        list.removeLast();
        return this;
    }

    @Override
    public Pipeline<T> removeFirst() {
        list.removeFirst();
        return this;
    }

    @Override
    public Pipeline<T> remove(int index) {
        list.remove(index);
        return this;
    }

    @Override
    public T get(int index) {
        return list.get(index);
    }

    @Override
    public T getFirst() {
        return list.getFirst();
    }

    @Override
    public T getLast() {
        return list.getLast();
    }

    @Override
    public List<T> list() {
        return Collections.unmodifiableList(list);
    }

    @Override
    public List<T> slice(int startIndex, int endIndex) {
        return list.subList(startIndex, endIndex);
    }

}
