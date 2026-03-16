package com.ppwx.easysearch.qp.eval;

import com.ppwx.easysearch.qp.ner.Entity;
import com.ppwx.easysearch.qp.ner.EntityType;
import com.ppwx.easysearch.qp.similarity.CosineSimilarity;
import com.ppwx.easysearch.qp.similarity.JaccardSimilarity;
import com.ppwx.easysearch.qp.similarity.LevenshteinDistanceSimilarity;
import com.ppwx.easysearch.qp.similarity.SequenceSimilarityScore;

import java.util.*;

/**
 * 查询相关性评估结果
 * 输入：关键词、查询结果
 * 输出：查询相关性评估结果
 */
public class QueryCorrelationResult {
    
    private String query;                              // 原始查询关键词
    private List<Entity> queryEntities;                 // 从查询中提取的实体（品类、品牌、机型等）
    private List<ProductResult> searchResults;         // 搜索结果列表
    private List<ProductRelevanceScore> relevanceScores; // 每个结果的相关性得分
    
    // 相关性权重配置（可调整）
    private static final double WEIGHT_CATEGORY = 0.15;  // 品类权重
    private static final double WEIGHT_BRAND = 0.15;     // 品牌权重
    private static final double WEIGHT_MODEL = 0.50;     // 机型权重（最重要）
    private static final double WEIGHT_TITLE = 0.20;     // 标题相似度权重
    
    // 匹配阈值
    private static final double EXACT_MATCH_THRESHOLD = 0.95;  // 精确匹配阈值
    private static final double PARTIAL_MATCH_THRESHOLD = 0.70; // 部分匹配阈值
    
    private SequenceSimilarityScore<Double> titleSimilarity = new CosineSimilarity();
    
    public QueryCorrelationResult(String query, List<Entity> queryEntities, 
                                  List<ProductResult> searchResults) {
        this.query = query;
        this.queryEntities = queryEntities != null ? queryEntities : Collections.emptyList();
        this.searchResults = searchResults != null ? searchResults : Collections.emptyList();
        this.relevanceScores = calculateRelevanceScores();
    }
    
    /**
     * 计算所有搜索结果的相关性得分
     */
    private List<ProductRelevanceScore> calculateRelevanceScores() {
        List<ProductRelevanceScore> scores = new ArrayList<>();
        
        for (ProductResult result : searchResults) {
            ProductRelevanceScore score = calculateRelevanceScore(result);
            scores.add(score);
        }
        
        // 按相关性得分降序排序
        scores.sort((a, b) -> Double.compare(b.getTotalScore(), a.getTotalScore()));
        
        return scores;
    }
    
    /**
     * 计算单个商品的相关性得分
     */
    private ProductRelevanceScore calculateRelevanceScore(ProductResult product) {
        ProductRelevanceScore score = new ProductRelevanceScore(product);
        
        // 1. 品类匹配度
        double categoryScore = calculateCategoryScore(product);
        score.setCategoryScore(categoryScore);
        
        // 2. 品牌匹配度
        double brandScore = calculateBrandScore(product);
        score.setBrandScore(brandScore);
        
        // 3. 机型匹配度
        double modelScore = calculateModelScore(product);
        score.setModelScore(modelScore);
        
        // 4. 标题相似度
        double titleScore = calculateTitleScore(product);
        score.setTitleScore(titleScore);
        
        // 5. 计算综合得分（加权平均）
        double totalScore = categoryScore * WEIGHT_CATEGORY +
                           brandScore * WEIGHT_BRAND +
                           modelScore * WEIGHT_MODEL +
                           titleScore * WEIGHT_TITLE;
        score.setTotalScore(totalScore);
        
        return score;
    }
    
