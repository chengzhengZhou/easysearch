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

package com.ppwx.easysearch.qp.support;

import com.ppwx.easysearch.qp.format.WordFormat;
import com.ppwx.easysearch.qp.format.WordFormats;
import com.ppwx.easysearch.qp.intervention.InterventionService;
import com.ppwx.easysearch.qp.ner.Entity;
import com.ppwx.easysearch.qp.ner.EntityNormalizer;
import com.ppwx.easysearch.qp.ner.EntityRecognizer;
import com.ppwx.easysearch.qp.ner.EntityType;
import com.ppwx.easysearch.qp.synonym.SynonymService;
import com.ppwx.easysearch.qp.synonym.SynonymEngine;
import com.ppwx.easysearch.qp.tokenizer.Token;
import com.ppwx.easysearch.qp.tokenizer.Tokenizer;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * QueryProcessor 集成测试和各 Stage 单元测试。
 */
public class QueryProcessorTest {

    // ==================== Stage 单元测试 ====================

    @Test
    public void formatStage_normalizes() {
        WordFormat format = WordFormats.ignoreCase();
        FormatStage stage = new FormatStage(format);

        QueryContext ctx = new QueryContext("Hello WORLD");
        stage.process(ctx);

        assertEquals("hello world", ctx.getNormalizedQuery());
        assertNotNull(ctx.getTrace().get("format.costMs"));
    }

    @Test
    public void formatStage_nullQuery() {
        FormatStage stage = new FormatStage(WordFormats.none());
        QueryContext ctx = new QueryContext(null);
        stage.process(ctx);
        assertNull(ctx.getNormalizedQuery());
    }

    @Test
    public void interventionStage_rewritesAndTracksHit() {
        // 使用 mock：InterventionService 将 "手击壳" 改写为 "手机壳"
        InterventionService service = new InterventionService(null, null) {
            @Override
            public String rewrite(String query) {
                if ("手击壳".equals(query)) {
                    return "手机壳";
                }
                return query;
            }
        };

        InterventionStage stage = new InterventionStage(service);
        QueryContext ctx = new QueryContext("手击壳");
        ctx.setNormalizedQuery("手击壳");
        stage.process(ctx);

        assertEquals("手机壳", ctx.getIntervenedQuery());
        assertEquals(true, ctx.getTrace().get("intervention.hit"));
    }

    @Test
    public void interventionStage_noHit() {
        InterventionService service = new InterventionService(null, null) {
            @Override
            public String rewrite(String query) {
                return query;
            }
        };

        InterventionStage stage = new InterventionStage(service);
        QueryContext ctx = new QueryContext("手机壳");
        ctx.setNormalizedQuery("手机壳");
        stage.process(ctx);

        assertEquals("手机壳", ctx.getIntervenedQuery());
        assertEquals(false, ctx.getTrace().get("intervention.hit"));
    }

    @Test
    public void tokenizerStage_tokenizes() {
        Tokenizer tokenizer = new Tokenizer() {
            @Override
            public List<Token> tokenize(String text) {
                List<Token> tokens = new ArrayList<>();
                tokens.add(Token.builder().text("手机").startIndex(0).endIndex(2).confidence(1.0).build());
                tokens.add(Token.builder().text("壳").startIndex(2).endIndex(3).confidence(1.0).build());
                return tokens;
            }
        };

        TokenizerStage stage = new TokenizerStage(tokenizer);
        QueryContext ctx = new QueryContext("手机壳");
        stage.process(ctx);

        assertNotNull(ctx.getTokens());
        assertEquals(2, ctx.getTokens().size());
        assertEquals("手机", ctx.getTokens().get(0).getText());
        assertEquals(2, ctx.getTrace().get("tokenizer.tokenCount"));
    }

    @Test
    public void synonymStage_rewrite() {
        SynonymService service = new SynonymService(new SynonymEngine()) {
            @Override
            public String rewrite(String query) {
                if ("苹果".equals(query)) {
                    return "iPhone";
                }
                return query;
            }
        };

        SynonymStage stage = new SynonymStage(service);
        QueryContext ctx = new QueryContext("苹果", ProcessOptions.defaults());
        stage.process(ctx);

        assertEquals("iPhone", ctx.getRewrittenQuery());
        assertEquals(true, ctx.getTrace().get("synonym.rewrite.hit"));
    }

    @Test
    public void synonymStage_expand() {
        SynonymService service = new SynonymService(new SynonymEngine()) {
            @Override
            public List<String> expand(String query) {
                return Arrays.asList("苹果", "iPhone", "Apple");
            }
        };

        SynonymStage stage = new SynonymStage(service);
        ProcessOptions options = ProcessOptions.builder()
                .enableSynonymRewrite(false)
                .enableSynonymExpand(true)
                .build();
        QueryContext ctx = new QueryContext("苹果", options);
        stage.process(ctx);

        assertNotNull(ctx.getExpandedQueries());
        assertEquals(3, ctx.getExpandedQueries().size());
        assertEquals(3, ctx.getTrace().get("synonym.expand.count"));
    }

