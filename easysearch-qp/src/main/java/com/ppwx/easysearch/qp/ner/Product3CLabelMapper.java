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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 3C CRF 标签到 easysearch 实体类型的映射。
 */
public final class Product3CLabelMapper {

    private static final Map<String, EntityType> TYPE_MAP;

    static {
        Map<String, EntityType> map = new HashMap<>();
        map.put("BRD", EntityType.BRAND);
        map.put("CAT", EntityType.CATEGORY);
        map.put("SER", EntityType.SERIES);
        map.put("MOD", EntityType.MODEL);
        map.put("VER", EntityType.VERSION);
        map.put("PRM", EntityType.PRM);
        map.put("CON", EntityType.CONDITION);
        map.put("CLR", EntityType.COLOR);
        map.put("REG", EntityType.REGION);
        map.put("YR", EntityType.YEAR);
        TYPE_MAP = Collections.unmodifiableMap(map);
    }

    private Product3CLabelMapper() {
    }

    public static EntityType map(String rawType) {
        if (rawType == null) {
            return EntityType.UNKNOWN;
        }
        EntityType type = TYPE_MAP.get(rawType.trim().toUpperCase());
        return type != null ? type : EntityType.UNKNOWN;
    }

    public static Map<String, EntityType> mappings() {
        return TYPE_MAP;
    }
}
