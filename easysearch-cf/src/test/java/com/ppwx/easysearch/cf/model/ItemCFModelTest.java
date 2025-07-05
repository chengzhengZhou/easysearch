/**
 * Copyright (C), 2010-2024, 爱回收
 * FileName: YanXuanCFModelTest
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2024/8/7 17:54
 * Description: 严选数据测试
 */
package com.ppwx.easysearch.cf.model;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import com.ppwx.easysearch.cf.data.CFTextDataModel;
import com.ppwx.easysearch.cf.data.ScoreSumMatrix;
import com.ppwx.easysearch.cf.repository.ChronicleMapDataRepository;
import com.ppwx.easysearch.cf.repository.MemoryModelDataRepository;
import com.ppwx.easysearch.cf.repository.ModelDataRepository;
import com.ppwx.easysearch.cf.similarity.SumSimilarity;
import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.conf.Configured;
import net.librec.eval.rating.MAEEvaluator;
import net.librec.eval.rating.RMSEEvaluator;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.SparseMatrix;
import net.librec.math.structure.SymmMatrix;
import net.librec.recommender.item.RecommendedItemList;
import net.librec.similarity.RecommenderSimilarity;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 *
 * 严选数据测试
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2024/08/07 17:54
 * @since 1.0.0
 */
public class ItemCFModelTest {

