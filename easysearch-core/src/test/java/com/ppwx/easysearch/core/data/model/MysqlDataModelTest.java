/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: MysqlDataModelTest
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/12/11 17:55
 * Description: MysqlDataModel测试类
 */
package com.ppwx.easysearch.core.data.model;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import com.ppwx.easysearch.core.data.Column;
import com.ppwx.easysearch.core.data.DataSet;
import com.ppwx.easysearch.core.data.ListDataSet;
import com.ppwx.easysearch.core.data.codec.DataCopier;
import com.ppwx.easysearch.core.data.codec.DataSetMeta;
import com.ppwx.easysearch.core.data.element.DoubleColumn;
import com.ppwx.easysearch.core.function.timeliness.TimelinessMsFunc;
import com.ppwx.easysearch.core.metrics.InMemoryMetricsRegistry;
import com.ppwx.easysearch.core.metrics.Metrics;
import com.ppwx.easysearch.core.metrics.MetricsRegistry;
import com.ppwx.easysearch.core.pipeline.DataHandlerAdapter;
import com.ppwx.easysearch.core.pipeline.DataHandlerContext;
import com.ppwx.easysearch.core.pipeline.DataPipeline;
import com.ppwx.easysearch.core.pipeline.DataPipelineBuilder;
import com.ppwx.easysearch.core.pipeline.handler.DuplicateIdSkipDataHandler;
import com.ppwx.easysearch.core.pipeline.handler.ScoreSortDataHandler;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.ppwx.easysearch.core.data.model.Key.GLOBAL_SCORE;

/**
 *
 * MysqlDataModel测试类
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/11 17:55
 * @since 1.0.0
 */
public class MysqlDataModelTest {

    private JdbcTemplate jdbcTemplate;

