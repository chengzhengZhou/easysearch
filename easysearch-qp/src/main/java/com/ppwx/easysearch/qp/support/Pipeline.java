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
 * @author ext.ahs.zhouchzh1@jd.com
 * @interface Pipeline
 * @description 管道类
 * @date 2024/11/1 19:35
 **/
public interface Pipeline<T> {

    /**
     * 加入到列表的末尾
     * @param t 元素
     * @return this
     */
    Pipeline addLast(final T t);

    /**
     * 加入到列表的开头
     * @param t 元素
     * @return this
     */
    Pipeline addFirst(final T t);

    /**
     * 设置元素 index 位置为 t
     * @param index 下标志
     * @param t 元素
     * @return this
     */
    Pipeline set(final int index, final T t);

    /**
     * 移除最后一个元素
     * @return this
     */
    Pipeline removeLast();

    /**
     * 移除第一个元素
     * @return this
     */
    Pipeline removeFirst();

    /**
     * 移除 index 位置的元素
     * @param index 下标值
     * @return this
     */
    Pipeline remove(final int index);

    /**
     * 获取指定位置的元素
     * @param index 下标
     * @return 元素
     */
    T get(final int index);

    /**
     * 获取第一个位置的元素
     * @return 元素
     */
    T getFirst();

    /**
     * 获取最后一个位置的元素
     * @return 元素
     */
    T getLast();

    /**
     * 获取所有的元素列表
     * @return 所有的元素列表
     */
    List<T> list();

    /**
     * 进行 slice 分片返回一个从 startIndex~endIndex 的新列表
     * 1. 如果超过数组下标则直接报错
     * @param startIndex 开始下标
     * @param endIndex 结束下标
     * @return 截取后的元素列表
     */
    List<T> slice(final int startIndex, final int endIndex);

}
