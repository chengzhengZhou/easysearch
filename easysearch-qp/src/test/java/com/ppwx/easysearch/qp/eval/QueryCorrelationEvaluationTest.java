package com.ppwx.easysearch.qp.eval;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.hankcs.hanlp.HanLP;
import com.ppwx.easysearch.qp.data.DictionaryResourceLoader;
import com.ppwx.easysearch.qp.data.MetaResourceLoader;
import com.ppwx.easysearch.qp.data.MetaTermOpt;
import com.ppwx.easysearch.qp.format.WordFormat;
import com.ppwx.easysearch.qp.format.WordFormatHalfWidth;
import com.ppwx.easysearch.qp.format.WordFormatSpecialChars;
import com.ppwx.easysearch.qp.format.WordFormats;
import com.ppwx.easysearch.qp.ner.*;
import com.ppwx.easysearch.qp.prediction.NerCategoryPrediction;
import com.ppwx.easysearch.qp.tokenizer.HanlpSegmentation;
import com.ppwx.easysearch.qp.tokenizer.ThreeCTokenizer;
import com.ppwx.easysearch.qp.util.StreamUtil;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@RunWith(MockitoJUnitRunner.class)
public class QueryCorrelationEvaluationTest {

    private static final String QUERY_URL = "https://dubai.aihuishou.com/ahs-yanxuan-service/products/search-goods-v2";

    ThreeCTokenizer tokenizer;

    EntityRecognizer recognizer;

    WordFormat wordFormat;

    NerCategoryPrediction prediction;

    MetaObserver metaObserver;

    @Before
    public void load() throws IOException {
        HanlpSegmentation segmentation = new HanlpSegmentation();
        tokenizer = new ThreeCTokenizer(segmentation);

        DefaultEntityIdentityMapper mapper = new DefaultEntityIdentityMapper();
        recognizer = ThreeCEntityRecognizerFactory.createEntityRecognizer(mapper);

        wordFormat = WordFormats.chains(WordFormats.truncate(),
                new WordFormatHalfWidth(),
                new WordFormatSpecialChars(),
                WordFormats.ignoreCase());
        metaObserver = new MetaObserver();

        prediction = new NerCategoryPrediction(recognizer, tokenizer);

        HanLP.Config.enableDebug(false);
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
        metaLoader.addObserver(metaObserver);
        metaLoader.loadResources();
    }

    @Test
    public void testEvaluation() {
        String query = "14";
        // 1. 创建查询相关性评估结果
        List<Entity> queryEntities = extractEntities(query);
        List<QueryCorrelationResult.ProductResult> searchResults = query(query);

        QueryCorrelationResult result = new QueryCorrelationResult(
                query,
                queryEntities,
                searchResults
        );

        // 2. 获取相关性得分
        //List<QueryCorrelationResult.ProductRelevanceScore> scores = result.getRelevanceScores();

        // 3. 批量评估多个查询
        List<QueryCorrelationResult> allResults = Lists.newArrayList(result);
        QueryCorrelationEvalMetrics metrics = new QueryCorrelationEvalMetrics(allResults);

        // 4. 查看评估报告
        System.out.println(metrics.generateReport());
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
        int highFreqCount = 1000;    // 高频查询采样数
        int midFreqCount = 1500;     // 中频查询采样数
        int lowFreqCount = 1500;     // 低频查询采样数
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

        for (Map.Entry<String, List<QueryData>> entry : sampledData.entrySet()) {
            String tier = entry.getKey();
            List<QueryData> queries = entry.getValue();

            System.out.println("\n【" + tier + "】 样本数: " + queries.size());
            int processed = 0;
            List<QueryCorrelationResult> allResults = Lists.newArrayList();
            for (QueryData queryData : queries) {
                List<Entity> queryEntities = extractEntities(queryData.getKeyword());
                List<QueryCorrelationResult.ProductResult> searchResults = query(queryData.getKeyword());

                QueryCorrelationResult result = new QueryCorrelationResult(
                    queryData.getKeyword(),
                    queryEntities,
                    searchResults
                );

                allResults.add(result);
                processed++;
                if (processed % 100 == 0) {
                    System.out.println("已处理: " + processed + " 条");
                }
            }
            QueryCorrelationEvalMetrics metrics = new QueryCorrelationEvalMetrics(allResults);
            System.out.println(metrics.generateReport());
        }
    }

