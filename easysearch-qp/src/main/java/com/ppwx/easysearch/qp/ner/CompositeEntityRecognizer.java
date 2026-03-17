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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hankcs.hanlp.corpus.tag.Nature;
import com.ppwx.easysearch.qp.ner.recognizer.EntityTypeRecognizer;
import com.ppwx.easysearch.qp.tokenizer.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 组合实体识别器
 * 通过组合多个EntityTypeRecognizer实现可扩展的实体识别
 * 
 */
public class CompositeEntityRecognizer implements EntityRecognizer {
    
    private static final Logger log = LoggerFactory.getLogger(CompositeEntityRecognizer.class);
    
    private final List<EntityTypeRecognizer> recognizers;
    
    public CompositeEntityRecognizer(List<EntityTypeRecognizer> recognizers) {
        // 按优先级排序
        this.recognizers = recognizers.stream()
                .filter(EntityTypeRecognizer::isEnabled)
                .sorted(Comparator.comparingInt(EntityTypeRecognizer::getPriority).reversed())
                .collect(Collectors.toList());
    }
    
    @Override
    public Collection<Entity> extractEntities(String originText, List<Token> tokens) {
        Map<EntityType, List<Entity>> entitiesMap = new HashMap<>();
        
        // 初始化各实体类型的列表
        recognizers.forEach(r -> 
            entitiesMap.putIfAbsent(r.getSupportedType(), new ArrayList<>()));
        
        // 添加整个原文作为额外token
        List<Token> moreTokens = Lists.newArrayList(tokens);
        if (moreTokens.size() > 1) {
            moreTokens.add(Token.builder()
                    .text(originText)
                    .type(Nature.nz.toString())
                    .startIndex(0)
                    .endIndex(originText.length())
                    .build());
        }
        
        // 对每个token应用所有识别器
        for (Token token : moreTokens) {
            String word = token.getText();
            String nature = token.getType();
            
            for (EntityTypeRecognizer recognizer : recognizers) {
                try {
                    Entity entity = recognizer.recognize(word, nature);
                    if (entity != null) {
                        // 设置实体的位置信息
                        if (entity.getEndOffset() <= 0) {
                            entity.setStartOffset(token.getStartIndex());
                            entity.setEndOffset(token.getEndIndex());
                        }
                        entitiesMap.get(recognizer.getSupportedType()).add(entity);
                    }
                } catch (Exception e) {
                    log.error("Error in recognizer {} for word: {}", 
                            recognizer.getClass().getSimpleName(), word, e);
                }
            }
        }
        
        // 后处理
        postProcessEntities(entitiesMap);
        
        // 收集所有实体用于置信度计算
        List<Entity> allEntities = entitiesMap.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        
        // 重新计算置信度（考虑上下文）
        recalculateConfidence(allEntities);
        
        log.debug("Extracted entities: {}", entitiesMap);
        return allEntities;
    }
    
    /**
     * 后处理：去重和合并相关实体
     */
    private void postProcessEntities(Map<EntityType, List<Entity>> entities) {
        // 去重处理 - 对每种类型的实体进行去重
        entities.forEach((type, entityList) -> {
            if (entityList != null && !entityList.isEmpty()) {
                deduplicateEntities(entityList);
            }
        });
        
        // 合并相邻的型号实体
        //mergeAdjacentEntities(entities.get(EntityType.MODEL));
        
        // 合并相邻的品牌实体
        //mergeAdjacentEntities(entities.get(EntityType.BRAND));
    }
    
