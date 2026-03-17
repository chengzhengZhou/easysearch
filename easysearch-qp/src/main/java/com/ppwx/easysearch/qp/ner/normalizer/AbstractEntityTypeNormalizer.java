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
 * 归一化器抽象基类
 * 提供通用的归一化逻辑框架
 * 
 * @author ext.ahs.zhouchzh1@jd.com
 * @date 2024/10/9
 */
public abstract class AbstractEntityTypeNormalizer implements EntityTypeNormalizer {
    
    protected final EntityType entityType;
    
    public AbstractEntityTypeNormalizer(EntityType entityType) {
        this.entityType = entityType;
    }
    
    @Override
    public EntityType getSupportedType() {
        return entityType;
    }
    
    @Override
    public String normalize(String word) {
        if (word == null || word.trim().isEmpty()) {
            return word;
        }
        return doNormalize(word.trim());
    }
    
    /**
     * 子类实现具体的归一化逻辑
     * 
     * @param word 原始词（已trim）
     * @return 归一化后的词
     */
    protected abstract String doNormalize(String word);
}