    /**
     * 高中低频数据导出
     */
    @Test
    public void generateGroundTruthBySampling2() throws Exception {
        int highFreqCount = 100;    // 高频查询采样数
        int midFreqCount = 100;     // 中频查询采样数
        int lowFreqCount = 100;     // 低频查询采样数
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

        // 输出数据
        Set<String> keys = sampledData.keySet();
        for (String key : keys) {
            List<QueryData> queries = sampledData.get(key);
            List<String> collect = queries.stream().map(QueryData::getKeyword).collect(Collectors.toList());
            FileUtil.writeLines(collect, FileUtil.file("export/" + key + ".txt"), StandardCharsets.UTF_8, false);
        }
    }

    private List<Entity> extractEntities(String query) {
        String formatQuery = wordFormat.format(new StringBuilder(query)).toString();
        List<Entity> queryEntities = (List<Entity>) recognizer.extractEntities(formatQuery, tokenizer.tokenize(formatQuery));
        // 平铺品牌和品类
        List<Entity> flatter = Lists.newArrayList();
        queryEntities.stream()
                .filter(entity -> entity.getType() == EntityType.MODEL)
                .forEach(entity -> {
                    MetaTermOpt metaTermOpt = metaObserver.getMetaTermOpt(entity);
                    if (metaTermOpt != null) {
                        flatter.add(new Entity(metaTermOpt.getBrandName(),
                                EntityType.BRAND, metaTermOpt.getBrandName(),
                                Lists.newArrayList(metaTermOpt.getBrandId())));
                        flatter.add(new Entity(metaTermOpt.getCategoryName(),
                                EntityType.CATEGORY, metaTermOpt.getCategoryName(),
                                Lists.newArrayList(metaTermOpt.getCategoryId())));
                    }
                });
        flatter.addAll(queryEntities);
        return flatter;
    }

    /**
     * 调用搜索服务请求
     * @param keyword
     * @return
     */
    private List<QueryCorrelationResult.ProductResult> query(String keyword) {
        try(HttpResponse search = search(keyword)) {
            if (!search.isOk()) {
                System.out.println("搜索服务请求失败: " + search.body());
                return Collections.emptyList();
            }
            JSONArray objects = JSON.parseObject(search.body()).getJSONArray("data");
            AtomicInteger rank = new AtomicInteger(1);
            return objects.stream().map(obj -> {
                JSONObject jsonObject = (JSONObject) obj;
                QueryCorrelationResult.ProductResult productResult = new QueryCorrelationResult.ProductResult();
                productResult.setProductId(jsonObject.getString("saleGoodsNo"));
                productResult.setCategoryId(jsonObject.getString("gaeaCategoryId"));
                productResult.setCategory(metaObserver.getCategoryName(jsonObject.getString("gaeaCategoryId")));
                productResult.setBrandId(jsonObject.getString("gaeaBrandId"));
                productResult.setBrand(metaObserver.getBrandName(jsonObject.getString("gaeaBrandId")));
                productResult.setModelId(jsonObject.getString("gaeaProductId"));
                productResult.setModel(metaObserver.getModelName(jsonObject.getString("gaeaProductId")));
                productResult.setRank(rank.getAndIncrement());
                // 优先使用机型名称
                if (StringUtils.isNotBlank(productResult.getModel())) {
                    productResult.setTitle(productResult.getModel());
                } else {
                    productResult.setTitle(jsonObject.getString("name"));
                }
                return productResult;
            }).collect(Collectors.toList());
        }
    }

    private HttpResponse search(String keyword) {
        JSONObject json = new JSONObject();
        json.put("keyword", keyword);
        json.put("pageSize", 10);
        json.put("scene", "search");

        HttpRequest post = HttpRequest.post(QUERY_URL);
        post.header("ahs-device-id", "123456789");
        post.header("ahs-app-id", "10002");
        post.header("user-agent", "java client");
        post.body(json.toJSONString(), "application/json");
        return post.execute();
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
}
