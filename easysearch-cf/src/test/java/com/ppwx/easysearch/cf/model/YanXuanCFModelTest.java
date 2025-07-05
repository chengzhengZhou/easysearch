/**
 * Copyright (C), 2010-2024, 爱回收
 * FileName: YanXuanCFModelTest
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2024/8/7 17:54
 * Description: 严选数据测试
 */
package com.ppwx.easysearch.cf.model;

import com.google.common.base.Splitter;
import com.google.common.collect.BiMap;
import com.google.common.collect.Maps;
import com.ppwx.easysearch.cf.data.CFTextDataModel;
import com.ppwx.easysearch.cf.data.UserRating;
import com.ppwx.easysearch.cf.rank.ItemCFRankService;
import com.ppwx.easysearch.cf.rank.RankService;
import com.ppwx.easysearch.cf.repository.ESModelDataRepository;
import com.ppwx.easysearch.cf.repository.MemoryModelDataRepository;
import com.ppwx.easysearch.cf.repository.ModelDataRepository;
import com.ppwx.easysearch.cf.repository.ModelDataRepositoryProxy;
import com.ppwx.easysearch.cf.similarity.SumSimilarity;
import com.ppwx.easysearch.cf.util.DataSourceFactory;
import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.conf.Configured;
import net.librec.data.model.TextDataModel;
import net.librec.eval.rating.MAEEvaluator;
import net.librec.eval.rating.RMSEEvaluator;
import net.librec.filter.GenericRecommendedFilter;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.SparseMatrix;
import net.librec.recommender.Recommender;
import net.librec.recommender.RecommenderContext;
import net.librec.recommender.cf.ItemKNNRecommender;
import net.librec.recommender.item.ItemEntry;
import net.librec.recommender.item.RecommendedItem;
import net.librec.recommender.item.RecommendedItemList;
import net.librec.similarity.PCCSimilarity;
import net.librec.similarity.RecommenderSimilarity;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * 严选数据测试
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2024/08/07 17:54
 * @since 1.0.0
 */
public class YanXuanCFModelTest {

    private ItemCFModel model;

    private CFTextDataModel dataModel;

    private ModelDataRepository repository;

    private Map<String, String> allItems;

    private void train(Configuration conf) throws LibrecException {
        train(conf, false);
    }

    private void train(Configuration conf, boolean rec) throws LibrecException {
        if (this.repository == null) {
            this.repository = new MemoryModelDataRepository();
        }
        conf.set("dfs.data.dir", getSourceFile());
        CFTextDataModel dataModel = new CFTextDataModel(conf);
        dataModel.buildDataModel();

        conf.set("rec.recommender.similarity.key" ,"item");
        RecommenderSimilarity similarity = new SumSimilarity();
        similarity.buildSimilarityMatrix(dataModel);
        // train
        ItemCFModel itemCFModel = new ItemCFModel();
        itemCFModel.setDataModel(dataModel);
        itemCFModel.setSimilarity(similarity);
        itemCFModel.setRepository(repository);
        itemCFModel.train();
        itemCFModel.saveModel();
        this.dataModel = dataModel;
        this.model = itemCFModel;
    }

    private String getSourceFile() {
        URL url = this.getClass().getResource("/data/test");
        return url.getFile().substring(1);
    }

