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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pipeline 的默认实现
 *
 * @className DefaultPipeline
 **/
public class DefaultPipeline<T> implements Pipeline<T> {

    private final ArrayList<T> items = new ArrayList<>();

    @Override
    public Pipeline<T> addFirst(T t) {
        items.add(0, t);
        return this;
    }

    @Override
    public Pipeline<T> addLast(T t) {
        items.add(t);
        return this;
    }

    @Override
    public List<T> list() {
        return Collections.unmodifiableList(items);
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.isEmpty();
    }

}
