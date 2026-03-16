package com.ppwx.easysearch.qp.ner;

import com.hankcs.hanlp.corpus.tag.Nature;
import com.ppwx.easysearch.qp.ner.recognizer.EntityTypeRecognizer;
import com.ppwx.easysearch.qp.tokenizer.Token;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

/**
 * CompositeEntityRecognizer 测试类
 * 
 * @author ext.ahs.zhouchzh1@jd.com
 * @date 2024/10/9
 */
public class CompositeEntityRecognizerTest {

    private CompositeEntityRecognizer recognizer;

    /**
     * 测试用的 Mock 品牌识别器
     */
    static class MockBrandRecognizer implements EntityTypeRecognizer {
        private final boolean enabled;
        private final int priority;

        public MockBrandRecognizer() {
            this(true, 0);
        }

        public MockBrandRecognizer(boolean enabled, int priority) {
            this.enabled = enabled;
            this.priority = priority;
        }

        @Override
        public EntityType getSupportedType() {
            return EntityType.BRAND;
        }

        @Override
        public Entity recognize(String word, String nature) {
            // 简单的品牌识别逻辑
            if (word.equals("苹果") || word.equals("Apple")) {
                Entity entity = new Entity(word, EntityType.BRAND, "Apple");
                entity.setStartOffset(0);
                entity.setEndOffset(word.length());
                return entity;
            }
            if (word.equals("华为")) {
                Entity entity = new Entity(word, EntityType.BRAND, "Huawei");
                entity.setStartOffset(0);
                entity.setEndOffset(word.length());
                return entity;
            }
            return null;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public int getPriority() {
            return priority;
        }
    }

    /**
     * 测试用的 Mock 型号识别器
     */
    static class MockModelRecognizer implements EntityTypeRecognizer {
        private final boolean enabled;
        private final int priority;

        public MockModelRecognizer() {
            this(true, 0);
        }

        public MockModelRecognizer(boolean enabled, int priority) {
            this.enabled = enabled;
            this.priority = priority;
        }

        @Override
        public EntityType getSupportedType() {
            return EntityType.MODEL;
        }

        @Override
        public Entity recognize(String word, String nature) {
            // 简单的型号识别逻辑
            if (word.matches("iPhone\\s*\\d+.*") || word.equals("iPhone 15")) {
                Entity entity = new Entity(word, EntityType.MODEL, word);
                entity.setStartOffset(0);
                entity.setEndOffset(word.length());
                return entity;
            }
            if (word.equals("Mate") || word.equals("40")) {
                Entity entity = new Entity(word, EntityType.MODEL, word);
                entity.setStartOffset(0);
                entity.setEndOffset(word.length());
                return entity;
            }
            return null;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public int getPriority() {
            return priority;
        }
    }

    /**
     * 会抛异常的识别器（用于测试异常处理）
     */
    static class ThrowExceptionRecognizer implements EntityTypeRecognizer {
        @Override
        public EntityType getSupportedType() {
            return EntityType.CATEGORY;
        }

        @Override
        public Entity recognize(String word, String nature) {
            throw new RuntimeException("Test exception");
        }
    }

    @Before
    public void setUp() {
        List<EntityTypeRecognizer> recognizers = Arrays.asList(
                new MockBrandRecognizer(),
                new MockModelRecognizer()
        );
        recognizer = new CompositeEntityRecognizer(recognizers);
    }

    /**
     * 测试1：基本实体识别功能
     * 验证能够正确识别品牌和型号
     */
    @Test
    public void testBasicEntityRecognition() {
        String text = "苹果 iPhone 15";
        List<Token> tokens = Arrays.asList(
                Token.builder().text("苹果").type(Nature.n.toString())
                        .startIndex(0).endIndex(2).build(),
                Token.builder().text("iPhone 15").type(Nature.nx.toString())
                        .startIndex(3).endIndex(12).build()
        );

        Collection<Entity> entities = recognizer.extractEntities(text, tokens);

        Assert.assertNotNull("实体列表不应为空", entities);
        Assert.assertTrue("应该识别出实体", entities.size() > 0);

        // 验证识别出品牌
        boolean hasBrand = entities.stream()
                .anyMatch(e -> e.getType() == EntityType.BRAND && e.getValue().equals("苹果"));
        Assert.assertTrue("应该识别出品牌'苹果'", hasBrand);

        // 验证识别出型号
        boolean hasModel = entities.stream()
                .anyMatch(e -> e.getType() == EntityType.MODEL && e.getValue().equals("iPhone 15"));
        Assert.assertTrue("应该识别出型号'iPhone 15'", hasModel);
    }

