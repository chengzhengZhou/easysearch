package com.ppwx.easysearch.qp.prediction;

import com.ppwx.easysearch.qp.data.MetaTermOpt;
import com.ppwx.easysearch.qp.data.ResourceObserver;
import com.ppwx.easysearch.qp.ner.Entity;
import com.ppwx.easysearch.qp.ner.EntityRecognizer;
import com.ppwx.easysearch.qp.ner.EntityType;
import com.ppwx.easysearch.qp.tokenizer.Token;
import com.ppwx.easysearch.qp.tokenizer.Tokenizer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.ppwx.easysearch.qp.data.MetaTermOpt.*;

/**
 * 基于实体识别的类目预测
 * 根据EntityRecognizer实体识别的结果输出品类、品牌、机型的分类
 * 
 * @author ext.ahs.zhouchzh1@jd.com
 * @date 2025/10/10
 */
public class NerCategoryPrediction extends ResourceObserver<MetaTermOpt> implements CategoryPrediction {

    private static final Logger log = LoggerFactory.getLogger(NerCategoryPrediction.class);
    
    // 核心依赖组件
    private final EntityRecognizer entityRecognizer;
    private final Tokenizer tokenizer;

    // 索引结构（用于快速查找）
    private final Map<String, MetaTermOpt> categoryIdIndex;
    private final Map<String, MetaTermOpt> brandIdIndex;
    private final Map<String, MetaTermOpt> productIdIndex;
    
    // 配置参数
    private double confidenceThreshold = 0.5;
    private int maxResults = 10;

    /**
     * 构造函数
     * 
     * @param entityRecognizer 实体识别器
     * @param tokenizer 分词器
     */
    public NerCategoryPrediction(EntityRecognizer entityRecognizer,
                                 Tokenizer tokenizer) {
        this.entityRecognizer = entityRecognizer;
        this.tokenizer = tokenizer;
        this.categoryIdIndex = new HashMap<>();
        this.brandIdIndex = new HashMap<>();
        this.productIdIndex = new HashMap<>();
    }

    @Override
    public List<Category> predict(String query) {
        if (StringUtils.isBlank(query)) {
            return Collections.emptyList();
        }
        
        log.debug("Predicting categories for query: {}", query);
        
        try {
            // 1. 分词
            List<Token> tokens = tokenizer.tokenize(query);
            log.debug("Tokenized into {} tokens", tokens.size());
            
            // 2. 实体识别
            Collection<Entity> entities = entityRecognizer.extractEntities(query, tokens);
            log.debug("Extracted entities {}", entities);
            
            // 3. 按实体类型分组
            Map<EntityType, List<Entity>> entityMap = groupEntitiesByType(entities);
            
            // 4. 映射并生成Category结果

            // 4.1 处理品类实体
            List<Category> categories = processCategoryEntities(entityMap.get(EntityType.CATEGORY));
            List<Category> results = new ArrayList<>(categories);
            
            // 4.2 处理品牌实体
            List<Category> brands = processBrandEntities(entityMap.get(EntityType.BRAND));
            results.addAll(brands);
            
            // 4.3 处理机型实体（需要关联品牌和品类）
            List<Category> models = processModelEntities(
                entityMap.get(EntityType.MODEL),
                getBestBrand(brands),
                getBestCategory(categories)
            );
            results.addAll(models);
            
            // 5. 后处理：去重、排序、限制结果数
            List<Category> finalResults = postProcess(results);
            
            log.debug("Prediction completed with {} results", finalResults.size());
            return finalResults;
            
        } catch (Exception e) {
            log.error("Error predicting categories for query: {}", query, e);
            return Collections.emptyList();
        }
    }

    @Override
    protected void doUpdate(MetaTermOpt arg) {
        if (MetaTermOpt.DELETE == arg.getOpt()) {
            switch (arg.getTermType()) {
                case CATEGORY:
                    categoryIdIndex.remove(arg.getCategoryId());
                    break;
                case BRAND:
                    brandIdIndex.remove(arg.getBrandId());
                    break;
                case MODEL:
                    productIdIndex.remove(arg.getModelId());
                    break;
            }
        } else {
            switch (arg.getTermType()) {
                case CATEGORY:
                    categoryIdIndex.put(arg.getCategoryId(), arg);
                    break;
                case BRAND:
                    brandIdIndex.put(arg.getBrandId(), arg);
                    break;
                case MODEL:
                    productIdIndex.put(arg.getModelId(), arg);
                    break;
            }
        }
    }

