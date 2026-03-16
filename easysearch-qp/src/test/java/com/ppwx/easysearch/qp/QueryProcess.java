package com.ppwx.easysearch.qp;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.LineHandler;
import com.google.common.collect.Lists;
import com.ppwx.easysearch.qp.data.ArffInstance;
import com.ppwx.easysearch.qp.data.ArffSourceConvertor;
import com.ppwx.easysearch.qp.ner.FindEntity;
import com.ppwx.easysearch.qp.ner.NamedEntityRecognition;
import com.ppwx.easysearch.qp.prediction.Category;
import com.ppwx.easysearch.qp.similarity.LevenshteinDistanceSimilarity;
import com.ppwx.easysearch.qp.support.CustomStopChar;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className QueryProcessTest
 * @description 查询处理
 * @date 2024/10/12 16:34
 **/
public class QueryProcess {

    private NamedEntityRecognition recognition;

    private ArffSourceConvertor brandSource;

    private ArffSourceConvertor categorySource;

    private ArffSourceConvertor productSource;

    private Map<Integer, ArffInstance> brandIdMap;

    private Map<String, ArffInstance> brandCHMap;

    private Map<String, ArffInstance> brandENMap;

    private Map<Integer, List<ArffInstance>> brandProductsMap;

    private double confidence = 0.8d;

    @Before
    public void init() throws IOException {
        this.recognition = new NamedEntityRecognition("ner/brand_1.dic",
                "ner/category_2.dic", "ner/product_4.dic", "ner/quality_3.dic", "ner/sku_0.dic");

        ArffSourceConvertor brandSource = new ArffSourceConvertor("meta/brand_meta.arff");
        brandSource.readData();
        this.brandSource = brandSource;

        ArffSourceConvertor categorySource = new ArffSourceConvertor("meta/category_meta.arff");
        categorySource.readData();
        this.categorySource = categorySource;

        ArffSourceConvertor productSource = new ArffSourceConvertor("meta/product_meta.arff");
        productSource.readData();
        this.productSource = productSource;

        brandProductsMap = productSource.getInstances().stream()
                .collect(Collectors.groupingBy(v -> (Integer) v.getValueByIndex(2)));

        brandIdMap = brandSource.getInstances().stream()
                .collect(Collectors.toMap(v -> (Integer) v.getValueByIndex(0), Function.identity()));
        brandCHMap = brandSource.getInstances().stream()
                .collect(Collectors.toMap(v -> (String) v.getValueByIndex(1), Function.identity()));
        brandENMap = brandSource.getInstances().stream()
                .filter(instance -> StringUtils.isNoneBlank((String) instance.getValueByIndex(2)))
                .collect(Collectors.toMap(v -> (String) v.getValueByIndex(2), Function.identity()));
    }

    @Test
    public void testQueryProcess() {
        //String query = "苹果 iPhone 15 Pro Max";
        //String query = "OPPO Find X7";
        //String query = "Find X7";
        String query = "vivo X100";
        queryProcess(query);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        for (int i = 0; i < 1000; i++) {
            queryProcess(query);
        }

        stopWatch.stop();
        System.out.println("执行耗时：" + stopWatch.getTotalTimeMillis() + " ms");
    }

    @Test
    public void testQueryProcessWorks() {
        queryProcess("努比亚 红魔 8 Pro");
    }

    @Test
    public void testHotKeywordProcess() throws IOException {
        String file = "top200-keyword.csv";
        QPStarter starter = new QPStarter();
        FileUtil.readUtf8Lines(FileUtil.file(file), (LineHandler) line -> {
            String word = line.split(",")[0];
            List<FindEntity> entities = starter.recognize(word);
            List<Category> metas = starter.mapping(word);
            StringBuilder entityDesc = new StringBuilder();
            if (CollectionUtils.isEmpty(entities)) {
                entityDesc.append("未识别到任何实体");
            } else {
                entities.forEach(entry -> entityDesc.append(entry.getTerm())
                                .append(" > ").append(starter.getType(entry.getType())));
            }

            StringBuilder mapDesc = new StringBuilder();
            mapDesc.setLength(0);
            if (CollectionUtils.isEmpty(metas)) {
                mapDesc.append("未对应上任何类目");
            } else {
                metas.forEach(category -> {
                    mapDesc.append(String.format("%s > %s：%s", category.getScore(),
                            starter.getType(category.getType()), category.getCategoryName()));
                });
            }

            System.out.printf("%s ; %s ; %s\r\n", word, entityDesc, mapDesc);
        });
    }

    @Test
    public void testSynonym() throws IOException {
        QPStarter starter = new QPStarter();
        System.out.println(starter.synonymMatch("折叠"));
        System.out.println(starter.synonymMatch("相机"));
        System.out.println(starter.synonymMatch("苹果 11 12"));
    }

