package com.ppwx.easysearch.core.query;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className PageQueryAttr
 * @description 分页参数pagesize、page
 * @date 2024/10/14 19:25
 **/
public class PageQueryAttr extends QueryAttr<Integer> {

    private Integer val;

    public PageQueryAttr(Integer val) {
        this.val = val;
    }

    @Override
    public Integer getValue() {
        return val;
    }
}
