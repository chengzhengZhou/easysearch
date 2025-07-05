package com.ppwx.easysearch.cf.repository;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.ppwx.easysearch.cf.data.CFSimilarity;
import com.ppwx.easysearch.cf.data.UserRating;
import com.ppwx.easysearch.core.common.DataException;
import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className ESModelDataRepository
 * @description 使用ES存储数据模型
 * @date 2025/2/8 17:48
 **/
public class ESModelDataRepository extends AbstractPersistentDataRepository{

    private static final Logger log = LoggerFactory.getLogger(ESModelDataRepository.class);
    /**
     * es client
     */
    private final RestHighLevelClient client;
    /**
     * 批处理
     */
    private final BulkProcessor bulkProcessor;
    /**
     * 前缀
     */
    private String prefix = "rec_";
    /**
     * 后缀
     */
    private String tail = "_temp";
    /**
     * 用户类型判断
     */
    private Function<String, Integer> userTypePredicate;

    public ESModelDataRepository(RestHighLevelClient restHighLevelClient) {
        this.client = restHighLevelClient;
        this.userTypePredicate = this::getUserTypePredicate;
        this.bulkProcessor = bulkProcessor(restHighLevelClient);
    }

    public void setUserTypePredicate(Function<String, Integer> userTypePredicate) {
        this.userTypePredicate = userTypePredicate;
    }

    private BulkProcessor bulkProcessor(RestHighLevelClient client) {
        return BulkProcessor.builder(
                        (bulkRequest, bulkResponseActionListener) ->
                                client.bulkAsync(bulkRequest, RequestOptions.DEFAULT, bulkResponseActionListener),
                        new BulkProcessor.Listener() {
                            @Override
                            public void beforeBulk(long l, BulkRequest bulkRequest) {}
                            @Override
                            public void afterBulk(long l, BulkRequest bulkRequest, BulkResponse bulkResponse) {}
                            @Override
                            public void afterBulk(long l, BulkRequest bulkRequest, Throwable throwable) {
                                log.error("bulkProcessor error.", throwable);
                            }
                        })
                .setBulkActions(1000)
                .setBulkSize(new ByteSizeValue(5, ByteSizeUnit.MB))
                .setFlushInterval(TimeValue.timeValueSeconds(5))
                .setBackoffPolicy(BackoffPolicy.constantBackoff(TimeValue.timeValueMillis(100), 3))
                .build();
    }

    /**
     * 简单区分登录用户和临时用户
     */
    private Integer getUserTypePredicate(String uid) {
        if (uid.length() > 10 && uid.length() < 20) {
            return 1;
        }
        return 2;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setTail(String tail) {
        this.tail = tail;
    }

    @Override
    public Collection<UserRating> getUserRating(String userId) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                .size(1)
                .fetchSource(new String[]{"actions"}, null)
                .query(QueryBuilders.idsQuery().addIds(userId));
        SearchRequest searchRequest = new SearchRequest(getUserActionIndex())
                .source(sourceBuilder);
        SearchHits hits;
        try {
            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
            hits = response.getHits();
        } catch (IOException e) {
            throw new DataException(e);
        }
        if (hits.getTotalHits().value > 0) {
            return decompress((String) hits.iterator().next().getSourceAsMap().get("actions"));
        }
        return Collections.emptyList();
    }

    @Override
    public CFSimilarity getCFScoreV2() {
        try {
            return getCF(QueryBuilders.matchAllQuery());
        } catch (IOException e) {
            throw new DataException(e);
        }
    }

    @Override
    public CFSimilarity getCFScoreV2(Date beginTime, Date endTime) {
        throw new DataException("not support option");
    }

