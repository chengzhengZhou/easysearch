package com.ppwx.easysearch.qp.ner.recognizer;

import com.ppwx.easysearch.qp.ner.Entity;
import com.ppwx.easysearch.qp.ner.EntityNormalizer;
import com.ppwx.easysearch.qp.ner.EntityType;
import com.ppwx.easysearch.qp.ner.normalizer.OriginalSliceStrategy;
import com.ppwx.easysearch.qp.ner.normalizer.SpaceJoinStrategy;
import com.ppwx.easysearch.qp.tokenizer.Token;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

/**
 * CRFEntityRecognizer 单元测试。
 * 使用 parseBmeosToEntitiesForTest 直接验证 BMEOS 解析逻辑；extractEntities 仅做边界与集成测试。
 */
public class CRFEntityRecognizerTest {

    private static List<Token> tokens(String text, int start, int end, String pos) {
        return Collections.singletonList(
                Token.builder().text(text).type(pos).startIndex(start).endIndex(end).build());
    }

    private static List<Token> tokens(String[] words, String[] pos, int[] starts, int[] ends) {
        if (words.length != pos.length || words.length != starts.length || words.length != ends.length) {
            throw new IllegalArgumentException();
        }
        List<Token> list = new ArrayList<>();
        for (int i = 0; i < words.length; i++) {
            list.add(Token.builder().text(words[i]).type(pos[i]).startIndex(starts[i]).endIndex(ends[i]).build());
        }
        return list;
    }

    // ---------- BMEOS 解析测试（不依赖 CRF 模型） ----------

    @Test
    public void testParseBmeos_EmptyInput() {
        List<Entity> entities = new CRFEntityRecognizer().parseBmeosToEntitiesForTest(
                "", Collections.emptyList(), new String[0], new String[0]);
        Assert.assertTrue(entities.isEmpty());
    }

    @Test
    public void testParseBmeos_AllO_NoEntity() {
        String text = "这是一段话";
        List<Token> tokens = tokens(
                new String[]{"这", "是", "一段", "话"},
                new String[]{"r", "v", "m", "n"},
                new int[]{0, 1, 2, 4},
                new int[]{1, 2, 4, 5});
        String[] tags = {"O", "O", "O", "O"};
        String[] pos = {"r", "v", "m", "n"};
        List<Entity> entities = new CRFEntityRecognizer().parseBmeosToEntitiesForTest(text, tokens, pos, tags);
        Assert.assertTrue(entities.isEmpty());
    }

    @Test
    public void testParseBmeos_S_DefinedType_ProducesEntity() {
        String text = "华为";
        List<Token> tokens = tokens("华为", 0, 2, "BRAND");
        String[] tags = {"S"};
        String[] pos = {"BRAND"};
        List<Entity> entities = new CRFEntityRecognizer().parseBmeosToEntitiesForTest(text, tokens, pos, tags);
        Assert.assertEquals(1, entities.size());
        Entity e = entities.get(0);
        Assert.assertEquals("华为", e.getValue());
        Assert.assertEquals(EntityType.BRAND, e.getType());
        Assert.assertEquals(0, e.getStartOffset());
        Assert.assertEquals(2, e.getEndOffset());
    }

    @Test
    public void testParseBmeos_S_UndefinedType_NoEntity() {
        String text = "某个词";
        List<Token> tokens = tokens("某个词", 0, 3, "NR");
        String[] tags = {"S"};
        String[] pos = {"NR"};
        List<Entity> entities = new CRFEntityRecognizer().parseBmeosToEntitiesForTest(text, tokens, pos, tags);
        Assert.assertTrue(entities.isEmpty());
    }

    @Test
    public void testParseBmeos_B_E_Span_ProducesEntity() {
        String text = "平板m5";
        List<Token> tokens = tokens(
                new String[]{"平板", "m5"},
                new String[]{"n", "nx"},
                new int[]{0, 2},
                new int[]{2, 4});
        String[] tags = {"B-MODEL", "E-MODEL"};
        String[] pos = {"n", "nx"};
        List<Entity> entities = new CRFEntityRecognizer().parseBmeosToEntitiesForTest(text, tokens, pos, tags);
        Assert.assertEquals(1, entities.size());
        Entity e = entities.get(0);
        Assert.assertEquals("平板m5", e.getValue());
        Assert.assertEquals(EntityType.MODEL, e.getType());
        Assert.assertEquals(0, e.getStartOffset());
        Assert.assertEquals(4, e.getEndOffset());
    }

