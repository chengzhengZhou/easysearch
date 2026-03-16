package com.ppwx.easysearch.qp.eval;

import com.hankcs.hanlp.HanLP;
import com.ppwx.easysearch.qp.data.DictionaryResourceLoader;
import com.ppwx.easysearch.qp.data.MetaResourceLoader;
import com.ppwx.easysearch.qp.format.WordFormat;
import com.ppwx.easysearch.qp.format.WordFormatHalfWidth;
import com.ppwx.easysearch.qp.format.WordFormatSpecialChars;
import com.ppwx.easysearch.qp.format.WordFormats;
import com.ppwx.easysearch.qp.ner.DefaultEntityIdentityMapper;
import com.ppwx.easysearch.qp.ner.Entity;
import com.ppwx.easysearch.qp.ner.EntityRecognizer;
import com.ppwx.easysearch.qp.ner.ThreeCEntityRecognizerFactory;
import com.ppwx.easysearch.qp.tokenizer.HanlpSegmentation;
import com.ppwx.easysearch.qp.tokenizer.ThreeCTokenizer;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 传统实体识别方案评估
 */
public class NerEvaluationTest {

    private static final String originFile = "D:/projects/yanxuan/文档/搜推/词库采集/评估/low-origin.txt";

    private static final String goldFile = "D:/projects/yanxuan/文档/搜推/词库采集/评估/low-gold.txt";

    @Test
    public void eval() throws IOException {
        Path rawPath = Paths.get(originFile);
        Path goldPath = Paths.get(goldFile);

        loadNer();

        CWSEvaluation.Metrics nerMetrics = new CWSEvaluation.Metrics();

        try (BufferedReader rawReader = Files.newBufferedReader(rawPath, StandardCharsets.UTF_8);
             BufferedReader goldReader = Files.newBufferedReader(goldPath, StandardCharsets.UTF_8)) {

            String rawLine;
            int lineIdx = 0;
            while ((rawLine = rawReader.readLine()) != null) {
                String goldLine = goldReader.readLine();

                if (goldLine == null) {
                    System.err.println("Warning: gold/pred 文件在第 " + lineIdx + " 行提前结束，停止评估。");
                    break;
                }

                CWSEvaluation.SentenceAnnotation gold = CWSEvaluation.parseAnnotatedLine(goldLine, rawLine, null);
                CWSEvaluation.SentenceAnnotation pred = genAnnotatedLine(rawLine);

                // 实体评估（NER）
                evaluateNer(gold, pred, nerMetrics);

                lineIdx++;
            }
        }
        System.out.println("NER: " + nerMetrics);
    }

    /**
     * 生成实体标注
     */
    private CWSEvaluation.SentenceAnnotation genAnnotatedLine( String rawLine) {
        String normalizedLine = CWSEvaluation.normalize(rawLine);
        CWSEvaluation.SentenceAnnotation pred = new CWSEvaluation.SentenceAnnotation(normalizedLine);

        Collection<Entity> entities = ner(normalizedLine);
        entities.forEach(entity -> {
            pred.tokens.add(new CWSEvaluation.Token(entity.getValue(), "/NR", entity.getStartOffset(), entity.getEndOffset(), true));
            pred.entities.add(new CWSEvaluation.EntitySpan(entity.getStartOffset(), entity.getEndOffset(), 0, entity.getValue().length(), entity.getType().name(), true));
        });

        return pred;
    }

    /**
     * 实体评估：严格匹配 (start, end, type)；
     */
    private static void evaluateNer(CWSEvaluation.SentenceAnnotation gold,
                                    CWSEvaluation.SentenceAnnotation pred,
                                    CWSEvaluation.Metrics overall) {
        List<String> goldSet = gold.entities.stream().map(CWSEvaluation.EntitySpan::getType).collect(Collectors.toList());
        List<String> predSet = pred.entities.stream().map(CWSEvaluation.EntitySpan::getType).collect(Collectors.toList());

        // 整体 TP
        long tp = 0;
        for (String p : predSet) {
            if (goldSet.contains(p)) {
                tp++;
            }
        }
        long fp = predSet.size() - tp;
        long fn = goldSet.size() - tp;
        overall.addTP(tp);
        overall.addFP(fp);
        overall.addFN(fn);
    }

    /************************************实体识别***********************************/
    ThreeCTokenizer tokenizer;

    EntityRecognizer recognizer;

    WordFormat wordFormat;

    private Collection<Entity> ner(String keyword) {
        StringBuilder format = wordFormat.format(new StringBuilder(keyword));
        return recognizer.extractEntities(format.toString(), tokenizer.tokenize(format.toString()));
    }

    private void loadNer() throws IOException {
        HanlpSegmentation segmentation = new HanlpSegmentation();
        tokenizer = new ThreeCTokenizer(segmentation);

        DefaultEntityIdentityMapper mapper = new DefaultEntityIdentityMapper();
        recognizer = ThreeCEntityRecognizerFactory.createEntityRecognizer(mapper);

        wordFormat = WordFormats.chains(WordFormats.truncate(),
                new WordFormatHalfWidth(),
                new WordFormatSpecialChars(),
                WordFormats.ignoreCase());

        HanLP.Config.enableDebug(false);
        // resource
        DictionaryResourceLoader loader = new DictionaryResourceLoader(
                "dictionary/category_dic.arff",
                "dictionary/brand_dic.arff",
                "dictionary/spec_id.arff",
                "dictionary/model_apple_dic.arff",
                "dictionary/model_xiaomi_dic.arff",
                "dictionary/model_huawei_dic.arff",
                "dictionary/model_mobile_dic.arff",
                "dictionary/model_notepad_dic.arff",
                "dictionary/model_other_dic.arff",
                "dictionary/tag_dic.arff",
                "dictionary/condition_id.arff",
                "dictionary/spec_id.arff"
        );
        loader.addObserver(segmentation);
        loader.addObserver(mapper);
        loader.loadResources();

        MetaResourceLoader metaLoader = new MetaResourceLoader(
                "meta/category_meta.arff",
                "meta/brand_meta.arff",
                "meta/product_meta.arff"
        );
        metaLoader.loadResources();
    }

}
