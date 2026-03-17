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

package com.ppwx.easysearch.qp.ner;

import com.ppwx.easysearch.qp.ner.recognizer.DictEntityRecognizer;
import com.ppwx.easysearch.qp.tokenizer.Token;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * PriorityEntityRecognizer 单元测试
 */
public class PriorityEntityRecognizerTest {

    private DictEntityRecognizer dictRecognizer;
    private PriorityEntityRecognizer recognizer;

    @Before
    public void setUp() {
        String dictContent = "影石\tBRAND\t影石\n" +
                "华为\tBRAND\t华为\n" +
                "影石摄像机\tMODEL\t影石摄像机\n";
        dictRecognizer = DictEntityRecognizer.fromStream(
                new java.io.ByteArrayInputStream(dictContent.getBytes(StandardCharsets.UTF_8)));
        recognizer = new PriorityEntityRecognizer(dictRecognizer);
    }

    @Test
    public void testDefaultConstructor() {
        PriorityEntityRecognizer recognizer = new PriorityEntityRecognizer();
        Assert.assertNotNull(recognizer);
    }

    @Test
    public void testDictFirstConstructor() {
        PriorityEntityRecognizer recognizer = new PriorityEntityRecognizer(MergeStrategy.DICT_FIRST);
        Assert.assertNotNull(recognizer);
    }

    @Test
    public void testCrfFirstConstructor() {
        PriorityEntityRecognizer recognizer = new PriorityEntityRecognizer(MergeStrategy.CRF_FIRST);
        Assert.assertNotNull(recognizer);
    }

    @Test
    public void testEmptyInput() {
        Collection<Entity> entities = recognizer.extractEntities("", Collections.emptyList());
        Assert.assertNotNull(entities);
        Assert.assertTrue(entities.isEmpty());
    }

    @Test
    public void testNullText() {
        Collection<Entity> entities = recognizer.extractEntities(null, Collections.emptyList());
        Assert.assertNotNull(entities);
        Assert.assertTrue(entities.isEmpty());
    }

    @Test
    public void testNullTokens() {
        Collection<Entity> entities = recognizer.extractEntities("有内容", null);
        Assert.assertNotNull(entities);
        Assert.assertTrue(entities.isEmpty());
    }

    @Test
    public void testEmptyTokens() {
        Collection<Entity> entities = recognizer.extractEntities("有内容", Collections.emptyList());
        Assert.assertNotNull(entities);
        Assert.assertTrue(entities.isEmpty());
    }

    @Test
    public void testWithDictLoaded_ReturnsDictEntities() {
        String text = "影石";
        List<Token> tokens = Collections.singletonList(
                Token.builder().text("影石").type("nr").startIndex(0).endIndex(2).build());

        Collection<Entity> entities = recognizer.extractEntities(text, tokens);

        Assert.assertFalse(entities.isEmpty());
        Assert.assertTrue(entities.stream().anyMatch(e ->
                "影石".equals(e.getValue()) && EntityType.BRAND == e.getType()));
    }

    @Test
    public void testWithDictLoaded_MultipleEntities() {
        String text = "影石和华为";
        List<Token> tokens = Arrays.asList(
                Token.builder().text("影石").type("nr").startIndex(0).endIndex(2).build(),
                Token.builder().text("和").type("c").startIndex(2).endIndex(3).build(),
                Token.builder().text("华为").type("nr").startIndex(3).endIndex(5).build());

        Collection<Entity> entities = recognizer.extractEntities(text, tokens);

        Assert.assertTrue(entities.size() >= 2);
        Assert.assertTrue(entities.stream().anyMatch(e -> "影石".equals(e.getValue())));
        Assert.assertTrue(entities.stream().anyMatch(e -> "华为".equals(e.getValue())));
    }