    @Test
    public void testParseBmeos_B_M_E_Span_ProducesEntity() {
        String text = "华为平板M5";
        List<Token> tokens = tokens(
                new String[]{"华为", "平板", "M5"},
                new String[]{"BRAND", "n", "nx"},
                new int[]{0, 2, 4},
                new int[]{2, 4, 6});
        String[] tags = {"B-MODEL", "M-MODEL", "E-MODEL"};
        String[] pos = {"BRAND", "n", "nx"};
        List<Entity> entities = new CRFEntityRecognizer().parseBmeosToEntitiesForTest(text, tokens, pos, tags);
        Assert.assertEquals(1, entities.size());
        Entity e = entities.get(0);
        Assert.assertEquals("华为平板M5", e.getValue());
        Assert.assertEquals(EntityType.MODEL, e.getType());
        Assert.assertEquals(0, e.getStartOffset());
        Assert.assertEquals(6, e.getEndOffset());
    }

    @Test
    public void testParseBmeos_HuaweiTabletM5_S_And_B_E() {
        // 华为(S, BRAND) + 平板(B-MODEL) + m5(E-MODEL)
        String text = "华为平板m5";
        List<Token> tokens = tokens(
                new String[]{"华为", "平板", "m5"},
                new String[]{"BRAND", "NR", "NN"},
                new int[]{0, 2, 4},
                new int[]{2, 4, 6});
        String[] tags = {"S", "B-MODEL", "E-MODEL"};
        String[] pos = {"BRAND", "NR", "NN"};
        List<Entity> entities = new CRFEntityRecognizer().parseBmeosToEntitiesForTest(text, tokens, pos, tags);
        Assert.assertEquals(2, entities.size());
        Entity brand = entities.stream().filter(x -> x.getType() == EntityType.BRAND).findFirst().orElse(null);
        Entity model = entities.stream().filter(x -> x.getType() == EntityType.MODEL).findFirst().orElse(null);
        Assert.assertNotNull(brand);
        Assert.assertNotNull(model);
        Assert.assertEquals("华为", brand.getValue());
        Assert.assertEquals(0, brand.getStartOffset());
        Assert.assertEquals(2, brand.getEndOffset());
        Assert.assertEquals("平板m5", model.getValue());
        Assert.assertEquals(2, model.getStartOffset());
        Assert.assertEquals(6, model.getEndOffset());
    }

    @Test
    public void testParseBmeos_E_UndefinedType_NoEntity() {
        String text = "某型号";
        List<Token> tokens = tokens(
                new String[]{"某", "型号"},
                new String[]{"r", "n"},
                new int[]{0, 1},
                new int[]{1, 3});
        String[] tags = {"B-XXX", "E-XXX"};
        String[] pos = {"r", "n"};
        List<Entity> entities = new CRFEntityRecognizer().parseBmeosToEntitiesForTest(text, tokens, pos, tags);
        Assert.assertTrue(entities.isEmpty());
    }

    @Test
    public void testParseBmeos_NullTag_TreatedAsO() {
        String text = "词";
        List<Token> tokens = tokens("词", 0, 1, "n");
        String[] tags = {null};
        String[] pos = {"n"};
        List<Entity> entities = new CRFEntityRecognizer().parseBmeosToEntitiesForTest(text, tokens, pos, tags);
        Assert.assertTrue(entities.isEmpty());
    }

    @Test
    public void testParseBmeos_LengthMismatch_ReturnsEmpty() {
        String text = "ab";
        List<Token> tokens = Arrays.asList(
                Token.builder().text("a").type("n").startIndex(0).endIndex(1).build(),
                Token.builder().text("b").type("n").startIndex(1).endIndex(2).build());
        String[] pos = {"n", "n"};
        String[] tags = {"O"};
        List<Entity> entities = new CRFEntityRecognizer().parseBmeosToEntitiesForTest(text, tokens, pos, tags);
        Assert.assertTrue(entities.isEmpty());
    }

    // ---------- extractEntities 边界与集成测试 ----------

