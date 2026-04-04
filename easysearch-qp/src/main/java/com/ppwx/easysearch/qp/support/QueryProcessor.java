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

import com.ppwx.easysearch.qp.correction.SpellCorrectionEngine;
import com.ppwx.easysearch.qp.format.WordFormat;
import com.ppwx.easysearch.qp.intervention.InterventionService;
import com.ppwx.easysearch.qp.ner.EntityNormalizer;
import com.ppwx.easysearch.qp.ner.EntityRecognizer;
import com.ppwx.easysearch.qp.synonym.SynonymService;
import com.ppwx.easysearch.qp.tokenizer.Tokenizer;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询处理器门面：Builder 模式构建，内部使用 {@link Pipeline Pipeline&lt;Stage&gt;} 编排所有阶段。
 * <p>
 * 默认处理链路：Format → Intervention → SpellCorrection → Tokenizer → Synonym → NER → Normalizer
 * <p>
 * 使用示例：
 * <pre>
 * QueryProcessor processor = QueryProcessor.builder()
 *     .withFormat(WordFormats.chains(WordFormats.ignoreCase(), WordFormats.truncate()))
 *     .withInterventionService(interventionService)
 *     .withTokenizer(tokenizer)
 *     .withSynonymService(synonymService)
 *     .withEntityRecognizer(entityRecognizer)
 *     .withEntityNormalizer(entityNormalizer)
 *     .build();
 *
 * QueryContext ctx = processor.process("苹果手机壳");
 * </pre>
 */
public class QueryProcessor {

    private final Pipeline<Stage> pipeline;

    private QueryProcessor(Pipeline<Stage> pipeline) {
        this.pipeline = pipeline;
    }

    /**
     * 使用默认选项处理 query。
     *
     * @param query 原始查询
     * @return 处理后的查询上下文
     */
    public QueryContext process(String query) {
        return process(query, ProcessOptions.defaults());
    }

    /**
     * 使用指定选项处理 query。
     *
     * @param query   原始查询
     * @param options 处理选项
     * @return 处理后的查询上下文
     */
    public QueryContext process(String query, ProcessOptions options) {
        QueryContext ctx = new QueryContext(query, options != null ? options : ProcessOptions.defaults());

        List<Stage> stages = pipeline.list();
        for (Stage stage : stages) {
            stage.process(ctx);
        }

        return ctx;
    }

    /**
     * 获取当前 pipeline 中的所有 Stage（只读）。
     */
    public List<Stage> getStages() {
        return pipeline.list();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * QueryProcessor 构建器。
     * <p>
     * 通过 withXxx 方法注入各模块组件，build 时自动按默认顺序创建对应 Stage 并注入 Pipeline。
     * 也支持通过 {@link #addStageFirst(Stage)} / {@link #addStageLast(Stage)} 自定义扩展。
     */
    public static class Builder {

        private WordFormat wordFormat;
        private InterventionService interventionService;
        private SpellCorrectionEngine spellCorrectionEngine;
        private Tokenizer tokenizer;
        private SynonymService synonymService;
        private EntityRecognizer entityRecognizer;
        private EntityNormalizer entityNormalizer;

        private final List<Stage> extraFirstStages = new ArrayList<>();
        private final List<Stage> extraLastStages = new ArrayList<>();

        /**
         * 设置输入预处理格式化器。
         */
        public Builder withFormat(WordFormat wordFormat) {
            this.wordFormat = wordFormat;
            return this;
        }

        /**
         * 设置干预服务。
         */
        public Builder withInterventionService(InterventionService interventionService) {
            this.interventionService = interventionService;
            return this;
        }

        /**
         * 设置拼写纠错引擎（可选）。
         */
        public Builder withSpellCorrection(SpellCorrectionEngine engine) {
            this.spellCorrectionEngine = engine;
            return this;
        }

        /**
         * 设置分词器。
         */
        public Builder withTokenizer(Tokenizer tokenizer) {
            this.tokenizer = tokenizer;
            return this;
        }

        /**
         * 设置同义词服务（可选）。
         */
        public Builder withSynonymService(SynonymService synonymService) {
            this.synonymService = synonymService;
            return this;
        }

        /**
         * 设置实体识别器。
         */
        public Builder withEntityRecognizer(EntityRecognizer entityRecognizer) {
            this.entityRecognizer = entityRecognizer;
            return this;
        }

        /**
         * 设置实体归一化器（可选）。
         */
        public Builder withEntityNormalizer(EntityNormalizer entityNormalizer) {
            this.entityNormalizer = entityNormalizer;
            return this;
        }

        /**
         * 在默认 Stage 链路前添加自定义 Stage。
         */
        public Builder addStageFirst(Stage stage) {
            extraFirstStages.add(stage);
            return this;
        }

        /**
         * 在默认 Stage 链路后添加自定义 Stage。
         */
        public Builder addStageLast(Stage stage) {
            extraLastStages.add(stage);
            return this;
        }

        /**
         * 构建 QueryProcessor。
         * <p>
         * 按默认顺序组装 Pipeline：extra-first → Format → Intervention → SpellCorrection
         * → Tokenizer → Synonym → NER → Normalizer → extra-last
         */
        public QueryProcessor build() {
            Pipeline<Stage> pipeline = new DefaultPipeline<>();

            // 前置自定义 Stage
            for (Stage stage : extraFirstStages) {
                pipeline.addLast(stage);
            }

            // 默认链路
            if (wordFormat != null) {
                pipeline.addLast(new FormatStage(wordFormat));
            }
            if (interventionService != null) {
                pipeline.addLast(new InterventionStage(interventionService));
            }
            if (spellCorrectionEngine != null) {
                pipeline.addLast(new SpellCorrectionStage(spellCorrectionEngine));
            }
            if (tokenizer != null) {
                pipeline.addLast(new TokenizerStage(tokenizer));
            }
            if (synonymService != null) {
                pipeline.addLast(new SynonymStage(synonymService));
            }
            if (entityRecognizer != null) {
                pipeline.addLast(new NerStage(entityRecognizer));
            }
            if (entityNormalizer != null) {
                pipeline.addLast(new NormalizerStage(entityNormalizer));
            }

            // 后置自定义 Stage
            for (Stage stage : extraLastStages) {
                pipeline.addLast(stage);
            }

            return new QueryProcessor(pipeline);
        }
    }
}
