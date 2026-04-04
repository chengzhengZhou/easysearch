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

import com.ppwx.easysearch.qp.correction.CorrectionResult;
import com.ppwx.easysearch.qp.ner.Entity;
import com.ppwx.easysearch.qp.tokenizer.Token;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 查询处理上下文，贯穿整条处理链路。
 * <p>
 * 各 {@link Stage} 读写其特定字段，并通过 {@link #putTrace(String, Object)} 记录命中和耗时信息。
 * <p>
 * 线程非安全：每次请求新建一个实例。
 */
public class QueryContext {

    /** 用户原始 query */
    private final String originalQuery;

    /** 预处理后的 query（format 阶段输出） */
    private String normalizedQuery;

    /** 干预后的 query（intervention 阶段输出） */
    private String intervenedQuery;

    /** 纠错后的 query（correction 阶段输出） */
    private String correctedQuery;

    /** 纠错详情 */
    private CorrectionResult correctionResult;

    /** 分词结果 */
    private List<Token> tokens;

    /** 同义词改写后的 query */
    private String rewrittenQuery;

    /** 同义词扩展后的候选 query 列表 */
    private List<String> expandedQueries;

    /** 实体列表 */
    private List<Entity> entities;

    /** 处理选项 */
    private ProcessOptions options;

    /** trace 信息（保持写入顺序） */
    private final Map<String, Object> trace;

    public QueryContext(String originalQuery) {
        this.originalQuery = originalQuery;
        this.trace = new LinkedHashMap<>();
    }

    public QueryContext(String originalQuery, ProcessOptions options) {
        this.originalQuery = originalQuery;
        this.options = options;
        this.trace = new LinkedHashMap<>();
    }

    // ==================== trace ====================

    public void putTrace(String key, Object value) {
        trace.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getTrace(String key) {
        return (T) trace.get(key);
    }

    public Map<String, Object> getTrace() {
        return trace;
    }

    // ==================== getters & setters ====================

    public String getOriginalQuery() {
        return originalQuery;
    }

    public String getNormalizedQuery() {
        return normalizedQuery;
    }

    public void setNormalizedQuery(String normalizedQuery) {
        this.normalizedQuery = normalizedQuery;
    }

    public String getIntervenedQuery() {
        return intervenedQuery;
    }

    public void setIntervenedQuery(String intervenedQuery) {
        this.intervenedQuery = intervenedQuery;
    }

    public String getCorrectedQuery() {
        return correctedQuery;
    }

    public void setCorrectedQuery(String correctedQuery) {
        this.correctedQuery = correctedQuery;
    }

    public CorrectionResult getCorrectionResult() {
        return correctionResult;
    }

    public void setCorrectionResult(CorrectionResult correctionResult) {
        this.correctionResult = correctionResult;
    }

    public List<Token> getTokens() {
        return tokens;
    }

    public void setTokens(List<Token> tokens) {
        this.tokens = tokens;
    }

    public String getRewrittenQuery() {
        return rewrittenQuery;
    }

    public void setRewrittenQuery(String rewrittenQuery) {
        this.rewrittenQuery = rewrittenQuery;
    }

    public List<String> getExpandedQueries() {
        return expandedQueries;
    }

    public void setExpandedQueries(List<String> expandedQueries) {
        this.expandedQueries = expandedQueries;
    }

    public List<Entity> getEntities() {
        return entities;
    }

    public void setEntities(List<Entity> entities) {
        this.entities = entities;
    }

    public void setEntities(Collection<Entity> entities) {
        this.entities = entities != null ? new ArrayList<>(entities) : null;
    }

    public ProcessOptions getOptions() {
        return options;
    }

    public void setOptions(ProcessOptions options) {
        this.options = options;
    }

    /**
     * 获取当前最终 query（按优先级：correctedQuery > intervenedQuery > normalizedQuery > originalQuery）。
     * <p>
     * 供后续 Stage 读取上一阶段的输出。
     */
    public String getCurrentQuery() {
        if (correctedQuery != null) {
            return correctedQuery;
        }
        if (intervenedQuery != null) {
            return intervenedQuery;
        }
        if (normalizedQuery != null) {
            return normalizedQuery;
        }
        return originalQuery;
    }

    @Override
    public String toString() {
        return "QueryContext{"
                + "originalQuery='" + originalQuery + '\''
                + ", normalizedQuery='" + normalizedQuery + '\''
                + ", intervenedQuery='" + intervenedQuery + '\''
                + ", correctedQuery='" + correctedQuery + '\''
                + ", tokens=" + tokens
                + ", rewrittenQuery='" + rewrittenQuery + '\''
                + ", expandedQueries=" + expandedQueries
                + ", entities=" + entities
                + '}';
    }
}