    private ModelDataRepository repository;

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
    }

    private String getSourceFile() {
        URL url = this.getClass().getResource("/data/test");
        return url.getFile().substring(1);
    }

    @Test
    public void trainModel() throws LibrecException, IOException {
        initRepository();

        Configuration conf = getCfTextDataModel("yanxuan/ua");
        conf.set(Configured.CONF_DATA_COLUMN_FORMAT, "UIR");
        train(conf);
        // predict
        predict();
    }

    @Test
    public void incrementalTrain() throws LibrecException, IOException {
        initRepository();
        //repository = new MemoryModelDataRepository();
        List<String> files = Lists.newArrayList("yanxuan/product-ua-1", "yanxuan/product-ua-2");
        for (String file : files) {
            CFTextDataModel dataModel = new CFTextDataModel(getCfTextDataModel(file));
            dataModel.buildDataModel();

            ItemCFModel itemCFModel = new ItemCFModel();
            itemCFModel.setDataModel(dataModel);
            itemCFModel.setRepository(repository);
            itemCFModel.incrementalTrain();
            itemCFModel.setMaxUserPrefers(100);
        }
        // predict
        predict();
    }

    @Test
    public void testTranAndEvaluator() throws IOException, LibrecException {
        initRepository();
        //repository = new MemoryModelDataRepository();
        List<String> files = Lists.newArrayList("yanxuan/product-ua-1", "yanxuan/product-ua-2",
                "yanxuan/product-ua-3", "yanxuan/product-ua-4");
        for (String file : files) {
            CFTextDataModel dataModel = new CFTextDataModel(getCfTextDataModel(file));
            dataModel.buildDataModel();

            ItemCFModel itemCFModel = new ItemCFModel();
            itemCFModel.setDataModel(dataModel);
            itemCFModel.setRepository(repository);
            itemCFModel.setMaxUserPrefers(50);
            itemCFModel.incrementalTrain();

            // evaluation
            itemCFModel.loadModel();
            itemCFModel.setRank(false);
            BiMap<Integer, String> users = dataModel.getUserMappingData().inverse();
            BiMap<Integer, String> items = dataModel.getItemMappingData().inverse();
            double globalMean = ((SparseMatrix) dataModel.getTrainDataSet()).mean();
            SparseMatrix testData = (SparseMatrix) dataModel.getTestDataSet();

            RecommendedItemList recommendedList = new RecommendedItemList(users.size() - 1, users.size());
            for (MatrixEntry matrixEntry : testData) {
                int userIdx = matrixEntry.row();
                int itemIdx = matrixEntry.column();
                double predictRating = itemCFModel.predict(users.get(userIdx), items.get(itemIdx));
                if (Double.isNaN(predictRating) || predictRating == 0.0) {
                    recommendedList.addUserItemIdx(userIdx, itemIdx, globalMean);
                } else {
                    recommendedList.addUserItemIdx(userIdx, itemIdx, predictRating);
                }
            }

            RMSEEvaluator evaluator = new RMSEEvaluator();
            System.out.println("RMSE:" + evaluator.evaluate(testData, recommendedList));
            MAEEvaluator maeEvaluator = new MAEEvaluator();
            System.out.println("MAE:" + maeEvaluator.evaluate(testData, recommendedList));
        }
    }

    @Test
    public void testTranAndEvaluatorLess() throws IOException, LibrecException {
        initRepository();
        //repository = new MemoryModelDataRepository();
        List<String> files = Lists.newArrayList("yanxuan/product-ua-1", "yanxuan/product-ua-2",
                "yanxuan/product-ua-3", "yanxuan/product-ua-4");
        CFTextDataModel dataModel = null;
        for (String file : files) {
            dataModel = new CFTextDataModel(getCfTextDataModel(file));
            dataModel.buildDataModel();

            ItemCFModel itemCFModel = new ItemCFModel();
            itemCFModel.setDataModel(dataModel);
            itemCFModel.setRepository(repository);
            itemCFModel.setMaxUserPrefers(50);
            itemCFModel.incrementalTrain();
        }

        // evaluation
        BiMap<Integer, String> users = dataModel.getUserMappingData().inverse();
        BiMap<Integer, String> items = dataModel.getItemMappingData().inverse();
        SparseMatrix testData = (SparseMatrix) dataModel.getTestDataSet();
        double mean = ((SparseMatrix) dataModel.getTrainDataSet()).mean();

        List<String> testItems = Lists.newLinkedList();
        for (MatrixEntry matrixEntry : testData) {
            testItems.add(items.get(matrixEntry.column()));
        }

        com.ppwx.easysearch.cf.rating.MAEEvaluator maeEvaluator = new com.ppwx.easysearch.cf.rating.MAEEvaluator();
        com.ppwx.easysearch.cf.rating.RMSEEvaluator rmseEvaluator = new com.ppwx.easysearch.cf.rating.RMSEEvaluator();
        BiMap<String, Integer> newItems = HashBiMap.create();
        SymmMatrix newMatrix = new ScoreSumMatrix();
        ItemCFModel itemCFModel = ItemCFModel.initFrom(newItems, newMatrix);
        itemCFModel.setRank(false);
        repository.getSameItemCFScore().copyTo(newItems, newMatrix);
        repository.getCFScore(testItems).copyTo(newItems, newMatrix, true);

        for (MatrixEntry matrixEntry : testData) {
            String user = users.get(matrixEntry.row());
            String itemIdx = items.get(matrixEntry.column());
            double predictRating = itemCFModel.predict(repository.getUserRating(user), itemIdx);
            if (Double.isNaN(predictRating) || predictRating == 0.0D) {
                predictRating = mean;
            }
            maeEvaluator.add(predictRating, matrixEntry.get());
            rmseEvaluator.add(predictRating, matrixEntry.get());
        }
        System.out.println("RMSE:" + rmseEvaluator.evaluate());
        System.out.println("MAE:" + maeEvaluator.evaluate());
    }

    private Configuration getCfTextDataModel(String value) {
        Configuration conf = new Configuration();
        Configuration.Resource resource = new Configuration.Resource("rec/cf/itemknn-test.properties");
        conf.addResource(resource);
        conf.set("dfs.data.dir", getSourceFile());
        conf.set("data.splitter.trainset.ratio", "0.9");
        conf.set("rec.recommender.similarity.key", "item");
        conf.set("rec.similarity.shrinkage", "0");
        conf.set("data.input.path", value);
        return conf;
    }

    private void predict() throws LibrecException {
        ItemCFModel itemCFModel = new ItemCFModel();
        itemCFModel.setRepository(repository);
        itemCFModel.loadModel();
        Configuration configuration = getCfTextDataModel("yanxuan/product-ua");
        configuration.set("data.splitter.trainset.ratio", "0.8");
        CFTextDataModel dataModel = new CFTextDataModel(configuration);
        dataModel.buildDataModel();

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

    public void initRepository() throws IOException {
        /*DataSource dataSource = DataSourceFactory.newDataSource();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource, false);
        this.repository = new JdbcModelDataRepository( "t_", jdbcTemplate);*/

        /*this.repository = new ModelDataRepositoryProxy(new MemoryModelDataRepository(),
                new ESModelDataRepository(DataSourceFactory.newESClient()));*/

        this.repository = new ChronicleMapDataRepository(5000000, 1000000);
    }

}