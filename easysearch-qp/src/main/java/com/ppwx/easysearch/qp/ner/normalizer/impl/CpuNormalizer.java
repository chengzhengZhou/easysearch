package com.ppwx.easysearch.qp.ner.normalizer.impl;

import com.ppwx.easysearch.qp.ner.EntityType;
import com.ppwx.easysearch.qp.ner.normalizer.AbstractEntityTypeNormalizer;

import java.util.HashMap;
import java.util.Map;

/**
 * CPU归一化器
 * 
 * @author ext.ahs.zhouchzh1@jd.com
 * @date 2024/10/9
 */
public class CpuNormalizer extends AbstractEntityTypeNormalizer {
    
    private static final Map<String, String> CPU_NORMALIZATION = new HashMap<>();
    
    static {
        CPU_NORMALIZATION.put("i3", "intel core i3");
        CPU_NORMALIZATION.put("i5", "intel core i5");
        CPU_NORMALIZATION.put("i7", "intel core i7");
        CPU_NORMALIZATION.put("i9", "intel core i9");
        CPU_NORMALIZATION.put("麒麟", "kirin");
        CPU_NORMALIZATION.put("天玑", "dimensity");
        CPU_NORMALIZATION.put("联发科", "mediatek");
        CPU_NORMALIZATION.put("高通", "qualcomm");
    }
    
    public CpuNormalizer() {
        super(EntityType.CPU);
    }
    
    @Override
    protected String doNormalize(String word) {
        String normalized = word.toLowerCase();
        
        // 查找CPU映射
        for (Map.Entry<String, String> entry : CPU_NORMALIZATION.entrySet()) {
            if (normalized.contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }
        return word;
    }
}

