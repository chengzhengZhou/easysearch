package com.ppwx.easysearch.qp.ner.recognizer.impl;

import com.ppwx.easysearch.qp.ner.EntityIdentityMapper;
import com.ppwx.easysearch.qp.ner.EntityType;
import com.ppwx.easysearch.qp.ner.normalizer.EntityTypeNormalizer;
import com.ppwx.easysearch.qp.ner.recognizer.AbstractDictionaryBasedRecognizer;

/**
 * 标签实体识别器
 * 
 * @author ext.ahs.zhouchzh1@jd.com
 * @date 2024/10/9
 */
public class TagRecognizer extends AbstractDictionaryBasedRecognizer {
    
    public TagRecognizer(EntityTypeNormalizer normalizer,
                        EntityIdentityMapper identityMapper) {
        super(EntityType.TAG, normalizer, identityMapper);
    }
}

