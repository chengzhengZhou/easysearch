package com.ppwx.easysearch.qp.prediction;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ppwx.easysearch.qp.data.ArffInstance;
import com.ppwx.easysearch.qp.data.ArffSourceConvertor;
import com.ppwx.easysearch.qp.ner.FindEntity;
import com.ppwx.easysearch.qp.ner.NamedEntityRecognition;
import com.ppwx.easysearch.qp.similarity.LevenshteinDistanceSimilarity;
import com.ppwx.easysearch.qp.support.CustomStopChar;
import com.ppwx.easysearch.qp.util.StrTrimUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 类目、品牌、机型 预测
 * 该实现依赖实体识别后按特定规则计算与元数据的相关度
 *
 * @author ext.ahs.zhouchzh1@jd.com
 * @className MultiCategoryPrediction
 * @description 多类目预测
 * @date 2024/11/1 16:03
 **/
public class MultiCategoryPrediction implements CategoryPrediction {

    private static final String CATEGORY = "category";
    private static final String BRAND = "brand";
    private static final String PRODUCT = "product";
    /**
     * 命名实体识别
     */
    private final NamedEntityRecognition namedEntityRecognition;
    /**
     * 品牌数据
     */
    private final ArffSourceConvertor brandSource;
    /**
     * 分类数据
     */
    private final ArffSourceConvertor categorySource;
    /**
     * 机型数据
     */
    private final ArffSourceConvertor productSource;
    /**
     * 置信度
     */
    private double confidence = 0.9d;
    /**
     * 前k个
     */
    private int topK = 1;
    /**
     * 分类id索引
     */
    private Map<Integer, ArffInstance> categoryIdMap;
    /**
     * 分类名称索引
     */
    private Map<String, ArffInstance> categoryMap;
    /**
     * 品牌id索引
     */
    private Map<Integer, ArffInstance> brandIdMap;
    /**
     * 品牌中文名称索引
     */
    private Map<String, ArffInstance> brandCHMap;
    /**
     * 品牌英文名称索引
     */
    private Map<String, ArffInstance> brandENMap;
    /**
     * 品牌机型映射
     */
    private Map<Integer, List<ArffInstance>> brandProductsMap;

    public MultiCategoryPrediction(NamedEntityRecognition namedEntityRecognition,
                                   ArffSourceConvertor categorySource,
                                   ArffSourceConvertor brandSource,
                                   ArffSourceConvertor productSource) {
        this.namedEntityRecognition = namedEntityRecognition;
        this.brandSource = brandSource;
        this.categorySource = categorySource;
        this.productSource = productSource;
        buildIndex();
    }

    private void buildIndex() {
        // 构建索引
        categoryIdMap = categorySource.getInstances().stream()
                .collect(Collectors.toMap(v -> (Integer) v.getValueByIndex(0), Function.identity()));
        categoryMap = Maps.newHashMapWithExpectedSize(64);
        categorySource.getInstances().forEach(instance -> {
            String value = (String) instance.getValueByIndex(1);
            Arrays.stream(value.split(",")).forEach(item -> categoryMap.put(item, instance));
        });
        brandIdMap = brandSource.getInstances().stream()
                .collect(Collectors.toMap(v -> (Integer) v.getValueByIndex(0), Function.identity()));
        brandCHMap = brandSource.getInstances().stream()
                .collect(Collectors.toMap(v -> (String) v.getValueByIndex(1), Function.identity()));
        brandENMap = brandSource.getInstances().stream()
                .filter(instance -> StringUtils.isNoneBlank((String) instance.getValueByIndex(2)))
                .collect(Collectors.toMap(v -> (String) v.getValueByIndex(2), Function.identity()));
        brandProductsMap = productSource.getInstances().stream()
                .collect(Collectors.groupingBy(v -> (Integer) v.getValueByIndex(2)));
    }

