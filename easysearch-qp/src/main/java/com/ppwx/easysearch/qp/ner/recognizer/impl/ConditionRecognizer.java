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
import com.ppwx.easysearch.qp.ner.recognizer.AbstractDictionaryBasedRecognizer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 成色实体识别器
 * 
 * @author ext.ahs.zhouchzh1@jd.com
 * @date 2024/10/9
 */
public class ConditionRecognizer extends AbstractDictionaryBasedRecognizer {
    
    private static final Pattern CONDITION_PATTERN = Pattern.compile("(\\d{1,2})\\s*(成新|新)");
    
    public ConditionRecognizer(EntityTypeNormalizer normalizer,
                              EntityIdentityMapper identityMapper) {
        super(EntityType.CONDITION, normalizer, identityMapper);
    }
    
    @Override
    protected Entity recognizeByRules(String word, String nature) {
        Matcher matcher = CONDITION_PATTERN.matcher(word);
        if (matcher.find()) {
            String normalized;
            String num = matcher.group(1);
            if (num.length() == 1) {
                normalized = num + "0" + "新";
            } else {
                normalized = num + "新";
            }
            return new Entity(word, EntityType.CONDITION, normalized,
                    identityMapper.map(entityType, matcher.group(), normalized), matcher.start(), matcher.end());
        }
        return null;
    }
}

