package com.ppwx.easysearch.qp.ner.normalizer.impl;

import com.ppwx.easysearch.qp.ner.EntityType;
import com.ppwx.easysearch.qp.ner.normalizer.AbstractEntityTypeNormalizer;

/**
 * 品类归一化器
 * 
 * @author ext.ahs.zhouchzh1@jd.com
 * @date 2024/10/9
 */
public class CategoryNormalizer extends AbstractEntityTypeNormalizer {
    
    public CategoryNormalizer() {
        super(EntityType.CATEGORY);
    }
    
    @Override
    protected String doNormalize(String word) {
        // 品类归一化逻辑
        return word;
    }
}