    @Override
    public List<Category> predict(String query) {
        List<Category> categories = Lists.newArrayList();
        List<FindEntity> entities = namedEntityRecognition.recognize(query);
        // find origin category
        Optional<FindEntity> categoryEntity = entities.stream().filter(entity -> StringUtils.equals(entity.getType(), CATEGORY)).findFirst();
        if (categoryEntity.isPresent()) {
            String category = StrTrimUtil.trim(categoryEntity.get().getTerm(), CustomStopChar::isNotStopChar);
            ArffInstance originCategory = categoryMap.get(category);
            if (originCategory != null) {
                categories.add(new Category(1.0, (String) originCategory.getValueByIndex(1),
                        (Integer) originCategory.getValueByIndex(0), CATEGORY));
            }
        }
        // find origin brand
        ArffInstance originBrand;
        Optional<FindEntity> brandEntity = entities.stream().filter(entity -> StringUtils.equals(entity.getType(), BRAND)).findFirst();
        if (brandEntity.isPresent()) {
            String brand = StrTrimUtil.trim(brandEntity.get().getTerm(), CustomStopChar::isNotStopChar);
            originBrand = brandCHMap.containsKey(brand) ? brandCHMap.get(brand) : brandENMap.get(brand);
            if (originBrand != null) {
                categories.add(new Category(1.0, (String) originBrand.getValueByIndex(1),
                        (Integer) originBrand.getValueByIndex(0), BRAND));
            }
        } else {
            originBrand = null;
        }
        // find possible product
        Optional<FindEntity> findEntity = entities.stream().filter(entity -> StringUtils.equals(entity.getType(), PRODUCT)).findFirst();
        List<Meta> products = null;
        if (findEntity.isPresent()) {
            List<ArffInstance> productStream = productSource.getInstances();
            if (originBrand != null) {
                productStream = brandProductsMap.get((Integer) originBrand.getValueByIndex(0));
            }
            LevenshteinDistanceSimilarity similarity = new LevenshteinDistanceSimilarity();
            products = productStream.parallelStream().map(arffInstance -> {
                String productName = (String) arffInstance.getValueByAttrName("product_name");
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
                // 不带品牌需要保证置信度足够高
                Double skuScore = similarity.apply(originTerm, productName, CustomStopChar::isStopChar);
                Double score = skuScore;
                if (skuScore.compareTo(0.95) >= 0) {
                    return new Meta(score, arffInstance);
                } else {
                    if (brandCH != null) {
                        score = similarity.apply(brandCH + originTerm, productName, CustomStopChar::isStopChar);
                    }
                    if (brandEN != null) {
                        Double enScore = similarity.apply(brandEN + originTerm, productName, CustomStopChar::isStopChar);
                        score = score == null ? enScore : Math.max(score, enScore);
                    }

                    if (score.compareTo(confidence) >= 0) {
                        return new Meta(score, arffInstance);
                    }
                }
                return null;
            }).filter(Objects::nonNull)
                    .sorted(Comparator.comparing(Meta::getScore).reversed())
                    .limit(topK)
                    .collect(Collectors.toList());
        }

        if (products != null) {
            products.forEach(meta -> categories.add(buildCategory(meta.score, meta.val)));
        }

        return categories;
    }

    private Category buildCategory(double score, ArffInstance productArff) {
        Integer productId = (Integer) productArff.getValueByAttrName("id");
        String productName = (String) productArff.getValueByAttrName("product_name");
        Integer brandId = (Integer) productArff.getValueByAttrName("brand_id");
        Integer categoryId = (Integer) productArff.getValueByAttrName("category_id");
        Category category = new Category(score, productName, productId, PRODUCT);
        Category node = category;
        // brand
        ArffInstance brandArff = brandIdMap.get(brandId);
        if (brandArff != null) {
            Category brand = new Category(score,  (String) brandArff.getValueByAttrName("brand_name_ch"),
                    (Integer) brandArff.getValueByAttrName("id"), BRAND);
            category.setParent(brand);
            category = brand;
        }
        // category
        ArffInstance categoryArff = categoryIdMap.get(categoryId);
        if (categoryArff != null) {
            Category root = new Category(score,  (String) categoryArff.getValueByAttrName("category_name"),
                    (Integer) categoryArff.getValueByAttrName("id"), CATEGORY);
            category.setParent(root);
        }
        return node;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public NamedEntityRecognition getNamedEntityRecognition() {
        return namedEntityRecognition;
    }

    public ArffSourceConvertor getBrandSource() {
        return brandSource;
    }

    public ArffSourceConvertor getCategorySource() {
        return categorySource;
    }

    public ArffSourceConvertor getProductSource() {
        return productSource;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public static class Meta implements Comparable<Meta>{

        private double score;

        private ArffInstance val;

        public Meta(double score, ArffInstance val) {
            this.score = score;
            this.val = val;
        }

        @Override
        public int compareTo(Meta other) {
            return Double.compare(other.score, this.score);
        }

        public double getScore() {
            return score;
        }
    }
}
