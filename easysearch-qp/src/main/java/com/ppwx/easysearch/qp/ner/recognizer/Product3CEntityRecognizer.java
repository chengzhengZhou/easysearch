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

package com.ppwx.easysearch.qp.ner.recognizer;

import com.hankcs.hanlp.model.crf.Product3CCRFRecognizer;
import com.ppwx.easysearch.qp.ner.*;
import com.ppwx.easysearch.qp.ner.product3c.FeatureColumnBuilder;
import com.ppwx.easysearch.qp.ner.product3c.Product3CTextNormalizer;
import com.ppwx.easysearch.qp.tokenizer.Token;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * 二手 3C 字符级 CRF 实体识别器。
 * <p>
 * 输入为原始 query，内部完成文本归一化、拼写归一、6 列特征构造、BIO 解析和 offset 回映射。
 */
public class Product3CEntityRecognizer implements EntityRecognizer {

    public static final String O_TAG = "O";
    public static final String B_TAG_PREFIX = "B-";
    public static final String I_TAG_PREFIX = "I-";
    public static final String SOURCE = "product3c-crf";

    private static final Logger log = LoggerFactory.getLogger(Product3CEntityRecognizer.class);

    private final Product3CNerConfig config;

    private volatile Product3CCRFRecognizer crfRecognizer;
    private volatile FeatureColumnBuilder featureColumnBuilder;
    private volatile boolean initFailed;

    public Product3CEntityRecognizer() {
        this(Product3CNerConfig.fromSystemProperties());
    }

    public Product3CEntityRecognizer(Product3CNerConfig config) {
        this.config = config != null ? config : new Product3CNerConfig();
    }

