/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: JdSearchBuilder
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/8/22 18:30
 * Description: 京东检索查询构造器
 */
package com.ppwx.easysearch.core.query;

import com.google.common.base.Joiner;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * 检索查询构造器
 * 相关规则参考文档https://cf.jd.com/pages/viewpage.action?pageId=72738717
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/08/22 18:30
 * @since 1.0.0
 */
public class SearchBuilder {
    /**
     * LxxMyy( xx为最大值，yy为最小值,返回大于等于yy并且小于等于xx的商品)
     */
    private static final String MIN = "M";
    /**
     * LxxMyy( xx为最大值，yy为最小值,返回大于等于yy并且小于等于xx的商品)
     */
    private static final String MAX = "L";
    /**
     * not_{字段名}
     */
    private static final String NOT = "not_";
    /**
     * 分隔符
     */
    private static final String SEPARATOR = ";";
    /**
     * key分隔符
     */
    private static final String KEY_SEPARATOR = ",,";

    /**
     * 查询关键词key
     */
    private String key;
    /**
     * 二次搜索词
     */
    private String sirKey;
    /**
     * 过滤
     */
    private Map<String, String> filters;
    /**
     * 自定义过滤，用于支持特殊的过滤条件
     */
    private Map<String, String> customFilters;
    /**
     * 排序
     */
    private String sort;

    /**
     * 分页大小
     */
    protected Integer pageSize;
    /**
     * 页码，从1开始
     */
    protected Integer page;
    /**
     * 开始位置，从第几个商品开始
     */
    private Integer start;

    /**
     * 附加参数
     */
    private Map<String, String> attributes;

    /**
     * 是否编码
     */
    private boolean encode = true;

    /**
     * 默认构造器
     *
     * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
     * @param
     * @date 2023/8/23 17:19
     * @return
     */
    private SearchBuilder() {

    }

    /**
     * 搜索词，若无会设置no_key=yes
     */
    public SearchBuilder key(String key) {
        this.key = StringUtils.trim(key);
        return this;
    }

    /**
     * 特殊key搜索
     * 详见https://cf.jd.com/pages/viewpage.action?pageId=72738717
     */
    public SearchBuilder specialKey(String field, String value) {
        this.key = field + KEY_SEPARATOR + StringUtils.trim(value);
        return this;
    }