    @Test
    public void testItemCFModelWorks() throws LibrecException {
        repository = new MemoryModelDataRepository();
        Configuration conf = new Configuration();
        Configuration.Resource resource = new Configuration.Resource("rec/cf/itemknn-test.properties");
        conf.addResource(resource);
        conf.set("data.input.path", "yanxuan/product-ua");
        //conf.set("data.input.path", "yanxuan/ua");
        conf.set(Configured.CONF_DATA_COLUMN_FORMAT, "UIRT");
        conf.set("data.splitter.trainset.ratio", "0.8");
        conf.set("rec.recommender.similarity.key", "item");
        conf.set("rec.similarity.shrinkage", "0");
        train(conf);

        ItemCFModel itemCFModel = model;
        itemCFModel.predict();
        /*ItemCFModel itemCFModel = new ItemCFModel();
        itemCFModel.setRepository(repository);
        itemCFModel.loadModel();
        itemCFModel.predict();
*/
        BiMap<Integer, String> users = dataModel.getUserMappingData().inverse();
        BiMap<Integer, String> items = dataModel.getItemMappingData().inverse();

        SparseMatrix testData = (SparseMatrix) dataModel.getTestDataSet();
        RecommendedItemList recommendedList = new RecommendedItemList(users.size() - 1, users.size());
        for (MatrixEntry matrixEntry : testData) {
            int userIdx = matrixEntry.row();
            int itemIdx = matrixEntry.column();
            double predictRating = itemCFModel.predict(users.get(userIdx), items.get(itemIdx));
            if (Double.isNaN(predictRating)) {
                recommendedList.addUserItemIdx(userIdx, itemIdx, itemCFModel.globalMean);
            } else {
                recommendedList.addUserItemIdx(userIdx, itemIdx, predictRating);
            }
        }

        RMSEEvaluator evaluator = new RMSEEvaluator();
        System.out.println("RMSE:" + evaluator.evaluate(testData, recommendedList));
        MAEEvaluator maeEvaluator = new MAEEvaluator();
        System.out.println("MAE:" + maeEvaluator.evaluate(testData, recommendedList));
    }

    @Test
    public void testItemKNNRecWorks() throws LibrecException {
        Configuration conf = new Configuration();
        Configuration.Resource resource = new Configuration.Resource("rec/cf/itemknn-test.properties");
        conf.addResource(resource);
        conf.set("dfs.data.dir", getSourceFile());
        //conf.set("data.input.path", "yanxuan/ua");
        conf.set("data.input.path", "yanxuan/product-ua");
        conf.set("data.splitter.trainset.ratio", "0.8");
        conf.set("rec.recommender.isranking", "true");
        conf.set("rec.recommender.similarity.key", "item");
        conf.set("rec.similarity.shrinkage", "0");
        TextDataModel dataModel = new TextDataModel(conf);
        dataModel.buildDataModel();
        // build recommender context
        RecommenderContext context = new RecommenderContext(conf, dataModel);
        // build similarity
        RecommenderSimilarity similarity = new PCCSimilarity();
        similarity.buildSimilarityMatrix(dataModel);
        context.setSimilarity(similarity);
        // build recommender
        conf.set("rec.neighbors.knn.number", "10");
        Recommender recommender = new ItemKNNRecommender();
        recommender.setContext(context);
        // run recommender algorithm
        recommender.recommend(context);
        // evaluate the recommended result
        // 均方根误差：差值累积 / 总数，平方开根
        /*RecommenderEvaluator evaluator = new RMSEEvaluator();
        System.out.println("RMSE:" + recommender.evaluate(evaluator));
        // 平均绝对误差：
        evaluator = new MAEEvaluator();
        System.out.println("MAE:" + recommender.evaluate(evaluator));*/

        List<RecommendedItem> recommendedList = recommender.getRecommendedList();
        GenericRecommendedFilter filter = new GenericRecommendedFilter();
        filter.setUserIdList(Arrays.asList("2208181745584263", "2205231928347677", "201508915575"));
        //filter.setItemIdList(itemIdList);
        recommendedList = filter.filter(recommendedList);
        for (RecommendedItem recommendedItem : recommendedList) {
            System.out.println(
                    "user:" + recommendedItem.getUserId() + " " +
                            "item:" + recommendedItem.getItemId() + " " +
                            "value:" + recommendedItem.getValue()
            );
        }
    }

