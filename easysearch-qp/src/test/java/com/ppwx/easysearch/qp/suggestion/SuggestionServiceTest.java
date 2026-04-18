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

package com.ppwx.easysearch.qp.suggestion;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

/**
 * SuggestionService 集成测试。
 */
public class SuggestionServiceTest {

    private SuggestionService service;

    @Before
    public void setUp() throws IOException {
        SuggestionEngine engine = new SuggestionEngine();
        engine.load("suggestion/test_suggestion_dict.txt");

        service = SuggestionService.builder()
                .withEngine(engine)
                .withConfig(SuggestionConfig.defaults().setTopK(10))
                .build();
    }

    @Test
    public void suggest_returnsResults() {
        List<Suggestion> results = service.suggest("苹果");
        assertFalse(results.isEmpty());
    }

    @Test
    public void suggest_orderedByScore() {
        List<Suggestion> results = service.suggest("苹果");
        for (int i = 1; i < results.size(); i++) {
            assertTrue(results.get(i - 1).getScore() >= results.get(i).getScore());
        }
    }

    @Test
    public void suggest_respectsLimit() {
        List<Suggestion> results = service.suggest("苹果", 2);
        assertTrue(results.size() <= 2);
    }

    @Test
    public void suggest_emptyForNull() {
        assertTrue(service.suggest(null).isEmpty());
    }

    @Test
    public void suggest_emptyForEmpty() {
        assertTrue(service.suggest("").isEmpty());
    }

    @Test
    public void suggest_containsHighlight() {
        List<Suggestion> results = service.suggest("苹果");
        if (!results.isEmpty()) {
            for (Suggestion s : results) {
                if (s.getText().startsWith("苹果")) {
                    assertNotNull(s.getHighlight());
                    assertTrue(s.getHighlight().contains("<em>苹果</em>"));
                }
            }
        }
    }

    @Test
    public void suggest_hasSources() {
        List<Suggestion> results = service.suggest("苹果");
        for (Suggestion s : results) {
            assertFalse(s.getSources().isEmpty());
        }
    }

    @Test
    public void suggest_pinyinInput() {
        // 拼音输入也应能返回结果
        List<Suggestion> results = service.suggest("pingguo");
        assertFalse(results.isEmpty());
    }

    @Test
    public void suggest_prefixOnlyConfig() throws IOException {
        SuggestionEngine engine = new SuggestionEngine();
        engine.load("suggestion/test_suggestion_dict.txt");

        SuggestionConfig config = SuggestionConfig.defaults()
                .setEnablePrefix(true)
                .setEnablePinyin(false);

        SuggestionService prefixOnly = SuggestionService.builder()
                .withEngine(engine)
                .withConfig(config)
                .build();

        List<Suggestion> results = prefixOnly.suggest("苹果");
        assertFalse(results.isEmpty());
        for (Suggestion s : results) {
            assertTrue(s.getSources().contains("prefix"));
            assertFalse(s.getSources().contains("pinyin"));
        }
    }

    @Test
    public void suggest_pinyinOnlyConfig() throws IOException {
        SuggestionEngine engine = new SuggestionEngine();
        engine.load("suggestion/test_suggestion_dict.txt");

        SuggestionConfig config = SuggestionConfig.defaults()
                .setEnablePrefix(false)
                .setEnablePinyin(true);

        SuggestionService pinyinOnly = SuggestionService.builder()
                .withEngine(engine)
                .withConfig(config)
                .build();

        List<Suggestion> results = pinyinOnly.suggest("pingguo");
        assertFalse(results.isEmpty());
        for (Suggestion s : results) {
            assertTrue(s.getSources().contains("pinyin"));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void builder_throwsWithoutEngine() {
        SuggestionService.builder().build();
    }

    @Test
    public void getChannels_defaultHasTwoChannels() {
        assertEquals(2, service.getChannels().size());
    }

    @Test
    public void addCustomChannel() throws IOException {
        SuggestionEngine engine = new SuggestionEngine();
        engine.load("suggestion/test_suggestion_dict.txt");

        InvertedIndexRecallChannel customChannel = new InvertedIndexRecallChannel();

        SuggestionService customService = SuggestionService.builder()
                .withEngine(engine)
                .addChannel(customChannel)
                .build();

        // 默认 2 个（prefix + pinyin）+ 1 个自定义 = 3
        assertEquals(3, customService.getChannels().size());
    }
}
