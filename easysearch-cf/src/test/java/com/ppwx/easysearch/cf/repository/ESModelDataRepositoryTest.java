package com.ppwx.easysearch.cf.repository;

import com.ppwx.easysearch.cf.data.CFSimilarity;
import com.ppwx.easysearch.cf.data.CFTextDataModel;
import com.ppwx.easysearch.cf.model.ItemCFModel;
import com.ppwx.easysearch.cf.similarity.SumSimilarity;
import com.ppwx.easysearch.cf.util.DataSourceFactory;
import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.similarity.RecommenderSimilarity;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className ESModelDataRepositoryTest
 * @description ESModelDataRepository²âÊÔÀà
 * @date 2025/2/10 16:48
 **/
public class ESModelDataRepositoryTest {

    private RestHighLevelClient restHighLevelClient;
    private ESModelDataRepository repository;

    @Before
    public void init() {
        this.restHighLevelClient = DataSourceFactory.newESClient();
        repository = new ESModelDataRepository(restHighLevelClient);
    }

    @Test
    public void testESModelDataRepositoryWorks() throws LibrecException {
        Configuration conf = new Configuration();
        Configuration.Resource resource = new Configuration.Resource("rec/cf/itemknn-test.properties");
        conf.addResource(resource);
        conf.set("data.input.path", "filmtrust/rating");
        conf.set("data.splitter.trainset.ratio", "0.8");
        conf.set("rec.recommender.similarity.key", "item");
        conf.set("rec.similarity.shrinkage", "0");
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
        itemCFModel.predict();
        itemCFModel.setMaxUserPrefers(50);

        double predict = itemCFModel.predict("1220", "244");
        System.out.println(">>>>>>>>>>" + predict);
    }

    @Test
    public void testLoadModelWorks() {
        ItemCFModel itemCFModel = new ItemCFModel();
        itemCFModel.setRepository(repository);
        itemCFModel.loadModel();
        itemCFModel.predict();

        double predict = itemCFModel.predict("1220", "244");
        System.out.println(">>>>>>>>>>" + predict);

    }

    @Test
    public void testUpsert() throws InterruptedException {
        Map<String, Double> map = new HashMap<>();
        map.put("1019-1019", 7.5);
        CFSimilarity similarity = new CFSimilarity(map);
        repository.saveCFScore(similarity);
        repository.getBulkProcessor().awaitClose(1, TimeUnit.MINUTES);
    }

    private String getSourceFile() {
        URL url = this.getClass().getResource("/data/test");
        return url.getFile().substring(1);
    }
}
