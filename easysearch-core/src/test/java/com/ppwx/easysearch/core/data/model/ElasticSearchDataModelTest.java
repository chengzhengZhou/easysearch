/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: ElasticSearchDataModelTest
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/12/12 11:01
 * Description: ElasticSearchDataModelTest
 */
package com.ppwx.easysearch.core.data.model;

import com.google.common.collect.Maps;
import com.ppwx.easysearch.core.data.Column;
import com.ppwx.easysearch.core.data.DataSet;
import com.ppwx.easysearch.core.data.element.DoubleColumn;
import com.ppwx.easysearch.core.function.normalize.FiniteNormalizeFunc;
import com.ppwx.easysearch.core.pipeline.*;
import com.ppwx.easysearch.core.pipeline.handler.DuplicateIdSkipDataHandler;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.ppwx.easysearch.core.data.model.Key.GLOBAL_SCORE;

/**
 *
 * ElasticSearchDataModelTest
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/12 11:01
 * @since 1.0.0
 */
public class ElasticSearchDataModelTest {

    private RestHighLevelClient client;

    private Map<String, ElasticSearchFieldType> mappingTypes;

    @Before
    public void init() {
        RestClientBuilder restClientBuilder = RestClient.builder(new HttpHost("localhost", 9200));
        this.client = new RestHighLevelClient(restClientBuilder);
        mappingTypes = Maps.newLinkedHashMap();
        mappingTypes.put("nick_name", ElasticSearchFieldType.STRING);
        mappingTypes.put("age", ElasticSearchFieldType.INTEGER);
        mappingTypes.put("gender", ElasticSearchFieldType.INTEGER);
        mappingTypes.put("create_time", ElasticSearchFieldType.DATE);
    }

    @Test
    public void testElasticSearchDataModelWorks() {
        AbstractElasticSearchDataModel dataModel = new InnerDataHandler(mappingTypes);
        dataModel.loadDataModel();
        DataSet dataset = dataModel.getDataset();
        List<Map<String, Column>> all = dataset.all();
        all.forEach(map -> {
            map.keySet().forEach(k -> System.out.print(k + " : " + map.get(k).getRawData() + ","));
            System.out.println("");
        });
    }

    @Test
    public void testDataModelPipelineWorks() {
        AbstractElasticSearchDataModel dataModel = new InnerDataHandler(mappingTypes);

        DataPipelineBuilder builder = new DataPipelineBuilder();
        builder.mode(true);
        builder.handler(pipeline -> {
            pipeline.addLast("duplicateIdSkip", new DuplicateIdSkipDataHandler());
            pipeline.addLast("reScoreHandler", new CustomScore());
        });
        builder.dataModel(dataModel);
        DataPipeline pipeline = builder.build();

        dataModel.loadDataModel();
        pipeline.fireDataPrepare();
        pipeline.fireDataComplete();

        List<Map<String, Column>> all = dataModel.getDataset().all();
        all.forEach(map -> {
            map.keySet().forEach(k -> System.out.print(k + " : " + map.get(k).getRawData() + ","));
            System.out.println("");
        });
    }

    @After
    public void destroy() throws IOException {
        if (this.client != null) {
            this.client.close();
        }
    }

    @DataHandler.Sharable
    private class CustomScore extends DataHandlerAdapter {

        @Override
        public void dataComplete(DataHandlerContext ctx) throws Exception {
            long l = System.currentTimeMillis();
            long l1 = TimeUnit.DAYS.toMillis(100);
            FiniteNormalizeFunc normalize = new FiniteNormalizeFunc(l - l1, l + l1);
            DataSet dataset = ctx.dataModel().getDataset();
            int size = dataset.size();
            for (int i = 0; i < size; i++) {
                Map<String, Column> item = dataset.row(i);
                Column column = item.get("create_time");
                if (column != null) {
                    Long aLong = column.asLong();
                    double score = normalize.score(aLong);
                    dataset.set(i, GLOBAL_SCORE, new DoubleColumn(score));
                }
            }
        }
    }

    private class InnerDataHandler extends AbstractElasticSearchDataModel {

        /**
         * 构造函数
         *
         * @param mappingTypes
         * @return
         */
        public InnerDataHandler(Map<String, ElasticSearchFieldType> mappingTypes) {
            super(mappingTypes);
        }

        @Override
        protected SearchHits doQuery() throws IOException {
            // 通用的条件查询
            MatchAllQueryBuilder matchAllQueryBuilder = QueryBuilders.matchAllQuery();
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.query(matchAllQueryBuilder);
            sourceBuilder.size(100);

            SearchRequest request = new SearchRequest("student_index");
            request.source(sourceBuilder);
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            return response.getHits();
        }
    }

}