    /**
     * 计算品类匹配度
     */
    private double calculateCategoryScore(ProductResult product) {
        Entity queryCategory = findEntityByType(EntityType.CATEGORY);
        if (queryCategory == null) {
            // 查询中没有品类信息，返回中性分数
            return 0.5;
        }

        // ID匹配（最可靠）
        if (isIdMatch(queryCategory.getId(), product.getCategoryId())) {
            return 1.0;
        }

        if (product.getCategory() == null) {
            return 0.0;
        }

        // 精确匹配
        if (isExactMatch(queryCategory, product.getCategory())) {
            return 0.95;
        }
        
        // 标准化值匹配
        if (isNormalizedMatch(queryCategory.getNormalizedValue(), product.getCategory())) {
            return 0.8;
        }
        
        // 包含关系匹配
        if (isContainsMatch(queryCategory.getNormalizedValue(), product.getCategory())) {
            return 0.6;
        }
        
        return 0.0;
    }
    
    /**
     * 计算品牌匹配度
     */
    private double calculateBrandScore(ProductResult product) {
        Entity queryBrand = findEntityByType(EntityType.BRAND);
        if (queryBrand == null) {
            return 0.5;
        }

        // ID匹配（最可靠）
        if (isIdMatch(queryBrand.getId(), product.getBrandId())) {
            return 1.0;
        }

        if (product.getBrand() == null) {
            return 0.0;
        }
        
        // 精确匹配
        if (isExactMatch(queryBrand, product.getBrand())) {
            return 0.95;
        }
        
        // 标准化值匹配
        if (isNormalizedMatch(queryBrand.getNormalizedValue(), product.getBrand())) {
            return 0.8;
        }
        
        // 包含关系匹配
        if (isContainsMatch(queryBrand.getNormalizedValue(), product.getBrand())) {
            return 0.6;
        }
        
        return 0.0;
    }
    
    /**
     * 计算机型匹配度（最重要）
     */
    private double calculateModelScore(ProductResult product) {
        Entity queryModel = findEntityByType(EntityType.MODEL);
        if (queryModel == null) {
            return 0.5;
        }

        // ID匹配（最可靠，机型通常有唯一ID）
        if (isIdMatch(queryModel.getId(), product.getModelId())) {
            return 1.0;
        }
        
        if (product.getModel() == null) {
            return 0.0;
        }
        
        // 精确匹配
        if (isExactMatch(queryModel, product.getModel())) {
            return 0.95;
        }
        
        // 标准化值匹配
        if (isNormalizedMatch(queryModel.getNormalizedValue(), product.getModel())) {
            return 0.85;
        }
        
        // 包含关系匹配（机型可能有变体，如"iPhone 15 Pro" vs "iPhone 15 Pro Max"）
        if (isContainsMatch(queryModel.getNormalizedValue(), product.getModel()) ||
            isContainsMatch(product.getModel(), queryModel.getNormalizedValue())) {
            return 0.7;
        }
        
        // 使用相似度计算（处理拼写变体）
        double similarity = calculateStringSimilarity(
            queryModel.getNormalizedValue(), 
            product.getModel()
        );
        if (similarity >= EXACT_MATCH_THRESHOLD) {
            return 0.8;
        } else if (similarity >= PARTIAL_MATCH_THRESHOLD) {
            return 0.5;
        }
        
        return 0.0;
    }
    
    /**
     * 计算标题相似度
     */
    private double calculateTitleScore(ProductResult product) {
        if (product.getTitle() == null || product.getTitle().trim().isEmpty()) {
            return 0.0;
        }
        
        if (query == null || query.trim().isEmpty()) {
            return 0.5;
        }
        
        // 使用余弦相似度计算标题与查询的相似度
        try {
            Double similarity = titleSimilarity.apply(query, product.getTitle(), null);
            return similarity != null ? similarity : 0.0;
        } catch (Exception e) {
            // 如果计算失败，使用简单的包含关系
            String normalizedQuery = normalize(query);
            String normalizedTitle = normalize(product.getTitle());
            
            if (normalizedTitle.contains(normalizedQuery)) {
                return 0.6;
            }
            if (normalizedQuery.contains(normalizedTitle)) {
                return 0.4;
            }
            return 0.0;
        }
    }
    
