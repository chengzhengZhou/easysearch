package com.ppwx.easysearch.qp.eval;

import com.ppwx.easysearch.qp.ner.Entity;
import com.ppwx.easysearch.qp.ner.EntityType;

import java.util.*;

/**
 * NER评估指标类
 * 用于计算实体识别的各项性能指标
 */
public class NerEvaluationMetrics {
    
    // 混淆矩阵
    private int truePositive = 0;   // 正确识别的实体
    private int falsePositive = 0;  // 错误识别的实体
    private int falseNegative = 0;  // 漏识别的实体
    private int trueNegative = 0;   // 正确识别为无实体
    
    // 按实体类型统计
    private Map<EntityType, EntityTypeMetrics> typeMetrics = new HashMap<>();
    
    // 业务指标
    private int totalQueries = 0;           // 总查询数
    private int queriesWithEntities = 0;    // 识别出实体的查询数
    private int totalEntitiesRecognized = 0; // 总识别实体数
    
    // 性能指标
    private List<Long> processingTimes = new ArrayList<>();
    
    // 置信度分布
    private Map<String, ConfidenceMetrics> confidenceMetrics = new HashMap<>();
    
    // 边界匹配统计
    private int exactBoundaryMatches = 0;    // 精确边界匹配
    private int partialBoundaryMatches = 0;  // 部分边界匹配
    
    /**
     * 实体类型指标
     */
    public static class EntityTypeMetrics {
        private EntityType type;
        private int tp = 0;
        private int fp = 0;
        private int fn = 0;
        
        public EntityTypeMetrics(EntityType type) {
            this.type = type;
        }
        
        public void addTruePositive() { tp++; }
        public void addFalsePositive() { fp++; }
        public void addFalseNegative() { fn++; }
        
        public double getPrecision() {
            return tp + fp == 0 ? 0 : (double) tp / (tp + fp);
        }
        
        public double getRecall() {
            return tp + fn == 0 ? 0 : (double) tp / (tp + fn);
        }
        
        public double getF1() {
            double p = getPrecision();
            double r = getRecall();
            return p + r == 0 ? 0 : 2 * p * r / (p + r);
        }
        
        public EntityType getType() { return type; }
        public int getTp() { return tp; }
        public int getFp() { return fp; }
        public int getFn() { return fn; }
    }
    
    /**
     * 置信度区间指标
     */
    public static class ConfidenceMetrics {
        private String range;
        private int correct = 0;
        private int total = 0;
        
        public ConfidenceMetrics(String range) {
            this.range = range;
        }
        
        public void addCorrect() { 
            correct++; 
            total++; 
        }
        
        public void addIncorrect() { 
            total++; 
        }
        
        public double getAccuracy() {
            return total == 0 ? 0 : (double) correct / total;
        }
        
        public String getRange() { return range; }
        public int getCorrect() { return correct; }
        public int getTotal() { return total; }
    }
    
    /**
     * 添加单个查询的评估结果
     */
    public void addQueryResult(QueryEvaluationResult result) {
        totalQueries++;
        
        if (result.getPredictedEntities() != null && !result.getPredictedEntities().isEmpty()) {
            queriesWithEntities++;
            totalEntitiesRecognized += result.getPredictedEntities().size();
        }
        
        truePositive += result.getTruePositive();
        falsePositive += result.getFalsePositive();
        falseNegative += result.getFalseNegative();
        
        if (result.getGroundTruthEntities() == null || result.getGroundTruthEntities().isEmpty()) {
            if (result.getPredictedEntities() == null || result.getPredictedEntities().isEmpty()) {
                trueNegative++;
            }
        }
        
        // 按类型统计
        for (Map.Entry<EntityType, QueryEvaluationResult.EntityTypeResult> entry : result.getTypeResults().entrySet()) {
            EntityTypeMetrics metrics = typeMetrics.computeIfAbsent(
                entry.getKey(), EntityTypeMetrics::new);
            QueryEvaluationResult.EntityTypeResult typeResult = entry.getValue();
            for (int i = 0; i < typeResult.getTp(); i++) metrics.addTruePositive();
            for (int i = 0; i < typeResult.getFp(); i++) metrics.addFalsePositive();
            for (int i = 0; i < typeResult.getFn(); i++) metrics.addFalseNegative();
        }
        
        // 置信度统计
        if (result.getPredictedEntities() != null) {
            for (Entity entity : result.getPredictedEntities()) {
                String range = getConfidenceRange(entity.getConfidence());
                ConfidenceMetrics cm = confidenceMetrics.computeIfAbsent(
                    range, ConfidenceMetrics::new);
                
                if (result.isEntityCorrect(entity)) {
                    cm.addCorrect();
                } else {
                    cm.addIncorrect();
                }
            }
        }
        
        // 边界匹配统计
        exactBoundaryMatches += result.getExactBoundaryMatches();
        partialBoundaryMatches += result.getPartialBoundaryMatches();
        
        // 性能指标
        if (result.getProcessingTimeMs() > 0) {
            processingTimes.add(result.getProcessingTimeMs());
        }
    }
    