    @Test
    public void trainModel() throws LibrecException {
        initJdbcRepository();

        Configuration conf = new Configuration();
        Configuration.Resource resource = new Configuration.Resource("rec/cf/itemknn-test.properties");
        conf.addResource(resource);
        conf.set("data.input.path", "yanxuan/product-ua");
        conf.set(Configured.CONF_DATA_COLUMN_FORMAT, "UIRT");
        //conf.set("data.input.path", "yanxuan/ua");
        conf.set("data.splitter.trainset.ratio", "0.9");
        conf.set("rec.recommender.similarity.key", "item");
        conf.set("rec.similarity.shrinkage", "0");
        train(conf);
    }

    public void initJdbcRepository() {
        /*DataSource dataSource = DataSourceFactory.newDataSource();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource, false);
        this.repository = new JdbcModelDataRepository( "t_", jdbcTemplate);*/
        // es datasource
        this.repository = new ModelDataRepositoryProxy(new MemoryModelDataRepository(),
                new ESModelDataRepository(DataSourceFactory.newESClient()));
    }

    public void loadItems() throws IOException {
        try (InputStream resourceAsStream = this.getClass().getResourceAsStream("/data/test/yanxuan/items.txt");
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resourceAsStream, "GBK"))) {
            allItems = Maps.newHashMap();
            Splitter splitter = Splitter.on("==");
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                Iterator<String> iterator = splitter.split(line).iterator();
                allItems.put(iterator.next(), iterator.next());
            }
        }

    }

    @Test
    public void incrementalTrain() throws LibrecException {
        initJdbcRepository();

        Configuration conf = new Configuration();
        Configuration.Resource resource = new Configuration.Resource("rec/cf/itemknn-test.properties");
        conf.addResource(resource);
        conf.set("dfs.data.dir", getSourceFile());
        conf.set("data.input.path", "yanxuan/ua_5");
        conf.set("data.splitter.trainset.ratio", "0.9");
        conf.set("rec.recommender.similarity.key", "item");
        conf.set("rec.similarity.shrinkage", "0");
        CFTextDataModel dataModel = new CFTextDataModel(conf);
        dataModel.buildDataModel();

        ItemCFModel itemCFModel = new ItemCFModel();
        itemCFModel.setDataModel(dataModel);
        itemCFModel.setRepository(repository);
        itemCFModel.incrementalTrain();
    }

    @Test
    public void testPredict() throws IOException {
        loadItems();
        initJdbcRepository();

        ItemCFModel itemCFModel = new ItemCFModel();
        itemCFModel.setRepository(repository);
        itemCFModel.loadModel();
        itemCFModel.predict();

        String user = "2306221757133297";
        System.out.println("---------------------" + user + "-------------------------");
        List<ItemEntry<String, Double>> list = recommendRank(user, itemCFModel, allItems.keySet());
        for (ItemEntry<String, Double> entry : list) {
            System.out.println(allItems.get(entry.getKey()) + " > "  + entry.getValue());
        }

        user = "2408061619127766";
        System.out.println("---------------------" + user + "-------------------------");
        list = recommendRank(user, itemCFModel, allItems.keySet());
        for (ItemEntry<String, Double> entry : list) {
            System.out.println(allItems.get(entry.getKey()) + " > "  + entry.getValue());
        }
        user = "2304010943510733";
        System.out.println("---------------------" + user + "-------------------------");
        list = recommendRank(user, itemCFModel, allItems.keySet());
        for (ItemEntry<String, Double> entry : list) {
            System.out.println(allItems.get(entry.getKey()) + " > "  + entry.getValue());
        }
        user = "2302181054316539";
        System.out.println("---------------------" + user + "-------------------------");
        list = recommendRank(user, itemCFModel, allItems.keySet());
        for (ItemEntry<String, Double> entry : list) {
            System.out.println(allItems.get(entry.getKey()) + " > "  + entry.getValue());
        }
        user = "2308240855331080";
        System.out.println("---------------------" + user + "-------------------------");
        list = recommendRank(user, itemCFModel, allItems.keySet());
        for (ItemEntry<String, Double> entry : list) {
            System.out.println(allItems.get(entry.getKey()) + " > "  + entry.getValue());
        }
        user = "2403171547451965";
        System.out.println("---------------------" + user + "-------------------------");
        list = recommendRank(user, itemCFModel, allItems.keySet());
        for (ItemEntry<String, Double> entry : list) {
            System.out.println(allItems.get(entry.getKey()) + " > "  + entry.getValue());
        }
        user = "2101152145890096";
        System.out.println("---------------------" + user + "-------------------------");
        list = recommendRank(user, itemCFModel, allItems.keySet());
        for (ItemEntry<String, Double> entry : list) {
            System.out.println(allItems.get(entry.getKey()) + " > "  + entry.getValue());
        }
        user = "2408041957046050";
        System.out.println("---------------------" + user + "-------------------------");
        list = recommendRank(user, itemCFModel, allItems.keySet());
        for (ItemEntry<String, Double> entry : list) {
            System.out.println(allItems.get(entry.getKey()) + " > "  + entry.getValue());
        }
        user = "2308252112850567";
        System.out.println("---------------------" + user + "-------------------------");
        list = recommendRank(user, itemCFModel, allItems.keySet());
        for (ItemEntry<String, Double> entry : list) {
            System.out.println(allItems.get(entry.getKey()) + " > "  + entry.getValue());
        }
        user = "2309101017634370";
        System.out.println("---------------------" + user + "-------------------------");
        list = recommendRank(user, itemCFModel, allItems.keySet());
        for (ItemEntry<String, Double> entry : list) {
            System.out.println(allItems.get(entry.getKey()) + " > "  + entry.getValue());
        }

    }

    @Test
    public void testJdbcItemPredict() throws IOException {
        loadItems();
        initJdbcRepository();

        RankService rankService = new ItemCFRankService(repository);

        String user = "2306221757133297";
        System.out.println("---------------------" + user + "-------------------------");
        Collection<ItemEntry<String, Double>> list = rankService.rank(user, 10);
        for (ItemEntry<String, Double> entry : list) {
            System.out.println(allItems.get(entry.getKey()) + " > "  + entry.getValue());
        }

        user = "2408061619127766";
        System.out.println("---------------------" + user + "-------------------------");
        list = rankService.rank(user, 10);
        for (ItemEntry<String, Double> entry : list) {
            System.out.println(allItems.get(entry.getKey()) + " > "  + entry.getValue());
        }

    }

    protected List<ItemEntry<String, Double>> recommendRank(String user, ItemCFModel model, Collection<String> allItems) {
        Set<String> userPreItems = repository.getUserRating(user).stream().map(UserRating::getItemId).collect(Collectors.toSet());
        List<String> items = allItems.stream().filter(item -> !userPreItems.contains(item)).collect(Collectors.toList());
        List<ItemEntry<String, Double>> idScoreList = model.batchPredict(user, items);
        idScoreList = net.librec.util.Lists.sortItemEntryListTopK(idScoreList, true, 10);
        return idScoreList;
    }


    /*************************************************采取机型相似*************************************************/
    public void loadProducts() throws IOException {
        try (InputStream resourceAsStream = this.getClass().getResourceAsStream("/data/test/yanxuan/products.txt");
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resourceAsStream, "GBK"))) {
            allItems = Maps.newHashMap();
            Splitter splitter = Splitter.on("==");
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                Iterator<String> iterator = splitter.split(line).iterator();
                allItems.put(iterator.next(), iterator.next());
            }
        }
    }

    @Test
    public void testProductPredict() throws IOException {
        loadProducts();
        initJdbcRepository();

        ItemCFModel itemCFModel = new ItemCFModel();
        itemCFModel.setRepository(repository);
        itemCFModel.loadModel();
        itemCFModel.predict();

        String user = "2306221757133297";
        System.out.println("---------------------" + user + "-------------------------");
        List<ItemEntry<String, Double>> list = recommendRank(user, itemCFModel, allItems.keySet());
        for (ItemEntry<String, Double> entry : list) {
            System.out.println(allItems.get(entry.getKey()) + " > "  + entry.getValue());
        }

        user = "2408061619127766";
        System.out.println("---------------------" + user + "-------------------------");
        list = recommendRank(user, itemCFModel, allItems.keySet());
        for (ItemEntry<String, Double> entry : list) {
            System.out.println(allItems.get(entry.getKey()) + " > "  + entry.getValue());
        }
        user = "2304010943510733";
        System.out.println("---------------------" + user + "-------------------------");
        list = recommendRank(user, itemCFModel, allItems.keySet());
        for (ItemEntry<String, Double> entry : list) {
            System.out.println(allItems.get(entry.getKey()) + " > "  + entry.getValue());
        }
        user = "2302181054316539";
        System.out.println("---------------------" + user + "-------------------------");
        list = recommendRank(user, itemCFModel, allItems.keySet());
        for (ItemEntry<String, Double> entry : list) {
            System.out.println(allItems.get(entry.getKey()) + " > "  + entry.getValue());
        }
        user = "2308240855331080";
        System.out.println("---------------------" + user + "-------------------------");
        list = recommendRank(user, itemCFModel, allItems.keySet());
        for (ItemEntry<String, Double> entry : list) {
            System.out.println(allItems.get(entry.getKey()) + " > "  + entry.getValue());
        }
        user = "2403171547451965";
        System.out.println("---------------------" + user + "-------------------------");
        list = recommendRank(user, itemCFModel, allItems.keySet());
        for (ItemEntry<String, Double> entry : list) {
            System.out.println(allItems.get(entry.getKey()) + " > "  + entry.getValue());
        }
        user = "2101152145890096";
        System.out.println("---------------------" + user + "-------------------------");
        list = recommendRank(user, itemCFModel, allItems.keySet());
        for (ItemEntry<String, Double> entry : list) {
            System.out.println(allItems.get(entry.getKey()) + " > "  + entry.getValue());
        }
        user = "2408041957046050";
        System.out.println("---------------------" + user + "-------------------------");
        list = recommendRank(user, itemCFModel, allItems.keySet());
        for (ItemEntry<String, Double> entry : list) {
            System.out.println(allItems.get(entry.getKey()) + " > "  + entry.getValue());
        }
        user = "2308252112850567";
        System.out.println("---------------------" + user + "-------------------------");
        list = recommendRank(user, itemCFModel, allItems.keySet());
        for (ItemEntry<String, Double> entry : list) {
            System.out.println(allItems.get(entry.getKey()) + " > "  + entry.getValue());
        }
        user = "2309101017634370";
        System.out.println("---------------------" + user + "-------------------------");
        list = recommendRank(user, itemCFModel, allItems.keySet());
        for (ItemEntry<String, Double> entry : list) {
            System.out.println(allItems.get(entry.getKey()) + " > "  + entry.getValue());
        }

    }

    @Test
    public void testJdbcProductPredict() throws IOException, InterruptedException {
        loadProducts();
        initJdbcRepository();

        ItemCFRankService rankService = new ItemCFRankService(repository);

        String user = "2306221757133297";
        System.out.println("---------------------" + user + "-------------------------");
        Collection<ItemEntry<String, Double>> list = rankService.rank(user, 10);
        for (ItemEntry<String, Double> entry : list) {
            System.out.println(allItems.get(entry.getKey()) + " > "  + entry.getValue());
        }
        user = "2306221757133297";
        System.out.println("---------------------" + user + "-------------------------");
        list = rankService.rank(user, 10);
        for (ItemEntry<String, Double> entry : list) {
            System.out.println(allItems.get(entry.getKey()) + " > "  + entry.getValue());
        }

        user = "2309101017634370";
        System.out.println("---------------------" + user + "-------------------------");
        list = rankService.rank(user, 10);
        for (ItemEntry<String, Double> entry : list) {
            System.out.println(allItems.get(entry.getKey()) + " > "  + entry.getValue());
        }
    }
}