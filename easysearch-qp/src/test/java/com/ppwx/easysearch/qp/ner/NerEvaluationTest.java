package com.ppwx.easysearch.qp.ner;

import com.ppwx.easysearch.qp.data.DictionaryResourceLoader;
import com.ppwx.easysearch.qp.data.MetaResourceLoader;
import com.ppwx.easysearch.qp.eval.GroundTruthAnnotation;
import com.ppwx.easysearch.qp.eval.NerEvaluationMetrics;
import com.ppwx.easysearch.qp.format.*;
import com.ppwx.easysearch.qp.eval.QueryEvaluationResult;
import com.ppwx.easysearch.qp.prediction.NerCategoryPrediction;
import com.ppwx.easysearch.qp.tokenizer.HanlpSegmentation;
import com.ppwx.easysearch.qp.tokenizer.ThreeCTokenizer;
import com.ppwx.easysearch.qp.tokenizer.Token;
import com.ppwx.easysearch.qp.util.StreamUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 命名实体识别准确度测试
 * 使用【top10w-20251010.csv】中的数据，该数据是采集用户的搜索数据
 * 
 * 评估指标体系：
 * 1. 基础指标：准确率(Precision)、召回率(Recall)、F1值、准确度(Accuracy)
 * 2. 实体级别指标：按实体类型统计、实体边界准确率、标准化准确率
 * 3. 业务指标：覆盖率、平均识别实体数、置信度分布
 * 4. 性能指标：响应时间、吞吐量
 * 
 * 评估方案：
 * 1. 有标注数据：使用人工标注的Ground Truth进行精确评估
 * 2. 无标注数据：基于规则的自动评估（词典覆盖率、置信度等）
 * 3. 分层评估：按搜索频率、查询复杂度分层统计
 */
public class NerEvaluationTest {

    private ThreeCTokenizer tokenizer;
    private EntityRecognizer recognizer;
    private WordFormat wordFormat;
    private NerCategoryPrediction prediction;

    @Before
    public void setUp() throws Exception {
        HanlpSegmentation segmentation = new HanlpSegmentation();
        tokenizer = new ThreeCTokenizer(segmentation);

        DefaultEntityIdentityMapper mapper = new DefaultEntityIdentityMapper();
        recognizer = ThreeCEntityRecognizerFactory.createEntityRecognizer(mapper);

        wordFormat = WordFormats.chains(new WordFormatHalfWidth(), new WordFormatSpecialChars(),
                new WordFormatTrim(), new WordFormatIgnoreCase());

        prediction = new NerCategoryPrediction(recognizer, tokenizer);

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

    /**
     * 命名实体识别
     */
    private Collection<Entity> ner(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return null;
        }
        // 格式化
        keyword = wordFormat.format(new StringBuilder(keyword)).toString();

        // 分词
        List<Token> tokens = tokenizer.tokenize(keyword);

        // 识别
        return recognizer.extractEntities(keyword, tokens);
    }

    /**
     * 1. 基于标注数据的完整评估
     * 使用人工标注的Ground Truth数据进行精确评估
     */
    @Test
    public void testWithGroundTruth() throws Exception {
        System.out.println("===== 基于标注数据的NER评估 =====\n");
        
        // 加载标注数据
        String annotationFile = "eval/ground_truth_ner.json";
        GroundTruthAnnotation groundTruth = null;
        
        try {
            groundTruth = GroundTruthAnnotation.loadFromJson(annotationFile);
            System.out.println("已加载标注数据: " + groundTruth.getAnnotationCount() + " 条");
        } catch (FileNotFoundException e) {
            System.out.println("警告: 标注文件不存在，将创建示例模板: " + annotationFile);
            createGroundTruthTemplate(annotationFile);
            return;
        }
        
        // 执行评估
        NerEvaluationMetrics metrics = new NerEvaluationMetrics();
        
        for (String query : groundTruth.getAnnotatedQueries()) {
            long startTime = System.currentTimeMillis();
            Collection<Entity> predicted = ner(query);
            long endTime = System.currentTimeMillis();
            
            List<Entity> truth = groundTruth.getAnnotation(query);
            QueryEvaluationResult result = new QueryEvaluationResult(query, predicted, truth);
            result.setProcessingTimeMs(endTime - startTime);
            
            metrics.addQueryResult(result);
        }
        
        // 输出评估报告
        System.out.println(metrics.generateReport());
    }

