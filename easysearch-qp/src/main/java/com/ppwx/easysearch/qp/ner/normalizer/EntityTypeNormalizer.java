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

package com.ppwx.easysearch.qp.ner.normalizer;

import com.ppwx.easysearch.qp.ner.EntityType;

/**
 * 单一实体类型归一化器接口
 * 每个具体实现负责一种实体类型的归一化
 * 
 * @author ext.ahs.zhouchzh1@jd.com
 * @date 2024/10/9
 */
public interface EntityTypeNormalizer {
    
    /**
     * 获取该归一化器支持的实体类型
     * 
     * @return 支持的实体类型
     */
    EntityType getSupportedType();
    
    /**
     * 归一化实体值
     * 
     * @param word 原始实体词
     * @return 归一化后的实体词
     */
    String normalize(String word);
    
    /**
     * 是否启用
     * 
     * @return true表示启用，false表示禁用
     */
    default boolean isEnabled() {
        return true;
    }
}

