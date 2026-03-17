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

package com.ppwx.easysearch.core.data.model;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @interface ICache
 * @description todo
 * @date 2024/11/28 17:27
 **/
public interface ICache <T, R> {

    /**
     * @description 获取缓存数据
     * @author ext.ahs.zhouchzh1@jd.com
     * @date 2024/11/28 17:29
     * @param key
     * @return R
     */
    R getIfPresent(T key);

    /**
     * @description 添加缓存数据
     * @author ext.ahs.zhouchzh1@jd.com
     * @date 2024/11/28 17:29
     * @param key
     * @param value
     * @return void
     */
    void put(T key, R value);

}