    /**
     * 按实体类型分组
     */
    private Map<EntityType, List<Entity>> groupEntitiesByType(Collection<Entity> entities) {
        if (entities == null) {
            return Collections.emptyMap();
        }
        
        return entities.stream()
            .filter(e -> e != null && e.getConfidence() >= confidenceThreshold)
            .collect(Collectors.groupingBy(
                Entity::getType,
                Collectors.toList()
            ));
    }

    /**
     * 处理品类实体
     * 通过Entity的ID列表查找对应的品类元数据
     */
    private List<Category> processCategoryEntities(List<Entity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Category> categories = new ArrayList<>();
        
        for (Entity entity : entities) {
            try {
                // 方案1: 通过ID列表直接查找
                if (entity.getId() != null && !entity.getId().isEmpty()) {
                    for (String id : entity.getId()) {
                        MetaTermOpt categoryData = categoryIdIndex.get(id);
                        if (categoryData != null) {
                            Category category = buildCategoryFromData(
                                entity.getConfidence(),
                                categoryData,
                                CATEGORY
                            );
                            if (category != null) {
                                categories.add(category);
                                log.debug("Found category from ID {}: {}", id, category.getCategoryName());
                            }
                        }
                    }
                }
                // 方案2: 通过标准化值进行模糊匹配（备选方案）
                else if (StringUtils.isNotBlank(entity.getNormalizedValue())) {
                    Category fuzzyMatch = fuzzyMatchCategory(entity.getNormalizedValue(), entity.getConfidence());
                    if (fuzzyMatch != null) {
                        categories.add(fuzzyMatch);
                        log.debug("Found category from fuzzy match: {}", fuzzyMatch.getCategoryName());
                    }
                }
            } catch (Exception e) {
                log.error("Error processing category entity: {}", entity, e);
            }
        }
        
        return categories;
    }

