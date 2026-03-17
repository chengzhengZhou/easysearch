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