    /**
     * 2. 基于规则的自动评估（无需标注数据）
     * 适用于大规模数据的快速评估
     */
    @Test
    public void testAutoEvaluation() throws Exception {
        System.out.println("===== 基于规则的自动评估 =====\n");
        
        // 读取搜索词数据
        List<QueryData> queries = loadTopQueries("top10w-20251010.csv", 100000);
        System.out.println("加载查询数据: " + queries.size() + " 条\n");
        
        // 自动评估统计
        AutoEvaluationStats stats = new AutoEvaluationStats();
        
        for (QueryData queryData : queries) {
            long startTime = System.nanoTime();
            Collection<Entity> entities = ner(queryData.getKeyword());
            long endTime = System.nanoTime();
            
            stats.addResult(queryData, entities, (endTime - startTime) / 1_000_000);
        }
        
        // 输出统计报告
        stats.printReport();
    }

    /**
     * 3. 分层评估：按搜索频率分层
     */
    @Test
    public void testStratifiedEvaluation() throws Exception {
        System.out.println("===== 分层评估（按搜索频率） =====\n");
        
        List<QueryData> allQueries = loadTopQueries("top10w-20251010.csv", 10000);
        
        // 按搜索频率分层
        Map<String, List<QueryData>> stratified = stratifyByFrequency(allQueries);
        
        for (Map.Entry<String, List<QueryData>> entry : stratified.entrySet()) {
            String tier = entry.getKey();
            List<QueryData> queries = entry.getValue();
            
            System.out.println("\n【" + tier + "】 样本数: " + queries.size());
            
            AutoEvaluationStats stats = new AutoEvaluationStats();
            for (QueryData queryData : queries) {
                long startTime = System.nanoTime();
                Collection<Entity> entities = ner(queryData.getKeyword());
                long endTime = System.nanoTime();
                stats.addResult(queryData, entities, (endTime - startTime) / 1_000_000);
            }
            
            stats.printReport();
        }
    }

    /**
     * 4. 典型案例分析
     */
    @Test
    public void testCaseAnalysis() throws Exception {
        System.out.println("===== 典型案例分析 =====\n");
        
        // 选择一些典型案例
        String[] testCases = {
            "苹果iPhone 15 Pro Max",
            "华为 Mate X6",
            "三星 Galaxy Z Fold7",
            "苹果手机",
            "笔记本",
            "典藏",
            "小米 15 Ultra",
            "苹果15 pro max",
            "OPPO Find X8 Ultra"
        };
        
        for (String query : testCases) {
            System.out.println("查询: " + query);
            
            long startTime = System.nanoTime();
            Collection<Entity> entities = ner(query);
            long endTime = System.nanoTime();
            
            if (entities != null && !entities.isEmpty()) {
                for (Entity entity : entities) {
                    System.out.println(String.format("  [%s] %s -> %s (置信度: %.2f, 位置: %d-%d)",
                        entity.getType().getDescription(),
                        entity.getValue(),
                        entity.getNormalizedValue(),
                        entity.getConfidence(),
                        entity.getStartOffset(),
                        entity.getEndOffset()));
                }
            } else {
                System.out.println("  未识别到实体");
            }
            
            System.out.println("  处理时间: " + (endTime - startTime) / 1_000_000.0 + " ms\n");
        }
    }

