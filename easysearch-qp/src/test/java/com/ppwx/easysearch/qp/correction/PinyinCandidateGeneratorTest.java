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

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * PinyinCandidateGenerator 拼音候选生成器单元测试。
 */
public class PinyinCandidateGeneratorTest {

    @Test
    public void generate_emptyTerm() {
        PinyinIndex index = new PinyinIndex();
        index.build(Arrays.asList(new DictEntry("手机壳", null, 100)));
        PinyinCandidateGenerator gen = new PinyinCandidateGenerator(index, 10);

        assertTrue(gen.generate("").isEmpty());
        assertTrue(gen.generate(null).isEmpty());
    }

    @Test
    public void generate_pinyinMatch() {
        List<DictEntry> entries = Arrays.asList(
                new DictEntry("手机壳", null, 1000),
                new DictEntry("手机膜", null, 800),
                new DictEntry("华为", null, 500)
        );
        PinyinIndex index = new PinyinIndex();
        index.build(entries);

        PinyinCandidateGenerator gen = new PinyinCandidateGenerator(index, 10);

        // "手机壳" 和 "手机膜" 拼音不同但相近，可能会被召回
        List<DictEntry> results = gen.generate("手机壳");

        // 输入本身不应出现在结果中
        assertFalse(results.stream().anyMatch(e -> "手机壳".equals(e.getWord())));
    }

    @Test
    public void generate_maxCandidates() {
        List<DictEntry> entries = Arrays.asList(
                new DictEntry("手机壳", null, 100),
                new DictEntry("手机膜", null, 200),
                new DictEntry("手机套", null, 300),
                new DictEntry("手机支架", null, 400),
                new DictEntry("手机充电器", null, 500)
        );
        PinyinIndex index = new PinyinIndex();
        index.build(entries);

        PinyinCandidateGenerator gen = new PinyinCandidateGenerator(index, 2);
        List<DictEntry> results = gen.generate("手机壳");
        assertTrue(results.size() <= 2);
    }

    @Test
    public void generate_sortedByFrequency() {
        List<DictEntry> entries = Arrays.asList(
                new DictEntry("手机膜", null, 100),
                new DictEntry("手机套", null, 1000)
        );
        PinyinIndex index = new PinyinIndex();
        index.build(entries);

        PinyinCandidateGenerator gen = new PinyinCandidateGenerator(index, 10);
        List<DictEntry> results = gen.generate("手机壳");

        if (results.size() >= 2) {
            assertTrue(results.get(0).getFrequency() >= results.get(1).getFrequency());
        }
    }

    @Test
    public void generate_excludesSelf() {
        List<DictEntry> entries = Arrays.asList(
                new DictEntry("手机壳", null, 1000),
                new DictEntry("华为", null, 500)
        );
        PinyinIndex index = new PinyinIndex();
        index.build(entries);

        PinyinCandidateGenerator gen = new PinyinCandidateGenerator(index, 10);
        List<DictEntry> results = gen.generate("手机壳");

        assertFalse(results.stream().anyMatch(e -> "手机壳".equals(e.getWord())));
    }
}
