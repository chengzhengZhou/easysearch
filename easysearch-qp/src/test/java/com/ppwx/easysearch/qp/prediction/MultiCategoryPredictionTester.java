package com.ppwx.easysearch.qp.prediction;

import com.ppwx.easysearch.qp.data.ArffSourceConvertor;
import com.ppwx.easysearch.qp.ner.NamedEntityRecognition;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className MultiCategoryPredictionTester
 * @description MultiCategoryPrediction测试类
 * @date 2024/11/1 17:40
 **/
public class MultiCategoryPredictionTester {

    @Test
    public void testMultiCategoryPredictionWorks() throws IOException {
        NamedEntityRecognition recognition = new NamedEntityRecognition("ner/brand_1.dic",
                "ner/category_2.dic", "ner/product_4.dic", "ner/quality_3.dic", "ner/sku_0.dic");
        ArffSourceConvertor categorySource = new ArffSourceConvertor("meta/category_meta.arff");
        categorySource.readData();
        ArffSourceConvertor brandSource = new ArffSourceConvertor("meta/brand_meta.arff");
        brandSource.readData();
        ArffSourceConvertor productSource = new ArffSourceConvertor("meta/product_meta.arff");
        productSource.readData();

        MultiCategoryPrediction prediction = new MultiCategoryPrediction(recognition, categorySource, brandSource, productSource);
        List<Category> categories = prediction.predict("华为平板");
        System.out.println(categories);
    }

}
