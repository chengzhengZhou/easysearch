package com.ppwx.easysearch.qp.eval;

import com.ppwx.easysearch.qp.ner.Entity;
import com.ppwx.easysearch.qp.ner.EntityType;

import java.util.*;

/**
 * 单个查询的评估结果
 */
public class QueryEvaluationResult {
    
    private String query;                           // 原始查询
    private Collection<Entity> predictedEntities;   // 预测的实体
    private Collection<Entity> groundTruthEntities; // 标注的真实实体
    
    private int truePositive = 0;    // 正确识别的实体数
    private int falsePositive = 0;   // 错误识别的实体数
    private int falseNegative = 0;   // 漏识别的实体数
    
    private int exactBoundaryMatches = 0;   // 精确边界匹配数
    private int partialBoundaryMatches = 0; // 部分边界匹配数
    
    private Map<EntityType, EntityTypeResult> typeResults = new HashMap<>();
    private Set<Entity> correctEntities = new HashSet<>();
    
    private long processingTimeMs;   // 处理时间（毫秒）
    
    public QueryEvaluationResult(String query, Collection<Entity> predictedEntities, 
                                Collection<Entity> groundTruthEntities) {
        this.query = query;
        this.predictedEntities = predictedEntities != null ? predictedEntities : Collections.emptyList();
        this.groundTruthEntities = groundTruthEntities;
        
        if (groundTruthEntities != null) {
            evaluate();
        }
    }
    
    /**
     * 执行评估
     */
    private void evaluate() {
        Set<Entity> matchedTruth = new HashSet<>();
        Set<Entity> matchedPred = new HashSet<>();
        
        // 对每个预测实体，找到最佳匹配的真实实体
        for (Entity pred : predictedEntities) {
            Entity bestMatch = null;
            double bestScore = 0;
            
            for (Entity truth : groundTruthEntities) {
                if (matchedTruth.contains(truth)) continue;
                
                double score = calculateMatchScore(pred, truth);
                if (score > bestScore) {
                    bestScore = score;
                    bestMatch = truth;
                }
            }
            
            // 匹配阈值：0.5
            if (bestScore >= 0.5 && bestMatch != null) {
                truePositive++;
                matchedTruth.add(bestMatch);
                matchedPred.add(pred);
                correctEntities.add(pred);
                
                // 统计边界匹配情况
                if (bestScore >= 0.9) {
                    exactBoundaryMatches++;
                } else {
                    partialBoundaryMatches++;
                }
                
                // 按类型统计
                updateTypeResult(pred.getType(), true, false, false);
            } else {
                falsePositive++;
                updateTypeResult(pred.getType(), false, true, false);
            }
        }
        
        // 计算漏识别的实体
        for (Entity truth : groundTruthEntities) {
            if (!matchedTruth.contains(truth)) {
                falseNegative++;
                updateTypeResult(truth.getType(), false, false, true);
            }
        }
    }
    
    /**
     * 计算两个实体的匹配得分
     * 考虑：类型、值、位置
     */
    private double calculateMatchScore(Entity pred, Entity truth) {
        // 类型不匹配，直接返回0
        if (pred.getType() != truth.getType()) {
            return 0;
        }
        
        double score = 0;
        
        // 1. 标准化值匹配 (权重: 0.4)
        if (normalizedEquals(pred.getNormalizedValue(), truth.getNormalizedValue())) {
            score += 0.4;
        } else if (containsOrContained(pred.getNormalizedValue(), truth.getNormalizedValue())) {
            score += 0.2;
        }
        
        // 2. 原始值匹配 (权重: 0.3)
        if (normalizedEquals(pred.getValue(), truth.getValue())) {
            score += 0.3;
        } else if (containsOrContained(pred.getValue(), truth.getValue())) {
            score += 0.15;
        }
        
        // 3. 位置匹配 (权重: 0.3)
        if (pred.getStartOffset() > 0 && truth.getStartOffset() > 0) {
            double overlap = calculateOverlap(pred, truth);
            score += 0.3 * overlap;
        } else {
            // 如果没有位置信息，这部分得分按值匹配给
            score += 0.3;
        }
        
        return score;
    }
    