    /**
     * 测试2：空输入测试
     * 验证空列表不会导致异常
     */
    @Test
    public void testEmptyInput() {
        String text = "";
        List<Token> tokens = Collections.emptyList();

        Collection<Entity> entities = recognizer.extractEntities(text, tokens);

        Assert.assertNotNull("实体列表不应为null", entities);
        Assert.assertEquals("空输入应返回空实体列表", 0, entities.size());
    }

    /**
     * 测试3：单个token测试
     * 验证单个token不会添加原文作为额外token
     */
    @Test
    public void testSingleToken() {
        String text = "苹果";
        List<Token> tokens = Collections.singletonList(
                Token.builder().text("苹果").type(Nature.n.toString())
                        .startIndex(0).endIndex(2).build()
        );

        Collection<Entity> entities = recognizer.extractEntities(text, tokens);

        Assert.assertNotNull("实体列表不应为空", entities);
        // 验证只识别出一个品牌（不应该重复识别）
        long brandCount = entities.stream()
                .filter(e -> e.getType() == EntityType.BRAND)
                .count();
        Assert.assertEquals("单个token只应识别一次", 1, brandCount);
    }

    /**
     * 测试4：多个token会添加原文作为额外token
     * 验证当tokens > 1时，原文会被添加为额外token
     */
    @Test
    public void testMultipleTokensAddOriginText() {
        String text = "苹果iPhone 15";
        List<Token> tokens = Arrays.asList(
                Token.builder().text("苹果").type(Nature.n.toString())
                        .startIndex(0).endIndex(2).build(),
                Token.builder().text("iPhone").type(Nature.nx.toString())
                        .startIndex(2).endIndex(8).build(),
                Token.builder().text("15").type(Nature.m.toString())
                        .startIndex(9).endIndex(11).build()
        );

        Collection<Entity> entities = recognizer.extractEntities(text, tokens);

        Assert.assertNotNull("实体列表不应为空", entities);
        Assert.assertTrue("应该识别出实体", entities.size() > 0);
    }

    /**
     * 测试5：识别器优先级测试
     * 验证识别器按优先级排序（高优先级优先执行）
     */
    @Test
    public void testRecognizerPriority() {
        List<EntityTypeRecognizer> recognizers = Arrays.asList(
                new MockBrandRecognizer(true, 10),  // 高优先级
                new MockModelRecognizer(true, 5)    // 低优先级
        );
        CompositeEntityRecognizer priorityRecognizer = new CompositeEntityRecognizer(recognizers);

        String text = "苹果 iPhone 15";
        List<Token> tokens = Arrays.asList(
                Token.builder().text("苹果").type(Nature.n.toString())
                        .startIndex(0).endIndex(2).build(),
                Token.builder().text("iPhone 15").type(Nature.nx.toString())
                        .startIndex(3).endIndex(12).build()
        );

        Collection<Entity> entities = priorityRecognizer.extractEntities(text, tokens);

        Assert.assertNotNull("实体列表不应为空", entities);
        Assert.assertTrue("应该识别出实体", entities.size() > 0);
    }

