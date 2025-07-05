package com.ppwx.easysearch.core.query;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className SearchBuilderTest
 * @description 简易查询语句构造器
 * @date 2024/10/14 18:10
 **/
public class SearchBuilderTest {

    @Test
    public void testSearchBuilderWorks() {
        String query = SearchBuilder.builder()
                .encode(false)
                //.key("iphone 15")
                .valueFilter("categoryId", "655")
                //.valuesFilter("categoryId", Lists.newArrayList("2694", "12348", "842"))
                //.valuesFilter("brandId", Lists.newArrayList("14026"))
                //.betweenFilter("price", "1000", "2000")
                .notFilter("brandId", Lists.newArrayList("14026", "8557", "133787", "2032", "25591"))
                .sortDesc("composite")
                .customValues("name", "sophiszhou")
                .customValues("age", "18")
                .build().toQueryStr();
        System.out.println(query);

        SearchCriteria criteria = SearchCriteria.from(query, false);
        System.out.println(criteria);
    }

    @Test
    public void testSearchCriteriaWorks() {
        String query = "sort_type=sort_auto_desc&use_pagecache=yes&pagesize=10&filter_type=not_brandId,3||4;price,M15L20;categoryId,255;brandId,1||2&page=1&key=iphone 15&interleave=brand||category";
        SearchCriteria searchCriteria = SearchCriteria.from(query, false);
        Assert.assertEquals(searchCriteria.getKey().getValue(), "iphone 15");
        Assert.assertTrue(searchCriteria.getFilterByName("brandId").get().getMultiValues().contains("3"));
        Assert.assertTrue(searchCriteria.getAttrByName("use_pagecache").get().getValue().getValue().equals("yes"));
   }

   @Test
   public void testSearchCriteriaToStringWorks() {
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.setKey(new KeyQueryAttr("iphone 15"));
        searchCriteria.setSort(new SortQueryAttr(new SortQueryAttr.OrderBy("composite", "desc")));
        searchCriteria.setPage(new PageQueryAttr(1));
        searchCriteria.setPageSize(new PageQueryAttr(20));
        searchCriteria.addFilter(new FilterCondition("brand", "12", true));
        searchCriteria.addAttr(new ValueQueryAttr("interleave", "brand,category"));
        System.out.println(searchCriteria.toQueryString(false));
        System.out.println(searchCriteria.sign());
    }

    @Test
    public void testReadAndWriteWorks() {
        String query = "sort_type=sort_auto_desc&use_pagecache=yes&filter_type=not_brandId,3||4;price,M15L20;categoryId,255;brandId,1||2&pagesize=10&page=1&key=iphone 15";
        SearchCriteria searchCriteria = SearchCriteria.from(query, false);
        Assert.assertEquals(searchCriteria.toQueryString(false), query);
    }

    @Test
    public void testSortMatcher() {
        Pattern pattern = Pattern.compile("sort_(\\w+)_(asc|desc)+");
        Matcher sortAutoDesc = pattern.matcher("sort_auto_as");
        sortAutoDesc.find();
        System.out.println(sortAutoDesc.group(1));
        System.out.println(sortAutoDesc.group(2));
    }

    @Test
    public void testBetween() {
        Pattern pattern = Pattern.compile("M(\\d+)?|L(\\d+)?");
        Matcher matcher = pattern.matcher("M10L30");
        while (matcher.find()) {
            System.out.println(matcher.group());
        }
    }

}
