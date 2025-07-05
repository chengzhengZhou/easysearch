package com.ppwx.easysearch.core.query;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className SearchCriteria
 * @description 检索条件
 * @date 2024/10/14 19:18
 **/
public class SearchCriteria {

    private static final String KEY = "key";
    private static final String SIR_KEY = "sir_key";
    private static final String SORT_TYPE = "sort_type";
    private static final String FILTER_TYPE = "filter_type";
    private static final String PAGE = "page";
    private static final String PAGE_SIZE = "pagesize";
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

    private static final String SORT_PRE = "sort_";
    /**
     * 分隔符
     */
    private static final String SEPARATOR = ";";
    /**
     * 查询词
     */
    private KeyQueryAttr key;
    /**
     * 二次查询词
     */
    private KeyQueryAttr sirKey;
    /**
     * 过滤条件
     */
    private FilterQueryAttr filter;
    /**
     * 排序条件
     */
    private SortQueryAttr sort;
    /**
     * 页码
     */
    private PageQueryAttr page;
    /**
     * 分页大小
     */
    private PageQueryAttr pageSize;
    /**
     * 自定义的参数
     */
    private List<ValueQueryAttr> customAttrs;

    /**
     * 排序
     */
    private final Pattern sortPattern = Pattern.compile(SORT_PRE + "(\\w+)_(asc|desc)+");
    /**
     * 区间
     */
    private final Pattern filterPattern = Pattern.compile("M(\\d+)?|L(\\d+)?");

    public SearchCriteria() {
    }

    private SearchCriteria(String query) {
        parseQuery(query);
    }

    private void parseQuery(String query) {
        StringBuilder builder = new StringBuilder();
        char[] charArray = (query + "&").toCharArray();
        for (char ch : charArray) {
            if (ch == '&') {
                // parse part
                if (builder.length() > 0) {
                    int seq = builder.indexOf("=");
                    if (seq <= 0) {
                        throw new RuntimeException("illegal query string part: " + builder);
                    }
                    String field = builder.substring(0, seq);
                    String trimVal = StringUtils.trim(builder.substring(seq + 1));
                    if (StringUtils.equalsIgnoreCase(field, KEY)) {
                        this.key = new KeyQueryAttr(trimVal);
                    } else if (StringUtils.equalsIgnoreCase(field, SIR_KEY)) {
                        this.sirKey = new KeyQueryAttr(trimVal);
                    } else if (StringUtils.equalsIgnoreCase(field, SORT_TYPE)) {
                        Matcher matcher = sortPattern.matcher(trimVal);
                        if (matcher.find()) {
                            this.sort = new SortQueryAttr(new SortQueryAttr.OrderBy(matcher.group(1), matcher.group(2)));
                        } else {
                            throw new RuntimeException("illegal query sort part: " + builder);
                        }
                    } else if (StringUtils.equalsIgnoreCase(field, FILTER_TYPE)) {
                        List<FilterCondition> filters = Lists.newArrayList();
                        Iterator<String> iterator = Splitter.on(SEPARATOR).split(trimVal).iterator();
                        FilterCondition condition;
                        String next;
                        boolean isNegative;
                        while (iterator.hasNext()) {
                            next = iterator.next();
                            int index = next.indexOf(",");
                            String firstPart = next.substring(0, index).trim();
                            String restPart = next.substring(index + 1);
                            isNegative = firstPart.startsWith(NOT);
                            if (isNegative) {
                                firstPart = firstPart.substring(4);
                            }

                            if (restPart.indexOf("||") > 0) {
                                // multi value
                                condition = new FilterCondition(firstPart, Lists.newArrayList(restPart.split("\\|\\|")), isNegative);
                            } else if (filterPattern.matcher(restPart).find()) {
                                // between
                                Matcher matcher = filterPattern.matcher(restPart);
                                String minVal = null;
                                String maxVal = null;
                                while (matcher.find()) {
                                    String group = matcher.group();
                                    if (group.startsWith(MIN)) {
                                        minVal = group.substring(1);
                                    } else {
                                        maxVal = group.substring(1);
                                    }
                                }
                                condition = new FilterCondition(firstPart, minVal, maxVal, isNegative);
                            } else {
                                // single value
                                condition = new FilterCondition(firstPart, restPart, isNegative);
                            }
                            filters.add(condition);
                        }
                        this.filter = new FilterQueryAttr(filters);
                    } else if (StringUtils.equalsIgnoreCase(field, PAGE)) {
                        this.page = new PageQueryAttr(StringUtils.isBlank(trimVal) ? null : Integer.valueOf(trimVal));
                    } else if (StringUtils.equalsIgnoreCase(field, PAGE_SIZE)) {
                        this.pageSize = new PageQueryAttr(StringUtils.isBlank(trimVal) ? null : Integer.valueOf(trimVal));
                    } else {
                        addAttr(new ValueQueryAttr(field, trimVal));
                    }
                }
                builder.setLength(0);
            } else {
                builder.append(ch);
            }
        }
    }

