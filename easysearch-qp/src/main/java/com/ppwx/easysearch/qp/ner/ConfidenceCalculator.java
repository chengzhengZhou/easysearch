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

import java.util.Collection;

/**
 * 实体识别置信度计算器
 * 基于多种因素动态计算实体识别的置信度
 * 
 */
public class ConfidenceCalculator {
    
    // 基础置信度
    private static final double BASE_CONFIDENCE_EXACT_MATCH = 0.95;      // 精确匹配
    private static final double BASE_CONFIDENCE_PARTIAL_MATCH = 0.75;    // 部分匹配
    private static final double BASE_CONFIDENCE_FUZZY_MATCH = 0.60;      // 模糊匹配
    
    // 长度因素
    private static final int MIN_RELIABLE_LENGTH = 2;   // 最小可靠长度（字符数）
    private static final int OPTIMAL_LENGTH = 4;        // 最优长度
    
    // 上下文加成
    private static final double CONTEXT_BOOST = 0.05;   // 有上下文时的加成
    private static final double ID_BOOST = 0.03;        // 有ID映射时的加成
    
    /**
     * 计算实体识别的置信度
     * 
     * @param entity 实体
     * @param matchType 匹配类型
     * @param hasContext 是否有上下文支持（如品牌+型号组合）
     * @param allEntities 同一查询中识别出的所有实体（用于上下文分析）
     * @return 置信度值 [0.0, 1.0]
     */
    public static double calculate(Entity entity, MatchType matchType, 
                                   boolean hasContext, Collection<Entity> allEntities) {
        double confidence = getBaseConfidence(matchType);
        
        // 1. 长度因素调整
        confidence = adjustByLength(confidence, entity.getValue());
        
        // 2. 实体类型调整
        confidence = adjustByEntityType(confidence, entity.getType());
        
        // 3. 上下文支持调整
        if (hasContext) {
            confidence += CONTEXT_BOOST;
        }
        
        // 4. ID映射调整
        if (entity.getId() != null && !entity.getId().isEmpty()) {
            confidence += ID_BOOST;
        }
        
        // 5. 多实体组合验证
        confidence = adjustByMultiEntityContext(confidence, entity, allEntities);
        
        // 确保在[0.0, 1.0]范围内
        return Math.max(0.0, Math.min(1.0, confidence));
    }
    
    /**
     * 简化版本：仅根据匹配类型和长度计算
     */
    public static double calculate(String value, EntityType type, MatchType matchType) {
        double confidence = getBaseConfidence(matchType);
        confidence = adjustByLength(confidence, value);
        confidence = adjustByEntityType(confidence, type);
        return Math.max(0.0, Math.min(1.0, confidence));
    }
    
    /**
     * 获取基础置信度
     */
    private static double getBaseConfidence(MatchType matchType) {
        switch (matchType) {
            case EXACT_MATCH:
                return BASE_CONFIDENCE_EXACT_MATCH;
            case PARTIAL_MATCH:
                return BASE_CONFIDENCE_PARTIAL_MATCH;
            case FUZZY_MATCH:
                return BASE_CONFIDENCE_FUZZY_MATCH;
            case RULE_BASED:
                return 0.85;  // 基于规则的识别
            default:
                return 0.70;
        }
    }
    
    /**
     * 根据长度调整置信度
     * 短词容易误匹配，长词更可靠
     */
    private static double adjustByLength(double confidence, String value) {
        if (value == null) return confidence;
        
        int length = value.length();
        
        // 单字符：置信度打折
        if (length == 1) {
            return confidence * 0.5;
        }
        
        // 2-3字符：略微降低
        if (length < MIN_RELIABLE_LENGTH) {
            return confidence * 0.7;
        }
        
        // 2-4字符：略微降低
        if (length < OPTIMAL_LENGTH) {
            return confidence * 0.85;
        }
        
        // 4字符以上：正常或略微提升
        if (length >= OPTIMAL_LENGTH) {
            return Math.min(confidence * 1.02, 1.0);
        }
        
        return confidence;
    }
    
    /**
     * 根据实体类型调整置信度
     * 不同类型的实体识别难度不同
     */
    private static double adjustByEntityType(double confidence, EntityType type) {
        switch (type) {
            case BRAND:
                // 品牌通常比较确定
                return confidence;
                
            case MODEL:
                // 型号识别较为可靠
                return confidence;
                
            case CATEGORY:
                // 类别词可能有歧义
                return confidence * 0.95;
                
            case CONDITION:
                // 成色词较短，容易歧义
                return confidence * 0.90;
                
            case TAG:
                // 标签词歧义较多
                return confidence * 0.85;
                
            case COLOR:
            case SIZE:
            case STORAGE:
            case RAM:
                // 规格词较短，容易误匹配
                return confidence * 0.90;
                
            default:
                return confidence * 0.90;
        }
    }
    
    /**
     * 根据多实体上下文调整置信度
     * 如果识别出的实体之间有合理的组合关系，可以提升置信度
     */
    private static double adjustByMultiEntityContext(double confidence, Entity entity, 
                                                     Collection<Entity> allEntities) {
        if (allEntities == null || allEntities.size() <= 1) {
            return confidence;
        }
        
        EntityType type = entity.getType();
        boolean hasBrand = false;
        boolean hasModel = false;
        boolean hasCategory = false;
        
        for (Entity other : allEntities) {
            if (other == entity) continue;
            
            EntityType otherType = other.getType();
            if (otherType == EntityType.BRAND) hasBrand = true;
            if (otherType == EntityType.MODEL) hasModel = true;
            if (otherType == EntityType.CATEGORY) hasCategory = true;
        }
        
        // 品牌+型号组合：相互验证，提升置信度
        if (type == EntityType.BRAND && hasModel) {
            return Math.min(confidence + 0.03, 1.0);
        }
        if (type == EntityType.MODEL && hasBrand) {
            return Math.min(confidence + 0.03, 1.0);
        }
        
        // 品牌+类别组合：也是合理组合
        if (type == EntityType.BRAND && hasCategory) {
            return Math.min(confidence + 0.02, 1.0);
        }
        if (type == EntityType.CATEGORY && hasBrand) {
            return Math.min(confidence + 0.02, 1.0);
        }
        
        return confidence;
    }
    
    /**
     * 匹配类型枚举
     */
    public enum MatchType {
        
        EXACT_MATCH,
        
        
        PARTIAL_MATCH,
        
        
        FUZZY_MATCH,
        
        
        RULE_BASED,
        
        
        OTHER
    }
    
    /**
     * 判断置信度等级
     */
    public static ConfidenceLevel getConfidenceLevel(double confidence) {
        if (confidence >= 0.9) {
            return ConfidenceLevel.VERY_HIGH;
        } else if (confidence >= 0.8) {
            return ConfidenceLevel.HIGH;
        } else if (confidence >= 0.7) {
            return ConfidenceLevel.MEDIUM;
        } else if (confidence >= 0.6) {
            return ConfidenceLevel.LOW;
        } else {
            return ConfidenceLevel.VERY_LOW;
        }
    }
    
    /**
     * 置信度等级
     */
    public enum ConfidenceLevel {
        VERY_HIGH("非常高", "可直接使用"),
        HIGH("高", "基本可信"),
        MEDIUM("中等", "需要验证"),
        LOW("低", "谨慎使用"),
        VERY_LOW("很低", "不建议使用");
        
        private final String description;
        private final String suggestion;
        
        ConfidenceLevel(String description, String suggestion) {
            this.description = description;
            this.suggestion = suggestion;
        }
        
        public String getDescription() { return description; }
        public String getSuggestion() { return suggestion; }
    }
}