    @Test
    public void testExtractEntities_EmptyInput() {
        CRFEntityRecognizer recognizer = new CRFEntityRecognizer();
        Collection<Entity> entities = recognizer.extractEntities("", Collections.emptyList());
        Assert.assertTrue(entities.isEmpty());
    }

    @Test
    public void testExtractEntities_NullInput() {
        CRFEntityRecognizer recognizer = new CRFEntityRecognizer();
        Collection<Entity> entities = recognizer.extractEntities(null, Collections.emptyList());
        Assert.assertTrue(entities.isEmpty());
    }

    @Test
    public void testExtractEntities_NullTokens() {
        CRFEntityRecognizer recognizer = new CRFEntityRecognizer();
        Collection<Entity> entities = recognizer.extractEntities("有内容", null);
        Assert.assertTrue(entities.isEmpty());
    }

    @Test
    public void testExtractEntities_WithRealModel_ValidInput() {
        CRFEntityRecognizer recognizer = new CRFEntityRecognizer();
        String text = "华为平板m5";
        List<Token> tokens = Arrays.asList(
                Token.builder().text("华为").type("BRAND").startIndex(0).endIndex(2).build(),
                Token.builder().text("平板").type("NR").startIndex(2).endIndex(4).build(),
                Token.builder().text("m5").type("NN").startIndex(4).endIndex(6).build());
        Collection<Entity> entities = recognizer.extractEntities(text, tokens);
        // 模型存在时可能返回 0 或若干实体；仅断言结构合法且不抛异常
        Assert.assertNotNull(entities);
        for (Entity e : entities) {
            Assert.assertNotNull(e.getValue());
            Assert.assertNotNull(e.getType());
            Assert.assertTrue(e.getStartOffset() >= 0 && e.getEndOffset() <= text.length());
            Assert.assertTrue(e.getStartOffset() < e.getEndOffset());
        }
    }

    // ---------- 归一化策略测试 ----------

    @Test
    public void testNormalize_Default_SpaceJoin() {
        String text = "平板m5";
        List<Token> tokens = tokens(
                new String[]{"平板", "m5"},
                new String[]{"n", "nx"},
                new int[]{0, 2},
                new int[]{2, 4});
        String[] tags = {"B-MODEL", "E-MODEL"};
        String[] pos = {"n", "nx"};
        CRFEntityRecognizer recognizer = new CRFEntityRecognizer();
        List<Entity> entities = recognizer.parseBmeosToEntitiesForTest(text, tokens, pos, tags);
        Assert.assertEquals(1, entities.size());
        Entity e = entities.get(0);
        Assert.assertEquals("平板m5", e.getValue());
        Assert.assertEquals("平板 m5", e.getNormalizedValue());
    }

    @Test
    public void testNormalize_OriginalSliceStrategy_PreservesValue() {
        String text = "平板m5";
        List<Token> tokens = tokens(
                new String[]{"平板", "m5"},
                new String[]{"n", "nx"},
                new int[]{0, 2},
                new int[]{2, 4});
        String[] tags = {"B-MODEL", "E-MODEL"};
        String[] pos = {"n", "nx"};
        CRFEntityRecognizer recognizer = new CRFEntityRecognizer();
        recognizer.setDefaultTokenSpanStrategy(new OriginalSliceStrategy());
        List<Entity> entities = recognizer.parseBmeosToEntitiesForTest(text, tokens, pos, tags);
        Assert.assertEquals(1, entities.size());
        Entity e = entities.get(0);
        Assert.assertEquals("平板m5", e.getValue());
        Assert.assertEquals("平板m5", e.getNormalizedValue());
    }

    @Test
    public void testNormalize_WithEntityNormalizer() {
        String text = "华为";
        List<Token> tokens = tokens("华为", 0, 2, "BRAND");
        String[] tags = {"S"};
        String[] pos = {"BRAND"};
        CRFEntityRecognizer recognizer = new CRFEntityRecognizer();
        recognizer.setEntityNormalizer((entityType, word) -> word != null ? word + "_norm" : null);
        List<Entity> entities = recognizer.parseBmeosToEntitiesForTest(text, tokens, pos, tags);
        Assert.assertEquals(1, entities.size());
        Assert.assertEquals("华为_norm", entities.get(0).getNormalizedValue());
    }
}