    /**
     * 5. 实体类型覆盖率分析
     */
    @Test
    public void testEntityTypeCoverage() throws Exception {
        System.out.println("===== 实体类型覆盖率分析 =====\n");
        
        List<QueryData> queries = loadTopQueries("top10w-20251010.csv", 5000);
        
        Map<EntityType, Integer> typeCounts = new HashMap<>();
        int totalQueries = queries.size();
        int queriesWithEntities = 0;
        
        for (QueryData queryData : queries) {
            Collection<Entity> entities = ner(queryData.getKeyword());
            
            if (entities != null && !entities.isEmpty()) {
                queriesWithEntities++;
                for (Entity entity : entities) {
                    typeCounts.merge(entity.getType(), 1, Integer::sum);
                }
            }
        }
        
        final int finalQueriesWithEntities = queriesWithEntities;
        
        System.out.println("总查询数: " + totalQueries);
        System.out.println("识别出实体的查询数: " + finalQueriesWithEntities);
        System.out.println("覆盖率: " + String.format("%.2f%%", 100.0 * finalQueriesWithEntities / totalQueries));
        System.out.println("\n实体类型分布:");
        
        typeCounts.entrySet().stream()
            .sorted(Map.Entry.<EntityType, Integer>comparingByValue().reversed())
            .forEach(e -> System.out.println(String.format("  %-15s: %6d (%.2f%%)",
                e.getKey().getDescription(),
                e.getValue(),
                100.0 * e.getValue() / finalQueriesWithEntities)));
    }

    // ===== 辅助方法 =====

