package com.ppwx.easysearch.qp.data;

import cn.hutool.core.date.StopWatch;
import com.ppwx.easysearch.qp.similarity.CosineSimilarity;
import com.ppwx.easysearch.qp.similarity.JaccardSimilarity;
import com.ppwx.easysearch.qp.similarity.LevenshteinDistanceSimilarity;
import com.ppwx.easysearch.qp.support.CustomStopChar;
import com.ppwx.easysearch.qp.support.FoundWord;
import com.ppwx.easysearch.qp.support.WordMatchTree;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className TextSourceConvertorTest
 * @description todo
 * @date 2024/10/11 19:02
 **/
public class TextSourceConvertorTest {

    @Test
    public void testArffSourceConvertorWorks() throws IOException {
        ArffSourceConvertor convertor = new ArffSourceConvertor("meta/category_meta.arff");
        convertor.readData();
        String relationName = convertor.getRelationName();
        System.out.println(relationName);
        convertor.getInstances().forEach(arffInstance -> {
            System.out.println(arffInstance.getValueByAttrName("id") + " = " +
                    arffInstance.getValueByAttrName("category_name"));
        });
    }

    @Test
    public void testSimilarity() throws IOException {
        LevenshteinDistanceSimilarity similarity = new LevenshteinDistanceSimilarity();
        String val = "iphone15";
        ArffSourceConvertor convertor = new ArffSourceConvertor("meta/product_meta.arff");
        convertor.readData();
        convertor.getInstances().forEach(arffInstance -> {
            Double score = similarity.apply(val, (String) arffInstance.getValueByAttrName("product_name"), CustomStopChar::isStopChar);
            if (score.compareTo(0.8D) >= 0) {
                System.out.println(score + " > " + (String) arffInstance.getValueByAttrName("product_name"));
            }
        });
    }

    @Test
    public void testSim() {
        LevenshteinDistanceSimilarity levenshteinDistanceSimilarity = LevenshteinDistanceSimilarity.getDefaultInstance();
        CosineSimilarity cosineSimilarity = new CosineSimilarity();
        JaccardSimilarity jaccardSimilarity = new JaccardSimilarity();
        System.out.println(levenshteinDistanceSimilarity.apply("苹果15", "苹果 iphone 15", CustomStopChar::isStopChar));
        System.out.println(cosineSimilarity.apply("苹果15", "苹果iphone15", CustomStopChar::isStopChar));
        System.out.println(jaccardSimilarity.apply("苹果15", "iphone 15 苹果", CustomStopChar::isStopChar));

        StopWatch watch = new StopWatch();
        watch.start();
        for (int i = 0; i < 10000; i++) {
            // 最快，效果也比较好
            levenshteinDistanceSimilarity.apply("苹果15", "苹果 iphone 15", CustomStopChar::isStopChar);
        }
        watch.stop();
        System.out.println("levenshteinDistanceSimilarity expend:" + watch.getTotalTimeMillis());

        watch.start();
        for (int i = 0; i < 10000; i++) {
            // 次之，结果比较模糊些
            cosineSimilarity.apply("苹果15", "苹果 iphone 15", CustomStopChar::isStopChar);
        }
        watch.stop();
        System.out.println("cosineSimilarity expend:" + watch.getTotalTimeMillis());

        watch.start();
        for (int i = 0; i < 10000; i++) {
            // 结果同LevenshteinDistanceSimilarity一致，但效率差一半
            jaccardSimilarity.apply("苹果15", "苹果 iphone 15", CustomStopChar::isStopChar);
        }
        watch.stop();
        System.out.println("jaccardSimilarity expend:" + watch.getTotalTimeMillis());
    }

    @Test
    public void testLevenshteinWorks() {
        LevenshteinDistanceSimilarity levenshteinDistanceSimilarity = LevenshteinDistanceSimilarity.getDefaultInstance();
        levenshteinDistanceSimilarity.apply("133456", "653321", character -> true);
    }

    @Test
    public void test() {
        WordMatchTree dicTree = new WordMatchTree();
        dicTree.addWord("苹果 15");
        List<FoundWord> words = dicTree.matchAllWords("苹果 15");
        System.out.println(words);
    }
}
