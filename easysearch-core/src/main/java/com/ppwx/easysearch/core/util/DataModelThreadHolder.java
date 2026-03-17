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

package com.ppwx.easysearch.core.util;

import com.ppwx.easysearch.core.data.DataModel;

/**
 *
 * DataModel 线程副本
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/12 17:24
 * @since 1.0.0
 */
public final class DataModelThreadHolder {

    private static final ThreadLocal<DataModel> LOCAL = new ThreadLocal<>();

    private DataModelThreadHolder() {

    }

    /**
     * 创建线程存储副本
     *
     * @param dataModel
     * @return void
     */
    public static void set(DataModel dataModel) {
        LOCAL.set(dataModel);
    }

    /**
     * 从{@link Thread} 的map中获取对象
     *
     * @param
     * @return com.ppwx.easysearch.core.data.DataModel
     */
    public static DataModel get() {
        return LOCAL.get();
    }

    /**
     * 清除{@link Thread} map存储的对象
     *
     * @param
     * @return void
     */
    public static void clear() {
        LOCAL.remove();
    }

}