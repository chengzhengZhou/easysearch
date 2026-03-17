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

package com.ppwx.easysearch.qp.intervention;

import com.ppwx.easysearch.qp.source.TextLineSource;

import java.io.IOException;

/**
 * 干预词服务门面：封装词表改写 + 整句改写，提供统一 rewrite 能力。
 * <p>
 * 执行顺序：
 * 1. 先应用整句干预规则（SentenceInterventionEngine）
 * 2. 再应用词表干预规则（TermInterventionEngine）
 */
public class InterventionService {

    private final TermInterventionEngine termEngine;
    private final SentenceInterventionEngine sentenceEngine;

    public InterventionService(TermInterventionEngine termEngine,
                               SentenceInterventionEngine sentenceEngine) {
        this.termEngine = termEngine;
        this.sentenceEngine = sentenceEngine;
    }

    /**
     * 从路径创建并加载干预词服务。
     *
     * @param termPath      词表干预文件路径，可为空
     * @param sentencePath  整句干预文件路径，可为空
     */
    public static InterventionService create(String termPath, String sentencePath) throws IOException {
        TermInterventionEngine termEngine = null;
        SentenceInterventionEngine sentenceEngine = null;

        if (termPath != null && !termPath.isEmpty()) {
            termEngine = new TermInterventionEngine();
            termEngine.load(termPath);
        }

        if (sentencePath != null && !sentencePath.isEmpty()) {
            sentenceEngine = new SentenceInterventionEngine();
            sentenceEngine.load(sentencePath);
        }

        return new InterventionService(termEngine, sentenceEngine);
    }

    /**
     * 从统一资源源创建并加载干预词服务。
     */
    public static InterventionService create(TextLineSource termSource,
                                             TextLineSource sentenceSource) throws IOException {
        TermInterventionEngine termEngine = null;
        SentenceInterventionEngine sentenceEngine = null;

        if (termSource != null) {
            termEngine = new TermInterventionEngine();
            termEngine.load(termSource);
        }

        if (sentenceSource != null) {
            sentenceEngine = new SentenceInterventionEngine();
            sentenceEngine.load(sentenceSource);
        }

        return new InterventionService(termEngine, sentenceEngine);
    }

    /**
     * 对 query 进行干预改写：先整句再词表。
     */
    public String rewrite(String query) {
        if (query == null) {
            return null;
        }
        String rewritten = query;

        if (sentenceEngine != null && sentenceEngine.isLoaded()) {
            rewritten = sentenceEngine.rewrite(rewritten);
        }

        if (termEngine != null && termEngine.isLoaded()) {
            rewritten = termEngine.rewrite(rewritten);
        }

        return rewritten;
    }

    public TermInterventionEngine getTermEngine() {
        return termEngine;
    }

    public SentenceInterventionEngine getSentenceEngine() {
        return sentenceEngine;
    }
}