    /**
     * 合并相邻的实体
     */
    private void mergeAdjacentEntities(List<Entity> entities) {
        if (entities == null || entities.size() <= 1) {
            return;
        }
        
        List<Entity> merged = new ArrayList<>();
        Entity current = entities.get(0);
        
        for (int i = 1; i < entities.size(); i++) {
            Entity next = entities.get(i);
            
            if (current.getEndOffset() + 1 == next.getStartOffset()) {
                String mergedValue = current.getValue() + " " + next.getValue();
                String mergedNormalized = current.getNormalizedValue() + " " + next.getNormalizedValue();
                Set<String> ids = Sets.newHashSet();
                if (current.getId() != null) {
                    ids.addAll(current.getId());
                }
                if (next.getId() != null) {
                    ids.addAll(next.getId());
                }
                Entity mergedEntity = new Entity(mergedValue, current.getType(), mergedNormalized,
                        ids.isEmpty() ? null : Lists.newArrayList(ids));
                // 保持位置信息：从current的起始位置到next的结束位置
                mergedEntity.setStartOffset(current.getStartOffset());
                mergedEntity.setEndOffset(next.getEndOffset());
                // 保持置信度（取两者的平均值）
                mergedEntity.setConfidence((current.getConfidence() + next.getConfidence()) / 2.0);
                current = mergedEntity;
            } else {
                merged.add(current);
                current = next;
            }
        }
        
        merged.add(current);
        entities.clear();
        entities.addAll(merged);
    }
    
    /**
     * 实体去重：处理重复和重叠的实体
     * 去重规则：
     * 1. 完全相同的实体（normalizedValue和ID都相同）只保留一个
     * 2. 如果两个实体有位置重叠，根据优先级规则选择保留哪个
     */
    private void deduplicateEntities(List<Entity> entities) {
        if (entities == null || entities.size() <= 1) {
            return;
        }
        
        // 先按startOffset排序，便于后续处理
        entities.sort(Comparator.comparingInt(Entity::getStartOffset)
                .thenComparingInt(Entity::getEndOffset));
        
        List<Entity> result = new ArrayList<>();
        Set<String> seenValues = new HashSet<>();
        
        for (Entity entity : entities) {
            // 创建唯一标识：normalizedValue + ID
            String key = entity.getNormalizedValue();
            if (entity.getId() != null && !entity.getId().isEmpty()) {
                key += ":" + String.join(",", entity.getId());
            }
            
            boolean isDuplicate = false;
            
            // 检查是否与已添加的实体重复
            if (seenValues.contains(key)) {
                isDuplicate = true;
                log.debug("Found duplicate entity: {}, skipping", entity);
            } else {
                // 检查是否与已添加的实体有位置重叠
                Entity overlappingEntity = null;
                for (Entity existing : result) {
                    if (isOverlapping(entity, existing)) {
                        overlappingEntity = existing;
                        break;
                    }
                }
                
                if (overlappingEntity != null) {
                    // 如果重叠，选择保留更合适的实体
                    if (shouldReplace(entity, overlappingEntity)) {
                        result.remove(overlappingEntity);
                        String existingKey = overlappingEntity.getNormalizedValue();
                        if (overlappingEntity.getId() != null && !overlappingEntity.getId().isEmpty()) {
                            existingKey += ":" + String.join(",", overlappingEntity.getId());
                        }
                        seenValues.remove(existingKey);
                        log.debug("Replacing overlapping entity {} with {}", overlappingEntity, entity);
                    } else {
                        isDuplicate = true;
                        log.debug("Found overlapping entity, keeping existing: {}, skipping: {}", overlappingEntity, entity);
                    }
                }
            }
            
            if (!isDuplicate) {
                result.add(entity);
                seenValues.add(key);
            }
        }
        
        entities.clear();
        entities.addAll(result);
        
        log.debug("After deduplication: {} entities remain", entities.size());
    }
    
    /**
     * 判断两个实体是否有位置重叠
     * 
     * @param e1 实体1
     * @param e2 实体2
     * @return 如果有重叠返回true，否则返回false
     */
    private boolean isOverlapping(Entity e1, Entity e2) {
        // 如果没有位置信息，认为不重叠
        if (e1.getStartOffset() == 0 && e1.getEndOffset() == 0) {
            return false;
        }
        if (e2.getStartOffset() == 0 && e2.getEndOffset() == 0) {
            return false;
        }
        
        // 检查两个区间是否有重叠
        // 不重叠的条件是：e1完全在e2左边 或 e2完全在e1左边
        // 重叠的条件就是上述条件的否定
        return !(e1.getEndOffset() <= e2.getStartOffset() || 
                 e2.getEndOffset() <= e1.getStartOffset());
    }
    
