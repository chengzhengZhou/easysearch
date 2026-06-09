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

import com.hankcs.hanlp.model.crf.Product3CCRFSegmenter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 基于 Product3C CRF 分词模型的分词器。
 * <p>
 * 内部完成文本归一化、拼写归一化、4 列特征构造，输出带原始偏移的 Token 列表。
 * 词性固定为 "NN"；若需词性标注请在上层叠加。
 */
public class Product3CCWSTokenizer implements Tokenizer {

    private static final Logger log = LoggerFactory.getLogger(Product3CCWSTokenizer.class);

    public static final String DEFAULT_POS = "NN";

    private final String modelPath;
    private final String dictPath;
    private final String spellingPath;

    private volatile Product3CCRFSegmenter segmenter;
    private volatile boolean initFailed;

    public Product3CCWSTokenizer() {
        this(Product3CCRFSegmenter.CRF_MODEL, Product3CCRFSegmenter.DEFAULT_DICT_PATH, Product3CCRFSegmenter.DEFAULT_SPELLING_PATH);
    }

    public Product3CCWSTokenizer(String modelPath, String dictPath, String spellingPath) {
        this.modelPath = modelPath != null ? modelPath : Product3CCRFSegmenter.CRF_MODEL;
        this.dictPath = dictPath != null ? dictPath : Product3CCRFSegmenter.DEFAULT_DICT_PATH;
        this.spellingPath = spellingPath != null ? spellingPath : Product3CCRFSegmenter.DEFAULT_SPELLING_PATH;
    }

    public Product3CCWSTokenizer(String modelPath, String dictPath) {
        this(modelPath, dictPath, null);
    }

    public Product3CCWSTokenizer(String modelPath) {
        this(modelPath, null, null);
    }

    @Override
    public List<Token> tokenize(String text) {
        if (StringUtils.isBlank(text)) {
            return Collections.emptyList();
        }
        Product3CCRFSegmenter seg = getOrCreateSegmenter();
        if (seg == null) {
            return Collections.emptyList();
        }

        try {
            List<int[]> spans = seg.segmentWithOffsets(text);
            if (spans.isEmpty()) {
                return Collections.emptyList();
            }
            return buildTokens(text, spans);
        } catch (Exception e) {
            log.debug("Product3C CWS tokenize error: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private Product3CCRFSegmenter getOrCreateSegmenter() {
        if (initFailed) {
            return null;
        }
        if (segmenter == null) {
            synchronized (this) {
                if (segmenter == null) {
                    try {
                        segmenter = new Product3CCRFSegmenter(modelPath, dictPath, spellingPath);
                    } catch (IOException e) {
                        initFailed = true;
                        log.warn("Failed to load Product3C CWS segmenter: {}", e.getMessage());
                    }
                }
            }
        }
        return segmenter;
    }

    /**
     * 将 segmentWithOffsets 返回的 rawStart/rawEnd 区间转为 Token 列表。
     * spans[i] = {rawStart, rawEnd}，直接对应原始 text 的字符位置。
     */
    private static List<Token> buildTokens(String text, List<int[]> spans) {
        List<Token> tokens = new ArrayList<>(spans.size());
        for (int[] span : spans) {
            int start = span[0];
            int end = span[1];
            if (start < 0 || end > text.length() || start >= end) {
                continue;
            }
            String tokenText = text.substring(start, end);
            if (tokenText.trim().isEmpty()) {
                continue;
            }
            tokens.add(Token.builder()
                    .text(tokenText)
                    .type(DEFAULT_POS)
                    .startIndex(start)
                    .endIndex(end)
                    .confidence(1.0)
                    .build());
        }
        return tokens;
    }

    public boolean isInitFailed() {
        return initFailed;
    }
}