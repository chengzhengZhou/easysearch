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
