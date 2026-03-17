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

import com.ppwx.easysearch.qp.ner.Entity;
import com.ppwx.easysearch.qp.ner.EntityType;

/**
 * 单一实体类型识别器接口
 * 每个具体实现负责识别一种实体类型
 * 
 * @author ext.ahs.zhouchzh1@jd.com
 * @date 2024/10/9
 */
public interface EntityTypeRecognizer {
    
    /**
     * 获取该识别器支持的实体类型
     * 
     * @return 支持的实体类型
     */
    EntityType getSupportedType();
    
    /**
     * 从单个token中识别实体
     * 
     * @param word 词语
     * @param nature 词性
     * @return 识别到的实体，如果未识别到返回null
     */
    Entity recognize(String word, String nature);
    
    /**
     * 是否启用
     * 
     * @return true表示启用，false表示禁用
     */
    default boolean isEnabled() {
        return true;
    }
    
    /**
     * 优先级（数字越大优先级越高）
     * 
     * @return 优先级数值
     */
    default int getPriority() {
        return 0;
    }
}