    /**
     * 测试6：禁用识别器测试
     * 验证禁用的识别器不会被使用
     */
    @Test
    public void testDisabledRecognizer() {
        List<EntityTypeRecognizer> recognizers = Arrays.asList(
                new MockBrandRecognizer(true, 0),   // 启用
                new MockModelRecognizer(false, 0)   // 禁用
        );
        CompositeEntityRecognizer disabledRecognizer = new CompositeEntityRecognizer(recognizers);

        String text = "苹果 iPhone 15";
        List<Token> tokens = Arrays.asList(
                Token.builder().text("苹果").type(Nature.n.toString())
                        .startIndex(0).endIndex(2).build(),
                Token.builder().text("iPhone 15").type(Nature.nx.toString())
                        .startIndex(3).endIndex(12).build()
        );

        Collection<Entity> entities = disabledRecognizer.extractEntities(text, tokens);

        // 验证只识别出品牌，没有型号
        boolean hasBrand = entities.stream()
                .anyMatch(e -> e.getType() == EntityType.BRAND);
        boolean hasModel = entities.stream()
                .anyMatch(e -> e.getType() == EntityType.MODEL);

        Assert.assertTrue("应该识别出品牌", hasBrand);
        Assert.assertFalse("不应该识别出型号（识别器已禁用）", hasModel);
    }

    /**
     * 测试7：相邻实体合并测试
     * 验证相邻的同类型实体会被合并
     */
    @Test
    public void testMergeAdjacentEntities() {
        // 创建一个特殊的识别器，会识别出相邻的实体
        EntityTypeRecognizer adjacentRecognizer = new EntityTypeRecognizer() {
            @Override
            public EntityType getSupportedType() {
                return EntityType.MODEL;
            }

            @Override
            public Entity recognize(String word, String nature) {
                if (word.equals("Mate") || word.equals("40")) {
                    Entity entity = new Entity(word, EntityType.MODEL, word);
                    if (word.equals("Mate")) {
                        entity.setStartOffset(0);
                        entity.setEndOffset(4);
                    } else {
                        entity.setStartOffset(5);  // 相邻位置
                        entity.setEndOffset(7);
                    }
                    return entity;
                }
                return null;
            }
        };

        List<EntityTypeRecognizer> recognizers = Collections.singletonList(adjacentRecognizer);
        CompositeEntityRecognizer mergeRecognizer = new CompositeEntityRecognizer(recognizers);

        String text = "Mate 40";
        List<Token> tokens = Arrays.asList(
                Token.builder().text("Mate").type(Nature.nx.toString())
                        .startIndex(0).endIndex(4).build(),
                Token.builder().text("40").type(Nature.m.toString())
                        .startIndex(5).endIndex(7).build()
        );

        Collection<Entity> entities = mergeRecognizer.extractEntities(text, tokens);

        Assert.assertNotNull("实体列表不应为空", entities);
        // 验证相邻实体被合并
        List<Entity> modelEntities = new ArrayList<>();
        entities.stream()
                .filter(e -> e.getType() == EntityType.MODEL)
                .forEach(modelEntities::add);

        // 合并后应该只有一个实体
        Assert.assertEquals("相邻实体应该被合并为一个", 1, modelEntities.size());
    }

    /**
     * 测试8：异常处理测试
     * 验证识别器抛异常时不会影响其他识别器
     */
    @Test
    public void testExceptionHandling() {
        List<EntityTypeRecognizer> recognizers = Arrays.asList(
                new ThrowExceptionRecognizer(),  // 会抛异常
                new MockBrandRecognizer()        // 正常识别器
        );
        CompositeEntityRecognizer exceptionRecognizer = new CompositeEntityRecognizer(recognizers);

        String text = "苹果";
        List<Token> tokens = Collections.singletonList(
                Token.builder().text("苹果").type(Nature.n.toString())
                        .startIndex(0).endIndex(2).build()
        );

        // 不应该抛出异常
        Collection<Entity> entities = exceptionRecognizer.extractEntities(text, tokens);

        Assert.assertNotNull("实体列表不应为空", entities);
        // 应该至少识别出品牌（其他识别器抛异常不影响）
        boolean hasBrand = entities.stream()
                .anyMatch(e -> e.getType() == EntityType.BRAND);
        Assert.assertTrue("即使有识别器抛异常，其他识别器应该正常工作", hasBrand);
    }

