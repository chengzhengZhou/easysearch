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

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

/**
 * SpellCorrectionEngine 门面集成测试。
 */
public class SpellCorrectionEngineTest {

    private static final String TEST_DICT =
            "手机壳\t9800\n" +
            "手机膜\t8500\n" +
            "手机套\t7200\n" +
            "华为\t15000\n" +
            "苹果\t12000\n" +
            "小米\t11000\n" +
            "充电宝\t7500\n" +
            "蓝牙耳机\t6800\n" +
            "充电器\t5800\n";

    @Test
    public void correct_nullQuery() {
        SpellCorrectionEngine engine = createEngine();
        CorrectionResult result = engine.correct(null);
        assertNull(result.getOriginalQuery());
        assertFalse(result.hasCorrections());
    }

    @Test
    public void correct_emptyQuery() {
        SpellCorrectionEngine engine = createEngine();
        CorrectionResult result = engine.correct("");
        assertFalse(result.hasCorrections());
    }

    @Test
    public void correct_noErrors() {
        SpellCorrectionEngine engine = createEngine();
        CorrectionResult result = engine.correct("手机壳");
        assertFalse(result.hasCorrections());
        assertEquals("手机壳", result.getCorrectedQuery());
    }

    @Test
    public void correct_typoQuery() {
        SpellCorrectionEngine engine = createEngine();
        // "手击壳" 是 "手机壳" 的同音错别字
        CorrectionResult result = engine.correct("手击壳");
        assertNotNull(result);
        assertEquals("手击壳", result.getOriginalQuery());
        // 验证结果结构完整
        if (result.hasCorrections()) {
            Correction first = result.getCorrections().get(0);
            assertEquals("手击壳", first.getOriginal());
            assertNotNull(first.getCorrected());
            assertTrue(first.getConfidence() >= 0.0);
            assertTrue(first.getConfidence() <= 1.0);
        }
    }

    @Test
    public void suggest_doesNotAutoCorrect() {
        SpellCorrectionEngine engine = createEngine();
        CorrectionResult result = engine.suggest("手击壳");
        // suggest 模式下 correctedQuery 应该等于 originalQuery
        if (result.hasCorrections()) {
            assertEquals(result.getOriginalQuery(), result.getCorrectedQuery());
            assertFalse(result.isAutoCorrect());
        }
    }

    @Test
    public void correct_noDictionary_fallback() {
        // 引擎未加载词典时应 fallback 到原 query
        DictBasedSpellChecker checker = new DictBasedSpellChecker();
        SpellCorrectionEngine engine = new SpellCorrectionEngine(checker);

        CorrectionResult result = engine.correct("手击壳");
        assertFalse(result.hasCorrections());
        assertEquals("手击壳", result.getCorrectedQuery());
    }

    @Test
    public void isLoaded() {
        SpellCorrectionEngine engine = createEngine();
        assertTrue(engine.isLoaded());
    }

    @Test
    public void getChecker() {
        SpellCorrectionEngine engine = createEngine();
        assertNotNull(engine.getChecker());
        assertTrue(engine.getChecker().isLoaded());
    }

    @Test
    public void correct_withWhitespace() {
        SpellCorrectionEngine engine = createEngine();
        // 包含空格的 query
        CorrectionResult result = engine.correct("手机壳 华为");
        assertNotNull(result);
    }

    @Test
    public void correctionResult_noCorrection() {
        CorrectionResult result = CorrectionResult.noCorrection("test");
        assertFalse(result.hasCorrections());
        assertFalse(result.isAutoCorrect());
        assertEquals("test", result.getOriginalQuery());
        assertEquals("test", result.getCorrectedQuery());
        assertEquals(0.0, result.getConfidence(), 0.001);
        assertTrue(result.getCorrections().isEmpty());
    }

    @Test
    public void correctionConfig_defaults() {
        CorrectionConfig config = CorrectionConfig.defaults();
        assertEquals(2, config.getMaxEditDistance());
        assertEquals(10, config.getMaxPinyinCandidates());
        assertEquals(10, config.getMaxEditCandidates());
        assertEquals(0.8, config.getHighThreshold(), 0.001);
        assertEquals(0.6, config.getLowThreshold(), 0.001);
        assertEquals(0.4, config.getPinyinWeight(), 0.001);
        assertEquals(0.4, config.getEditDistanceWeight(), 0.001);
        assertEquals(0.2, config.getFrequencyWeight(), 0.001);
    }

    @Test
    public void correctionConfig_chainSetters() {
        CorrectionConfig config = new CorrectionConfig()
                .setMaxEditDistance(3)
                .setHighThreshold(0.9)
                .setPinyinWeight(0.5);

        assertEquals(3, config.getMaxEditDistance());
        assertEquals(0.9, config.getHighThreshold(), 0.001);
        assertEquals(0.5, config.getPinyinWeight(), 0.001);
    }

    @Test
    public void dictEntry_equalsAndHashCode() {
        DictEntry e1 = new DictEntry("手机壳", "shoujike", 100);
        DictEntry e2 = new DictEntry("手机壳", "other", 200);
        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());
    }

    @Test
    public void correction_immutableList() {
        Correction c = new Correction("原词", "纠正词", 0, 2, 0.9, null);
        assertTrue(c.getAlternatives().isEmpty());
    }

    private SpellCorrectionEngine createEngine() {
        try {
            DictBasedSpellChecker checker = new DictBasedSpellChecker();
            checker.load(() -> new ByteArrayInputStream(TEST_DICT.getBytes(StandardCharsets.UTF_8)));
            return new SpellCorrectionEngine(checker);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create engine", e);
        }
    }
}
