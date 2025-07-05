package com.ppwx.easysearch.core.query;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className SortQueryAttr
 * @description 排序
 * @date 2024/10/14 19:25
 **/
public class SortQueryAttr extends QueryAttr<SortQueryAttr.OrderBy> {

    private OrderBy orderBy;

    public SortQueryAttr(OrderBy orderBy) {
        this.orderBy = orderBy;
    }

    @Override
    public SortQueryAttr.OrderBy getValue() {
        return orderBy;
    }

    public static class OrderBy {
        public OrderBy(String field, String order) {
            this.field = field;
            this.order = order;
        }

        private String field;

        private String order;

        public String getField() {
            return field;
        }

        public String getOrder() {
            return order;
        }
    }
}