    /**
     * 加载搜索词数据
     */
    private List<QueryData> loadTopQueries(String filename, int limit) throws IOException {
        List<QueryData> queries = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                StreamUtil.getResourceStream(filename)))) {
            String line;
            boolean isHeader = true;
            int count = 0;
            
            while ((line = br.readLine()) != null && count < limit) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                
                String[] parts = line.split(",", 2);
                if (parts.length >= 2) {
                    String keyword = parts[0].trim();
                    int frequency = Integer.parseInt(parts[1].trim());
                    queries.add(new QueryData(keyword, frequency));
                    count++;
                }
            }
        }
        
        return queries;
    }

    /**
     * 按搜索频率分层
     */
    private Map<String, List<QueryData>> stratifyByFrequency(List<QueryData> queries) {
        Map<String, List<QueryData>> stratified = new LinkedHashMap<>();
        
        // 计算分位数
        List<Integer> frequencies = queries.stream()
            .map(QueryData::getFrequency)
            .sorted(Comparator.reverseOrder())
            .collect(Collectors.toList());
        
        int p75Index = (int) (frequencies.size() * 0.25);
        int p50Index = (int) (frequencies.size() * 0.50);
        int p25Index = (int) (frequencies.size() * 0.75);
        
        int p75 = frequencies.get(p75Index);
        int p50 = frequencies.get(p50Index);
        int p25 = frequencies.get(p25Index);
        
        stratified.put("高频查询 (Top 25%)", new ArrayList<>());
        stratified.put("中高频查询 (25%-50%)", new ArrayList<>());
        stratified.put("中低频查询 (50%-75%)", new ArrayList<>());
        stratified.put("低频查询 (Bottom 25%)", new ArrayList<>());
        
        for (QueryData query : queries) {
            int freq = query.getFrequency();
            if (freq >= p75) {
                stratified.get("高频查询 (Top 25%)").add(query);
            } else if (freq >= p50) {
                stratified.get("中高频查询 (25%-50%)").add(query);
            } else if (freq >= p25) {
                stratified.get("中低频查询 (50%-75%)").add(query);
            } else {
                stratified.get("低频查询 (Bottom 25%)").add(query);
            }
        }
        
        return stratified;
    }

    /**
     * 6. 分层采样生成标注数据
     * 从top10w-20251010.csv中按频率分层采样，自动生成标注数据
     * 采样策略：
     * - 高频查询(Top 10%): 100条
     * - 中频查询(10%-50%): 150条
     * - 低频查询(50%以下): 150条
     * 总计: 400条
     */
    @Test
    public void generateGroundTruthBySampling() throws Exception {
        System.out.println("===== 分层采样生成标注数据 =====\n");
        
        // 采样配置
        int highFreqCount = 100;    // 高频查询采样数
        int midFreqCount = 150;     // 中频查询采样数
        int lowFreqCount = 150;     // 低频查询采样数
        int totalSamples = highFreqCount + midFreqCount + lowFreqCount;
        
        System.out.println("采样策略:");
        System.out.println("  高频查询 (Top 10%): " + highFreqCount + " 条");
        System.out.println("  中频查询 (10%-50%): " + midFreqCount + " 条");
        System.out.println("  低频查询 (50%以下): " + lowFreqCount + " 条");
        System.out.println("  总计: " + totalSamples + " 条\n");
        
        // 加载所有查询数据
        List<QueryData> allQueries = loadTopQueries("top10w-20251010.csv", 100000);
        System.out.println("加载数据: " + allQueries.size() + " 条\n");
        
        // 分层采样
        Map<String, List<QueryData>> sampledData = stratifiedSampling(
            allQueries, highFreqCount, midFreqCount, lowFreqCount);
        
        // 生成标注数据
        GroundTruthAnnotation annotation = new GroundTruthAnnotation();
        int processedCount = 0;
        int annotatedCount = 0;
        
        for (Map.Entry<String, List<QueryData>> entry : sampledData.entrySet()) {
            String tier = entry.getKey();
            List<QueryData> queries = entry.getValue();
            
            System.out.println("处理 " + tier + ": " + queries.size() + " 条");
            
            for (QueryData queryData : queries) {
                processedCount++;
                
                // 执行实体识别
                Collection<Entity> entities = ner(queryData.getKeyword());
                
                // 只保存识别出实体的查询
                if (entities != null && !entities.isEmpty()) {
                    List<GroundTruthAnnotation.AnnotatedEntity> annotatedEntities = new ArrayList<>();
                    for (Entity entity : entities) {
                        annotatedEntities.add(GroundTruthAnnotation.AnnotatedEntity.fromEntity(entity));
                    }
                    annotation.addAnnotation(queryData.getKeyword(), annotatedEntities);
                    annotatedCount++;
                }
                
                // 进度显示
                if (processedCount % 50 == 0) {
                    System.out.println("  已处理: " + processedCount + "/" + totalSamples + 
                                     " (已标注: " + annotatedCount + ")");
                }
            }
        }
        
        // 保存到文件
        String outputFile = "src/test/resources/eval/ground_truth_ner.json";
        File file = new File(outputFile);
        file.getParentFile().mkdirs();
        
        annotation.saveToJson(outputFile);
        
        System.out.println("\n===== 采样完成 =====");
        System.out.println("总处理: " + processedCount + " 条");
        System.out.println("成功标注: " + annotatedCount + " 条");
        System.out.println("覆盖率: " + String.format("%.2f%%", 100.0 * annotatedCount / processedCount));
        System.out.println("文件保存到: " + outputFile);
        System.out.println("\n说明：");
        System.out.println("1. 标注数据已自动生成，基于当前NER系统的识别结果");
        System.out.println("2. 建议人工校验和修正标注数据，确保质量");
        System.out.println("3. 修正后可运行 testWithGroundTruth 进行精确评估");
    }
    
    /**
     * 分层采样
     * @param allQueries 所有查询数据
     * @param highFreqCount 高频查询采样数
     * @param midFreqCount 中频查询采样数
     * @param lowFreqCount 低频查询采样数
     * @return 分层采样结果
     */
    private Map<String, List<QueryData>> stratifiedSampling(
            List<QueryData> allQueries, 
            int highFreqCount, 
            int midFreqCount, 
            int lowFreqCount) {
        
        Map<String, List<QueryData>> result = new LinkedHashMap<>();
        
        // 按频率排序
        allQueries.sort(Comparator.comparingInt(QueryData::getFrequency).reversed());
        
        // 计算分层边界
        int totalSize = allQueries.size();
        int highFreqEndIndex = (int) (totalSize * 0.10);  // Top 10%
        int midFreqEndIndex = (int) (totalSize * 0.50);   // Top 50%
        
        // 分层
        List<QueryData> highFreqQueries = allQueries.subList(0, Math.min(highFreqEndIndex, totalSize));
        List<QueryData> midFreqQueries = allQueries.subList(
            Math.min(highFreqEndIndex, totalSize), 
            Math.min(midFreqEndIndex, totalSize));
        List<QueryData> lowFreqQueries = allQueries.subList(
            Math.min(midFreqEndIndex, totalSize), 
            totalSize);
        
        // 随机采样
        result.put("高频查询 (Top 10%)", randomSample(highFreqQueries, highFreqCount));
        result.put("中频查询 (10%-50%)", randomSample(midFreqQueries, midFreqCount));
        result.put("低频查询 (50%以下)", randomSample(lowFreqQueries, lowFreqCount));
        
        return result;
    }
    
    /**
     * 随机采样
     */
    private List<QueryData> randomSample(List<QueryData> source, int sampleSize) {
        if (source.size() <= sampleSize) {
            return new ArrayList<>(source);
        }
        
        List<QueryData> result = new ArrayList<>();
        List<QueryData> temp = new ArrayList<>(source);
        Random random = new Random(42); // 固定种子，确保可重复
        
        for (int i = 0; i < sampleSize; i++) {
            int index = random.nextInt(temp.size());
            result.add(temp.remove(index));
        }
        
        return result;
    }

    /**
     * 创建标注数据模板
     */
    private void createGroundTruthTemplate(String outputFile) throws Exception {
        System.out.println("\n生成标注模板...");
        
        List<QueryData> samples = loadTopQueries("top10w-20251010.csv", 100);
        GroundTruthAnnotation template = new GroundTruthAnnotation();
        
        for (QueryData queryData : samples) {
            Collection<Entity> predicted = ner(queryData.getKeyword());
            if (predicted != null && !predicted.isEmpty()) {
                List<GroundTruthAnnotation.AnnotatedEntity> entities = new ArrayList<>();
                for (Entity entity : predicted) {
                    entities.add(GroundTruthAnnotation.AnnotatedEntity.fromEntity(entity));
                }
                template.addAnnotation(queryData.getKeyword(), entities);
            }
        }
        
        // 确保目录存在
        File file = new File(outputFile);
        file.getParentFile().mkdirs();
        
        template.saveToJson(outputFile);
        System.out.println("模板已保存到: " + outputFile);
        System.out.println("请人工校验并修改标注数据后重新运行测试");
    }

    // ===== 内部类 =====

    /**
     * 查询数据
     */
    static class QueryData {
        private String keyword;
        private int frequency;
        
        public QueryData(String keyword, int frequency) {
            this.keyword = keyword;
            this.frequency = frequency;
        }
        
        public String getKeyword() { return keyword; }
        public int getFrequency() { return frequency; }
    }

    /**
     * 自动评估统计
     */
    static class AutoEvaluationStats {
        private int totalQueries = 0;
        private int queriesWithEntities = 0;
        private int totalEntities = 0;
        private int highConfidenceEntities = 0;
        private List<Long> processingTimes = new ArrayList<>();
        private Map<EntityType, Integer> entityTypeCounts = new HashMap<>();
        
        public void addResult(QueryData queryData, Collection<Entity> entities, long timeMs) {
            totalQueries++;
            processingTimes.add(timeMs);
            
            if (entities != null && !entities.isEmpty()) {
                queriesWithEntities++;
                totalEntities += entities.size();
                
                for (Entity entity : entities) {
                    if (entity.getConfidence() >= 0.8) {
                        highConfidenceEntities++;
                    }
                    entityTypeCounts.merge(entity.getType(), 1, Integer::sum);
                }
            }
        }
        
        public void printReport() {
            System.out.println("【自动评估结果】");
            System.out.println("总查询数: " + totalQueries);
            System.out.println("识别出实体的查询数: " + queriesWithEntities);
            System.out.println(String.format("覆盖率: %.2f%%", 100.0 * queriesWithEntities / totalQueries));
            System.out.println(String.format("平均实体数/查询: %.2f", (double) totalEntities / totalQueries));
            System.out.println(String.format("高置信度实体占比: %.2f%%", 
                100.0 * highConfidenceEntities / Math.max(1, totalEntities)));
            
            double avgTime = processingTimes.stream().mapToLong(Long::longValue).average().orElse(0);
            System.out.println(String.format("平均处理时间: %.2f ms", avgTime));
            
            System.out.println("\n实体类型分布:");
            entityTypeCounts.entrySet().stream()
                .sorted(Map.Entry.<EntityType, Integer>comparingByValue().reversed())
                .limit(10)
                .forEach(e -> System.out.println(String.format("  %-15s: %d", 
                    e.getKey().getDescription(), e.getValue())));
        }
    }
}
