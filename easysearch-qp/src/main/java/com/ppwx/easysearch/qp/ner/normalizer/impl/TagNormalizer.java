package com.ppwx.easysearch.qp.ner.normalizer.impl;

import com.ppwx.easysearch.qp.ner.EntityType;
import com.ppwx.easysearch.qp.ner.normalizer.AbstractEntityTypeNormalizer;

/**
 * 标签归一化器
 * 
 * @author ext.ahs.zhouchzh1@jd.com
 * @date 2024/10/9
 */
public class TagNormalizer extends AbstractEntityTypeNormalizer {
    
    public TagNormalizer() {
        super(EntityType.TAG);
    }
    
    @Override
    protected String doNormalize(String word) {
        return word;
    }
}

