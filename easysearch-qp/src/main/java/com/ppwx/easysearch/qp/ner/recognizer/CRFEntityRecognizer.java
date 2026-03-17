package com.ppwx.easysearch.qp.ner.recognizer;

import com.hankcs.hanlp.model.crf.CRFNERecognizer;
import com.ppwx.easysearch.qp.ner.Entity;
import com.ppwx.easysearch.qp.ner.EntityNormalizer;
import com.ppwx.easysearch.qp.ner.EntityRecognizer;
import com.ppwx.easysearch.qp.ner.EntityType;
import com.ppwx.easysearch.qp.ner.normalizer.SpaceJoinStrategy;
import com.ppwx.easysearch.qp.ner.normalizer.TokenSpanToValueStrategy;
import com.ppwx.easysearch.qp.tokenizer.Token;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.*;

/**
 * 基于 HanLP CRF 的实体识别器。
 * 标注体系为 BMEOS：O 非实体，S 独立词实体（类型由词性决定），B/M/E 多词实体的开始/中间/结束并附加类型（如 B-MODEL）。
 */
public class CRFEntityRecognizer implements EntityRecognizer {

    public static final String O_TAG = "O";
    public static final String B_TAG_PREFIX = "B-";
    public static final String M_TAG_PREFIX = "M-";
    public static final String E_TAG_PREFIX = "E-";
    public static final String S_TAG = "S";

    public static final String CRF_NER_MODEL_RESOURCE = "data/model/vocab_ner_crf.txt.bin";

    private volatile CRFNERecognizer crfNerRecognizer;

    private volatile boolean initFailed;

    private String modelPath = CRF_NER_MODEL_RESOURCE;

    /** 按实体类型选择的 token 片段→候选串策略，未配置的类型使用 defaultTokenSpanStrategy */
    private Map<EntityType, TokenSpanToValueStrategy> tokenSpanStrategyByType;
    /** 默认的 token 片段→候选串策略，与 tokenSpanStrategyByType 均为 null 时使用内置空格拼接（兼容旧行为） */
    private TokenSpanToValueStrategy defaultTokenSpanStrategy;
    /** 对候选串按类型做规则归一化（如 ModelNormalizer），可为 null */
    private EntityNormalizer entityNormalizer;

    private CRFNERecognizer getOrCreateRecognizer() {
        if (initFailed) {
            return null;
        }
        if (crfNerRecognizer == null) {
            synchronized (this) {
                if (crfNerRecognizer == null) {
                    try {
                        crfNerRecognizer = loadRecognizer();
                    } catch (IOException e) {
                        initFailed = true;
                    }
                }
            }
        }
        return crfNerRecognizer;
    }

    private CRFNERecognizer loadRecognizer() throws IOException {
        /*Path tempFile = Files.createTempFile("hanlp_ner_", ".bin");
        try (InputStream is = StreamUtil.getResourceStream(modelPath)) {
            Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }*/
        return new CRFNERecognizer(modelPath);
    }

    @Override
    public Collection<Entity> extractEntities(String originText, List<Token> tokens) {
        if (originText == null || tokens == null || tokens.isEmpty()) {
            return Collections.emptyList();
        }
        CRFNERecognizer ner = getOrCreateRecognizer();
        if (ner == null) {
            return Collections.emptyList();
        }

        String[] wordArray = new String[tokens.size()];
        String[] posArray = new String[tokens.size()];
        for (int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            wordArray[i] = t.getText();
            posArray[i] = StringUtils.isNotBlank(t.getType()) ? t.getType() : "NN";
        }

        String[] nerTags;
        try {
            nerTags = ner.recognize(wordArray, posArray);
        } catch (Exception e) {
            return Collections.emptyList();
        }
        if (nerTags == null || nerTags.length != tokens.size()) {
            return Collections.emptyList();
        }

        return parseBmeosToEntities(originText, tokens, posArray, nerTags);
    }

    /**
     * 仅用于单元测试：使用给定的 BMEOS 标签解析实体，不依赖 CRF 模型。
     *
     * @param originText 原始文本
     * @param tokens     token 列表
     * @param posArray   词性数组，与 tokens 等长
     * @param nerTags    BMEOS 标签数组，与 tokens 等长
     * @return 解析出的实体列表
     */
    protected List<Entity> parseBmeosToEntitiesForTest(
            String originText,
            List<Token> tokens,
            String[] posArray,
            String[] nerTags) {
        if (originText == null || tokens == null || posArray == null || nerTags == null
                || tokens.size() != posArray.length || tokens.size() != nerTags.length) {
            return Collections.emptyList();
        }
        return parseBmeosToEntities(originText, tokens, posArray, nerTags);
    }

