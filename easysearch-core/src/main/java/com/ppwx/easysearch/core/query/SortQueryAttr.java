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
