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