    @Test
    public void synonymStage_skippedWhenBothDisabled() {
        SynonymStage stage = new SynonymStage(new SynonymService(new SynonymEngine()));
        ProcessOptions options = ProcessOptions.builder()
                .enableSynonymRewrite(false)
                .enableSynonymExpand(false)
                .build();
        QueryContext ctx = new QueryContext("test", options);
        stage.process(ctx);

        assertEquals(true, ctx.getTrace().get("synonym.skipped"));
    }

    @Test
    public void nerStage_extractsEntities() {
        EntityRecognizer recognizer = new EntityRecognizer() {
            @Override
            public Collection<Entity> extractEntities(String originText, List<Token> tokens) {
                List<Entity> entities = new ArrayList<>();
                entities.add(new Entity("苹果", EntityType.BRAND));
                return entities;
            }
        };

        NerStage stage = new NerStage(recognizer);
        QueryContext ctx = new QueryContext("苹果手机壳", ProcessOptions.defaults());
        ctx.setTokens(Arrays.asList(
                Token.builder().text("苹果").startIndex(0).endIndex(2).build(),
                Token.builder().text("手机壳").startIndex(2).endIndex(5).build()
        ));
        stage.process(ctx);

        assertNotNull(ctx.getEntities());
        assertEquals(1, ctx.getEntities().size());
        assertEquals("苹果", ctx.getEntities().get(0).getValue());
        assertEquals(1, ctx.getTrace().get("ner.entityCount"));
    }

    @Test
    public void nerStage_skippedWhenDisabled() {
        EntityRecognizer recognizer = new EntityRecognizer() {
            @Override
            public Collection<Entity> extractEntities(String originText, List<Token> tokens) {
                fail("Should not be called when NER is disabled");
                return Collections.emptyList();
            }
        };

        NerStage stage = new NerStage(recognizer);
        ProcessOptions options = ProcessOptions.builder().enableNer(false).build();
        QueryContext ctx = new QueryContext("test", options);
        stage.process(ctx);

        assertEquals(true, ctx.getTrace().get("ner.skipped"));
    }

    @Test
    public void normalizerStage_normalizesEntities() {
        EntityNormalizer normalizer = new EntityNormalizer() {
            @Override
            public String normalize(EntityType entityType, String word) {
                if (EntityType.BRAND.equals(entityType) && "苹果".equals(word)) {
                    return "Apple";
                }
                return word;
            }
        };

        NormalizerStage stage = new NormalizerStage(normalizer);
        QueryContext ctx = new QueryContext("苹果", ProcessOptions.defaults());
        List<Entity> entities = new ArrayList<>();
        entities.add(new Entity("苹果", EntityType.BRAND));
        ctx.setEntities(entities);
        stage.process(ctx);

        assertEquals("Apple", ctx.getEntities().get(0).getNormalizedValue());
        assertEquals(1, ctx.getTrace().get("normalizer.normalizedCount"));
    }

    @Test
    public void normalizerStage_skippedWhenDisabled() {
        NormalizerStage stage = new NormalizerStage(new EntityNormalizer() {
            @Override
            public String normalize(EntityType entityType, String word) {
                fail("Should not be called");
                return word;
            }
        });
        ProcessOptions options = ProcessOptions.builder().enableNormalization(false).build();
        QueryContext ctx = new QueryContext("test", options);
        stage.process(ctx);

        assertEquals(true, ctx.getTrace().get("normalizer.skipped"));
    }

    // ==================== QueryProcessor 集成测试 ====================

    @Test
    public void processor_fullPipeline() {
        Tokenizer tokenizer = new Tokenizer() {
            @Override
            public List<Token> tokenize(String text) {
                List<Token> tokens = new ArrayList<>();
                tokens.add(Token.builder().text(text).startIndex(0).endIndex(text.length()).confidence(1.0).build());
                return tokens;
            }
        };

        EntityRecognizer recognizer = new EntityRecognizer() {
            @Override
            public Collection<Entity> extractEntities(String originText, List<Token> tokens) {
                return Collections.singletonList(new Entity("苹果", EntityType.BRAND));
            }
        };

        EntityNormalizer normalizer = new EntityNormalizer() {
            @Override
            public String normalize(EntityType entityType, String word) {
                return word.toUpperCase();
            }
        };

        QueryProcessor processor = QueryProcessor.builder()
                .withFormat(WordFormats.ignoreCase())
                .withTokenizer(tokenizer)
                .withEntityRecognizer(recognizer)
                .withEntityNormalizer(normalizer)
                .build();

        QueryContext ctx = processor.process("苹果手机壳");

        assertNotNull(ctx);
        assertEquals("苹果手机壳", ctx.getOriginalQuery());
        assertEquals("苹果手机壳", ctx.getNormalizedQuery());
        assertNotNull(ctx.getTokens());
        assertFalse(ctx.getTokens().isEmpty());
        assertNotNull(ctx.getEntities());
        assertEquals(1, ctx.getEntities().size());

        // trace 应包含各 stage 的 costMs
        Map<String, Object> trace = ctx.getTrace();
        assertNotNull(trace.get("format.costMs"));
        assertNotNull(trace.get("tokenizer.costMs"));
        assertNotNull(trace.get("ner.costMs"));
        assertNotNull(trace.get("normalizer.costMs"));
    }

