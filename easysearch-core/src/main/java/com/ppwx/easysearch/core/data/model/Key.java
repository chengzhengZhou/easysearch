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
 *
 * 配置信息key值
 *
 * @since 1.0.0
 */
public final class Key {

    private Key() {

    }

    /**
     * 索引
     */
    public static final String INDEX = "index";
    /**
     * 索引文档类型
     */
    public static final String TYPE = "type";
    /**
     * 字段
     */
    public static final String COLUMN = "column";
    /**
     * 名称
     */
    public static final String NAME = "name";
    /**
     * 条目唯一id
     */
    public static final String GLOBAL_ID = "gid";
    /**
     * 分值
     */
    public static final String GLOBAL_SCORE = "gScore";
    /**
     * 召回路标识
     */
    public static final String GLOBAL_STAINING = "gStaining";
    /**
     * 评分明细
     */
    public static final String GLOBAL_SCORE_DETAIL = "scoreDetail";

    /**
     * 纬度
     */
    public static final String COLUMN_LATITUDE = "lat";

    /**
     * 纬度
     */
    public static final String COLUMN_LONGITUDE = "lon";
}