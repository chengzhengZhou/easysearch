/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: Key
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/12/12 12:43
 * Description: 配置信息key值
 */
package com.ppwx.easysearch.core.data.model;

/**
 *
 * 配置信息key值
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/12 12:43
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