    /**
     * 计算位置重叠度
     */
    private double calculateOverlap(Entity e1, Entity e2) {
        int start1 = e1.getStartOffset();
        int end1 = e1.getEndOffset();
        int start2 = e2.getStartOffset();
        int end2 = e2.getEndOffset();
        
        int overlapStart = Math.max(start1, start2);
        int overlapEnd = Math.min(end1, end2);
        
        if (overlapStart >= overlapEnd) {
            return 0; // 没有重叠
        }
        
        int overlapLen = overlapEnd - overlapStart;
        int unionLen = Math.max(end1, end2) - Math.min(start1, start2);
        
        return unionLen == 0 ? 0 : (double) overlapLen / unionLen;
    }
    
    /**
     * 标准化比较
     */
    private boolean normalizedEquals(String s1, String s2) {
        if (s1 == null || s2 == null) return false;
        return normalize(s1).equals(normalize(s2));
    }
    
    /**
     * 检查包含关系
     */
    private boolean containsOrContained(String s1, String s2) {
        if (s1 == null || s2 == null) return false;
        String n1 = normalize(s1);
        String n2 = normalize(s2);
        return n1.contains(n2) || n2.contains(n1);
    }
    
    /**
     * 字符串标准化：移除空格和标点符号
     */
    private String normalize(String s) {
        if (s == null) return "";
        
        StringBuilder result = new StringBuilder();
        for (char c : s.toLowerCase().toCharArray()) {
            // 跳过空白字符
            if (Character.isWhitespace(c)) continue;
            
            // 跳过标点符号
            if (isPunctuation(c)) continue;
            
            result.append(c);
        }
        return result.toString();
    }
    
    /**
     * 判断是否为标点符号
     */
    private boolean isPunctuation(char c) {
        // 英文标点
        if (c == '.' || c == ',' || c == ';' || c == ':' || c == '!' || c == '?') return true;
        if (c == '(' || c == ')' || c == '[' || c == ']' || c == '{' || c == '}') return true;
        if (c == '<' || c == '>' || c == '"' || c == '\'') return true;
        
        // 中文标点 (Unicode范围)
        if (c >= '\u3000' && c <= '\u303F') return true;  // CJK符号和标点
        if (c >= '\uFF00' && c <= '\uFFEF') return true;  // 全角ASCII
        if (c >= '\u2018' && c <= '\u201D') return true;  // 引号
        
        return false;
    }
    
    /**
     * 更新类型统计结果
     */
    private void updateTypeResult(EntityType type, boolean tp, boolean fp, boolean fn) {
        EntityTypeResult result = typeResults.computeIfAbsent(type, EntityTypeResult::new);
        if (tp) result.incrementTp();
        if (fp) result.incrementFp();
        if (fn) result.incrementFn();
    }
    
    /**
     * 判断实体是否正确
     */
    public boolean isEntityCorrect(Entity entity) {
        return correctEntities.contains(entity);
    }
    
    // ===== Getters =====
    
    public String getQuery() { return query; }
    public Collection<Entity> getPredictedEntities() { return predictedEntities; }
    public Collection<Entity> getGroundTruthEntities() { return groundTruthEntities; }
    public int getTruePositive() { return truePositive; }
    public int getFalsePositive() { return falsePositive; }
    public int getFalseNegative() { return falseNegative; }
    public int getExactBoundaryMatches() { return exactBoundaryMatches; }
    public int getPartialBoundaryMatches() { return partialBoundaryMatches; }
    public Map<EntityType, EntityTypeResult> getTypeResults() { return typeResults; }
    public long getProcessingTimeMs() { return processingTimeMs; }
    
    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
    
    /**
     * 按实体类型的评估结果
     */
    public static class EntityTypeResult {
        private EntityType type;
        private int tp = 0;
        private int fp = 0;
        private int fn = 0;
        
        public EntityTypeResult(EntityType type) {
            this.type = type;
        }
        
        public void incrementTp() { tp++; }
        public void incrementFp() { fp++; }
        public void incrementFn() { fn++; }
        
        public EntityType getType() { return type; }
        public int getTp() { return tp; }
        public int getFp() { return fp; }
        public int getFn() { return fn; }
    }
}

