package com.ppwx.easysearch.core.pipeline.handler;

import com.ppwx.easysearch.core.data.Column;
import com.ppwx.easysearch.core.data.DataModel;
import com.ppwx.easysearch.core.data.DataSet;
import com.ppwx.easysearch.core.data.ListDataSet;
import com.ppwx.easysearch.core.pipeline.DataHandler;
import com.ppwx.easysearch.core.pipeline.DataHandlerAdapter;
import com.ppwx.easysearch.core.pipeline.DataHandlerContext;
import com.ppwx.easysearch.core.similarity.LevenshteinDistanceSimilarity;
import com.ppwx.easysearch.core.similarity.SequenceSimilarityScore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.ppwx.easysearch.core.util.SearchLog.getLogger;

/**
 * TODO 待完善
 * @author ext.ahs.zhouchzh1@jd.com
 * @className MMRInterleaveDataHandler
 * @description 基于MMR算法的打散策略
 * @date 2025/1/23 17:11
 **/
@DataHandler.Sharable
public class MMRInterleaveDataHandler extends DataHandlerAdapter {

    /**
     * 调节参数
     */
    private double lambda = 0.7;

    private final SequenceSimilarityScore<Double> similarity = new LevenshteinDistanceSimilarity();

    private String query;

    public void setLambda(double lambda) {
        this.lambda = lambda;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    @Override
    public void dataComplete(DataHandlerContext ctx) throws Exception {
        getLogger().debug("---------------execute MMRInterleaveDataHandler-------------");
        DataModel dataModel = ctx.dataModel();
        DataSet dataset = dataModel.getDataset();

        List<Map<String, Column>> items = applyMMR(ctx, dataset.all(), lambda);
        dataModel.setDataSet(new ListDataSet(dataset.columnNames(), items));

        ctx.fireDataComplete();
    }

    private List<Map<String, Column>> applyMMR(DataHandlerContext ctx, List<Map<String, Column>> items, double lambda) {
        List<Map<String, Column>> selectedItems = new ArrayList<>();
        List<Map<String, Column>> remainingItems = new ArrayList<>(items);

        // 初始化：选择相关性最高的物品作为第一个
        remainingItems.sort((a, b) -> Double.compare(simTarget(b, ctx), simTarget(a, ctx)));
        selectedItems.add(remainingItems.remove(0));

        // 迭代选择
        while (!remainingItems.isEmpty()) {
            Map<String, Column> bestItem = null;
            double bestMMR = Double.NEGATIVE_INFINITY;

            for (Map<String, Column> item : remainingItems) {
                double relevance = simTarget(item, ctx);
                double redundancy = 0;

                for (Map<String, Column> selected : selectedItems) {
                    redundancy = Math.max(redundancy, sim(item, selected));
                }

                double mmr = lambda * relevance - (1 - lambda) * redundancy;

                if (mmr > bestMMR) {
                    bestMMR = mmr;
                    bestItem = item;
                }
            }

            selectedItems.add(bestItem);
            remainingItems.remove(bestItem);
        }

        return selectedItems;
    }

    protected double simTarget(Map<String, Column> item, DataHandlerContext ctx) {
        //String query = (String) ctx.attr(AttributeKey.valueOf("query")).get();
        //return similarity.apply(item.get("productName").asString(), query, null);
        return similarity.apply(item.get("productName").asString(), query, null);
    }

    /**
     * @description 相关性
     * @author ext.ahs.zhouchzh1@jd.com
     * @date 2025/1/23 17:53
     * @param a
     * @param b
     * @return double
     */
    protected double sim(Map<String, Column> a, Map<String, Column> b) {
        return similarity.apply(a.get("productName").asString(), b.get("productName").asString(), null);
    }
}