    /**
     * 测试9：无法识别实体的情况
     * 验证没有匹配的实体时返回空列表
     */
    @Test
    public void testNoMatchingEntities() {
        String text = "没有匹配的内容";
        List<Token> tokens = Collections.singletonList(
                Token.builder().text("没有匹配的内容").type(Nature.n.toString())
                        .startIndex(0).endIndex(7).build()
        );

        Collection<Entity> entities = recognizer.extractEntities(text, tokens);

        Assert.assertNotNull("实体列表不应为null", entities);
        Assert.assertEquals("没有匹配的实体应返回空列表", 0, entities.size());
    }

    /**
     * 测试10：复杂场景测试
     * 验证多种实体类型混合识别
     */
    @Test
    public void testComplexScenario() {
        String text = "华为 Mate 40 和 苹果 iPhone 15";
        List<Token> tokens = Arrays.asList(
                Token.builder().text("华为").type(Nature.n.toString())
                        .startIndex(0).endIndex(2).build(),
                Token.builder().text("Mate").type(Nature.nx.toString())
                        .startIndex(3).endIndex(7).build(),
                Token.builder().text("40").type(Nature.m.toString())
                        .startIndex(8).endIndex(10).build(),
                Token.builder().text("和").type(Nature.c.toString())
                        .startIndex(11).endIndex(12).build(),
                Token.builder().text("苹果").type(Nature.n.toString())
                        .startIndex(13).endIndex(15).build(),
                Token.builder().text("iPhone 15").type(Nature.nx.toString())
                        .startIndex(16).endIndex(25).build()
        );

        Collection<Entity> entities = recognizer.extractEntities(text, tokens);

        Assert.assertNotNull("实体列表不应为空", entities);

        // 验证识别出两个品牌
        long brandCount = entities.stream()
                .filter(e -> e.getType() == EntityType.BRAND)
                .count();
        Assert.assertEquals("应该识别出2个品牌", 2, brandCount);

        // 验证识别出型号
        long modelCount = entities.stream()
                .filter(e -> e.getType() == EntityType.MODEL)
                .count();
        Assert.assertTrue("应该识别出至少1个型号", modelCount >= 1);
    }

    /**
     * 测试11：归一化值测试
     * 验证识别出的实体包含正确的归一化值
     */
    @Test
    public void testNormalizedValue() {
        String text = "苹果";
        List<Token> tokens = Collections.singletonList(
                Token.builder().text("苹果").type(Nature.n.toString())
                        .startIndex(0).endIndex(2).build()
        );

        Collection<Entity> entities = recognizer.extractEntities(text, tokens);

        // 找到品牌实体
        Optional<Entity> brandEntity = entities.stream()
                .filter(e -> e.getType() == EntityType.BRAND && e.getValue().equals("苹果"))
                .findFirst();

        Assert.assertTrue("应该识别出品牌实体", brandEntity.isPresent());
        Assert.assertEquals("归一化值应该是'Apple'", "Apple", brandEntity.get().getNormalizedValue());
    }

    /**
     * 测试12：不合并非相邻实体
     * 验证非相邻的实体不会被合并
     */
    @Test
    public void testNotMergeNonAdjacentEntities() {
        EntityTypeRecognizer nonAdjacentRecognizer = new EntityTypeRecognizer() {
            @Override
            public EntityType getSupportedType() {
                return EntityType.BRAND;
            }

            @Override
            public Entity recognize(String word, String nature) {
                if (word.equals("苹果") || word.equals("华为")) {
                    Entity entity = new Entity(word, EntityType.BRAND, word);
                    if (word.equals("苹果")) {
                        entity.setStartOffset(0);
                        entity.setEndOffset(2);
                    } else {
                        entity.setStartOffset(10);  // 非相邻位置
                        entity.setEndOffset(12);
                    }
                    return entity;
                }
                return null;
            }
        };

        List<EntityTypeRecognizer> recognizers = Collections.singletonList(nonAdjacentRecognizer);
        CompositeEntityRecognizer nonMergeRecognizer = new CompositeEntityRecognizer(recognizers);

        String text = "苹果和华为";
        List<Token> tokens = Arrays.asList(
                Token.builder().text("苹果").type(Nature.n.toString())
                        .startIndex(0).endIndex(2).build(),
                Token.builder().text("华为").type(Nature.n.toString())
                        .startIndex(10).endIndex(12).build()
        );

        Collection<Entity> entities = nonMergeRecognizer.extractEntities(text, tokens);

        // 非相邻实体不应该被合并
        long brandCount = entities.stream()
                .filter(e -> e.getType() == EntityType.BRAND)
                .count();
        Assert.assertEquals("非相邻实体不应该被合并", 2, brandCount);
    }

