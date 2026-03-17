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

package com.ppwx.easysearch.qp.ner.normalizer.impl;

import com.ppwx.easysearch.qp.ner.EntityType;
import com.ppwx.easysearch.qp.ner.normalizer.AbstractEntityTypeNormalizer;

/**
 * 品牌归一化器
 * 
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

