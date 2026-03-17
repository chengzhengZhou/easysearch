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
import com.ppwx.easysearch.qp.ner.EntityIdentityMapper;
import com.ppwx.easysearch.qp.ner.EntityType;
import com.ppwx.easysearch.qp.ner.normalizer.EntityTypeNormalizer;

import java.util.List;

/**
 * 基于词典和词性的实体识别器抽象基类
 * 将归一化器作为依赖注入
 * 
 * @author ext.ahs.zhouchzh1@jd.com
 * @date 2024/10/9
 */
public abstract class AbstractDictionaryBasedRecognizer implements EntityTypeRecognizer {
    
    protected final EntityType entityType;
    protected final EntityTypeNormalizer normalizer;
    protected final EntityIdentityMapper identityMapper;
    
    public AbstractDictionaryBasedRecognizer(EntityType entityType,
                                            EntityTypeNormalizer normalizer,
                                            EntityIdentityMapper identityMapper) {
        this.entityType = entityType;
        this.normalizer = normalizer;
        this.identityMapper = identityMapper;
    }
    
    @Override
    public EntityType getSupportedType() {
        return entityType;
    }
    
    @Override
    public Entity recognize(String word, String nature) {
        // 归一化
        String normalize = normalizer != null ? normalizer.normalize(word) : word;

        // ID映射
        List<String> ids = identityMapper.map(entityType, word, normalize);

        // 检查词性
        if (entityType.name().equalsIgnoreCase(nature)) {
            return new Entity(word, entityType, normalize, ids);
        }

        // 尝试id匹配
        if (ids != null) {
            return new Entity(word, entityType, normalize, ids);
        }

        // 尝试规则识别
        return recognizeByRules(word, nature);
    }
    
    /**
     * 基于规则的识别（子类可选实现）
     * 
     * @param word 原始词
     * @param nature 词性
     * @return 识别到的实体，如果未识别到返回null
     */
    protected Entity recognizeByRules(String word, String nature) {
        return null;
    }
}

