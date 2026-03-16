package com.ppwx.easysearch.qp.eval;

import cn.hutool.core.date.StopWatch;
import com.ppwx.easysearch.qp.data.ArffSourceConvertor;
import com.ppwx.easysearch.qp.format.WordFormat;
import com.ppwx.easysearch.qp.format.WordFormatHalfWidth;
import com.ppwx.easysearch.qp.format.WordFormatSpecialChars;
import com.ppwx.easysearch.qp.format.WordFormats;
import com.ppwx.easysearch.qp.ner.EntityRecognizer;
import com.ppwx.easysearch.qp.ner.MergeStrategy;
import com.ppwx.easysearch.qp.ner.PriorityEntityRecognizer;
import com.ppwx.easysearch.qp.ner.recognizer.CRFEntityRecognizer;
import com.ppwx.easysearch.qp.ner.recognizer.DictEntityRecognizer;
import com.ppwx.easysearch.qp.source.CompositeTextLineSource;
import com.ppwx.easysearch.qp.source.PathTextLineSource;
import com.ppwx.easysearch.qp.source.TextLineSource;
import com.ppwx.easysearch.qp.synonym.SynonymService;
import com.ppwx.easysearch.qp.tokenizer.CRFTokenizer;
import com.ppwx.easysearch.qp.tokenizer.DualPathTokenizer;
import com.ppwx.easysearch.qp.tokenizer.Token;
import com.ppwx.easysearch.qp.tokenizer.Tokenizer;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CWSPerformanceTest {

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

    public void dictSetUp() throws IOException {
        // 创建分词器（工厂方法加载词典，实例隔离）
        CRFTokenizer crfTokenizer = new CRFTokenizer();
        tokenizer = new DualPathTokenizer(crfTokenizer, crfTokenizer.getTagger());
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

    public void crfSetUp() throws IOException {
        // 创建分词器（工厂方法加载词典，实例隔离）
        CRFTokenizer crfTokenizer = new CRFTokenizer();
        tokenizer = new DualPathTokenizer(crfTokenizer, crfTokenizer.getTagger());
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
        recognizer = new PriorityEntityRecognizer(MergeStrategy.DICT_FIRST, new CRFEntityRecognizer(), dictRecognizer);
        // 同义词归一
        synonymService = SynonymService.create("synonym/synonym.txt");
        // 字符过滤器
        wordFormat = WordFormats.chains(WordFormats.truncate(),
                new WordFormatHalfWidth(),
                new WordFormatSpecialChars(),
                WordFormats.ignoreCase());
    }

    /**
     * DICT: avg: 0.2646966362779ms
     * CRF: avg: 0.2964476579691921ms
     */
    @Test
    public void test() throws IOException {
        //dictSetUp();
        crfSetUp();
        ArffSourceConvertor productArff = new ArffSourceConvertor("meta/product_meta.arff");
        productArff.readData();
        // warm up
        String query = "iphone15";
        query = wordFormat.format(new StringBuilder(query)).toString();
        query = synonymService.rewrite(query);
        recognizer.extractEntities(query, tokenizer.tokenize(query));
        double count = productArff.getInstances().size();
        System.out.println("warm up done, load count: " + count);

        StopWatch stopWatch = StopWatch.create("CWS Performance Test");
        stopWatch.start();

        productArff.getInstances().forEach(instance -> {
            String productName = instance.getValueByAttrName("product_name").toString();
            //productName = wordFormat.format(new StringBuilder(productName)).toString();
            //productName = synonymService.rewrite(productName);
            List<Token> tokenize = tokenizer.tokenize(productName);
            recognizer.extractEntities(productName, tokenize);
        });

        stopWatch.stop();
        System.out.println(stopWatch.prettyPrint(TimeUnit.MILLISECONDS));
        System.out.println("avg: " + (stopWatch.getTotalTimeMillis() / count) + "ms");
    }

}
