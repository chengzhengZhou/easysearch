/**
 * Copyright (C), 2010-2024, 爱回收
 * FileName: JdbcModelDataRepositoryTest
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2024/8/5 22:29
 * Description: JdbcModelDataRepository测试类
 */
package com.ppwx.easysearch.cf.repository;

import com.ppwx.easysearch.cf.data.CFSimilarity;
import com.ppwx.easysearch.cf.data.CFTextDataModel;
import com.ppwx.easysearch.cf.model.ItemCFModel;
import com.ppwx.easysearch.cf.similarity.SumSimilarity;
import com.ppwx.easysearch.cf.util.DataSourceFactory;
import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.similarity.RecommenderSimilarity;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;

/**
 *
 * JdbcModelDataRepository测试类
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2024/08/05 22:29
 * @since 1.0.0
 */
public class JdbcModelDataRepositoryTest {

    private JdbcModelDataRepository repository;

    @Before
    public void init() {
        DataSource dataSource = DataSourceFactory.newDataSource();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        this.repository = new JdbcModelDataRepository( "t_", jdbcTemplate);
        //repository.setScoreShards(4);
        //repository.setUserShards(2);
    }

    @Test
    public void testJdbcModelDataRepositoryWorks() throws LibrecException {
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

    private String getSourceFile() {
        URL url = this.getClass().getResource("/data/test");
        return url.getFile().substring(1);
    }

    @Test
    public void testGetCFScore() throws ParseException {
        Date begin = DateUtils.parseDate("2024-08-07 11:28:28", "yyyy-MM-dd HH:mm:ss");
        Date end = DateUtils.parseDate("2024-08-07 11:28:30", "yyyy-MM-dd HH:mm:ss");
        CFSimilarity cfScoreV2 = repository.getCFScoreV2(begin, end);
        System.out.println(cfScoreV2.getScores().size());
    }

    @Test
    public void testGetCFByItem() {
        CFSimilarity cfScore = repository.getCFScore(Arrays.asList("3679"));
    }
}