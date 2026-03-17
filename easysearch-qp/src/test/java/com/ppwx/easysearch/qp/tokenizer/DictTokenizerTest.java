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

package com.ppwx.easysearch.qp.tokenizer;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.*;

/**
 * DictTokenizer 双向最长匹配分词器测试。
 */
public class DictTokenizerTest {

    private static final String SAMPLE_DICT = "影石\tnz\n摄像机\tn\n影石摄像机\tnz\n华为\tnz\n手机\tn\n华为手机\tnz\n";

    private DictTokenizer tokenizer;

    @Before
    public void setUp() {
        tokenizer = DictTokenizer.fromStream(new ByteArrayInputStream(SAMPLE_DICT.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void tokenizeWithDict() {
        List<Token> tokens = tokenizer.tokenize("影石摄像机");
        assertNotNull(tokens);
        assertTrue(tokens.size() <= 2);
        // 双向匹配：正向 [影石摄像机]，反向 [影石摄像机]，词数相同取正向，应得到 1 个词
        assertEquals(1, tokens.size());
        assertEquals("影石摄像机", tokens.get(0).getText());
        assertEquals(0, tokens.get(0).getStartIndex());
        assertEquals(5, tokens.get(0).getEndIndex());
    }

    @Test
    public void tokenizeBidirectionalPreferFewerSegments() {
        // “华为手机” 正向可 [华为][手机] 或 [华为手机] 取决于最长匹配；反向同理。双向取词数少者
        List<Token> tokens = tokenizer.tokenize("华为手机很好用");
        assertNotNull(tokens);
        assertTrue(tokens.size() >= 2);
        // 至少包含 “华为手机” 或 “华为”“手机”，以及 “很”“好”“用”
        String first = tokens.get(0).getText();
        assertTrue("华为".equals(first) || "华为手机".equals(first));
    }

    @Test
    public void tokenizeEmptyAndNull() {
        assertTrue(tokenizer.tokenize("").isEmpty());
        assertTrue(tokenizer.tokenize(null).isEmpty());
    }

    @Test
    public void loadFromClasspathOrPath() throws IOException {
        DictTokenizer fromPath = DictTokenizer.fromPath("dict-tokenizer-sample.txt");
        List<Token> tokens = fromPath.tokenize("影石摄像机");
        assertNotNull(tokens);
        assertFalse(tokens.isEmpty());
    }
}