    @Test
    public void processor_minimalPipeline() {
        // 只配置分词
        Tokenizer tokenizer = new Tokenizer() {
            @Override
            public List<Token> tokenize(String text) {
                return Collections.singletonList(
                        Token.builder().text(text).startIndex(0).endIndex(text.length()).build());
            }
        };

        QueryProcessor processor = QueryProcessor.builder()
                .withTokenizer(tokenizer)
                .build();

        QueryContext ctx = processor.process("hello");
        assertNull(ctx.getNormalizedQuery());
        assertNull(ctx.getIntervenedQuery());
        assertNotNull(ctx.getTokens());
        assertEquals(1, ctx.getTokens().size());
    }

    @Test
    public void processor_customStages() {
        final List<String> order = new ArrayList<>();

        Stage preStage = new AbstractStage("pre-custom") {
            @Override
            protected void doProcess(QueryContext ctx) {
                order.add("pre");
            }
        };

        Stage postStage = new AbstractStage("post-custom") {
            @Override
            protected void doProcess(QueryContext ctx) {
                order.add("post");
            }
        };

        Tokenizer tokenizer = new Tokenizer() {
            @Override
            public List<Token> tokenize(String text) {
                order.add("tokenizer");
                return Collections.emptyList();
            }
        };

        QueryProcessor processor = QueryProcessor.builder()
                .addStageFirst(preStage)
                .withTokenizer(tokenizer)
                .addStageLast(postStage)
                .build();

        processor.process("test");

        assertEquals(Arrays.asList("pre", "tokenizer", "post"), order);
    }

    @Test
    public void processor_getStages() {
        QueryProcessor processor = QueryProcessor.builder()
                .withFormat(WordFormats.none())
                .withTokenizer(new Tokenizer() {
                    @Override
                    public List<Token> tokenize(String text) {
                        return Collections.emptyList();
                    }
                })
                .build();

        List<Stage> stages = processor.getStages();
        assertEquals(2, stages.size());
        assertEquals("format", stages.get(0).name());
        assertEquals("tokenizer", stages.get(1).name());
    }

    @Test
    public void processOptions_defaults() {
        ProcessOptions options = ProcessOptions.defaults();
        assertTrue(options.isEnableSynonymRewrite());
        assertFalse(options.isEnableSynonymExpand());
        assertTrue(options.isEnableSpellCorrection());
        assertTrue(options.isEnableNer());
        assertTrue(options.isEnableNormalization());
    }

    @Test
    public void processOptions_builder() {
        ProcessOptions options = ProcessOptions.builder()
                .enableSynonymRewrite(false)
                .enableSynonymExpand(true)
                .enableSpellCorrection(false)
                .enableNer(false)
                .enableNormalization(false)
                .build();

        assertFalse(options.isEnableSynonymRewrite());
        assertTrue(options.isEnableSynonymExpand());
        assertFalse(options.isEnableSpellCorrection());
        assertFalse(options.isEnableNer());
        assertFalse(options.isEnableNormalization());
    }

    @Test
    public void queryContext_getCurrentQuery_priority() {
        QueryContext ctx = new QueryContext("original");
        assertEquals("original", ctx.getCurrentQuery());

        ctx.setNormalizedQuery("normalized");
        assertEquals("normalized", ctx.getCurrentQuery());

        ctx.setIntervenedQuery("intervened");
        assertEquals("intervened", ctx.getCurrentQuery());

        ctx.setCorrectedQuery("corrected");
        assertEquals("corrected", ctx.getCurrentQuery());
    }

    @Test
    public void queryContext_traceOperations() {
        QueryContext ctx = new QueryContext("test");
        ctx.putTrace("key1", "value1");
        ctx.putTrace("key2", 42);

        assertEquals("value1", ctx.<String>getTrace("key1"));
        assertEquals(42, ctx.<Integer>getTrace("key2").intValue());
        assertEquals(2, ctx.getTrace().size());
    }

    @Test
    public void abstractStage_recordsCostMs() {
        AbstractStage stage = new AbstractStage("test-stage") {
            @Override
            protected void doProcess(QueryContext ctx) {
                // 空操作
            }
        };

        QueryContext ctx = new QueryContext("test");
        stage.process(ctx);

        Long costMs = ctx.getTrace("test-stage.costMs");
        assertNotNull(costMs);
        assertTrue(costMs >= 0);
    }
}