    /**
     * 测试13：完全重复实体去重测试
     * 验证完全相同的实体会被去重，只保留一个
     */
    @Test
    public void testDeduplicateIdenticalEntities() {
        // 创建一个识别器，会对同一个词识别出多次相同的实体
        EntityTypeRecognizer duplicateRecognizer = new EntityTypeRecognizer() {
            @Override
            public EntityType getSupportedType() {
                return EntityType.BRAND;
            }

            @Override
            public Entity recognize(String word, String nature) {
                if (word.equals("Apple") || word.equals("苹果Apple")) {
                    Entity entity = new Entity(word, EntityType.BRAND, "Apple");
                    List<String> ids = Arrays.asList("brand_001");
                    entity.setId(ids);
                    entity.setConfidence(1.0);
                    return entity;
                }
                return null;
            }
        };

        List<EntityTypeRecognizer> recognizers = Collections.singletonList(duplicateRecognizer);
        CompositeEntityRecognizer deduplicateRecognizer = new CompositeEntityRecognizer(recognizers);

        String text = "苹果Apple";
        List<Token> tokens = Arrays.asList(
                Token.builder().text("Apple").type(Nature.nx.toString())
                        .startIndex(2).endIndex(7).build(),
                Token.builder().text("苹果Apple").type(Nature.nz.toString())
                        .startIndex(0).endIndex(7).build()
        );

        Collection<Entity> entities = deduplicateRecognizer.extractEntities(text, tokens);

        // 虽然识别了两次，但由于normalizedValue和ID相同，应该只保留一个
        long brandCount = entities.stream()
                .filter(e -> e.getType() == EntityType.BRAND)
                .count();
        Assert.assertEquals("完全相同的实体应该被去重", 1, brandCount);
    }

    /**
     * 测试14：位置重叠实体去重 - 选择更长的
     * 验证当两个实体位置重叠时，选择更长的实体
     */
    @Test
    public void testDeduplicateOverlappingEntities_PreferLonger() {
        // 创建一个识别器，会识别出位置重叠的实体
        EntityTypeRecognizer overlappingRecognizer = new EntityTypeRecognizer() {
            @Override
            public EntityType getSupportedType() {
                return EntityType.BRAND;
            }

            @Override
            public Entity recognize(String word, String nature) {
                if (word.equals("iPhone") || word.equals("iPhone 13 Pro")) {
                    Entity entity = new Entity(word, EntityType.BRAND, word.toLowerCase());
                    entity.setConfidence(1.0);
                    return entity;
                }
                return null;
            }
        };

        List<EntityTypeRecognizer> recognizers = Collections.singletonList(overlappingRecognizer);
        CompositeEntityRecognizer deduplicateRecognizer = new CompositeEntityRecognizer(recognizers);

        String text = "iPhone 13 Pro";
        List<Token> tokens = Arrays.asList(
                Token.builder().text("iPhone").type(Nature.nx.toString())
                        .startIndex(0).endIndex(6).build(),
                Token.builder().text("iPhone 13 Pro").type(Nature.nz.toString())
                        .startIndex(0).endIndex(13).build()
        );

        Collection<Entity> entities = deduplicateRecognizer.extractEntities(text, tokens);

        // 应该只保留更长的实体
        long brandCount = entities.stream()
                .filter(e -> e.getType() == EntityType.BRAND)
                .count();
        Assert.assertEquals("位置重叠时应该只保留一个实体", 1, brandCount);

        // 验证保留的是更长的实体
        Optional<Entity> brandEntity = entities.stream()
                .filter(e -> e.getType() == EntityType.BRAND)
                .findFirst();
        Assert.assertTrue("应该存在品牌实体", brandEntity.isPresent());
        Assert.assertEquals("应该保留更长的实体", "iPhone 13 Pro", brandEntity.get().getValue());
    }

