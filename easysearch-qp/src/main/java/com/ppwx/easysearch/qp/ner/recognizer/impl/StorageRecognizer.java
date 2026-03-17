/*
 * Copyright 2026 chengzhengZhou
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ppwx.easysearch.qp.ner.recognizer.impl;

import com.ppwx.easysearch.qp.ner.Entity;
import com.ppwx.easysearch.qp.ner.EntityIdentityMapper;
import com.ppwx.easysearch.qp.ner.EntityType;
import com.ppwx.easysearch.qp.ner.normalizer.EntityTypeNormalizer;
import com.ppwx.easysearch.qp.ner.recognizer.EntityTypeRecognizer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 存储容量实体识别器
 * 
 * 支持以下格式：
 * 1. 单存储容量：64gb, 128gb, 512gb, 1tb 等
 * 2. 内存+存储：8g+256g, 12g+512g 等
 * 
 */
public class StorageRecognizer implements EntityTypeRecognizer {
    
    // 匹配"内存+存储"格式，如：8g+256g, 12g+512g, 16g+1t
    private static final Pattern RAM_STORAGE_PATTERN = Pattern.compile(
            "(?<![a-zA-Z\\u4e00-\\u9fa5])" + // 前面不能是字母或汉字
            "(\\d+)\\s*[Gg]\\s*\\+\\s*(\\d+)\\s*([GgTt])" + // 数字+g+数字+g/t
            "(?![a-zA-Z\\u4e00-\\u9fa5])", // 后面不能是字母或汉字（除了可选的b）
            Pattern.CASE_INSENSITIVE
    );
    
    // 匹配单存储容量格式，如：64gb, 128gb, 512gb, 1tb
    // 要求：前面不能是字母/汉字，必须有完整的gb/tb单位
    private static final Pattern SINGLE_STORAGE_PATTERN = Pattern.compile(
            "(?<![a-zA-Z\\u4e00-\\u9fa5])" + // 前面不能是字母或汉字
            "(\\d+)\\s*([GgTt])[Bb]?" + // 数字+gb/tb
            "(?![a-zA-Z\\u4e00-\\u9fa5])", // 后面不能是字母或汉字
            Pattern.CASE_INSENSITIVE
    );
    
    private final EntityIdentityMapper identityMapper;
    
    public StorageRecognizer(EntityTypeNormalizer normalizer,
                            EntityIdentityMapper identityMapper) {
        // normalizer参数保留用于接口一致性，但不存储
        this.identityMapper = identityMapper;
    }
    
    @Override
    public EntityType getSupportedType() {
        return EntityType.STORAGE;
    }
    
    @Override
    public Entity recognize(String word, String nature) {
        // 优先匹配"内存+存储"格式
        Matcher ramStorageMatcher = RAM_STORAGE_PATTERN.matcher(word);
        if (ramStorageMatcher.find()) {
            String ramSize = ramStorageMatcher.group(1);
            String storageSize = ramStorageMatcher.group(2);
            String storageUnit = ramStorageMatcher.group(3).toLowerCase();
            String normalized = ramSize + "g+" + storageSize + storageUnit;
            return new Entity(word, EntityType.STORAGE, normalized,
                    identityMapper.map(EntityType.STORAGE, ramStorageMatcher.group(), normalized), 
                    ramStorageMatcher.start(), ramStorageMatcher.end());
        }
        
        // 匹配单存储容量格式
        Matcher singleMatcher = SINGLE_STORAGE_PATTERN.matcher(word);
        if (singleMatcher.find()) {
            String size = singleMatcher.group(1);
            String unit = singleMatcher.group(2).toLowerCase() + "b";
            String normalized = size + unit;
            return new Entity(word, EntityType.STORAGE, normalized,
                    identityMapper.map(EntityType.STORAGE, singleMatcher.group(), normalized), 
                    singleMatcher.start(), singleMatcher.end());
        }
        
        return null;
    }
}