    /**
     * 获取置信度区间
     */
    private String getConfidenceRange(double confidence) {
        if (confidence >= 0.9) return "0.9-1.0";
        if (confidence >= 0.8) return "0.8-0.9";
        if (confidence >= 0.7) return "0.7-0.8";
        if (confidence >= 0.6) return "0.6-0.7";
        if (confidence >= 0.5) return "0.5-0.6";
        return "0.0-0.5";
    }
    
    // ===== 整体指标计算 =====
    
    /**
     * 准确率 (Precision)
     */
    public double getPrecision() {
        return truePositive + falsePositive == 0 ? 0 : 
            (double) truePositive / (truePositive + falsePositive);
    }
    
    /**
     * 召回率 (Recall)
     */
    public double getRecall() {
        return truePositive + falseNegative == 0 ? 0 : 
            (double) truePositive / (truePositive + falseNegative);
    }
    
    /**
     * F1分数
     */
    public double getF1Score() {
        double p = getPrecision();
        double r = getRecall();
        return p + r == 0 ? 0 : 2 * p * r / (p + r);
    }
    
    /**
     * 准确度 (Accuracy)
     */
    public double getAccuracy() {
        int total = truePositive + falsePositive + falseNegative + trueNegative;
        return total == 0 ? 0 : (double) (truePositive + trueNegative) / total;
    }
    
    /**
     * 覆盖率：识别出至少一个实体的查询占比
     */
    public double getCoverage() {
        return totalQueries == 0 ? 0 : (double) queriesWithEntities / totalQueries;
    }
    
    /**
     * 平均每个查询识别的实体数
     */
    public double getAvgEntitiesPerQuery() {
        return totalQueries == 0 ? 0 : (double) totalEntitiesRecognized / totalQueries;
    }
    
    /**
     * 平均处理时间（毫秒）
     */
    public double getAvgProcessingTime() {
        return processingTimes.isEmpty() ? 0 : 
            processingTimes.stream().mapToLong(Long::longValue).average().orElse(0);
    }
    
    /**
     * P95处理时间（毫秒）
     */
    public long getP95ProcessingTime() {
        if (processingTimes.isEmpty()) return 0;
        List<Long> sorted = new ArrayList<>(processingTimes);
        Collections.sort(sorted);
        int index = (int) Math.ceil(sorted.size() * 0.95) - 1;
        return sorted.get(Math.max(0, index));
    }
    
    /**
     * P99处理时间（毫秒）
     */
    public long getP99ProcessingTime() {
        if (processingTimes.isEmpty()) return 0;
        List<Long> sorted = new ArrayList<>(processingTimes);
        Collections.sort(sorted);
        int index = (int) Math.ceil(sorted.size() * 0.99) - 1;
        return sorted.get(Math.max(0, index));
    }
    
    /**
     * 吞吐量（每秒处理的查询数）
     */
    public double getThroughput() {
        double avgTime = getAvgProcessingTime();
        return avgTime == 0 ? 0 : 1000.0 / avgTime;
    }
    
    /**
     * 精确边界匹配率
     */
    public double getExactBoundaryMatchRate() {
        int total = exactBoundaryMatches + partialBoundaryMatches;
        return total == 0 ? 0 : (double) exactBoundaryMatches / total;
    }
    
