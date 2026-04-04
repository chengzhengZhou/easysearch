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

package com.ppwx.easysearch.qp.correction;

import com.ppwx.easysearch.qp.source.TextLineSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 拼写纠错服务门面：封装基于词典的纠错引擎，提供统一 API。
 * <p>
 * 与 {@link com.ppwx.easysearch.qp.intervention.InterventionService}、
 * {@link com.ppwx.easysearch.qp.synonym.SynonymService} 风格一致。
 * <p>
 * 使用方式：
 * <pre>
 * SpellCorrectionEngine engine = SpellCorrectionEngine.create("/path/to/dict.txt");
 * CorrectionResult result = engine.correct("手击壳");
 * // result.getCorrectedQuery() → "手机壳"
 * </pre>
 */
public class SpellCorrectionEngine {

    private static final Logger log = LoggerFactory.getLogger(SpellCorrectionEngine.class);

    private final DictBasedSpellChecker checker;

    public SpellCorrectionEngine(DictBasedSpellChecker checker) {
        this.checker = checker;
    }

    /**
     * 从路径创建并加载纠错词典。
     *
     * @param dictPath 纠错词典文件路径，支持 classpath 和文件系统路径
     */
    public static SpellCorrectionEngine create(String dictPath) throws IOException {
        return create(dictPath, CorrectionConfig.defaults());
    }

    /**
     * 从路径创建并加载纠错词典，使用自定义配置。
     *
     * @param dictPath 纠错词典文件路径
     * @param config   纠错配置
     */
    public static SpellCorrectionEngine create(String dictPath, CorrectionConfig config)
            throws IOException {
        DictBasedSpellChecker checker = new DictBasedSpellChecker(config);
        checker.load(dictPath);
        return new SpellCorrectionEngine(checker);
    }

    /**
     * 从统一资源源创建并加载纠错词典。
     *
     * @param source 词典资源源
     */
    public static SpellCorrectionEngine create(TextLineSource source) throws IOException {
        return create(source, CorrectionConfig.defaults());
    }

    /**
     * 从统一资源源创建并加载纠错词典，使用自定义配置。
     *
     * @param source 词典资源源
     * @param config 纠错配置
     */
    public static SpellCorrectionEngine create(TextLineSource source, CorrectionConfig config)
            throws IOException {
        DictBasedSpellChecker checker = new DictBasedSpellChecker(config);
        checker.load(source);
        return new SpellCorrectionEngine(checker);
    }

    /**
     * 对 query 进行拼写纠错检查。
     * <p>
     * 返回完整的纠错结果，包含纠正后的 query、置信度和各处纠正详情。
     *
     * @param query 输入 query
     * @return 纠错结果
     */
    public CorrectionResult correct(String query) {
        if (query == null) {
            return CorrectionResult.noCorrection(null);
        }
        try {
            return checker.check(query);
        } catch (Exception e) {
            log.error("Spell correction failed for query: '{}', fallback to original", query, e);
            return CorrectionResult.noCorrection(query);
        }
    }

    /**
     * 对 query 进行拼写纠错建议（仅返回建议，不自动纠正）。
     *
     * @param query 输入 query
     * @return 纠错结果（autoCorrect 始终为 false）
     */
    public CorrectionResult suggest(String query) {
        CorrectionResult result = correct(query);
        // suggest 模式下不自动纠正
        if (result.hasCorrections()) {
            return new CorrectionResult(
                    result.getOriginalQuery(),
                    result.getOriginalQuery(), // 保持原 query 不变
                    result.getConfidence(),
                    result.getCorrections(),
                    false // 强制不自动纠正
            );
        }
        return result;
    }

    /**
     * 重新加载纠错词典。
     *
     * @param source 新的词典资源源
     */
    public void reload(TextLineSource source) throws IOException {
        checker.load(source);
    }

    /**
     * 重新加载纠错词典。
     *
     * @param dictPath 新的词典文件路径
     */
    public void reload(String dictPath) throws IOException {
        checker.load(dictPath);
    }

    /**
     * 纠错引擎是否已加载词典。
     */
    public boolean isLoaded() {
        return checker.isLoaded();
    }

    public DictBasedSpellChecker getChecker() {
        return checker;
    }
}