    @Before
    public void init() {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setUser("root");
        dataSource.setPassword("root");
        dataSource.setURL("jdbc:mysql://localhost:3306/test?serverTimezone=Asia/Shanghai&useSSL=false");
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    public void testMysqlDataModelWorks() {
        MysqlDataModel mysqlDataModel = new MysqlDataModel(jdbcTemplate);
        mysqlDataModel.setQuerySql("select id as gid,0 as score,nick_name as nickName,age from t_student");
        mysqlDataModel.loadDataModel();
        DataSet dataset = mysqlDataModel.getDataset();
        System.out.println(dataset.all());
    }

    @Test
    public void testMysqlDataPipelineWorks() {
        MysqlDataModel mysqlDataModel = new MysqlDataModel(jdbcTemplate);
        mysqlDataModel.setQuerySql("SELECT id as gid,0 as gScore, nick_name nickName, age, gender, create_time as createTime FROM t_student");
        mysqlDataModel.loadDataModel();

        DataPipelineBuilder pipelineBuilder = new DataPipelineBuilder();
        pipelineBuilder.dataModel(mysqlDataModel);
        DataPipeline pipeline = pipelineBuilder.build();
        pipeline.addLast("duplicateIdSkip", new DuplicateIdSkipDataHandler());
        pipeline.addLast("customHandler", new DataHandlerAdapter() {
            @Override
            public void dataComplete(DataHandlerContext ctx) throws Exception {
                DataSet dataset = ctx.dataModel().getDataset();
                TimelinessMsFunc timelinessFunc = new TimelinessMsFunc(dataset.column("createTime"));
                for (int i = 0; i < dataset.size(); i++) {
                    Map<String, Column> row = dataset.row(i);
                    Double score = timelinessFunc.apply(row.get("createTime"));
                    dataset.set(i, GLOBAL_SCORE, new DoubleColumn(score));
                }
                super.dataComplete(ctx);
            }
        });
        pipeline.addLast("scoreSortDataHandler", new ScoreSortDataHandler());
        pipeline.fireDataPrepare();
        pipeline.fireDataComplete();
        mysqlDataModel.getDataset().all().forEach(item -> {
            Column nickName = item.get("nickName");
            Column score = item.get(GLOBAL_SCORE);
            System.out.println("nickName:" + nickName.asString() + " :" + score.asString());
        });
    }

    @Test
    public void testTime() {
        for (int i = 0; i < 10; i++) {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            testMysqlDataPipelineWorks();
            stopWatch.stop();
            System.out.println("第" + i + "次：" + stopWatch.getTime());
        }
    }

    @Test
    public void testCacheDataModelDecoratorWorks() {
        MysqlDataModel mysqlDataModel = new MysqlDataModel(jdbcTemplate);
        mysqlDataModel.setQuerySql("SELECT id as gid,0 as gScore, nick_name nickName, age, gender, create_time as createTime FROM t_student");
        mysqlDataModel.loadDataModel();

        Cache<String, DataSet> cache = CacheBuilder.newBuilder()
                .initialCapacity(2)
                .expireAfterWrite(1, TimeUnit.MINUTES).build();
        CacheDataModelDecorator cacheDataModelDecorator;
        for (int i = 0; i < 10; i++) {
            cacheDataModelDecorator = new CacheDataModelDecorator(mysqlDataModel, cache);
            cacheDataModelDecorator.getDataset().all();
        }
    }

    @Test
    public void testResetCacheDataModelDecoratorWorks() {
        MysqlDataModel mysqlDataModel = new MysqlDataModel(jdbcTemplate);
        mysqlDataModel.setQuerySql("SELECT id as gid,0 as gScore, nick_name nickName, age, gender, create_time as createTime FROM t_student");
        mysqlDataModel.loadDataModel();

        Cache<String, DataSet> cache = CacheBuilder.newBuilder()
                .initialCapacity(2)
                .expireAfterWrite(1, TimeUnit.MINUTES).build();
        CacheDataModelDecorator cacheDataModelDecorator = new CacheDataModelDecorator(mysqlDataModel, cache);
        System.out.println(cacheDataModelDecorator.getDataset().all());

        cacheDataModelDecorator.setDataSet(new ListDataSet(Collections.emptyList(), Collections.emptyList()));
        System.out.println(cacheDataModelDecorator.getDataset().all());

    }

    @Test
    public void testWorks() {
        MysqlDataModel mysqlDataModel = new MysqlDataModel(jdbcTemplate);
        mysqlDataModel.setQuerySql("SELECT id as gid,0 as gScore, nick_name nickName, age, gender, create_time as createTime FROM t_student");
        mysqlDataModel.loadDataModel();

        DataSet dataSet = (DataSet) mysqlDataModel.getDataset().clone();
        System.out.println(dataSet.all());

    }

    private DefaultCache cacheInstance = new DefaultCache();

    @Test
    public void testDataModelCacheDecoratorWorks() {
        MysqlDataModel mysqlDataModel = new MysqlDataModel(jdbcTemplate);
        mysqlDataModel.setQuerySql("SELECT id as gid,0 as gScore, nick_name nickName, age, gender, create_time as createTime FROM t_student");
        DataModelCacheDecorator decorator = new DataModelCacheDecorator(mysqlDataModel, cacheInstance);
        decorator.setCacheKey("123");
        decorator.loadDataModel();
        decorator.getDataset();
    }

    private MetricsRegistry<Metrics> metricsRegistry = new InMemoryMetricsRegistry(new ConcurrentHashMap<>(10));

    @Test
    public void testSlidingTimeWindowMetricsWorks() {
        MysqlDataModel mysqlDataModel = new MysqlDataModel(jdbcTemplate);
        mysqlDataModel.setQuerySql("SELECT id as gid,0 as gScore, nick_name nickName, age, gender, create_time as createTime FROM t_student");
        DataModelAutoCacheDecorator decorator = new DataModelAutoCacheDecorator(mysqlDataModel, cacheInstance, metricsRegistry, 2, 10);
        decorator.setCacheKey("123");
        decorator.loadDataModel();
        decorator.getDataset();
    }

    @Test
    public void testCurrent() {
        for (int i = 0; i < 10; i++) {
            testSlidingTimeWindowMetricsWorks();
        }
    }

    @Test
    public void testProtostuffCodecWorks() throws Exception {
        MysqlDataModel mysqlDataModel = new MysqlDataModel(jdbcTemplate);
        mysqlDataModel.setQuerySql("SELECT id as gid,0 as gScore, nick_name nickName, age, gender, create_time as createTime FROM t_student");
        DataModelAutoCacheDecorator decorator = new DataModelAutoCacheDecorator(mysqlDataModel, cacheInstance, metricsRegistry, 2, 10);
        decorator.setCacheKey("123");
        decorator.loadDataModel();
        DataSet dataset = decorator.getDataset();
        DataSetMeta meta = new DataSetMeta();
        meta.decorate(dataset);
        byte[] serialize = DataCopier.serialize(meta);
        System.out.println(serialize.length);

        meta = (DataSetMeta) DataCopier.deserialize(serialize, meta.getClass());
        System.out.println(meta);
    }

    private DataSet dataset;

    @Before
    public void initDataset() {
        MysqlDataModel mysqlDataModel = new MysqlDataModel(jdbcTemplate);
        mysqlDataModel.setQuerySql("SELECT id as gid,0 as gScore, nick_name nickName, age, gender, create_time as createTime FROM t_student");
        mysqlDataModel.loadDataModel();
        dataset = mysqlDataModel.getDataset();
    }

    @Test
    public void testCurrentCoder() throws Exception {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        for (int i = 0; i < 1000; i++) {
            DataSetMeta meta = new DataSetMeta();
            meta.decorate(dataset);
            byte[] serialize = DataCopier.serialize(meta);
            DataCopier.deserialize(serialize, DataSetMeta.class);
        }
        stopWatch.stop();
        System.out.println("coder elapse: " + stopWatch.getTime());
    }

    static class DefaultCache implements ICache<String, DataSet> {

        private final Map<String, DataSet> map = new HashMap<>();

        @Override
        public DataSet getIfPresent(String key) {
            System.out.println("getIfPresent: " + key);
            return map.get(key);
        }

        @Override
        public void put(String key, DataSet value) {
            System.out.println("put: " + key);
            map.put(key, value);
        }
    }
}