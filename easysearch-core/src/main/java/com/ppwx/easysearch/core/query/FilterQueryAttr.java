package com.ppwx.easysearch.core.query;

import java.util.List;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className FilterQueryAttr
 * @description 条件过滤
 * @date 2024/10/14 19:31
 **/
public class FilterQueryAttr extends QueryAttr<List<FilterCondition>> {

    private List<FilterCondition> conditions;

    public FilterQueryAttr(List<FilterCondition> conditions) {
        this.conditions = conditions;
    }

    @Override
    public List<FilterCondition> getValue() {
        return conditions;
    }
}