    /**
     * 处理品牌实体
     */
    private List<Category> processBrandEntities(List<Entity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Category> brands = new ArrayList<>();
        
        for (Entity entity : entities) {
            try {
                if (entity.getId() != null && !entity.getId().isEmpty()) {
                    for (String id : entity.getId()) {
                        MetaTermOpt brandData = brandIdIndex.get(id);
                        if (brandData != null) {
                            Category brand = buildCategoryFromData(
                                entity.getConfidence(),
                                brandData,
                                BRAND
                            );
                            if (brand != null) {
                                brands.add(brand);
                                log.debug("Found brand from ID {}: {}", id, brand.getCategoryName());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error processing brand entity: {}", entity, e);
            }
        }
        
        return brands;
    }

    /**
     * 处理机型实体
     * 机型需要关联品牌和品类，构建层级关系
     */
    private List<Category> processModelEntities(List<Entity> entities, 
                                                Category contextBrand,
                                                Category contextCategory) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Category> models = new ArrayList<>();
        
        for (Entity entity : entities) {
            try {
                if (entity.getId() != null && !entity.getId().isEmpty()) {
                    for (String id : entity.getId()) {
                        MetaTermOpt productData = productIdIndex.get(id);
                        if (productData != null) {
                            // 构建完整的层级关系：机型 -> 品牌 -> 品类
                            Category model = buildProductHierarchy(
                                entity.getConfidence(),
                                productData,
                                contextBrand,
                                contextCategory
                            );
                            if (model != null) {
                                models.add(model);
                                log.debug("Found model from ID {}: {}", id, model.getCategoryName());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error processing model entity: {}", entity, e);
            }
        }
        
        return models;
    }

    /**
     * 构建产品层级关系
     * 机型 -> 品牌 -> 品类
     */
    private Category buildProductHierarchy(double confidence,
                                           MetaTermOpt productData,
                                          Category contextBrand,
                                          Category contextCategory) {
        try {
            // 1. 创建机型Category
            Category product = buildCategoryFromData(confidence, productData, MODEL);
            if (product == null) {
                return null;
            }
            
            // 2. 关联品牌
            Category brand = null;

            // 优先从产品数据中获取品牌信息
            String brandId = productData.getBrandId();
            if (brandId != null) {
                MetaTermOpt brandData = brandIdIndex.get(brandId);
                if (brandData != null) {
                    brand = buildCategoryFromData(confidence, brandData, BRAND);
                }
            }

            if (brand == null) {
                brand = contextBrand;
            }
            
            if (brand != null) {
                product.setParent(brand);
            }
            
            // 3. 关联品类
            Category category = null;
            String categoryId = productData.getCategoryId();
            if (categoryId != null) {
                MetaTermOpt categoryData = categoryIdIndex.get(categoryId);
                if (categoryData != null) {
                    category = buildCategoryFromData(confidence, categoryData, CATEGORY);
                }
            }

            if (category == null) {
                category = contextCategory;
            }
            
            if (category != null && brand != null) {
                brand.setParent(category);
            } else if (category != null) {
                product.setParent(category);
            }
            
            return product;
            
        } catch (Exception e) {
            log.error("Error building product hierarchy", e);
            return null;
        }
    }

    /**
     * 从ARFF数据构建Category对象
     */
    private Category buildCategoryFromData(double score, MetaTermOpt data, String type) {
        try {
            // 根据类型获取名称
            String name = null;
            Integer id = null;
            switch (type) {
                case CATEGORY:
                    name = data.getCategoryName();
                    id = Integer.valueOf(data.getCategoryId());
                    break;
                case BRAND:
                    name = data.getBrandName();
                    id = Integer.valueOf(data.getBrandId());
                    break;
                case MODEL:
                    name = data.getModelName();
                    id = Integer.valueOf(data.getModelId());
                    break;
            }
            
            if (name == null) {
                name = "Unknown";
            }
            
            return new Category(score, name, id, type);
            
        } catch (Exception e) {
            log.error("Error building category from data, type: {}", type, e);
            return null;
        }
    }

    /**
     * 获取最佳品牌
     */
    private Category getBestBrand(List<Category> brands) {
        if (brands == null || brands.isEmpty()) {
            return null;
        }
        return brands.stream()
            .max(Comparator.comparingDouble(Category::getScore))
            .orElse(null);
    }

    /**
     * 获取最佳品类
     */
    private Category getBestCategory(List<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            return null;
        }
        return categories.stream()
            .max(Comparator.comparingDouble(Category::getScore))
            .orElse(null);
    }

    /**
     * 后处理：去重、排序、限制数量
     */
    private List<Category> postProcess(List<Category> results) {
        if (results == null || results.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 使用Set来去重（基于equals和hashCode）
        Set<Category> uniqueResults = new LinkedHashSet<>(results);
        
        return uniqueResults.stream()
            .sorted(Comparator.comparingDouble(Category::getScore).reversed())  // 按置信度降序
            .limit(maxResults)  // 限制返回数量
            .collect(Collectors.toList());
    }

    /**
     * 模糊匹配品类（备选方案）
     */
    private Category fuzzyMatchCategory(String normalizedValue, double baseConfidence) {
        try {
            Set<Map.Entry<String, MetaTermOpt>> entries = categoryIdIndex.entrySet();
            for (Map.Entry<String, MetaTermOpt> entry : entries) {
                MetaTermOpt instance = entry.getValue();
                String categoryName = instance.getCategoryName();
                
                if (categoryName != null && 
                    (categoryName.contains(normalizedValue) || normalizedValue.contains(categoryName))) {
                    // 模糊匹配的置信度降低
                    double adjustedConfidence = baseConfidence * 0.7;
                    return buildCategoryFromData(adjustedConfidence, instance, CATEGORY);
                }
            }
        } catch (Exception e) {
            log.error("Error in fuzzy matching category", e);
        }
        
        return null;
    }

    public MetaTermOpt getCategoryIdIndex(String id) {
        return categoryIdIndex.get(id);
    }

    public MetaTermOpt getBrandIdIndex(String id) {
        return brandIdIndex.get(id);
    }

    public MetaTermOpt getProductIdIndex(String id) {
        return productIdIndex.get(id);
    }

    // Getters and Setters
    
    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }

    public void setConfidenceThreshold(double confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public EntityRecognizer getEntityRecognizer() {
        return entityRecognizer;
    }

    public Tokenizer getTokenizer() {
        return tokenizer;
    }
}
