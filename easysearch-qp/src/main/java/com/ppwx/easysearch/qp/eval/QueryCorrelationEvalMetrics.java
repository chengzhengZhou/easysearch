/*
 * Copyright 2026 chengzhengZhou
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ppwx.easysearch.qp.eval;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 查询相关性评估指标
 * 用于计算查询的各项性能指标
 * 评估指标：
 * NDCG@10 / NDCG@5：归一化折损累积增益
 * MRR：平均倒数排名
 * Precision@10 / Precision@5：准确率
 * Recall@10 / Recall@5：召回率
 * MAP：平均精确率
 * 平均相关性得分
 */
public class QueryCorrelationEvalMetrics {
    
    private List<QueryCorrelationResult> results;
    
    // 评估指标
    private double meanRelevanceScore;           // 平均相关性得分
    private double ndcgAt10;                     // NDCG@10
    private double ndcgAt5;                      // NDCG@5
    private double mrr;                          // Mean Reciprocal Rank
    private double precisionAt10;                 // Precision@10
    private double precisionAt5;                 // Precision@5
    private double recallAt10;                    // Recall@10
    private double recallAt5;                     // Recall@5
    private double map;                          // Mean Average Precision
    
    public QueryCorrelationEvalMetrics(List<QueryCorrelationResult> results) {
        this.results = results != null ? results : new ArrayList<>();
        calculateMetrics();
    }
    
    /**
     * 计算所有评估指标
     */
    private void calculateMetrics() {
        if (results.isEmpty()) {
            return;
        }
        
        double totalRelevanceScore = 0.0;
        double totalNdcg10 = 0.0;
        double totalNdcg5 = 0.0;
        double totalMrr = 0.0;
        double totalPrecision10 = 0.0;
        double totalPrecision5 = 0.0;
        double totalRecall10 = 0.0;
        double totalRecall5 = 0.0;
        double totalMap = 0.0;
        
        int queryCount = 0;
        
        for (QueryCorrelationResult result : results) {
            if (result.getRelevanceScores().isEmpty()) {
                continue;
            }
            
            queryCount++;
            
            // 平均相关性得分
            double avgScore = result.getRelevanceScores().stream()
                .mapToDouble(QueryCorrelationResult.ProductRelevanceScore::getTotalScore)
                .average()
                .orElse(0.0);
            totalRelevanceScore += avgScore;
            
            // NDCG@10
            double ndcg10 = calculateNDCG(result, 10);
            totalNdcg10 += ndcg10;
            
            // NDCG@5
            double ndcg5 = calculateNDCG(result, 5);
            totalNdcg5 += ndcg5;
            
            // MRR
            double mrr = calculateMRR(result);
            totalMrr += mrr;
            
            // Precision@K
            double prec10 = calculatePrecision(result, 10);
            totalPrecision10 += prec10;
            
            double prec5 = calculatePrecision(result, 5);
            totalPrecision5 += prec5;
            
            // Recall@K
            double rec10 = calculateRecall(result, 10);
            totalRecall10 += rec10;
            
            double rec5 = calculateRecall(result, 5);
            totalRecall5 += rec5;
            
            // MAP
            double map = calculateMAP(result);
            totalMap += map;
        }
        
        if (queryCount > 0) {
            this.meanRelevanceScore = totalRelevanceScore / queryCount;
            this.ndcgAt10 = totalNdcg10 / queryCount;
            this.ndcgAt5 = totalNdcg5 / queryCount;
            this.mrr = totalMrr / queryCount;
            this.precisionAt10 = totalPrecision10 / queryCount;
            this.precisionAt5 = totalPrecision5 / queryCount;
            this.recallAt10 = totalRecall10 / queryCount;
            this.recallAt5 = totalRecall5 / queryCount;
            this.map = totalMap / queryCount;
        }
    }
    
    /**
     * 计算NDCG@K (Normalized Discounted Cumulative Gain)
     * NDCG考虑了排序位置，位置越靠前权重越高
     */
    private double calculateNDCG(QueryCorrelationResult result, int k) {
        List<QueryCorrelationResult.ProductRelevanceScore> scores = 
            result.getRelevanceScores().stream()
                .limit(k)
                .collect(Collectors.toList());
        
        if (scores.isEmpty()) {
            return 0.0;
        }
        
        // 计算DCG
        double dcg = 0.0;
        for (int i = 0; i < scores.size(); i++) {
            double relevance = scores.get(i).getTotalScore();
            int position = i + 1;
            dcg += relevance / (Math.log(position + 1) / Math.log(2));
        }
        
        // 计算理想DCG (IDCG) - 按相关性得分降序排列
        List<Double> idealScores = scores.stream()
            .map(QueryCorrelationResult.ProductRelevanceScore::getTotalScore)
            .sorted((a, b) -> Double.compare(b, a))
            .collect(Collectors.toList());
        
        double idcg = 0.0;
        for (int i = 0; i < idealScores.size(); i++) {
            double relevance = idealScores.get(i);
            int position = i + 1;
            idcg += relevance / (Math.log(position + 1) / Math.log(2));
        }
        
        return idcg > 0 ? dcg / idcg : 0.0;
    }
    