    public List<QPMeta> queryProcess(String query) {
        List<QPMeta> result = Lists.newArrayList();
        List<FindEntity> entities = recognition.recognize(query);
        //System.out.println(entities);

        // find possible brand
        ArffInstance originBrand = null;
        Optional<FindEntity> brandEntity = entities.stream().filter(entity -> StringUtils.equals(entity.getType(), "brand")).findFirst();
        if (brandEntity.isPresent()) {
            String brand = brandEntity.get().getTerm();
            originBrand = brandCHMap.containsKey(brand) ? brandCHMap.get(brand) : brandENMap.get(brand);
        }
        // find possible product id
        Optional<FindEntity> findEntity = entities.stream().filter(entity -> StringUtils.equals(entity.getType(), "product")).findFirst();
        if (findEntity.isPresent()) {
            LevenshteinDistanceSimilarity similarity = new LevenshteinDistanceSimilarity();
            List<ArffInstance> products = productSource.getInstances();
            if (originBrand != null) {
                products = brandProductsMap.get((Integer) originBrand.getValueByIndex(0));
            }
            for (ArffInstance arffInstance : products) {
                String productName = (String) arffInstance.getValueByAttrName("product_name");
                /*if (productName.equals("ipad pro 6代 12.9英寸")) {
                    System.out.println("找到拉");
                }*/
                // use input brand primarily
                ArffInstance matchBrand = originBrand;
                if (matchBrand == null) {
                    Integer brandId = (Integer) arffInstance.getValueByAttrName("brand_id");
                    matchBrand = brandIdMap.get(brandId);
                }

                // match with brand
                String brandCH = (String) matchBrand.getValueByAttrName("brand_name_ch");
                String brandEN = (String) matchBrand.getValueByAttrName("brand_name_en");
                String originTerm = findEntity.get().getTerm();
                Double score = null;
                // 不带品牌需要保证置信度足够高
                Double skuScore = similarity.apply(originTerm, productName, CustomStopChar::isStopChar);
                score = skuScore;
                if (skuScore.compareTo(0.95) >= 0) {
                    result.add(new QPMeta(score, productName));
                } else {
                    if (brandCH != null) {
                        score = similarity.apply(brandCH + originTerm, productName, CustomStopChar::isStopChar);
                    }
                    if (brandEN != null) {
                        Double enScore = similarity.apply(brandEN + originTerm, productName, CustomStopChar::isStopChar);
                        score = score == null ? enScore : Math.max(score, enScore);
                    }

                    if (score.compareTo(confidence) >= 0) {
                        //System.out.println(score + " > mapped:" + productName);
                        result.add(new QPMeta(score, productName));
                    }
                }
            }
        }
        return result;
    }

    public List<QPMeta> queryProcessParalled(String query) {
        List<QPMeta> result = null;
        List<FindEntity> entities = recognition.recognize(query);
        // find possible brand
        ArffInstance originBrand;
        Optional<FindEntity> brandEntity = entities.stream().filter(entity -> StringUtils.equals(entity.getType(), "brand")).findFirst();
        if (brandEntity.isPresent()) {
            String brand = brandEntity.get().getTerm();
            originBrand = brandCHMap.containsKey(brand) ? brandCHMap.get(brand) : brandENMap.get(brand);
        } else {
            originBrand = null;
        }
        // find possible product id
        Optional<FindEntity> findEntity = entities.stream().filter(entity -> StringUtils.equals(entity.getType(), "product")).findFirst();
        if (findEntity.isPresent()) {
            LevenshteinDistanceSimilarity similarity = new LevenshteinDistanceSimilarity();
            List<ArffInstance> products = productSource.getInstances();
            if (originBrand != null) {
                products = brandProductsMap.get(originBrand.getValueByIndex(0));
            }
            result = products.parallelStream()
                    .map(arffInstance -> {
                        String productName = (String) arffInstance.getValueByAttrName("product_name");
                        // use input brand primarily
                        ArffInstance matchBrand = originBrand;
                        if (matchBrand == null) {
                            Integer brandId = (Integer) arffInstance.getValueByAttrName("brand_id");
                            matchBrand = brandIdMap.get(brandId);
                        }
                        if (matchBrand == null) {
                            System.out.println(">>>>>>>>>>>>>>>>>null brand:" + arffInstance.getValueByAttrName("brand_id"));
                            return null;
                        }
                        // match with brand
                        String brandCH = (String) matchBrand.getValueByAttrName("brand_name_ch");
                        String brandEN = (String) matchBrand.getValueByAttrName("brand_name_en");
                        String originTerm = findEntity.get().getTerm();
                        Double score;
                        // 不带品牌需要保证置信度足够高
                        Double skuScore = similarity.apply(originTerm, productName, CustomStopChar::isStopChar);
                        score = skuScore;
                        if (skuScore.compareTo(0.95) >= 0) {
                            return new QPMeta(score, productName);
                        } else {
                            if (brandCH != null) {
                                score = similarity.apply(brandCH + originTerm, productName, CustomStopChar::isStopChar);
                            }
                            if (brandEN != null) {
                                Double enScore = similarity.apply(brandEN + originTerm, productName, CustomStopChar::isStopChar);
                                score = score == null ? enScore : Math.max(score, enScore);
                            }

                            if (score.compareTo(confidence) >= 0) {
                                //System.out.println(score + " > mapped:" + productName);
                                return new QPMeta(score, productName);
                            }
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        return result == null ? Collections.emptyList() : result;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public NamedEntityRecognition getRecognition() {
        return recognition;
    }

    public static class QPMeta {
        private final Double score;

        private final String productName;

        public QPMeta(Double score, String productName) {
            this.score = score;
            this.productName = productName;
        }

        public Double getScore() {
            return score;
        }

        public String getProductName() {
            return productName;
        }
    }
}
