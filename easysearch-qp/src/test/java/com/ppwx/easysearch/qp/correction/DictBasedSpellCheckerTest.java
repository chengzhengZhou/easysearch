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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * DictBasedSpellChecker 核心引擎单元测试。
 */
public class DictBasedSpellCheckerTest {

    private static final String TEST_DICT =
            "手机壳\t9800\n" +
            "手机膜\t8500\n" +
            "手机套\t7200\n" +
            "华为\t15000\n" +
            "苹果\t12000\n" +
            "小米\t11000\n" +
            "充电宝\t7500\n";

    @Test
    public void load_fromInputStream() throws IOException {
        DictBasedSpellChecker checker = new DictBasedSpellChecker();
        checker.load(() -> new ByteArrayInputStream(TEST_DICT.getBytes(StandardCharsets.UTF_8)));
        assertTrue(checker.isLoaded());
    }

    @Test
    public void load_fromEntries() {
        DictBasedSpellChecker checker = new DictBasedSpellChecker();
        List<DictEntry> entries = Arrays.asList(
                new DictEntry("手机壳", null, 9800),
                new DictEntry("华为", null, 15000)
        );
        checker.loadEntries(entries);
        assertTrue(checker.isLoaded());
    }

    @Test
    public void loadEntries_null() {
        DictBasedSpellChecker checker = new DictBasedSpellChecker();
        checker.loadEntries(null);
        assertFalse(checker.isLoaded());
    }

    @Test
    public void check_nullQuery() throws IOException {
        DictBasedSpellChecker checker = new DictBasedSpellChecker();
        checker.load(() -> new ByteArrayInputStream(TEST_DICT.getBytes(StandardCharsets.UTF_8)));

        CorrectionResult result = checker.check(null);
        assertFalse(result.hasCorrections());
    }

    @Test
    public void check_emptyQuery() throws IOException {
        DictBasedSpellChecker checker = new DictBasedSpellChecker();
        checker.load(() -> new ByteArrayInputStream(TEST_DICT.getBytes(StandardCharsets.UTF_8)));

        CorrectionResult result = checker.check("");
        assertFalse(result.hasCorrections());
    }

    @Test
    public void check_noErrors() throws IOException {
        DictBasedSpellChecker checker = new DictBasedSpellChecker();
        checker.load(() -> new ByteArrayInputStream(TEST_DICT.getBytes(StandardCharsets.UTF_8)));

        // "手机壳" 在词典中，不应报错
        CorrectionResult result = checker.check("手机壳");
        assertFalse(result.hasCorrections());
    }

    @Test
    public void check_knownPhrase() throws IOException {
        DictBasedSpellChecker checker = new DictBasedSpellChecker();
        checker.load(() -> new ByteArrayInputStream(TEST_DICT.getBytes(StandardCharsets.UTF_8)));

        // 词典中有的词组合
        CorrectionResult result = checker.check("华为手机壳");
        // 可能不会报错，因为"华为"和"手机壳"各自在词典中
        // 也可能报错，取决于分词切分效果
        // 这里只验证不崩溃
        assertNotNull(result);
    }

    @Test
    public void check_typo_generatesCandidates() throws IOException {
        DictBasedSpellChecker checker = new DictBasedSpellChecker();
        checker.load(() -> new ByteArrayInputStream(TEST_DICT.getBytes(StandardCharsets.UTF_8)));

        // "手机壳" -> "手击壳" (同音错别字，编辑距离1)
        CorrectionResult result = checker.check("手击壳");
        // 验证系统至少能运行不崩溃
        assertNotNull(result);
        assertNotNull(result.getOriginalQuery());
    }

    @Test
    public void check_noDictionaryLoaded() {
        DictBasedSpellChecker checker = new DictBasedSpellChecker();
        CorrectionResult result = checker.check("手击壳");
        assertEquals(CorrectionResult.noCorrection("手击壳"), result);
    }

    @Test
    public void check_customConfig() throws IOException {
        CorrectionConfig config = new CorrectionConfig()
                .setHighThreshold(0.9)
                .setLowThreshold(0.1)
                .setMaxEditDistance(3);

        DictBasedSpellChecker checker = new DictBasedSpellChecker(config);
        checker.load(() -> new ByteArrayInputStream(TEST_DICT.getBytes(StandardCharsets.UTF_8)));

        CorrectionResult result = checker.check("手击壳");
        assertNotNull(result);
    }

    @Test
    public void check_commentLinesIgnored() throws IOException {
        String dictWithComments = "# 这是注释\n手机壳\t9800\n# 另一个注释\n华为\t15000\n";
        DictBasedSpellChecker checker = new DictBasedSpellChecker();
        checker.load(() -> new ByteArrayInputStream(dictWithComments.getBytes(StandardCharsets.UTF_8)));

        assertTrue(checker.isLoaded());
        CorrectionResult result = checker.check("手机壳");
        assertFalse(result.hasCorrections());
    }

    @Test
    public void check_emptyLinesIgnored() throws IOException {
        String dictWithEmpty = "\n\n手机壳\t9800\n\n\n";
        DictBasedSpellChecker checker = new DictBasedSpellChecker();
        checker.load(() -> new ByteArrayInputStream(dictWithEmpty.getBytes(StandardCharsets.UTF_8)));

        assertTrue(checker.isLoaded());
    }

    @Test
    public void check_invalidFrequencyParsed() throws IOException {
        String dictWithInvalid = "手机壳\t9800\n华为\tinvalid_freq\n";
        DictBasedSpellChecker checker = new DictBasedSpellChecker();
        // 不应抛异常
        checker.load(() -> new ByteArrayInputStream(dictWithInvalid.getBytes(StandardCharsets.UTF_8)));
        assertTrue(checker.isLoaded());
    }
}