    /**
     * 解析 BMEOS 标注为实体列表。
     * O：非实体；S：单 token 实体，类型取词性；B-XXX/M-XXX/E-XXX：多 token 实体，仅在 E-XXX 时产出且类型为已定义实体。
     */
    private List<Entity> parseBmeosToEntities(
            String originText,
            List<Token> tokens,
            String[] posArray,
            String[] nerTags) {
        List<Entity> entities = new ArrayList<>();
        int i = 0;
        Integer multiStart = null;
        String multiType = null;

        while (i < tokens.size()) {
            String tag = nerTags[i] != null ? nerTags[i].trim() : O_TAG;

            if (O_TAG.equals(tag)) {
                multiStart = null;
                multiType = null;
            } else if (S_TAG.equals(tag)) {
                multiStart = null;
                multiType = null;
                if (EntityType.isDefinedEntityType(posArray[i])) {
                    Entity e = makeEntity(originText, tokens, i, i + 1, EntityType.getByName(posArray[i]));
                    if (e != null) entities.add(e);
                }
            } else if (tag.startsWith(B_TAG_PREFIX)) {
                multiStart = i;
                multiType = tag.substring(2).trim();
            } else if (tag.startsWith(M_TAG_PREFIX)) {
                if (!tag.substring(2).trim().equals(multiType)) {
                    multiStart = null;
                    multiType = null;
                }
            } else if (tag.startsWith(E_TAG_PREFIX)) {
                String typeName = tag.substring(2).trim();
                if (typeName.equals(multiType) && multiStart != null && EntityType.isDefinedEntityType(typeName)) {
                    Entity e = makeEntity(originText, tokens, multiStart, i + 1, EntityType.getByName(typeName));
                    if (e != null) entities.add(e);
                }
                multiStart = null;
                multiType = null;
            }
            i++;
        }
        return entities;
    }

    private Entity makeEntity(
            String originText,
            List<Token> tokens,
            int startTokenIdx,
            int endTokenIdx,
            EntityType type) {
        if (startTokenIdx < 0 || endTokenIdx > tokens.size() || startTokenIdx >= endTokenIdx) {
            return null;
        }
        Token startToken = tokens.get(startTokenIdx);
        Token endToken = tokens.get(endTokenIdx - 1);
        int startOffset = startToken.getStartIndex();
        int endOffset = endToken.getEndIndex();
        startOffset = Math.min(startOffset, originText.length());
        endOffset = Math.min(endOffset, originText.length());
        if (startOffset >= endOffset) {
            return null;
        }
        String value = originText.substring(startOffset, endOffset);
        String normalized = computeNormalizedValue(originText, tokens, startTokenIdx, endTokenIdx, type);
        return new Entity(value, type, normalized, 0.5, startOffset, endOffset);
    }

    /**
     * 归一化：先按策略从 token 片段得到候选串，再按类型做规则归一化（若配置了 entityNormalizer）。
     * 未配置策略时保持原空格拼接行为以兼容旧逻辑。
     */
    private String computeNormalizedValue(String originText,
                                          List<Token> tokens,
                                          int startTokenIdx,
                                          int endTokenIdx,
                                          EntityType type) {
        TokenSpanToValueStrategy strategy = resolveTokenSpanStrategy(type);
        String raw;
        if (strategy != null) {
            raw = strategy.toValue(originText, tokens, startTokenIdx, endTokenIdx);
        } else {
            raw = new SpaceJoinStrategy().toValue(originText, tokens, startTokenIdx, endTokenIdx);
        }
        if (entityNormalizer != null) {
            return entityNormalizer.normalize(type, raw);
        }
        return raw;
    }

    private TokenSpanToValueStrategy resolveTokenSpanStrategy(EntityType type) {
        if (tokenSpanStrategyByType != null && tokenSpanStrategyByType.containsKey(type)) {
            return tokenSpanStrategyByType.get(type);
        }
        return defaultTokenSpanStrategy;
    }

    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }

    /**
     * 设置按实体类型选择的 token 片段→候选串策略；未配置的类型使用 defaultTokenSpanStrategy。
     */
    public void setTokenSpanStrategyByType(Map<EntityType, TokenSpanToValueStrategy> tokenSpanStrategyByType) {
        this.tokenSpanStrategyByType = tokenSpanStrategyByType;
    }

    /**
     * 设置默认的 token 片段→候选串策略；与按类型 map 均为 null 时使用内置空格拼接。
     */
    public void setDefaultTokenSpanStrategy(TokenSpanToValueStrategy defaultTokenSpanStrategy) {
        this.defaultTokenSpanStrategy = defaultTokenSpanStrategy;
    }

    /**
     * 设置对候选串按类型做规则归一化的归一化器（如 CompositeEntityNormalizer）。
     */
    public void setEntityNormalizer(EntityNormalizer entityNormalizer) {
        this.entityNormalizer = entityNormalizer;
    }
}