    // ===== Getters =====
    
    public int getTruePositive() { return truePositive; }
    public int getFalsePositive() { return falsePositive; }
    public int getFalseNegative() { return falseNegative; }
    public int getTrueNegative() { return trueNegative; }
    public int getTotalQueries() { return totalQueries; }
    public int getQueriesWithEntities() { return queriesWithEntities; }
    public Map<EntityType, EntityTypeMetrics> getTypeMetrics() { return typeMetrics; }
    public Map<String, ConfidenceMetrics> getConfidenceMetrics() { return confidenceMetrics; }
    
    /**
     * 打印详细报告
     */
    public String generateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n========== NER评估报告 ==========\n\n");
        
        // 整体指标
        sb.append("【整体指标】\n");
        sb.append(String.format("总查询数: %d\n", totalQueries));
        sb.append(String.format("准确率 (Precision): %.4f\n", getPrecision()));
        sb.append(String.format("召回率 (Recall): %.4f\n", getRecall()));
        sb.append(String.format("F1分数: %.4f\n", getF1Score()));
        sb.append(String.format("准确度 (Accuracy): %.4f\n", getAccuracy()));
        sb.append(String.format("覆盖率: %.4f\n", getCoverage()));
        sb.append(String.format("平均实体数/查询: %.2f\n", getAvgEntitiesPerQuery()));
        sb.append(String.format("精确边界匹配率: %.4f\n", getExactBoundaryMatchRate()));
        
        // 混淆矩阵
        sb.append("\n【混淆矩阵】\n");
        sb.append(String.format("TP (正确识别): %d\n", truePositive));
        sb.append(String.format("FP (误识别): %d\n", falsePositive));
        sb.append(String.format("FN (漏识别): %d\n", falseNegative));
        sb.append(String.format("TN (正确拒绝): %d\n", trueNegative));
        
        // 按实体类型统计
        sb.append("\n【按实体类型统计】\n");
        sb.append(String.format("%-15s %10s %10s %10s %10s %8s %8s\n", 
            "类型", "TP", "FP", "FN", "支持数", "准确率", "召回率", "F1"));
        for (int i = 0; i < 85; i++) sb.append("-");
        sb.append("\n");
        
        List<EntityType> sortedTypes = new ArrayList<>(typeMetrics.keySet());
        sortedTypes.sort(Comparator.comparing(EntityType::name));
        
        for (EntityType type : sortedTypes) {
            EntityTypeMetrics metrics = typeMetrics.get(type);
            sb.append(String.format("%-15s %10d %10d %10d %10d %8.4f %8.4f %8.4f\n",
                type.getDescription(),
                metrics.getTp(),
                metrics.getFp(),
                metrics.getFn(),
                metrics.getTp() + metrics.getFn(),
                metrics.getPrecision(),
                metrics.getRecall(),
                metrics.getF1()));
        }
        
        // 置信度分布
        sb.append("\n【置信度分布】\n");
        sb.append(String.format("%-15s %10s %10s %10s\n", "置信度区间", "正确数", "总数", "准确率"));
        for (int i = 0; i < 50; i++) sb.append("-");
        sb.append("\n");
        
        List<String> ranges = Arrays.asList("0.9-1.0", "0.8-0.9", "0.7-0.8", 
                                           "0.6-0.7", "0.5-0.6", "0.0-0.5");
        for (String range : ranges) {
            ConfidenceMetrics cm = confidenceMetrics.get(range);
            if (cm != null) {
                sb.append(String.format("%-15s %10d %10d %10.4f\n",
                    range, cm.getCorrect(), cm.getTotal(), cm.getAccuracy()));
            }
        }
        
        // 性能指标
        sb.append("\n【性能指标】\n");
        sb.append(String.format("平均处理时间: %.2f ms\n", getAvgProcessingTime()));
        sb.append(String.format("P95处理时间: %d ms\n", getP95ProcessingTime()));
        sb.append(String.format("P99处理时间: %d ms\n", getP99ProcessingTime()));
        sb.append(String.format("吞吐量: %.2f queries/s\n", getThroughput()));
        
        sb.append("\n================================\n");
        return sb.toString();
    }
}

