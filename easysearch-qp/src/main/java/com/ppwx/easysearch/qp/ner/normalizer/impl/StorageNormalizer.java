package com.ppwx.easysearch.qp.ner.normalizer.impl;

import com.ppwx.easysearch.qp.ner.EntityType;
import com.ppwx.easysearch.qp.ner.normalizer.AbstractEntityTypeNormalizer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 存储容量归一化器
 * 
 * @author ext.ahs.zhouchzh1@jd.com
 * @date 2024/10/9
 */
public class StorageNormalizer extends AbstractEntityTypeNormalizer {
    
    private static final Pattern UNIT_PATTERN = Pattern.compile("(\\d+)\\s*([GgTtMmKk][BbGg]?)");
    
    public StorageNormalizer() {
        super(EntityType.STORAGE);
    }
    
    @Override
    protected String doNormalize(String word) {
        Matcher matcher = UNIT_PATTERN.matcher(word);
        if (matcher.find()) {
            String number = matcher.group(1);
            String unit = matcher.group(2).toLowerCase();
            
            // 统一单位格式
            if (unit.startsWith("g")) {
                unit = "gb";
            } else if (unit.startsWith("t")) {
                unit = "tb";
            } else if (unit.startsWith("m")) {
                unit = "mb";
            } else if (unit.startsWith("k")) {
                unit = "kb";
            }
            
            return number + unit;
        }
        return word;
    }
}

