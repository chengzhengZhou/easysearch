package com.ppwx.easysearch.qp.ner.normalizer.impl;

import com.ppwx.easysearch.qp.ner.EntityType;
import com.ppwx.easysearch.qp.ner.normalizer.AbstractEntityTypeNormalizer;

import java.util.HashMap;
import java.util.Map;

/**
 * 成色归一化器
 * 
 * @author ext.ahs.zhouchzh1@jd.com
 * @date 2024/10/9
 */
public class ConditionNormalizer extends AbstractEntityTypeNormalizer {
    
    private static final Map<String, String> CONDITION_NORMALIZATION = new HashMap<>();
    
    static {
        CONDITION_NORMALIZATION.put("100新", "全新");
        CONDITION_NORMALIZATION.put("10成新", "全新");
        CONDITION_NORMALIZATION.put("九九新", "99新");
        CONDITION_NORMALIZATION.put("九五新", "95新");
        CONDITION_NORMALIZATION.put("九成新", "90新");
        CONDITION_NORMALIZATION.put("9成新", "90新");
        CONDITION_NORMALIZATION.put("八成新", "80新");
        CONDITION_NORMALIZATION.put("8成新", "80新");
        CONDITION_NORMALIZATION.put("七成新", "70新");
        CONDITION_NORMALIZATION.put("7成新", "70新");
        CONDITION_NORMALIZATION.put("六成新", "60新");
        CONDITION_NORMALIZATION.put("6成新", "60新");
        CONDITION_NORMALIZATION.put("五成新", "50新");
        CONDITION_NORMALIZATION.put("5成新", "50新");
    }
    
    public ConditionNormalizer() {
        super(EntityType.CONDITION);
    }
    
    @Override
    protected String doNormalize(String word) {
        // 查找成色映射
        for (Map.Entry<String, String> entry : CONDITION_NORMALIZATION.entrySet()) {
            if (word.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return word;
    }
}