    /**
     * @description 根据名称查询
     * @author ext.ahs.zhouchzh1@jd.com
     * @date 2024/10/15 14:07
     * @param name
     * @return Optional<FilterCondition>
     */
    public Optional<FilterCondition> getFilterByName(String name) {
        if (this.filter == null) {
            return Optional.empty();
        }
        return this.filter.getValue().stream().filter(attr -> StringUtils.equals(attr.getField(), name)).findFirst();
    }

    /**
     * @description 根据名称查询自定义字段
     * @author ext.ahs.zhouchzh1@jd.com
     * @date 2024/10/17 15:41
     * @param name
     * @return Optional<ValueQueryAttr>
     */
    public Optional<ValueQueryAttr> getAttrByName(String name) {
        if (this.customAttrs == null) {
            return Optional.empty();
        }
        return this.customAttrs.stream().filter(attr -> StringUtils.equals(attr.getValue().getKey(), name)).findFirst();
    }

    /**
     * @description 解析{@link SearchBuilder}构建的查询语句
     * @author ext.ahs.zhouchzh1@jd.com
     * @date 2024/10/14 19:55
     * @param query 查询语句
     * @param encoded 是否编码
     * @return SearchCriteria
     */
    public static SearchCriteria from(String query, boolean encoded) {
        if (query == null) {
            throw new RuntimeException("query string must exists.");
        }
        if (encoded) {
            try {
                query = URLDecoder.decode(query, StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        return new SearchCriteria(query);
    }

    /**
     * @description 转换为查询字符串
     * @author ext.ahs.zhouchzh1@jd.com
     * @date 2024/10/17 14:23
     * @param encode
     * @return String
     */
    public String toQueryString(boolean encode) {
        Map<String, String> params = new HashMap<>(16);
        // search key
        if (null != key) {
            params.put(KEY, key.getValue());
        }
        // sir_key
        if (null != sirKey) {
            params.put(SIR_KEY, sirKey.getValue());
        }
        // sort
        if (null != sort) {
            params.put(SORT_TYPE, SORT_PRE + sort.getValue().getField() + "_" + sort.getValue().getOrder());
        }
        // page
        if (null != page) {
            params.put(PAGE, page.getValue().toString());
        }
        if (null != pageSize) {
            params.put(PAGE_SIZE, pageSize.getValue().toString());
        }

        // filters
        StringBuilder builder = new StringBuilder();
        if (null != this.filter) {
            this.filter.getValue().forEach(condition -> {
                if (builder.length() > 0) {
                    builder.append(SEPARATOR);
                }
                // name
                if (condition.isNegative()) {
                    builder.append(NOT);
                }
                builder.append(condition.getField()).append(",");
                // value
                if (condition.isMulti()) {
                    builder.append(StringUtils.join(condition.getMultiValues(), "||"));
                } else if (condition.isBetween()) {
                    if (null != condition.getMinValue()) {
                        builder.append(MIN).append(condition.getMinValue());
                    }
                    if (null != condition.getMaxValue()) {
                        builder.append(MAX).append(condition.getMaxValue());
                    }
                } else {
                    builder.append(condition.getValue());
                }
            });
        }
        if (builder.length() > 0) {
            params.put(FILTER_TYPE, builder.toString());
        }
        // custom attrs
        if (null != this.customAttrs) {
            for (ValueQueryAttr attr : customAttrs) {
                params.put(attr.getValue().getKey(), attr.getValue().getValue());
            }
        }

        // join params
        builder.setLength(0);
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
     * @description 对查询参数进行签名，可用与缓存等场景
     * @author ext.ahs.zhouchzh1@jd.com
     * @date 2024/10/17 14:16
     * @return String
     */
    public String sign() {
        return DigestUtils.md5Hex(toQueryString(false));
    }

    public void addAttr(ValueQueryAttr attr) {
        if (this.customAttrs == null) {
            this.customAttrs = Lists.newArrayList();
        }
        this.customAttrs.add(attr);
    }

    public void addFilter(FilterCondition condition) {
        if (this.filter == null) {
            this.filter = new FilterQueryAttr(Lists.newArrayList());
        }
        this.filter.getValue().add(condition);
    }

    public KeyQueryAttr getKey() {
        return key;
    }

    public FilterQueryAttr getFilter() {
        return filter;
    }

    public SortQueryAttr getSort() {
        return sort;
    }

    public PageQueryAttr getPage() {
        return page;
    }

    public PageQueryAttr getPageSize() {
        return pageSize;
    }

    public KeyQueryAttr getSirKey() {
        return sirKey;
    }

    public void setKey(KeyQueryAttr key) {
        this.key = key;
    }

    public void setSirKey(KeyQueryAttr sirKey) {
        this.sirKey = sirKey;
    }

    public void setSort(SortQueryAttr sort) {
        this.sort = sort;
    }

    public void setPage(PageQueryAttr page) {
        this.page = page;
    }

    public void setPageSize(PageQueryAttr pageSize) {
        this.pageSize = pageSize;
    }
}
