package com.ppwx.easysearch.qp.data;

import com.hankcs.hanlp.HanLP;
import com.ppwx.easysearch.qp.format.WordFormat;
import com.ppwx.easysearch.qp.format.WordFormatHalfWidth;
import com.ppwx.easysearch.qp.format.WordFormatSpecialChars;
import com.ppwx.easysearch.qp.format.WordFormats;
import com.ppwx.easysearch.qp.ner.DefaultEntityIdentityMapper;
import com.ppwx.easysearch.qp.ner.EntityRecognizer;
import com.ppwx.easysearch.qp.ner.EntityType;
import com.ppwx.easysearch.qp.ner.ThreeCEntityRecognizerFactory;
import com.ppwx.easysearch.qp.prediction.Category;
import com.ppwx.easysearch.qp.prediction.NerCategoryPrediction;
import com.ppwx.easysearch.qp.tokenizer.HanlpSegmentation;
import com.ppwx.easysearch.qp.tokenizer.ThreeCTokenizer;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DataCheck {

    ThreeCTokenizer tokenizer;

    EntityRecognizer recognizer;

    WordFormat wordFormat;

    NerCategoryPrediction prediction;

    @Before
    public void load() throws IOException {
        HanlpSegmentation segmentation = new HanlpSegmentation();
        tokenizer = new ThreeCTokenizer(segmentation);

        DefaultEntityIdentityMapper mapper = new DefaultEntityIdentityMapper();
        recognizer = ThreeCEntityRecognizerFactory.createEntityRecognizer(mapper);

        wordFormat = WordFormats.chains(WordFormats.truncate(),
            new WordFormatHalfWidth(),
            new WordFormatSpecialChars(),
            WordFormats.ignoreCase());

        prediction = new NerCategoryPrediction(recognizer, tokenizer);

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
        metaLoader.addObserver(prediction);
        metaLoader.loadResources();
    }

    @Test
    public void check() throws IOException {
        Map<Integer, String> brandMap = new HashMap<>(2048);
        Map<Integer, String> categoryMap = new HashMap<>(256);
        ArffSourceConvertor brandArff = new ArffSourceConvertor("meta/brand_meta.arff");
        brandArff.readData();
        brandArff.getInstances().forEach(item -> {
            final Integer id = (Integer) item.getValueByAttrName("id");
            final String brandNameCh = (String) item.getValueByAttrName("brand_name_ch");
            brandMap.put(id, brandNameCh);
        });
        ArffSourceConvertor categoryArff = new ArffSourceConvertor("meta/category_meta.arff");
        categoryArff.readData();
        categoryArff.getInstances().forEach(item -> {
            final Integer id = (Integer) item.getValueByAttrName("id");
            final String categoryName = (String) item.getValueByAttrName("category_name");
            categoryMap.put(id, categoryName);
        });


        ArffSourceConvertor productArff = new ArffSourceConvertor("meta/product_meta.arff");
        productArff.readData();
        productArff.getInstances().forEach(item -> {
            final String productName = (String) item.getValueByAttrName("product_name");
            final Integer brandId = (Integer) item.getValueByAttrName("brand_id");
            final Integer categoryId = (Integer) item.getValueByAttrName("category_id");
            if (!brandMap.containsKey(brandId)) {
                System.out.println("品牌缺失：" + productName);
            }
            if (!categoryMap.containsKey(categoryId)) {
                System.out.println("品类缺失：" + productName);
            }
        });
    }

    @Test
    public void test() throws IOException {
        ArffSourceConvertor productArff = new ArffSourceConvertor("meta/product_meta.arff");
        productArff.readData();
        productArff.getInstances().forEach(item -> {
            final String productName = (String) item.getValueByAttrName("product_name");
            final List<Category> categories = prediction.predict(productName);
            List<Category> models = categories.stream()
                .filter(category -> EntityType.MODEL.name().equalsIgnoreCase(category.getType()))
                .collect(Collectors.toList());
            for (Category model : models) {
                if (model.getParent() == null || model.getParent().getParent() == null) {
                    System.out.println(model);
                }
            }
        });
    }

}
