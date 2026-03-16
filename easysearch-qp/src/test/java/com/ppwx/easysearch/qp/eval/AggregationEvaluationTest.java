package com.ppwx.easysearch.qp.eval;

import cn.hutool.core.io.FileUtil;
import com.hankcs.hanlp.HanLP;
import com.ppwx.easysearch.qp.format.WordFormat;
import com.ppwx.easysearch.qp.format.WordFormatHalfWidth;
import com.ppwx.easysearch.qp.format.WordFormatSpecialChars;
import com.ppwx.easysearch.qp.format.WordFormats;
import com.ppwx.easysearch.qp.intervention.InterventionService;
import com.ppwx.easysearch.qp.ner.Entity;
import com.ppwx.easysearch.qp.ner.EntityRecognizer;
import com.ppwx.easysearch.qp.ner.recognizer.CRFEntityRecognizer;
import com.ppwx.easysearch.qp.synonym.SynonymService;
import com.ppwx.easysearch.qp.tokenizer.*;
import com.ppwx.easysearch.qp.util.StreamUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 分词、实体识别链路测试，输出为PKU格式的文件
 */
public class AggregationEvaluationTest {


    private Tokenizer tokenizer;

    private EntityRecognizer recognizer;

    private InterventionService interventionService;

    private SynonymService synonymService;

    private WordFormat wordFormat;

    @Before
    public void setUp() throws IOException {
        // hanLP config
        HanLP.Config.DEBUG = true;

        wordFormat = WordFormats.chains(WordFormats.truncate(),
                new WordFormatHalfWidth(), new WordFormatSpecialChars(), WordFormats.ignoreCase());
        // tokenizer
        CRFTokenizer crfTokenizer = new CRFTokenizer();
        crfTokenizer.setCwsModelPath("data/model/vocab_cws_crf.txt.bin");
        crfTokenizer.setPosModelPath("data/model/vocab_pos_crf.txt.bin");
        DictTokenizer dictTokenizer = DictTokenizer.fromPath("dictionary/dict-core.txt");
        Tokenizer composite = new CRFCompositeTokenizer(crfTokenizer, dictTokenizer);
        tokenizer = new DualPathTokenizer(composite, crfTokenizer.getTagger());
        // entity recognizer
        CRFEntityRecognizer crfEntityRecognizer = new CRFEntityRecognizer();
        crfEntityRecognizer.setModelPath("data/model/vocab_ner_crf.txt.bin");
        recognizer = crfEntityRecognizer;
        // synonymService
        synonymService = SynonymService.create("synonym/synonym.txt");
        // invention
        interventionService = InterventionService.create(
                "intervention/term.txt", "intervention/sentence.txt");

    }

    @Test
    public void test() throws IOException {
        String query = "vivo X200 Ultra";
        System.out.println(analyze(query));
    }

    private String analyze(String query) {
        query = wordFormat.format(new StringBuilder(query)).toString();
        query = interventionService.rewrite(query);
        query = synonymService.rewrite(query);
        List<Token> tokenize = tokenizer.tokenize(query);
        //System.out.println(tokenize);

        Collection<Entity> entities = recognizer.extractEntities(query, tokenize);
        //System.out.println(entities);

        String line = toPKULine(tokenize, entities);
        //System.out.println(line);
        return line;
    }

    @Test
    public void testBatchHandle() throws IOException {
        File outFile = FileUtil.file("export/high-pred-dt4.txt");
        String path = "D:/projects/yanxuan/文档/搜推/词库采集/评估/high-origin.txt";
        try(InputStream vocabStream = FileUtil.getInputStream(path)) {
            StreamUtil.readUtf8Lines(vocabStream, line -> {
                String result = analyze(line);
                System.out.println(result);
                FileUtil.appendUtf8Lines(Collections.singletonList(result), outFile);
            });
        }
    }

    @Test
    public void testRecognize() throws IOException {
        String query = "vivo X200 Ultra";
        String analyze = analyze(query);
        System.out.println(analyze);
    }


    private static final String DEFAULT_POS = "NN";
    private static final String SEP = " ";

    /**
     * 将分词结果与实体识别结果合并，转换为 PKU 格式的一行。
     * 普通词：词语/词性；多词实体：[ 词1/词性1 词2/词性2 ... ]/实体类型
     * 与 CWSEvaluation 及 199801 标注格式兼容，便于后续评估。
     *
     * @param tokenize 分词结果（含 startIndex/endIndex）
     * @param entities 实体集合（含 startOffset/endOffset、type）
     * @return 一行 PKU 字符串，段之间用空格分隔
     */
    private String toPKULine(List<Token> tokenize, Collection<Entity> entities) {
        StringBuilder bd = new StringBuilder();
        for (Token token : tokenize) {
            int startIndex = token.getStartIndex();
            int endIndex = token.getEndIndex();
            String text = token.getText();
            String type = token.getType();
            Entity entity = matchEntity(entities, startIndex, endIndex);
            // not match entity
            if (entity == null) {
                bd.append(text).append("/").append(type == null ? DEFAULT_POS : type).append(SEP);
                continue;
            }
            // single word
            if (isSingleWord(entity, startIndex, endIndex)) {
                bd.append(text).append("/").append(entity.getType().name()).append(SEP);
                continue;
            }
            // multi words
            if (isBegin(entity, startIndex, endIndex)) {
                bd.append("[");
            }
            bd.append(text).append("/").append(type == null ? DEFAULT_POS : type).append(SEP);
            if (isEnd(entity, startIndex, endIndex)) {
                bd.deleteCharAt(bd.length() - 1);
                bd.append("]/").append(entity.getType().name()).append(SEP);
            }
        }
        bd.deleteCharAt(bd.length() - 1);
        return bd.toString();
    }

    private boolean isSingleWord(Entity entity, int startIndex, int endIndex) {
        return entity.getStartOffset() == startIndex && entity.getEndOffset() == endIndex;
    }

    private boolean isEnd(Entity entity, int startIndex, int endIndex) {
        return entity.getEndOffset() == endIndex;
    }

    private boolean isBegin(Entity entity, int startIndex, int endIndex) {
        return entity.getStartOffset() == startIndex;
    }

    private Entity matchEntity(Collection<Entity> entities, int startIndex, int endIndex) {
        for (Entity entity : entities) {
            if (entity.getStartOffset() <= startIndex && entity.getEndOffset() >= endIndex) {
                return entity;
            }
        }
        return null;
    }

}
