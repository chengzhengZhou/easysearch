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

import java.util.List;

/**
 * 管道 - 有序元素容器，支持头尾添加与只读遍历
 *
 * @interface Pipeline
 **/
public interface Pipeline<T> {

    /**
     * 在列表开头插入元素
     *
     * @param t 要插入的元素
     * @return 当前管道实例（支持链式调用）
     */
    Pipeline<T> addFirst(T t);

    /**
     * 在列表末尾追加元素
     *
     * @param t 要追加的元素
     * @return 当前管道实例（支持链式调用）
     */
    Pipeline<T> addLast(T t);

    /**
     * 返回所有元素的只读视图
     *
     * @return 不可变的元素列表
     */
    List<T> list();

    /**
     * 返回当前元素数量
     *
     * @return 元素数量
     */
    int size();

    /**
     * 判断管道是否为空
     *
     * @return 如果没有任何元素则返回 true
     */
    boolean isEmpty();

}
