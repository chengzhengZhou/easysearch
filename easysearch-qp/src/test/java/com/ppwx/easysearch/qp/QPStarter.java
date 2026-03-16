package com.ppwx.easysearch.qp;

import com.ppwx.easysearch.qp.data.ArffSourceConvertor;
import com.ppwx.easysearch.qp.ner.FindEntity;
import com.ppwx.easysearch.qp.ner.NamedEntityRecognition;
import com.ppwx.easysearch.qp.prediction.Category;
import com.ppwx.easysearch.qp.prediction.MultiCategoryPrediction;
import com.ppwx.easysearch.qp.synonym.SynonymMatcher;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StopWatch;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className QPStarter
 * @description 示例演示
 * @date 2024/10/25 15:43
 **/
public class QPStarter {

    public QPStarter() throws IOException {
        load();
    }

    public static void main(String[] args) throws IOException {
        PrintStream out = System.out;
        out.println("程序加载...");
        QPStarter starter = new QPStarter();
        out.println(">>>请输入搜索词：");

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            String next = scanner.nextLine();
            if ("quit".equals(next)) {
                scanner.close();
            } else {
                StopWatch watch = new StopWatch();
                watch.start();
                String origin = next;
                next = starter.synonymMatch(origin);
                if (!StringUtils.equals(origin, next)) {
                    out.println(">>>同义词替换：" + next);
                }
                List<FindEntity> entities = starter.recognize(next);
                out.println(">>>实体识别结果：");
                if (CollectionUtils.isEmpty(entities)) {
                    out.println("未识别到任何实体");
                } else {
                    entities.forEach(entry -> out.println(entry.getTerm() + " > " + starter.getType(entry.getType())));
                }

                List<Category> metas = starter.mapping(next);
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

    public String getType(String type) {
        if ("brand".equals(type)) {
            return "品牌";
        } else if ("category".equals(type)) {
            return "品类";
        } else if ("product".equals(type)) {
            return "机型属性";
        } else if ("quality".equals(type)){
            return "成色";
        } else {
            return "其他";
        }
    }

    private NamedEntityRecognition namedEntityRecognition;

    private SynonymMatcher synonymMatcher;

    private MultiCategoryPrediction prediction;

    private void load() throws IOException {
        this.namedEntityRecognition = new NamedEntityRecognition("ner/brand_1.dic",
                "ner/category_2.dic", "ner/product_4.dic", "ner/quality_3.dic", "ner/sku_0.dic");
        this.synonymMatcher = new SynonymMatcher("synonym/synonym_yx.arff");
        ArffSourceConvertor brandSource = new ArffSourceConvertor("meta/brand_meta.arff");
        brandSource.readData();
        ArffSourceConvertor categorySource = new ArffSourceConvertor("meta/category_meta.arff");
        categorySource.readData();
        ArffSourceConvertor productSource = new ArffSourceConvertor("meta/product_meta.arff");
        productSource.readData();
        this.prediction = new MultiCategoryPrediction(namedEntityRecognition, categorySource, brandSource, productSource);
        this.prediction.setConfidence(0.8);
    }

    public List<FindEntity> recognize(String val) {
        return namedEntityRecognition.recognize(val);
    }

    public String synonymMatch(String val) {
        return synonymMatcher.matchAndReplace(val, null);
    }

    public List<Category> mapping(String val) {
        return prediction.predict(val);
    }
}