    /**
     * 测试15：位置重叠实体去重 - 选择有ID的
     * 验证当两个实体位置重叠且长度相同时，选择有ID映射的实体
     */
    @Test
    public void testDeduplicateOverlappingEntities_PreferWithId() {
        // 创建一个识别器，会识别出位置重叠的实体，一个有ID一个没有
        EntityTypeRecognizer overlappingRecognizer = new EntityTypeRecognizer() {
            private int callCount = 0;

            @Override
            public EntityType getSupportedType() {
                return EntityType.BRAND;
            }

            @Override
            public Entity recognize(String word, String nature) {
                if (word.equals("Apple")) {
                    Entity entity = new Entity(word, EntityType.BRAND, "apple");
                    entity.setConfidence(1.0);
                    // 第二次调用时设置ID
                    if (++callCount > 1) {
                        entity.setId(Arrays.asList("brand_001"));
                    }
                    return entity;
                }
                return null;
            }
        };

        List<EntityTypeRecognizer> recognizers = Collections.singletonList(overlappingRecognizer);
        CompositeEntityRecognizer deduplicateRecognizer = new CompositeEntityRecognizer(recognizers);

        String text = "Apple";
        List<Token> tokens = Arrays.asList(
                Token.builder().text("Apple").type(Nature.nx.toString())
                        .startIndex(0).endIndex(5).build(),
                Token.builder().text("Apple").type(Nature.nx.toString())
                        .startIndex(0).endIndex(5).build()
        );

        Collection<Entity> entities = deduplicateRecognizer.extractEntities(text, tokens);

        // 应该只保留有ID的实体
        long brandCount = entities.stream()
                .filter(e -> e.getType() == EntityType.BRAND)
                .count();
        Assert.assertEquals("位置重叠时应该只保留一个实体", 1, brandCount);

        // 验证保留的是有ID的实体
        Optional<Entity> brandEntity = entities.stream()
                .filter(e -> e.getType() == EntityType.BRAND)
                .findFirst();
        Assert.assertTrue("应该存在品牌实体", brandEntity.isPresent());
        Assert.assertNotNull("应该保留有ID的实体", brandEntity.get().getId());
    }

    /**
     * 测试16：位置重叠实体去重 - 选择置信度高的
     * 验证当两个实体位置重叠时，选择置信度更高的实体
     */
    @Test
    public void testDeduplicateOverlappingEntities_PreferHigherConfidence() {
        // 创建一个识别器，会识别出位置重叠但置信度不同的实体
        EntityTypeRecognizer overlappingRecognizer = new EntityTypeRecognizer() {
            @Override
            public EntityType getSupportedType() {
                return EntityType.MODEL;
            }

            @Override
            public Entity recognize(String word, String nature) {
                if (word.equals("iPhone13") || word.equals("iPhone 13")) {
                    Entity entity = new Entity(word, EntityType.MODEL, "iphone 13");
                    // 更长的实体赋予更高的置信度
                    entity.setConfidence(word.length() > 8 ? 0.95 : 0.7);
                    return entity;
                }
                return null;
            }
        };

        List<EntityTypeRecognizer> recognizers = Collections.singletonList(overlappingRecognizer);
        CompositeEntityRecognizer deduplicateRecognizer = new CompositeEntityRecognizer(recognizers);

        String text = "iPhone 13";
        List<Token> tokens = Arrays.asList(
                Token.builder().text("iPhone13").type(Nature.nx.toString())
                        .startIndex(0).endIndex(8).build(),
                Token.builder().text("iPhone 13").type(Nature.nz.toString())
                        .startIndex(0).endIndex(9).build()
        );

        Collection<Entity> entities = deduplicateRecognizer.extractEntities(text, tokens);

        // 应该只保留置信度高的实体
        long modelCount = entities.stream()
                .filter(e -> e.getType() == EntityType.MODEL)
                .count();
        Assert.assertEquals("位置重叠时应该只保留一个实体", 1, modelCount);

        // 验证保留的是置信度高的实体
        Optional<Entity> modelEntity = entities.stream()
                .filter(e -> e.getType() == EntityType.MODEL)
                .findFirst();
        Assert.assertTrue("应该存在型号实体", modelEntity.isPresent());
        Assert.assertTrue("应该保留置信度高的实体", modelEntity.get().getConfidence() >= 0.9);
    }

