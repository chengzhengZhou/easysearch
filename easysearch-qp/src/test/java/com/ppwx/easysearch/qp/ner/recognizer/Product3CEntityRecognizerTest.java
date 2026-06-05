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

package com.ppwx.easysearch.qp.ner.recognizer;

import com.ppwx.easysearch.qp.ner.Entity;
import com.ppwx.easysearch.qp.ner.EntityType;
import com.ppwx.easysearch.qp.ner.Product3CLabelMapper;
import com.ppwx.easysearch.qp.ner.Product3CNerConfig;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Product3CEntityRecognizer 单元测试。
 */
public class Product3CEntityRecognizerTest {

    @Test
    public void testLabelMapping() {
        Assert.assertEquals(EntityType.BRAND, Product3CLabelMapper.map("BRD"));
        Assert.assertEquals(EntityType.CATEGORY, Product3CLabelMapper.map("CAT"));
        Assert.assertEquals(EntityType.SERIES, Product3CLabelMapper.map("SER"));
        Assert.assertEquals(EntityType.MODEL, Product3CLabelMapper.map("MOD"));
        Assert.assertEquals(EntityType.PRM, Product3CLabelMapper.map("PRM"));
        Assert.assertEquals(EntityType.UNKNOWN, Product3CLabelMapper.map("NOT_EXIST"));
    }

    @Test
    public void testParseBioToEntities() {
        Product3CNerConfig config = new Product3CNerConfig()
                .setDefaultConfidence(0.7D)
                .setModelVersion("test-version");
        Product3CEntityRecognizer recognizer = new Product3CEntityRecognizer(config);
        String text = "华为mate60 512g";
        String normalized = "华为mate60 512g";
        String[] tags = new String[]{
                "B-BRD", "I-BRD",
                "B-SER", "I-SER", "I-SER", "I-SER", "B-MOD", "I-MOD",
                "O",
                "B-PRM", "I-PRM", "I-PRM", "I-PRM"
        };
        List<Entity> entities = recognizer.parseBioToEntitiesForTest(
                text, normalized, identity(normalized.length()), endIdentity(normalized.length()), tags);

        Assert.assertEquals(4, entities.size());
        Assert.assertEquals("华为", entities.get(0).getValue());
        Assert.assertEquals(EntityType.BRAND, entities.get(0).getType());
        Assert.assertEquals("mate", entities.get(1).getValue());
        Assert.assertEquals(EntityType.SERIES, entities.get(1).getType());
        Assert.assertEquals("60", entities.get(2).getValue());
        Assert.assertEquals(EntityType.MODEL, entities.get(2).getType());
        Assert.assertEquals("512g", entities.get(3).getValue());
        Assert.assertEquals(EntityType.PRM, entities.get(3).getType());
        Assert.assertEquals(0.7D, entities.get(0).getConfidence(), 0.0001D);
        Assert.assertTrue(entities.get(0).getAttachment() instanceof Map);
        Assert.assertEquals("test-version", ((Map<?, ?>) entities.get(0).getAttachment()).get("modelVersion"));
    }

    @Test
    public void testParseBioUnknownTypeIgnored() {
        Product3CEntityRecognizer recognizer = new Product3CEntityRecognizer(new Product3CNerConfig());
        List<Entity> entities = recognizer.parseBioToEntitiesForTest(
                "abc", "abc", identity(3), endIdentity(3), new String[]{"B-XXX", "I-XXX", "O"});
        Assert.assertTrue(entities.isEmpty());
    }

    @Test
    public void testRecognizeByModel() {
        Product3CNerConfig product3CNerConfig = new Product3CNerConfig();
        product3CNerConfig.setDictDir("data/dict/3c");
        product3CNerConfig.setModelPath("data/model/vocab_ner_crf.txt.bin");
        Product3CEntityRecognizer recognizer = new Product3CEntityRecognizer(product3CNerConfig);
        Collection<Entity> entities = recognizer.extractEntities("苹果iphone15 256G", Collections.emptyList());
        System.out.println(entities);
    }

    private static int[] identity(int length) {
        int[] arr = new int[length];
        for (int i = 0; i < length; i++) {
            arr[i] = i;
        }
        return arr;
    }

    private static int[] endIdentity(int length) {
        int[] arr = new int[length];
        for (int i = 0; i < length; i++) {
            arr[i] = i + 1;
        }
        return arr;
    }
}
