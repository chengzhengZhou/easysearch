package com.ppwx.easysearch.qp;

import com.hankcs.hanlp.HanLP;
import com.ppwx.easysearch.qp.data.ArffSourceConvertor;
import com.ppwx.easysearch.qp.data.DictionaryResourceLoader;
import com.ppwx.easysearch.qp.data.MetaResourceLoader;
import com.ppwx.easysearch.qp.format.*;
import com.ppwx.easysearch.qp.ner.DefaultEntityIdentityMapper;
import com.ppwx.easysearch.qp.ner.EntityRecognizer;
import com.ppwx.easysearch.qp.ner.ThreeCEntityRecognizerFactory;
import com.ppwx.easysearch.qp.prediction.Category;
import com.ppwx.easysearch.qp.prediction.NerCategoryPrediction;
import com.ppwx.easysearch.qp.tokenizer.HanlpSegmentation;
import com.ppwx.easysearch.qp.tokenizer.ThreeCTokenizer;
import org.junit.Test;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StopWatch;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

public class QPStarterV2 {

    ThreeCTokenizer tokenizer;

    EntityRecognizer recognizer;

    WordFormat wordFormat;

    NerCategoryPrediction prediction;

    public static void main(String[] args) throws IOException {
        PrintStream out = System.out;
        out.println("程序加载...");
        QPStarterV2 starter = new QPStarterV2();
        out.println(">>>请输入搜索词：");

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            String next = scanner.nextLine();
            if ("quit".equals(next)) {
                scanner.close();
            } else {
                StopWatch watch = new StopWatch();
                watch.start();
                String word = next;
                word = starter.wordFormat.format(new StringBuilder(word)).toString();
                System.out.println(">>>预处理：" + word);

                /*List<Token> tokenize = starter.tokenizer.tokenize(word);
                System.out.println(">>>分词结果：" + tokenize);

                Collection<Entity> entities = starter.recognizer.extractEntities(word, tokenize);
                out.println(">>>实体识别结果：" + entities);*/

                List<Category> metas = starter.prediction.predict(word);
                out.println(">>>它可能对应类目为：");
                if (CollectionUtils.isEmpty(metas)) {
                    out.print("未对应上任何类目");
                } else {
                    metas.forEach(category -> {
                        System.out.printf("%s > %s：%s", category.getScore(), starter.getType(category.getType()), category.getCategoryName());
                        if (category.getParent() != null) {
                            category = category.getParent();
                            System.out.printf(" > %s：%s", starter.getType(category.getType()), category.getCategoryName());
                        }
                        if (category.getParent() != null) {
                            category = category.getParent();
                            System.out.printf(" > %s：%s", starter.getType(category.getType()), category.getCategoryName());
                        }
                        System.out.println();
                    });
                }
                watch.stop();
                out.println("-------------------------------" + watch.getTotalTimeMillis() + "------------------------------------");
            }
        }
    }

    public QPStarterV2() throws IOException {
        load();
    }

    private void load() throws IOException {
        HanlpSegmentation segmentation = new HanlpSegmentation();
        tokenizer = new ThreeCTokenizer(segmentation);

        DefaultEntityIdentityMapper mapper = new DefaultEntityIdentityMapper();
        recognizer = ThreeCEntityRecognizerFactory.createEntityRecognizer(mapper);

        wordFormat = WordFormats.chains(WordFormats.truncate(),
            new WordFormatHalfWidth(),
            new WordFormatSpecialChars(),
            WordFormats.ignoreCase());

        prediction = new NerCategoryPrediction(recognizer, tokenizer);

        HanLP.Config.enableDebug(true);
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
        metaLoader.addObserver(prediction);
        metaLoader.loadResources();
    }

    public String getType(String type) {
        if ("brand".equals(type)) {
            return "品牌";
        } else if ("category".equals(type)) {
            return "品类";
        } else if ("model".equals(type)) {
            return "机型属性";
        } else if ("quality".equals(type)){
            return "成色";
        } else {
            return "其他";
        }
    }

    @Test
    public void testModelMatches() throws IOException {
        QPStarterV2 qpStarterV2 = new QPStarterV2();
        qpStarterV2.prediction.predict("hello world");
        HanLP.Config.enableDebug(false);

        ArffSourceConvertor convertor = new ArffSourceConvertor("meta/product_meta.arff");
        convertor.readData();

        AtomicInteger count = new AtomicInteger();
        AtomicInteger badCount = new AtomicInteger();

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        convertor.getInstances().forEach(arffInstance -> {
            String productName = (String) arffInstance.getValueByAttrName("product_name");
            List<Category> cats = qpStarterV2.prediction.predict(wordFormat.format(new StringBuilder(productName)).toString());
            Integer modelId = (Integer) arffInstance.getValueByAttrName("id");
            Optional<Category> optional = cats.stream().filter(category -> "model".equals(category.getType())).findFirst();
            if (optional.isPresent() && optional.get().getCategoryId().equals(modelId)) {
                count.incrementAndGet();
            } else {
                badCount.incrementAndGet();
                System.out.println(">>>模型匹配失败：" + arffInstance.getValueByAttrName("product_name"));
            }
        });
        stopWatch.stop();
        System.out.println("耗时：" + stopWatch.getTotalTimeMillis());
        System.out.println("未匹配上的数量：" + badCount.get());
        System.out.println("匹配上的数量：" + count.get());
    }
}