    /**
     * 决定是否应该用新实体替换已存在的实体
     * 优先级规则：
     * 1. 置信度高的优先
     * 2. 有ID的优先（说明已经标准化并映射到了具体实体）
     * 3. 更长的实体优先（通常更具体）
     * 
     * @param newEntity 新实体
     * @param existingEntity 已存在的实体
     * @return 如果应该替换返回true，否则返回false
     */
    private boolean shouldReplace(Entity newEntity, Entity existingEntity) {
        // 1. 置信度差异明显时，选择置信度高的
        double confidenceDiff = newEntity.getConfidence() - existingEntity.getConfidence();
        if (Math.abs(confidenceDiff) > 0.1) {
            return confidenceDiff > 0;
        }
        
        // 2. 如果一个有ID一个没有，选择有ID的
        boolean newHasId = newEntity.getId() != null && !newEntity.getId().isEmpty();
        boolean existingHasId = existingEntity.getId() != null && !existingEntity.getId().isEmpty();
        if (newHasId != existingHasId) {
            return newHasId;
        }
        
        // 3. 选择更长的实体（通常更具体）
        int newLength = newEntity.getNormalizedValue().length();
        int existingLength = existingEntity.getNormalizedValue().length();
        return newLength > existingLength;
    }
    
    /**
     * 重新计算实体置信度
     * 在所有实体识别完成后，根据上下文信息重新计算置信度
     */
    private void recalculateConfidence(List<Entity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        
        // 检测是否有品牌+型号组合
        boolean hasBrandAndModel = hasBothTypes(entities, EntityType.BRAND, EntityType.MODEL);
        boolean hasBrandAndCategory = hasBothTypes(entities, EntityType.BRAND, EntityType.CATEGORY);
        
        for (Entity entity : entities) {
            double originalConfidence = entity.getConfidence();
            
            // 如果原始置信度是默认的1.0，则重新计算
            if (originalConfidence >= 0.99) {
                // 根据实体值长度和类型计算基础置信度
                double newConfidence;
                
                // 考虑上下文因素
                boolean hasContext = false;
                if (entity.getType() == EntityType.BRAND && hasBrandAndModel) {
                    hasContext = true;
                } else if (entity.getType() == EntityType.MODEL && hasBrandAndModel) {
                    hasContext = true;
                } else if (entity.getType() == EntityType.CATEGORY && hasBrandAndCategory) {
                    hasContext = true;
                }
                
                newConfidence = ConfidenceCalculator.calculate(
                    entity, 
                    determineMatchType(entity), 
                    hasContext, 
                    entities
                );
                
                entity.setConfidence(newConfidence);
                
                log.debug("Updated confidence for entity '{}' from {} to {}", 
                    entity.getValue(), originalConfidence, newConfidence);
            }
        }
    }
    
    /**
     * 判断是否同时存在两种类型的实体
     */
    private boolean hasBothTypes(List<Entity> entities, EntityType type1, EntityType type2) {
        boolean hasType1 = false;
        boolean hasType2 = false;
        
        for (Entity entity : entities) {
            if (entity.getType() == type1) hasType1 = true;
            if (entity.getType() == type2) hasType2 = true;
            if (hasType1 && hasType2) return true;
        }
        
        return false;
    }
    
    /**
     * 确定匹配类型
     * 根据实体特征判断是精确匹配、部分匹配还是规则匹配
     */
    private ConfidenceCalculator.MatchType determineMatchType(Entity entity) {
        // 如果有ID映射，说明是精确匹配
        if (entity.getId() != null && !entity.getId().isEmpty()) {
            return ConfidenceCalculator.MatchType.EXACT_MATCH;
        }
        
        // 如果value和normalizedValue相同，说明是精确匹配
        if (entity.getValue().equals(entity.getNormalizedValue())) {
            return ConfidenceCalculator.MatchType.EXACT_MATCH;
        }
        
        // 否则认为是部分匹配或规则匹配
        return ConfidenceCalculator.MatchType.PARTIAL_MATCH;
    }
}