    @Override
    public Collection<Entity> extractEntities(String originText, List<Token> tokens) {
        if (!config.isEnabled() || StringUtils.isBlank(originText)) {
            return Collections.emptyList();
        }
        Product3CCRFRecognizer recognizer = getOrCreateRecognizer();
        FeatureColumnBuilder builder = getOrCreateFeatureColumnBuilder();
        if (recognizer == null || builder == null) {
            return Collections.emptyList();
        }

        FeatureColumnBuilder.BuildResult buildResult;
        String[] tags;
        try {
            buildResult = builder.build(originText);
            String[][] columns = buildResult.getColumns();
            if (columns == null || columns.length != Product3CCRFRecognizer.NUM_COLUMNS || columns[0].length == 0) {
                return Collections.emptyList();
            }
            tags = recognizer.recognize(columns);
            if (tags == null || tags.length != columns[0].length) {
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.debug("Product3C NER recognize error: {}", e.getMessage());
            return Collections.emptyList();
        }

        List<Entity> entities = parseBioToEntities(originText, buildResult.getSpell(), tags);
        if (entities.isEmpty()) {
            entities = dictFallback(originText, buildResult);
        }
        entities.sort(Comparator.comparingInt(Entity::getStartOffset).thenComparingInt(Entity::getEndOffset));
        return entities;
    }

    private Product3CCRFRecognizer getOrCreateRecognizer() {
        if (initFailed) {
            return null;
        }
        if (crfRecognizer == null) {
            synchronized (this) {
                if (crfRecognizer == null) {
                    try {
                        crfRecognizer = loadRecognizer();
                    } catch (Exception e) {
                        handleInitFailure("Product3C CRF model", e);
                    }
                }
            }
        }
        return crfRecognizer;
    }

    private FeatureColumnBuilder getOrCreateFeatureColumnBuilder() {
        if (initFailed) {
            return null;
        }
        if (featureColumnBuilder == null) {
            synchronized (this) {
                if (featureColumnBuilder == null) {
                    try {
                        featureColumnBuilder = new FeatureColumnBuilder(config.getDictDir(), config.getSpellingPath());
                    } catch (Exception e) {
                        handleInitFailure("Product3C feature resources", e);
                    }
                }
            }
        }
        return featureColumnBuilder;
    }

    private Product3CCRFRecognizer loadRecognizer() throws IOException {
        if (StringUtils.isBlank(config.getModelPath())) {
            throw new IOException("3C NER modelPath is blank");
        }
        return new Product3CCRFRecognizer(config.getModelPath());
    }

    private void handleInitFailure(String resource, Exception e) {
        initFailed = true;
        if (config.isFailFast()) {
            throw new IllegalStateException("Failed to load " + resource, e);
        }
        log.warn("Failed to load {}: {}", resource, e.getMessage());
    }

    protected List<Entity> parseBioToEntitiesForTest(String originText,
                                                     String normalizedText,
                                                     int[] normToRawStart,
                                                     int[] normToRawEnd,
                                                     String[] tags) {
        Product3CTextNormalizer.SpellingResult spell = new Product3CTextNormalizer.SpellingResult(
                normalizedText,
                identity(normalizedText != null ? normalizedText.length() : 0),
                normToRawStart,
                normToRawEnd
        );
        return parseBioToEntities(originText, spell, tags);
    }

    private List<Entity> parseBioToEntities(String originText, Product3CTextNormalizer.SpellingResult spell, String[] tags) {
        if (originText == null || spell == null || spell.text == null || tags == null || spell.text.length() != tags.length) {
            return Collections.emptyList();
        }
        List<Entity> entities = new ArrayList<>();
        int i = 0;
        while (i < tags.length) {
            String tag = normalizeTag(tags[i]);
            if (!tag.startsWith(B_TAG_PREFIX)) {
                i++;
                continue;
            }
            String rawType = tag.substring(B_TAG_PREFIX.length()).trim();
            int j = i + 1;
            while (j < tags.length && (I_TAG_PREFIX + rawType).equals(normalizeTag(tags[j]))) {
                j++;
            }
            Entity entity = makeEntity(originText, spell, i, j, rawType);
            if (entity != null) {
                entities.add(entity);
            }
            i = j;
        }
        return entities;
    }

    private Entity makeEntity(String originText, Product3CTextNormalizer.SpellingResult spell, int start, int end, String rawType) {
        EntityType type = Product3CLabelMapper.map(rawType);
        if (type == EntityType.UNKNOWN || start < 0 || end <= start || end > spell.text.length()) {
            return null;
        }
        int startOffset = start < spell.normToRawStart.length ? spell.normToRawStart[start] : start;
        int endOffset = end - 1 < spell.normToRawEnd.length ? spell.normToRawEnd[end - 1] : end;
        startOffset = Math.max(0, Math.min(startOffset, originText.length()));
        endOffset = Math.max(0, Math.min(endOffset, originText.length()));
        if (startOffset >= endOffset) {
            return null;
        }
        String value = originText.substring(startOffset, endOffset);
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String normalizedValue = spell.text.substring(start, end);
        Entity entity = new Entity(value, type, normalizedValue, config.getDefaultConfidence(), startOffset, endOffset);
        entity.setAttachment(buildAttachment(rawType));
        return entity;
    }

    private List<Entity> dictFallback(String originText, FeatureColumnBuilder.BuildResult buildResult) {
        String[][] columns = buildResult.getColumns();
        if (columns.length < 3 || columns[2].length == 0) {
            return Collections.emptyList();
        }
        List<Entity> result = new ArrayList<>();
        String[] brandColumn = columns[2];
        Product3CTextNormalizer.SpellingResult spell = buildResult.getSpell();
        int i = 0;
        while (i < brandColumn.length) {
            if (!brandColumn[i].endsWith("头")) {
                i++;
                continue;
            }
            int j = i + 1;
            while (j < brandColumn.length && !O_TAG.equals(brandColumn[j])) {
                j++;
            }
            Entity entity = makeEntity(originText, spell, i, j, "BRD");
            if (entity != null) {
                result.add(entity);
            }
            i = j;
        }
        return result;
    }

    private Map<String, Object> buildAttachment(String rawType) {
        Map<String, Object> attachment = new HashMap<>();
        attachment.put("source", SOURCE);
        attachment.put("rawType", rawType);
        attachment.put("modelVersion", config.getModelVersion());
        FeatureColumnBuilder builder = featureColumnBuilder;
        if (builder != null && builder.getGazetteer() != null) {
            attachment.put("dictVersion", builder.getGazetteer().getDictVersion());
        }
        return attachment;
    }

    private static String normalizeTag(String tag) {
        return tag != null ? tag.trim() : O_TAG;
    }

    private static int[] identity(int length) {
        int[] arr = new int[length];
        for (int i = 0; i < length; i++) {
            arr[i] = i;
        }
        return arr;
    }

    public boolean isInitFailed() {
        return initFailed;
    }
}
