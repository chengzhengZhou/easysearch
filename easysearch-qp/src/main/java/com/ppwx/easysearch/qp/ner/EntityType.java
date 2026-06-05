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

package com.ppwx.easysearch.qp.ner;

/**
 * 实体类型枚举
 */
public enum EntityType {
    CATEGORY("类别"),
    BRAND("品牌"),
    SERIES("系列"),
    MODEL("型号"),
    CONDITION("成色"),
    COLOR("颜色"),
    VERSION("版本"),
    PRM("参数"),
    REGION("地区/网络"),
    YEAR("年份"),
    TAG("标签"),
    UNKNOWN("未知");
    
    private final String description;
    
    EntityType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }

    public static EntityType getByName(String name) {
        for (EntityType type : EntityType.values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return UNKNOWN;
    }

    /**
     * 判断是否为已定义的实体类型（非 UNKNOWN）
     *
     * @param name 类型名称，如 BRAND、MODEL 或词性 NR、NN
     * @return 若 name 对应枚举中已定义的实体类型返回 true，否则 false
     */
    public static boolean isDefinedEntityType(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        return getByName(name) != UNKNOWN;
    }
}