    /**
     * 计算MRR (Mean Reciprocal Rank)
     * 第一个相关结果的位置的倒数
     */
    private double calculateMRR(QueryCorrelationResult result) {
        List<QueryCorrelationResult.ProductRelevanceScore> scores = 
            result.getRelevanceScores();
        
        for (int i = 0; i < scores.size(); i++) {
            if (scores.get(i).isRelevant()) {
                return 1.0 / (i + 1);
            }
        }
        
        return 0.0;
    }
    
    /**
     * 计算Precision@K
     * 前K个结果中相关结果的比例
     */
    private double calculatePrecision(QueryCorrelationResult result, int k) {
        List<QueryCorrelationResult.ProductRelevanceScore> scores = 
            result.getRelevanceScores().stream()
                .limit(k)
                .collect(Collectors.toList());
        
        if (scores.isEmpty()) {
            return 0.0;
        }
        
        long relevantCount = scores.stream()
            .filter(QueryCorrelationResult.ProductRelevanceScore::isRelevant)
            .count();
        
        return (double) relevantCount / scores.size();
    }
    
    /**
     * 计算Recall@K
     * 前K个结果中相关结果数 / 总相关结果数
     */
    private double calculateRecall(QueryCorrelationResult result, int k) {
        List<QueryCorrelationResult.ProductRelevanceScore> allScores = 
            result.getRelevanceScores();
        
        long totalRelevant = allScores.stream()
            .filter(QueryCorrelationResult.ProductRelevanceScore::isRelevant)
            .count();
        
        if (totalRelevant == 0) {
            return 0.0;
        }
        
        List<QueryCorrelationResult.ProductRelevanceScore> topKScores = 
            allScores.stream()
                .limit(k)
                .collect(Collectors.toList());
        
        long relevantInTopK = topKScores.stream()
            .filter(QueryCorrelationResult.ProductRelevanceScore::isRelevant)
            .count();
        
        return (double) relevantInTopK / totalRelevant;
    }
    
    /**
     * 计算MAP (Mean Average Precision)
     * 平均精确率
     */
    private double calculateMAP(QueryCorrelationResult result) {
        List<QueryCorrelationResult.ProductRelevanceScore> scores = 
            result.getRelevanceScores();
        
        long totalRelevant = scores.stream()
            .filter(QueryCorrelationResult.ProductRelevanceScore::isRelevant)
            .count();
        
        if (totalRelevant == 0) {
            return 0.0;
        }
        
        double ap = 0.0;
        int relevantFound = 0;
        
        for (int i = 0; i < scores.size(); i++) {
            if (scores.get(i).isRelevant()) {
                relevantFound++;
                double precision = (double) relevantFound / (i + 1);
                ap += precision;
            }
        }
        
        return ap / totalRelevant;
    }
    
    // ===== Getters =====
    
    public double getMeanRelevanceScore() {
        return meanRelevanceScore;
    }
    
    public double getNdcgAt10() {
        return ndcgAt10;
    }
    
    public double getNdcgAt5() {
        return ndcgAt5;
    }
    
    public double getMrr() {
        return mrr;
    }
    
    public double getPrecisionAt10() {
        return precisionAt10;
    }
    
    public double getPrecisionAt5() {
        return precisionAt5;
    }
    
    public double getRecallAt10() {
        return recallAt10;
    }
    
    public double getRecallAt5() {
        return recallAt5;
    }
    
    public double getMap() {
        return map;
    }
    
    public List<QueryCorrelationResult> getResults() {
        return results;
    }
    
    /**
     * 生成评估报告
     */
    public String generateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("========== 查询相关性评估报告 ==========\n");
        sb.append(String.format("评估查询数量: %d\n", results.size()));
        sb.append(String.format("平均相关性得分: %.4f\n", meanRelevanceScore));
        sb.append(String.format("NDCG@10: %.4f\n", ndcgAt10));
        sb.append(String.format("NDCG@5: %.4f\n", ndcgAt5));
        sb.append(String.format("MRR: %.4f\n", mrr));
        sb.append(String.format("Precision@10: %.4f\n", precisionAt10));
        sb.append(String.format("Precision@5: %.4f\n", precisionAt5));
        sb.append(String.format("Recall@10: %.4f\n", recallAt10));
        sb.append(String.format("Recall@5: %.4f\n", recallAt5));
        sb.append(String.format("MAP: %.4f\n", map));
        sb.append("========================================\n");
        return sb.toString();
    }
}
