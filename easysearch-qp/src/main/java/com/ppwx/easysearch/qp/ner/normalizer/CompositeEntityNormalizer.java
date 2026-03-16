package com.ppwx.easysearch.qp.ner.normalizer;

import com.ppwx.easysearch.qp.ner.EntityNormalizer;
import com.ppwx.easysearch.qp.ner.EntityType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 组合归一化器
 * 实现旧的 EntityNormalizer 接口，确保向后兼容
 * 
 * @author ext.ahs.zhouchzh1@jd.com
 * @date 2024/10/9
 */
public class CompositeEntityNormalizer implements EntityNormalizer {
    
    private final Map<EntityType, EntityTypeNormalizer> normalizerMap;
    
    public CompositeEntityNormalizer(List<EntityTypeNormalizer> normalizers) {
        this.normalizerMap = new HashMap<>();
        for (EntityTypeNormalizer normalizer : normalizers) {
            if (normalizer.isEnabled()) {
                normalizerMap.put(normalizer.getSupportedType(), normalizer);
            }
        }
    }
    
    @Override
    public String normalize(EntityType entityType, String word) {
        EntityTypeNormalizer normalizer = normalizerMap.get(entityType);
        if (normalizer != null) {
            return normalizer.normalize(word);
        }
        // 如果没有对应的归一化器，返回原值
        return word;
    }
    
    /**
     * 获取指定类型的归一化器
     * 
     * @param entityType 实体类型
     * @return 对应的归一化器，如果不存在返回null
     */
    public EntityTypeNormalizer getNormalizer(EntityType entityType) {
        return normalizerMap.get(entityType);
    }
}

