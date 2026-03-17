package com.ppwx.easysearch.qp.tokenizer;

import com.hankcs.hanlp.model.crf.CRFPOSTagger;
import com.hankcs.hanlp.model.crf.CRFSegmenter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 基于 CRF 模型的分词与词性标注器。
 * 使用 HanLP 的 {@link CRFSegmenter} 分词、{@link CRFPOSTagger} 词性标注。
 */
public class CRFTokenizer implements Tokenizer {

    private static final Logger log = LoggerFactory.getLogger(CRFTokenizer.class);

    public static final String CRF_CWS_MODEL = "data/model/vocab_cws_crf.txt.bin";
    public static final String CRF_POS_MODEL = "data/model/vocab_pos_crf.txt.bin";

    private static final String DEFAULT_POS = "NN";

    private volatile CRFSegmenter segmenter;
    private volatile CRFPOSTagger tagger;
    private volatile boolean initFailed;

    private String cwsModelPath = CRF_CWS_MODEL;

    private String posModelPath = CRF_POS_MODEL;

    private CRFSegmenter getOrCreateSegmenter() {
        if (initFailed) {
            return null;
        }
        if (segmenter == null) {
            synchronized(this) {
                if (segmenter == null) {
                    try {
                        segmenter = loadSegmenter();
                    } catch (IOException e) {
                        log.warn("Failed to load CRF segmenter: {}", e.getMessage());
                        initFailed = true;
                    }
                }
            }
        }
        return segmenter;
    }

    private CRFPOSTagger getOrCreateTagger() {
        if (initFailed) {
            return null;
        }
        if (tagger == null) {
            synchronized (this) {
                if (tagger == null) {
                    try {
                        tagger = loadTagger();
                    } catch (IOException e) {
                        log.warn("Failed to load CRF POS tagger: {}", e.getMessage());
                        initFailed = true;
                    }
                }
            }
        }
        return tagger;
    }

    private CRFSegmenter loadSegmenter() throws IOException {
        /*Path tempFile = Files.createTempFile("hanlp_cws_", ".bin");
        try (InputStream is = StreamUtil.getResourceStream(cwsModelPath)) {
            Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }*/
        return new CRFSegmenter(cwsModelPath);
    }

    private CRFPOSTagger loadTagger() throws IOException {
        /*Path tempFile = Files.createTempFile("hanlp_pos_", ".bin");
        try (InputStream is = StreamUtil.getResourceStream(posModelPath)) {
            Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }*/
        return new CRFPOSTagger(posModelPath);
    }

    /**
     * CRF 分词 + 词性标注。
     *
     * @param text 输入文本
     * @return 分词结果（含词性与偏移）
     */
    @Override
    public List<Token> tokenize(String text) {
        if (StringUtils.isBlank(text)) {
            return Collections.emptyList();
        }
        CRFSegmenter seg = getOrCreateSegmenter();
        CRFPOSTagger posTagger = getOrCreateTagger();
        if (seg == null || posTagger == null) {
            return Collections.emptyList();
        }

        List<String> words;
        String[] posArray;
        try {
            words = seg.segment(text);
            if (words == null || words.isEmpty()) {
                return Collections.emptyList();
            }
            posArray = posTagger.tag(words);
            if (posArray == null || posArray.length != words.size()) {
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.debug("CRF tokenize error: {}", e.getMessage());
            return Collections.emptyList();
        }

        return buildTokens(text, words, posArray);
    }

    /**
     * 根据原文顺序匹配词列表，构建带 startIndex/endIndex 的 Token 列表。
     */
    private static List<Token> buildTokens(String text, List<String> words, String[] posArray) {
        List<Token> result = new ArrayList<>(words.size());
        int offset = 0;
        int len = text.length();

        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            if (word == null || word.isEmpty()) {
                continue;
            }
            int index = text.indexOf(word, offset);
            if (index < 0) {
                log.debug("Word not found in text at offset {}: '{}'", offset, word);
                return Collections.emptyList();
            }
            int endIndex = index + word.length();
            if (endIndex > len) {
                return Collections.emptyList();
            }
            String type = (posArray[i] != null && !posArray[i].trim().isEmpty())
                    ? posArray[i].trim() : DEFAULT_POS;
            result.add(Token.builder()
                    .text(word)
                    .type(type)
                    .startIndex(index)
                    .endIndex(endIndex)
                    .confidence(1.0)
                    .build());
            offset = endIndex;
        }
        return result;
    }

    /**
     * CRF 词性标注。
     *
     * @param text 输入文本
     * @return 词性标注结果
     */
    public String[] tag(String[] text) {
        CRFPOSTagger posTagger = getOrCreateTagger();
        if (posTagger == null) {
            return new String[0];
        }
        return posTagger.tag(text);
    }

    public CRFPOSTagger getTagger() {
        return getOrCreateTagger();
    }

    public CRFSegmenter getSegmenter() {
        return getOrCreateSegmenter();
    }

    public void setCwsModelPath(String cwsModelPath) {
        this.cwsModelPath = cwsModelPath;
    }

    public void setPosModelPath(String posModelPath) {
        this.posModelPath = posModelPath;
    }
}