    /**
     * 辅助方法：按类型查找实体
     */
    private Entity findEntityByType(EntityType type) {
        return queryEntities.stream()
                .filter(e -> e.getType() == type)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 精确匹配检查
     */
    private boolean isExactMatch(Entity entity, String value) {
        if (value == null) return false;
        return normalize(entity.getValue()).equals(normalize(value)) ||
               normalize(entity.getNormalizedValue()).equals(normalize(value));
    }
    
    /**
     * 标准化值匹配
     */
    private boolean isNormalizedMatch(String normalizedValue, String value) {
        if (normalizedValue == null || value == null) return false;
        return normalize(normalizedValue).equals(normalize(value));
    }
    
    /**
     * 包含关系匹配
     */
    private boolean isContainsMatch(String queryValue, String productValue) {
        if (queryValue == null || productValue == null) return false;
        String nq = normalize(queryValue);
        String np = normalize(productValue);
        return np.contains(nq) || nq.contains(np);
    }
    
    /**
     * ID匹配（最可靠）
     */
    private boolean isIdMatch(List<String> queryIds, String productId) {
        if (queryIds == null || queryIds.isEmpty() || productId == null) {
            return false;
        }
        return queryIds.contains(productId);
    }
    
    /**
     * 计算字符串相似度
     */
    private double calculateStringSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        try {
            JaccardSimilarity jaccard = new JaccardSimilarity();
            return jaccard.apply(s1, s2, null);
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    /**
     * 字符串标准化
     */
    private String normalize(String s) {
        if (s == null) return "";
        return s.toLowerCase().trim()
            .replaceAll("\\s+", "")
            .replaceAll("[\\p{Punct}]", "");
    }
    
    // ===== Getters =====
    
    public String getQuery() {
        return query;
    }
    
    public List<Entity> getQueryEntities() {
        return queryEntities;
    }
    
    public List<ProductResult> getSearchResults() {
        return searchResults;
    }
    
    public List<ProductRelevanceScore> getRelevanceScores() {
        return relevanceScores;
    }
    
    /**
     * 商品结果数据类
     */
    public static class ProductResult {
        private String productId;
        private String title;
        private String category;
        private String categoryId;
        private String brand;
        private String brandId;
        private String model;
        private String modelId;
        private int rank;  // 在搜索结果中的排名
        
        public ProductResult() {}
        
        public ProductResult(String productId, String title, String category, 
                            String brand, String model) {
            this.productId = productId;
            this.title = title;
            this.category = category;
            this.brand = brand;
            this.model = model;
        }
        
        // Getters and Setters
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        public String getCategoryId() { return categoryId; }
        public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
        
        public String getBrand() { return brand; }
        public void setBrand(String brand) { this.brand = brand; }
        
        public String getBrandId() { return brandId; }
        public void setBrandId(String brandId) { this.brandId = brandId; }
        
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        
        public String getModelId() { return modelId; }
        public void setModelId(String modelId) { this.modelId = modelId; }
        
        public int getRank() { return rank; }
        public void setRank(int rank) { this.rank = rank; }
    }
    
    /**
     * 商品相关性得分
     */
    public static class ProductRelevanceScore {
        private ProductResult product;
        private double categoryScore;
        private double brandScore;
        private double modelScore;
        private double titleScore;
        private double totalScore;
        private boolean isRelevant;  // 是否相关（基于阈值判断）
        
        public ProductRelevanceScore(ProductResult product) {
            this.product = product;
        }
        
        // Getters and Setters
        public ProductResult getProduct() { return product; }
        
        public double getCategoryScore() { return categoryScore; }
        public void setCategoryScore(double categoryScore) { 
            this.categoryScore = categoryScore; 
        }
        
        public double getBrandScore() { return brandScore; }
        public void setBrandScore(double brandScore) { 
            this.brandScore = brandScore; 
        }
        
        public double getModelScore() { return modelScore; }
        public void setModelScore(double modelScore) { 
            this.modelScore = modelScore; 
        }
        
        public double getTitleScore() { return titleScore; }
        public void setTitleScore(double titleScore) { 
            this.titleScore = titleScore; 
        }
        
        public double getTotalScore() { return totalScore; }
        public void setTotalScore(double totalScore) { 
            this.totalScore = totalScore;
            // 设置相关性标记（阈值可配置）
            this.isRelevant = totalScore >= 0.6;
        }
        
        public boolean isRelevant() { return isRelevant; }
    }
}
