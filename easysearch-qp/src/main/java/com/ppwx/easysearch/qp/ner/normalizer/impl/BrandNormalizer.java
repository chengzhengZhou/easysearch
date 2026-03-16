package com.ppwx.easysearch.qp.ner.normalizer.impl;

import com.ppwx.easysearch.qp.ner.EntityType;
import com.ppwx.easysearch.qp.ner.normalizer.AbstractEntityTypeNormalizer;

/**
 * 品牌归一化器
 * 
 * @author ext.ahs.zhouchzh1@jd.com
 * @date 2024/10/9
 */
public class BrandNormalizer extends AbstractEntityTypeNormalizer {
    
    public BrandNormalizer() {
        super(EntityType.BRAND);
    }
    
    @Override
    protected String doNormalize(String word) {
        // 品牌一般不需要特殊归一化
        return word;
    }
}