    @Test
    public void testResultSortedByOffset() {
        String text = "影石摄像机";
        List<Token> tokens = Arrays.asList(
                Token.builder().text("影石").type("nr").startIndex(0).endIndex(2).build(),
                Token.builder().text("摄像机").type("n").startIndex(2).endIndex(5).build());

        Collection<Entity> entities = recognizer.extractEntities(text, tokens);

        int prevEnd = -1;
        for (Entity e : entities) {
            Assert.assertTrue("实体应按 startOffset 排序", e.getStartOffset() >= prevEnd);
            prevEnd = e.getEndOffset();
        }
    }

    @Test
    public void testDictFirstAndCrfFirst_BothProduceValidResults() {
        String text = "华为平板m5";
        List<Token> tokens = Arrays.asList(
                Token.builder().text("华为").type("nr").startIndex(0).endIndex(2).build(),
                Token.builder().text("平板").type("n").startIndex(2).endIndex(4).build(),
                Token.builder().text("m5").type("nx").startIndex(4).endIndex(6).build());

        PriorityEntityRecognizer dictFirst = new PriorityEntityRecognizer(MergeStrategy.DICT_FIRST, dictRecognizer);
        PriorityEntityRecognizer crfFirst = new PriorityEntityRecognizer(MergeStrategy.CRF_FIRST, dictRecognizer);

        Collection<Entity> dictFirstEntities = dictFirst.extractEntities(text, tokens);
        Collection<Entity> crfFirstEntities = crfFirst.extractEntities(text, tokens);

        Assert.assertNotNull(dictFirstEntities);
        Assert.assertNotNull(crfFirstEntities);
        for (Entity e : dictFirstEntities) {
            Assert.assertNotNull(e.getValue());
            Assert.assertNotNull(e.getType());
            Assert.assertTrue(e.getStartOffset() >= 0 && e.getEndOffset() <= text.length());
        }
        for (Entity e : crfFirstEntities) {
            Assert.assertNotNull(e.getValue());
            Assert.assertNotNull(e.getType());
            Assert.assertTrue(e.getStartOffset() >= 0 && e.getEndOffset() <= text.length());
        }
    }

    @Test
    public void testMergeStrategy_DictFirst_PrefersDictOnOverlap() {
        Entity dictEntity = new Entity("华为", EntityType.BRAND, "华为品牌", 1.0, 0, 2);
        Entity crfEntity = new Entity("华为", EntityType.BRAND, "华为", 1.0, 0, 2);
        EntityMerger merger = new EntityMerger(MergeStrategy.DICT_FIRST);

        List<Entity> merged = merger.merge(Collections.singletonList(dictEntity), Collections.singletonList(crfEntity));

        Assert.assertEquals(1, merged.size());
        Assert.assertEquals("华为品牌", merged.get(0).getNormalizedValue());
    }

    @Test
    public void testMergeStrategy_CrfFirst_PrefersCrfOnOverlap() {
        Entity dictEntity = new Entity("华为", EntityType.BRAND, "华为品牌", 1.0, 0, 2);
        Entity crfEntity = new Entity("华为", EntityType.BRAND, "华为", 1.0, 0, 2);
        EntityMerger merger = new EntityMerger(MergeStrategy.CRF_FIRST);

        List<Entity> merged = merger.merge(Collections.singletonList(dictEntity), Collections.singletonList(crfEntity));

        Assert.assertEquals(1, merged.size());
        Assert.assertEquals("华为", merged.get(0).getNormalizedValue());
    }

    @Test
    public void testMergeStrategy_NoOverlap_CombinesBoth() {
        Entity dictEntity = new Entity("影石", EntityType.BRAND, "影石", 1.0, 0, 2);
        Entity crfEntity = new Entity("平板", EntityType.MODEL, "平板", 1.0, 2, 4);
        EntityMerger merger = new EntityMerger(MergeStrategy.DICT_FIRST);

        List<Entity> merged = merger.merge(Collections.singletonList(dictEntity), Collections.singletonList(crfEntity));

        Assert.assertEquals(2, merged.size());
        Assert.assertTrue(merged.stream().anyMatch(e -> "影石".equals(e.getValue())));
        Assert.assertTrue(merged.stream().anyMatch(e -> "平板".equals(e.getValue())));
    }
}