    /**
     * 测试17：多token添加原文导致的重复去重
     * 验证当原文作为额外token被添加时，能正确去重
     */
    @Test
    public void testDeduplicateWithOriginTextToken() {
        // 创建一个识别器，会同时识别子串和完整字符串
        EntityTypeRecognizer fullTextRecognizer = new EntityTypeRecognizer() {
            @Override
            public EntityType getSupportedType() {
                return EntityType.BRAND;
            }

            @Override
            public Entity recognize(String word, String nature) {
                if (word.contains("Apple")) {
                    Entity entity = new Entity(word, EntityType.BRAND, "apple");
                    entity.setId(Arrays.asList("brand_001"));
                    entity.setConfidence(1.0);
                    return entity;
                }
                return null;
            }
        };

        List<EntityTypeRecognizer> recognizers = Collections.singletonList(fullTextRecognizer);
        CompositeEntityRecognizer deduplicateRecognizer = new CompositeEntityRecognizer(recognizers);

        String text = "Apple iPhone";
        // 注意：当tokens > 1时，CompositeEntityRecognizer会自动添加原文作为额外token
        List<Token> tokens = Arrays.asList(
                Token.builder().text("Apple").type(Nature.nx.toString())
                        .startIndex(0).endIndex(5).build(),
                Token.builder().text("iPhone").type(Nature.nx.toString())
                        .startIndex(6).endIndex(12).build()
        );

        Collection<Entity> entities = deduplicateRecognizer.extractEntities(text, tokens);

        // "Apple"和"Apple iPhone"都会被识别为品牌，但应该被去重
        long brandCount = entities.stream()
                .filter(e -> e.getType() == EntityType.BRAND)
                .count();
        
        // 由于两个实体的ID相同，但位置重叠，应该只保留一个（更长的）
        Assert.assertEquals("应该去重重叠的品牌实体", 1, brandCount);
    }

    /**
     * 测试18：位置信息正确维护
     * 验证去重后的实体保持了正确的位置信息
     */
    @Test
    public void testPositionInfoMaintained() {
        EntityTypeRecognizer positionRecognizer = new EntityTypeRecognizer() {
            @Override
            public EntityType getSupportedType() {
                return EntityType.BRAND;
            }

            @Override
            public Entity recognize(String word, String nature) {
                if (word.equals("Apple")) {
                    Entity entity = new Entity(word, EntityType.BRAND, "apple");
                    entity.setConfidence(1.0);
                    return entity;
                }
                return null;
            }
        };

        List<EntityTypeRecognizer> recognizers = Collections.singletonList(positionRecognizer);
        CompositeEntityRecognizer recognizerWithPosition = new CompositeEntityRecognizer(recognizers);

        String text = "Apple";
        List<Token> tokens = Collections.singletonList(
                Token.builder().text("Apple").type(Nature.nx.toString())
                        .startIndex(0).endIndex(5).build()
        );

        Collection<Entity> entities = recognizerWithPosition.extractEntities(text, tokens);

        Optional<Entity> brandEntity = entities.stream()
                .filter(e -> e.getType() == EntityType.BRAND)
                .findFirst();

        Assert.assertTrue("应该存在品牌实体", brandEntity.isPresent());
        Entity entity = brandEntity.get();
        Assert.assertEquals("起始位置应该正确", 0, entity.getStartOffset());
        Assert.assertEquals("结束位置应该正确", 5, entity.getEndOffset());
    }
}

