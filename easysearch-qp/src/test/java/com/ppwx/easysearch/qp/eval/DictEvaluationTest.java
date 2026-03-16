package com.ppwx.easysearch.qp.eval;

import com.ppwx.easysearch.qp.format.WordFormat;
import com.ppwx.easysearch.qp.format.WordFormatHalfWidth;
import com.ppwx.easysearch.qp.format.WordFormatSpecialChars;
import com.ppwx.easysearch.qp.format.WordFormats;
import com.ppwx.easysearch.qp.ner.Entity;
import com.ppwx.easysearch.qp.ner.EntityRecognizer;
import com.ppwx.easysearch.qp.ner.PriorityEntityRecognizer;
import com.ppwx.easysearch.qp.ner.recognizer.DictEntityRecognizer;
import com.ppwx.easysearch.qp.source.CompositeTextLineSource;
import com.ppwx.easysearch.qp.source.PathTextLineSource;
import com.ppwx.easysearch.qp.source.TextLineSource;
import com.ppwx.easysearch.qp.synonym.SynonymService;
import com.ppwx.easysearch.qp.tokenizer.*;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class DictEvaluationTest {

    /**
     * 字符过滤器
     */
    private WordFormat wordFormat;
    /**
     * 分词器
     */
    private Tokenizer tokenizer;
    /**
     * 实体识别器
     */
    private EntityRecognizer recognizer;
    /**
     * 同义词服务
     */
    private SynonymService synonymService;

    @Before
    public void setUp() throws IOException {
        // 创建分词器（工厂方法加载词典，实例隔离）
        tokenizer = new DualPathTokenizer(new CRFTokenizer());
        // 创建实体识别器（工厂方法加载词典，实例隔离）
        TextLineSource s1 = new PathTextLineSource("ner/brand_ner.txt");
        TextLineSource s2 = new PathTextLineSource("ner/category_ner.txt");
        TextLineSource s3 = new PathTextLineSource("ner/model_apple_ner.txt");
        TextLineSource s4 = new PathTextLineSource("ner/model_huawei_ner.txt");
        TextLineSource s5 = new PathTextLineSource("ner/model_mobile_ner.txt");
        TextLineSource s6 = new PathTextLineSource("ner/model_notepad_ner.txt");
        TextLineSource s7 = new PathTextLineSource("ner/model_other_ner.txt");
        TextLineSource s8 = new PathTextLineSource("ner/model_xiaomi_ner.txt");
        TextLineSource s9 = new PathTextLineSource("ner/condition_ner.txt");
        DictEntityRecognizer dictRecognizer = DictEntityRecognizer.fromSource(
                new CompositeTextLineSource(s1, s2, s3, s4, s5, s6, s7, s8, s9));
        recognizer = new PriorityEntityRecognizer(dictRecognizer);
        // 同义词归一
        synonymService = SynonymService.create("synonym/synonym.txt");
        // 字符过滤器
        wordFormat = WordFormats.chains(WordFormats.truncate(),
                new WordFormatHalfWidth(),
                new WordFormatSpecialChars(),
                WordFormats.ignoreCase());
    }

    @Test
    public void testDicWorks() {
        String query = "xiaomi17";
        query = wordFormat.format(new StringBuilder(query)).toString();
        query = synonymService.rewrite(query);
        List<Token> tokenize = tokenizer.tokenize(query);
        System.out.println(tokenize);
        Collection<Entity> entities = recognizer.extractEntities(query, tokenize);
        System.out.println(entities);
    }
}