    /**
     * 利用滚动api遍历，保持快照状态
     */
    private CFSimilarity getCF(QueryBuilder boolQueryBuilder) throws IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        CFSimilarity cfSimilarity = new CFSimilarity();
        SearchRequest searchRequest = new SearchRequest(getCFIndex());
        searchRequest.scroll(TimeValue.timeValueMinutes(1));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                .query(boolQueryBuilder)
                .size(1000)
                .fetchSource(new String[] {"similarity_score"}, null);
        searchRequest.source(searchSourceBuilder);
        // 首批数据
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        String scrollId = searchResponse.getScrollId();
        SearchHits hits = searchResponse.getHits();
        float total = client.count(new CountRequest(getCFIndex()).query(boolQueryBuilder), RequestOptions.DEFAULT).getCount();
        float count = 0;
        while (hits.getHits().length > 0 && count <= total) {
            readCFData(hits, cfSimilarity);
            count += hits.getHits().length;
            log.info("getCF data total:{}, processRate:{}", total, String.format("%.2f", count/total));
            SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
            scrollRequest.scroll(TimeValue.timeValueMinutes(1));
            searchResponse = client.scroll(scrollRequest, RequestOptions.DEFAULT);
            scrollId = searchResponse.getScrollId();
            hits = searchResponse.getHits();
        }
        // delete scroll
        if (scrollId != null) {
            ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
            clearScrollRequest.addScrollId(scrollId);
            client.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
        }
        stopWatch.stop();
        log.info("finish getCF total:{}, elapse:{}", total, stopWatch.getTotalTimeSeconds());
        return cfSimilarity;
    }

    private void readCFData(SearchHits hits, CFSimilarity cfSimilarity) {
        for (SearchHit next : hits) {
            String[] ids = CFSimilarity.getBiItemId(next.getId());
            cfSimilarity.setScore(ids[0], ids[1],
                    (double) next.getSourceAsMap().getOrDefault("similarity_score", 0.0D));
        }
    }

    @Override
    public CFSimilarity getCFScore(Collection<String> items) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.should(QueryBuilders.termsQuery("item_id_h", items));
        boolQueryBuilder.should(QueryBuilders.termsQuery("item_id_l", items));
        try {
            return getCF(boolQueryBuilder);
        } catch (IOException e) {
            throw new DataException(e);
        }
    }

    @Override
    public CFSimilarity getSameItemCFScore() {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.filter(QueryBuilders.termQuery("identity", true));
        try {
            return getCF(boolQueryBuilder);
        } catch (IOException e) {
            throw new DataException(e);
        }
    }

    @Override
    public void saveUserRating(String userId, List<UserRating> ratings) {
        String data = compress(ratings);
        try {
            XContentBuilder xContentBuilder = XContentFactory.jsonBuilder()
                    .startObject()
                    .field("user_code", userId)
                    .field("actions", data)
                    .field("user_type", userTypePredicate.apply(userId))
                    .field("update_dt", System.currentTimeMillis())
                    .endObject();
            IndexRequest indexRequest = new IndexRequest(getUserActionIndex())
                    .id(userId).source(xContentBuilder, XContentType.JSON);
            client.index(indexRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new DataException(e);
        }
    }

    @Override
    public void saveUserRating(Multimap<String, UserRating> userMap) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        long updateTime = System.currentTimeMillis();
        for (String uid : userMap.keySet()) {
            IndexRequest indexRequest;
            try {
                XContentBuilder xContentBuilder = XContentFactory.jsonBuilder()
                        .startObject()
                        .field("user_code", uid)
                        .field("actions", compress(userMap.get(uid)))
                        .field("user_type", userTypePredicate.apply(uid))
                        .field("update_dt", updateTime)
                        .endObject();
                indexRequest = new IndexRequest(getUserActionIndex()).id(uid).source(xContentBuilder);
            } catch (IOException e) {
                throw new DataException(e);
            }
            bulkProcessor.add(indexRequest);
        }
        stopWatch.stop();
        log.info("finish saveUserRating total:{}, elapse:{}", userMap.size(), stopWatch.getTotalTimeSeconds());
    }

    @Override
    public void saveCFScore(CFSimilarity similarity) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Map<String, Double> scores = similarity.getScores();
        scores.forEach((id,score) -> {
            UpdateRequest indexRequest;
            String[] ids = CFSimilarity.getBiItemId(id);
            try {
                Map<String, Object> map = Maps.newHashMapWithExpectedSize(1);
                map.put("score", score);
                Script script = new Script(Script.DEFAULT_SCRIPT_TYPE, Script.DEFAULT_SCRIPT_LANG,
                        "ctx._source.similarity_score += params.score", map);
                XContentBuilder upsertBuilder = XContentFactory.jsonBuilder()
                        .startObject()
                        .field("item_id_h", ids[0])
                        .field("item_id_l", ids[1])
                        .field("similarity_score", score)
                        .field("identity", Objects.equals(ids[0], ids[1]))
                        .endObject();
                indexRequest = new UpdateRequest(getCFIndex(), id).script(script).upsert(upsertBuilder);
            } catch (IOException e) {
                throw new DataException(e);
            }
            bulkProcessor.add(indexRequest);
        });
        stopWatch.stop();
        log.info("finish saveCFScore total:{}, elapse:{}", scores.size(), stopWatch.getTotalTimeSeconds());
    }

    @Override
    public void cleanSimilarity(CleanStrategy strategy) {
        throw new DataException("not support clean option");
    }

    private String getUserActionIndex() {
        return prefix + UA + tail;
    }

    private String getCFIndex() {
        return prefix + CF + tail;
    }

    public BulkProcessor getBulkProcessor() {
        return bulkProcessor;
    }
}