    /**
     * 特殊key搜索
     * 详见https://cf.jd.com/pages/viewpage.action?pageId=72738717
     */
    public SearchBuilder specialKeys(String field, Collection<String> values) {
        StringBuilder builder = new StringBuilder();
        builder.append("[(]");
        Iterator<String> iterator = values.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            if (i > 0) {
                builder.append("[|]");
            }
            builder.append(field).append(KEY_SEPARATOR).append(StringUtils.trim(iterator.next()));
            ++i;
        }
        builder.append("[)]");
        this.key = builder.toString();
        return this;
    }

    /**
     * 二次搜索词
     */
    public SearchBuilder sirKey(String key) {
        this.sirKey = StringUtils.trim(key);
        return this;
    }

    /**
     * 分页大小
     */
    public SearchBuilder pageSize(Integer pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    /**
     * 页码
     */
    public SearchBuilder page(Integer page) {
        this.page = page;
        return this;
    }

    /**
     * 起始下表
     */
    public SearchBuilder start(Integer start) {
        this.start = start;
        return this;
    }

    /**
     * 是否使用cache
     *
     * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
     * @param useCache
     * @date 2023/8/23 17:11
     * @return com.jd.ershou.rec.call.SearchBuilder
     */
    public SearchBuilder useCache(boolean useCache) {
        attributes.put("usecache", BooleanUtils.toStringYesNo(useCache));
        return this;
    }

    /**
     * 是否使用page cache
     *
     * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
     * @param userPageCache
     * @date 2023/8/23 17:11
     * @return com.jd.ershou.rec.call.SearchBuilder
     */
    public SearchBuilder usePageCache(boolean userPageCache) {
        attributes.put("use_pagecache", BooleanUtils.toStringYesNo(userPageCache));
        return this;
    }

    /**
     * 拓展的相关配置字段
     *
     * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
     * @param field
     * @param value
     * @date 2023/8/23 17:44
     * @return com.jd.ershou.rec.call.SearchBuilder
     */
    public SearchBuilder customValues(String field, String value) {
        attributes.put(field, value);
        return this;
    }

    /**
     * 正排字段-范围过滤
     *
     * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
     * @param field
     * @param min 最小值，可空
     * @param max 最大值，可空
     * @date 2023/8/23 17:11
     * @return com.jd.ershou.rec.call.SearchBuilder
     */
    public SearchBuilder betweenFilter(String field, String min, String max) {
        assert min != null || max != null;

        StringBuilder builder = new StringBuilder();
        if (min != null) {
            builder.append(MIN).append(StringUtils.trim(min));
        }
        if (max != null) {
            builder.append(MAX).append(StringUtils.trim(max));
        }
        filters.put(field, builder.toString());
        return this;
    }

    /**
     * 正排字段-多值过滤
     *
     * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
     * @param field
     * @param values
     * @date 2023/8/23 17:11
     * @return com.jd.ershou.rec.call.SearchBuilder
     */
    public SearchBuilder valuesFilter(String field, Collection<String> values) {
        filters.put(field, Joiner.on("||").join(values));
        return this;
    }

    /**
     * 正排字段-单值过滤
     *
     * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
     * @param field
     * @param value
     * @date 2023/8/23 17:10
     * @return com.jd.ershou.rec.call.SearchBuilder
     */
    public SearchBuilder valueFilter(String field, String value) {
        filters.put(field, StringUtils.trim(value));
        return this;
    }

    /**
     * 正排字段-单值非过滤
     *
     * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
     * @param field
     * @param values
     * @date 2023/8/23 17:10
     * @return com.jd.ershou.rec.call.SearchBuilder
     */
    public SearchBuilder notFilter(String field, Collection<String> values) {
        filters.put(NOT + field, Joiner.on("||").join(values));
        return this;
    }

    /**
     * 正排字段-位置排序
     *
     * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
     * @param
     * @date 2023/8/23 17:10
     * @return com.jd.ershou.rec.call.SearchBuilder
     */
    public SearchBuilder locationFilter() {
        filters.put("longitude_latitude", "");
        return this;
    }

    /**
     * 自定义过滤
     *
     * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
     * @param alias
     * @param expression
     * @date 2023/8/23 16:17
     * @return com.jd.ershou.rec.call.SearchBuilder
     */
    public SearchBuilder customFilter(String alias, String expression) {
        customFilters.put(alias, expression);
        return this;
    }

    /**
     * 位置参数： location=纬度:经度:半径
     * 注：半径的单位是公里
     *
     * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
     * @param latitude
     * @param longitude
     * @param radius 传入的是公里
     * @date 2023/8/23 17:09
     * @return com.jd.ershou.rec.call.SearchBuilder
     */
    public SearchBuilder location(String latitude, String longitude, Integer radius) {
        StringBuilder builder = new StringBuilder();
        builder.append(latitude).append(":").append(longitude).append(":").append(radius);
        attributes.put("location", builder.toString());
        return this;
    }

    /**
     * 排序-正排
     *
     * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
     * @param field
     * @date 2023/8/23 16:40
     * @return com.jd.ershou.rec.call.SearchBuilder
     */
    public SearchBuilder sortAsc(String field) {
        this.sort = "sort_" + field + "_asc";
        return this;
    }

    /**
     * 排序-倒排
     *
     * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
     * @param field
     * @date 2023/8/23 16:50
     * @return com.jd.ershou.rec.call.SearchBuilder
     */
    public SearchBuilder sortDesc(String field) {
        this.sort = "sort_" + field + "_desc";
        return this;
    }

    /**
     * 排序-地址位置正排
     *
     * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
     * @param
     * @date 2023/8/23 17:09
     * @return com.jd.ershou.rec.call.SearchBuilder
     */
    public SearchBuilder sortLocationAsc() {
        this.sort = "sort_longitude_latitude_asc";
        return this;
    }

    /**
     * 是否对url编码
     *
     * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
     * @param encode
     * @date 2023/8/23 20:52
     * @return com.jd.ershou.rec.recall.SearchBuilder
     */
    public SearchBuilder encode(boolean encode) {
        this.encode = encode;
        return this;
    }

    /**
     * 构建成查询参数
     *
     * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
     * @param 
     * @date 2023/8/23 16:51
     * @return com.ppwx.easysearch.core.query.SearchBuilder
     */
    public SearchBuilder build() {
        return this;
    }

    /**
     * 构建成查询参数
     *
     * @author ext.ahs.zhouchzh1@jd.com
     * @date 2024/10/14 19:07
     * @return String
     */
    public String toQueryStr() {
        Map<String, String> params = new HashMap<>(16);
        // search key
        if (StringUtils.isNoneBlank(key)) {
            params.put("key", key);
        }
        // sir_key
        if (StringUtils.isNotBlank(sirKey)) {
            params.put("sir_key", sirKey);
        }
        // sort
        if (StringUtils.isNotBlank(sort)) {
            params.put("sort_type", sort);
        }
        // page
        if (null != page) {
            params.put("page", page.toString());
        }
        if (null != pageSize) {
            params.put("pagesize", pageSize.toString());
        }
        if (null != start) {
            params.put("start", start.toString());
        }
        // ext
        params.putAll(this.attributes);

        // filters
        StringBuilder valueBd = new StringBuilder();
        if (!this.filters.isEmpty()) {
            this.filters.forEach((k, v) -> {
                if (valueBd.length() > 0) {
                    valueBd.append(SEPARATOR);
                }
                valueBd.append(k).append(",").append(v);
            });
        }
        if (!this.customFilters.isEmpty()) {
            this.customFilters.values().forEach(v -> {
                if (valueBd.length() > 0) {
                    valueBd.append(SEPARATOR);
                }
                valueBd.append(v);
            });
        }
        if (valueBd.length() > 0) {
            params.put("filter_type", valueBd.toString());
        }

        // join params
        StringBuilder builder = new StringBuilder();
        params.forEach((k, v) -> {
            if (builder.length() > 0) {
                builder.append("&");
            }
            try {
                builder.append(k).append("=").append(encode ? URLEncoder.encode(v, StandardCharsets.UTF_8.name()) : v);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        });

        return builder.toString();
    }

    /**
     * 静态构建
     *
     * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
     * @param 
     * @date 2023/8/23 17:25
     * @return com.jd.ershou.rec.call.SearchBuilder
     */
    public static SearchBuilder builder() {
        SearchBuilder builder = new SearchBuilder();
        builder.filters = new HashMap<>(8);
        builder.customFilters = new HashMap<>(8);
        builder.attributes = new HashMap<>(8);
        return builder;
    }

    @Override
    public String toString() {
        return this.toQueryStr();
    }
}