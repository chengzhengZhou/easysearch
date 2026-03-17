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

package com.ppwx.easysearch.qp.ner.recognizer;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.Map;

/**
 * 型号实体行：在通用字段基础上提供型号相关方法。
 */
public class ModelDictEntityLine extends BaseDictEntityLine {

    private static final long serialVersionUID = 1L;

    /** 品牌 ID 在 attributes 中的 key */
    public static final String ATTR_BRAND_ID = "brandId";
    /** 类目 ID 在 attributes 中的 key */
    public static final String ATTR_CATEGORY_ID = "categoryId";
    /** 品牌名称 在 attributes 中的 key */
    public static final String ATTR_BRAND_NAME = "brandName";
    /** 类目名称 在 attributes 中的 key */
    public static final String ATTR_CATEGORY_NAME = "categoryName";
    /** 系列名称 */
    public static final String SERIES = "series";

    /**
     * 从 attributes 中取品牌 ID
     */
    @JSONField(serialize = false)
    public String getBrandId() {
        Map<String, String> attrs = getAttributes();
        return attrs != null ? attrs.get(ATTR_BRAND_ID) : null;
    }

    /**
     * 从 attributes 中取类目 ID
     */
    @JSONField(serialize = false)
    public String getCategoryId() {
        Map<String, String> attrs = getAttributes();
        return attrs != null ? attrs.get(ATTR_CATEGORY_ID) : null;
    }

    /**
     * 从 attributes 中取系列名称
     */
    @JSONField(serialize = false)
    public String getSeries() {
        Map<String, String> attrs = getAttributes();
        return attrs != null ? attrs.get(SERIES) : null;
    }

    @JSONField(serialize = false)
    public String getBrandName() {
        Map<String, String> attrs = getAttributes();
        return attrs != null ? attrs.get(ATTR_BRAND_NAME) : null;
    }

    @JSONField(serialize = false)
    public String getCategoryName() {
        Map<String, String> attrs = getAttributes();
        return attrs != null ? attrs.get(ATTR_CATEGORY_NAME) : null;
    }
}
