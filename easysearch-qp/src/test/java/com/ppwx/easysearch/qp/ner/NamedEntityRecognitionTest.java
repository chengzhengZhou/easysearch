package com.ppwx.easysearch.qp.ner;

import com.ppwx.easysearch.qp.support.FoundWord;
import com.ppwx.easysearch.qp.support.WordMatchTree;
import org.apache.commons.text.similarity.CosineDistance;
import org.apache.commons.text.similarity.JaccardSimilarity;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className NamedEntityRecognitionTest
 * @description todo
 * @date 2024/10/9 17:54
 **/
public class NamedEntityRecognitionTest {

    @Test
    public void testNamedEntityRecognitionWorks() {
        NamedEntityRecognition namedEntityRecognition = new NamedEntityRecognition(
                "ner/sku_0.dic", "ner/brand_1.dic",
                "ner/category_2.dic", "ner/product_4.dic");
        System.out.println(namedEntityRecognition.recognize("影驰GeForceRTX2070Super大将"));
        System.out.println(namedEntityRecognition.recognize("影驰 GeForceRTX 2070"));
        System.out.println(namedEntityRecognition.recognize("64g iphone 16 苹果"));
        System.out.println(namedEntityRecognition.recognize("苹果"));
        System.out.println(namedEntityRecognition.recognize("macbook pro"));
        System.out.println(namedEntityRecognition.recognize("小米15"));
        System.out.println(namedEntityRecognition.recognize("iphone 15 512g 苹果"));
        System.out.println(namedEntityRecognition.recognize("12.9英寸"));
        System.out.println(namedEntityRecognition.recognize("苹果苹果"));
        System.out.println(namedEntityRecognition.recognize("Redmi Turbo 3"));
        System.out.println(namedEntityRecognition.recognize("苹果13pro原装"));
        System.out.println(namedEntityRecognition.recognize("华为 Mate 30 Pro (5G版)"));
        System.out.println(namedEntityRecognition.recognize("苹果13pro"));
        System.out.println(namedEntityRecognition.recognize("华硕 天选2"));
        System.out.println(namedEntityRecognition.recognize("苹果 iPhone XS Max"));
        System.out.println(namedEntityRecognition.recognize("小米10至尊"));
        System.out.println(namedEntityRecognition.recognize("荣耀 Honor+"));
        System.out.println(namedEntityRecognition.recognize("苹果 iPhone 14 Plus"));
        System.out.println(namedEntityRecognition.recognize("苹果 AirPods Pro (第二代)"));
        System.out.println(namedEntityRecognition.recognize("荣耀 Magic6"));
        System.out.println(namedEntityRecognition.recognize("华为手机"));
        System.out.println(namedEntityRecognition.recognize("GPD Win 3"));
        System.out.println(namedEntityRecognition.recognize("华为 FreeBuds 4i"));
        System.out.println(namedEntityRecognition.recognize("msi微星 泰坦16 A13V"));
        System.out.println(namedEntityRecognition.recognize("努比亚 红魔 8 Pro"));
        System.out.println(namedEntityRecognition.recognize("苹果键盘"));
        System.out.println(namedEntityRecognition.recognize("苹果15por白色"));
        System.out.println(namedEntityRecognition.recognize("华为x3"));
        System.out.println(namedEntityRecognition.recognize("15pro"));
        System.out.println(namedEntityRecognition.recognize("K70至尊"));
        System.out.println(namedEntityRecognition.recognize("微星 rtx 2060 super ventus gp"));
    }

    @Test
    public void test() {
        WordMatchTree tree = new WordMatchTree();
        tree.addWord("大");
        tree.addWord("大土豆");
        tree.addWord("土豆");
        tree.addWord("出锅");
        tree.addWord("刚出锅");
        String text = "我有一颗大土豆，刚出锅的";
        List<String> matchAll = tree.matchAll(text);
        Assert.assertEquals(matchAll.toString(), "[大土豆, 刚出锅]");

        List<FoundWord> foundWords = tree.matchAllWords(text);
        for (FoundWord foundWord : foundWords) {
            System.out.println(foundWord.getFoundWord() + " " + foundWord.getStartIndex() + "-" + foundWord.getEndIndex());
        }
    }

    @Test
    public void testPattern() {
        Pattern compile = Pattern.compile("^\\w+_\\d+.dic$");
        Matcher matcher = compile.matcher("category_0223.dic");
        System.out.println(matcher.matches());
    }

    @Test
    public void testSimilarity() {
        NamedEntityRecognition namedEntityRecognition = new NamedEntityRecognition("ner/brand_1.dic",
                "ner/category_2.dic", "ner/product_4.dic", "ner/quality_3.dic", "ner/sku_0.dic");
        String origin = namedEntityRecognition.recognize("苹果iphone16").stream().map(FindEntity::getTerm).collect(Collectors.joining());
        String target = "苹果iphone16";
        JaccardSimilarity jaccardSimilarity = new JaccardSimilarity();
        Double score = jaccardSimilarity.apply(origin, target);
        System.out.println(score);

        CosineDistance cosineSimilarity = new CosineDistance();
        score = cosineSimilarity.apply(origin, target);
        System.out.println(score);
    }

    @Test
    public void testMultiNamedEntityRecognitionWorks() {
        NamedEntityRecognition namedEntityRecognition = new NamedEntityRecognition(
                "ner/sku_0.dic", "ner/brand_1.dic",
                "ner/category_2.dic", "ner/quality_3.dic", "ner/product_4.dic");
        System.out.println(namedEntityRecognition.recognize("99新苹果 iphone 13256g"));
    }
}
