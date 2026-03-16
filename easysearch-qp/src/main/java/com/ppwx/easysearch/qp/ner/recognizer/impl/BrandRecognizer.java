package com.ppwx.easysearch.qp.ner.recognizer.impl;

import com.ppwx.easysearch.qp.ner.EntityIdentityMapper;
import com.ppwx.easysearch.qp.ner.EntityType;
import com.ppwx.easysearch.qp.ner.normalizer.EntityTypeNormalizer;
import com.ppwx.easysearch.qp.ner.recognizer.AbstractDictionaryBasedRecognizer;

/**
 * 品牌实体识别器
 * 
 * @author ext.ahs.zhouchzh1@jd.com
 * @date 2024/10/9
 */
public class BrandRecognizer extends AbstractDictionaryBasedRecognizer {
    
    public BrandRecognizer(EntityTypeNormalizer normalizer,
                          EntityIdentityMapper identityMapper) {
        super(EntityType.BRAND, normalizer, identityMapper);
    }
    
    @Override
    public int getPriority() {
        return 10;
    }
}

