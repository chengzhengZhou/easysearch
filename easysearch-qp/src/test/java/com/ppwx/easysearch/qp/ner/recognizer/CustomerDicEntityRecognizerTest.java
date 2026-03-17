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
import com.ppwx.easysearch.qp.tokenizer.Token;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * DictEntityRecognizer 单元测试
 */
public class CustomerDicEntityRecognizerTest {

    private DictEntityRecognizer recognizer;

    @Before
    public void setUp() {
        String dictContent = "影石\tBRAND\t影石\n" +
                "摄像机\tCATEGORY\t摄像机\n" +
                "影石摄像机\tMODEL\t影石摄像机\n" +
                "华为\tBRAND\t华为\n" +
                "华为平板\tCATEGORY\t华为平板\n" +
                "华为平板M5\tMODEL\t华为平板M5\n";
        recognizer = DictEntityRecognizer.fromStream(
                new ByteArrayInputStream(dictContent.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void testSingleWordEntity() {
        String text = "影石";
        List<Token> tokens = Collections.singletonList(
                Token.builder().text("影石").type("nr").startIndex(0).endIndex(2).build());

        Collection<Entity> entities = recognizer.extractEntities(text, tokens);

        Assert.assertEquals(1, entities.size());
        Entity entity = entities.iterator().next();
        Assert.assertEquals("影石", entity.getValue());
        Assert.assertEquals(EntityType.BRAND, entity.getType());
        Assert.assertEquals(0, entity.getStartOffset());
        Assert.assertEquals(2, entity.getEndOffset());
    }

    @Test
    public void testMultiWordEntity_LongestMatch() {
        // 影石 + 摄像机 = 影石摄像机 (MODEL)
        String text = "影石摄像机";
        List<Token> tokens = Arrays.asList(
                Token.builder().text("影石").type("nr").startIndex(0).endIndex(2).build(),
                Token.builder().text("摄像机").type("n").startIndex(2).endIndex(5).build());

        Collection<Entity> entities = recognizer.extractEntities(text, tokens);

        Assert.assertEquals(1, entities.size());
        Entity entity = entities.iterator().next();
        Assert.assertEquals("影石摄像机", entity.getValue());
        Assert.assertEquals(EntityType.MODEL, entity.getType());
        Assert.assertEquals(0, entity.getStartOffset());
        Assert.assertEquals(5, entity.getEndOffset());
    }

    @Test
    public void testHuaweiTabletM5() {
        String text = "华为平板M5 10.1英寸青春版";
        List<Token> tokens = Arrays.asList(
                Token.builder().text("华为").type("nr").startIndex(0).endIndex(2).build(),
                Token.builder().text("平板").type("n").startIndex(2).endIndex(4).build(),
                Token.builder().text("M5").type("nx").startIndex(4).endIndex(6).build(),
                Token.builder().text("10.1英寸").type("q").startIndex(7).endIndex(13).build(),
                Token.builder().text("青春版").type("n").startIndex(13).endIndex(16).build());

        Collection<Entity> entities = recognizer.extractEntities(text, tokens);

        Assert.assertEquals(1, entities.size());
        Entity entity = entities.iterator().next();
        Assert.assertEquals("华为平板M5", entity.getValue());
        Assert.assertEquals(EntityType.MODEL, entity.getType());
        Assert.assertEquals(0, entity.getStartOffset());
        Assert.assertEquals(6, entity.getEndOffset());
    }

    @Test
    public void testEmptyInput() {
        Collection<Entity> entities = recognizer.extractEntities("", Collections.emptyList());
        Assert.assertTrue(entities.isEmpty());
    }

    @Test
    public void testNullInput() {
        Collection<Entity> entities = recognizer.extractEntities(null, Collections.emptyList());
        Assert.assertTrue(entities.isEmpty());
    }

    @Test
    public void testNoMatch() {
        String text = "没有匹配的内容";
        List<Token> tokens = Collections.singletonList(
                Token.builder().text("没有匹配的内容").type("n").startIndex(0).endIndex(7).build());

        Collection<Entity> entities = recognizer.extractEntities(text, tokens);
        Assert.assertTrue(entities.isEmpty());
    }

    @Test
    public void testMultipleEntities() {
        String text = "影石和华为";
        List<Token> tokens = Arrays.asList(
                Token.builder().text("影石").type("nr").startIndex(0).endIndex(2).build(),
                Token.builder().text("和").type("c").startIndex(2).endIndex(3).build(),
                Token.builder().text("华为").type("nr").startIndex(3).endIndex(5).build());

        Collection<Entity> entities = recognizer.extractEntities(text, tokens);

        Assert.assertEquals(2, entities.size());
        boolean hasYingshi = entities.stream().anyMatch(e -> "影石".equals(e.getValue()) && e.getType() == EntityType.BRAND);
        boolean hasHuawei = entities.stream().anyMatch(e -> "华为".equals(e.getValue()) && e.getType() == EntityType.BRAND);
        Assert.assertTrue(hasYingshi);
        Assert.assertTrue(hasHuawei);
    }

    @Test
    public void testLoadFromClasspath() throws IOException {
        DictEntityRecognizer fromPath = DictEntityRecognizer.fromPath("ner/custom_entity.dic");
        Assert.assertNotNull(fromPath.getDictionary());

        String text = "影石摄像机";
        List<Token> tokens = Arrays.asList(
                Token.builder().text("影石").type("nr").startIndex(0).endIndex(2).build(),
                Token.builder().text("摄像机").type("n").startIndex(2).endIndex(5).build());

        Collection<Entity> entities = fromPath.extractEntities(text, tokens);
        Assert.assertEquals(1, entities.size());
        Assert.assertEquals("影石摄像机", entities.iterator().next().getValue());
    }

    @Test
    public void testJsonFormat() {
        String jsonDict = "{\"entity\":\"iPhone 15 Pro\",\"type\":\"MODEL\",\"normalizedValue\":\"iPhone 15 Pro\"}\n";
        DictEntityRecognizer jsonRecognizer = DictEntityRecognizer.fromStream(
                new ByteArrayInputStream(jsonDict.getBytes(StandardCharsets.UTF_8)));

        String text = "iPhone 15 Pro";
        List<Token> tokens = Arrays.asList(
                Token.builder().text("iPhone").type("nx").startIndex(0).endIndex(6).build(),
                Token.builder().text("15").type("m").startIndex(7).endIndex(9).build(),
                Token.builder().text("Pro").type("nx").startIndex(10).endIndex(13).build());

        Collection<Entity> entities = jsonRecognizer.extractEntities(text, tokens);
        Assert.assertEquals(1, entities.size());
        Entity entity = entities.iterator().next();
        Assert.assertEquals("iPhone 15 Pro", entity.getValue());
        Assert.assertEquals(EntityType.MODEL, entity.getType());
        Assert.assertEquals("iPhone 15 Pro", entity.getNormalizedValue());
    }
}
