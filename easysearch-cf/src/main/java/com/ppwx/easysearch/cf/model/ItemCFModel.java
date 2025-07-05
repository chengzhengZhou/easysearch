/**
 * Copyright (C), 2010-2024, 爱回收
 * FileName: ItemCFModel
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2024/7/26 17:40
 * Description: 基于物品的协同过滤
 */
package com.ppwx.easysearch.cf.model;

import com.google.common.collect.*;
import com.ppwx.easysearch.cf.data.CFDataModel;
import com.ppwx.easysearch.cf.data.CFSimilarity;
import com.ppwx.easysearch.cf.data.ScoreSumMatrix;
import com.ppwx.easysearch.cf.data.UserRating;
import com.ppwx.easysearch.cf.rating.RecEvaluator;
import com.ppwx.easysearch.cf.repository.ModelDataRepository;
import net.librec.math.structure.*;
import net.librec.recommender.item.ItemEntry;
import net.librec.similarity.RecommenderSimilarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 *
 * 基于物品的协同过滤预测模型
 * 传统的协同过滤适合离线批量处理后将推荐的结果进行缓存，待线上取用无法捕捉用户实时兴趣
 * 本实现方案综合考虑了内存和效率，将其中核心的物品相似矩阵进行内存计算方便进行实时计算
 * 而将大量的用户行为记录存储到外部设备，增量行为数据进行实时计算后同步到各分布式节点
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2024/07/26 17:40
 * @since 1.0.0
 */
public class ItemCFModel extends AbstractIncrementalModel {

    private static final Logger LOG = LoggerFactory.getLogger(ItemCFModel.class);

    /**
     * 数据模型
     */
    private CFDataModel dataModel;
    /**
     * 相识度评分矩阵
     */
    private RecommenderSimilarity similarity;
    /**
     * 模式判断开关
     */
    private boolean isPredict = false;
    /**
     * 训练集
     */
    private SparseMatrix trainMatrix;
    /**
     * 测试集
     */
    private SparseMatrix testMatrix;
    /**
     * 时间
     */
    private SparseMatrix datetimeMatrix;
    /**
     * CF算分矩阵
     * sc1 * sc2
     */
    private SymmMatrix symmMatrix;
    /**
     * itemId -> idx 映射
     */
    private BiMap<String, Integer> itemIdMap;
    /**
     * userId -> idx 映射
     */
    private BiMap<String, Integer> userIdMap;
    /**
     * 最新加载时间
     */
    private Date lastLoadTime;
    /**
     * 仓储
     */
    private ModelDataRepository repository;
    /**
     * 限定用户的偏好记录数
     */
    private int maxUserPrefers = 20;
    /**
     * per item mean of ratings
     */
    private DenseVector itemMeans;
    /**
     * global mean of ratings
     */
    protected Double globalMean;
    /**
     * 用户偏好存储熔断标识
     */
    private boolean uaHystrix;
    /**
     * 评分矩阵熔断标识
     */
    private boolean itemCFHystrix;
    /**
     * rank标识
     */
    private boolean isRank = true;

    public void setDataModel(CFDataModel dataModel) {
        this.dataModel = dataModel;
    }

    public void setSimilarity(RecommenderSimilarity similarity) {
        this.similarity = similarity;
    }

    public void setRepository(ModelDataRepository repository) {
        this.repository = repository;
    }

    public void setUaHystrix(boolean uaHystrix) {
        this.uaHystrix = uaHystrix;
    }

    public void setItemCFHystrix(boolean itemCFHystrix) {
        this.itemCFHystrix = itemCFHystrix;
    }

    public void setRank(boolean rank) {
        isRank = rank;
    }

    public void setMaxUserPrefers(int maxUserPrefers) {
        this.maxUserPrefers = maxUserPrefers;
    }

    @Override
    public void loadModel() {
        // 直接加载评分矩阵
        lastLoadTime = Calendar.getInstance().getTime();
        BiMap<String, Integer> itemIds = HashBiMap.create();
        SymmMatrix symmMatrix = new ScoreSumMatrix();
        CFSimilarity cfSimilarity = repository.getCFScoreV2();
        cfSimilarity.copyTo(itemIds, symmMatrix);
        this.itemIdMap = itemIds;
        this.symmMatrix = symmMatrix;
        this.predict();
    }

    @Override
    public void saveModel() {
        // save UA
        Multimap<String, UserRating> userMap = HashMultimap.create();
        BiMap<Integer, String> users = userIdMap.inverse();
        BiMap<Integer, String> items = itemIdMap.inverse();
        String uid;
        SparseMatrix matrix = trainMatrix;
        for (int i = 0; i < matrix.numRows(); i++) {
            uid = users.get(i);
            for (Integer idx : matrix.getColumns(i)) {
                //System.out.print(uid + ":" + items.get(idx) + "=" + matrix.get(i, idx));
                //System.out.print(" | ");
                if (datetimeMatrix == null) {
                    userMap.put(uid, new UserRating(items.get(idx), matrix.get(i, idx), null));
                } else {
                    userMap.put(uid, new UserRating(items.get(idx), matrix.get(i, idx), new Date(Double.valueOf(datetimeMatrix.get(i, idx)).longValue())));
                }
            }
            //System.out.println();
        }
        saveUserAction(userMap);

        // save Score
        saveScoreModel();
    }

    private void saveUserAction(Multimap<String, UserRating> userMap) {
        LOG.info("-------------------saveUserAction:{}, hystrix:{}-------------------", userMap.size(), uaHystrix);
        if (uaHystrix) {
            return;
        }
        Multimap<String, UserRating> newMap = HashMultimap.create();
        userMap.keySet().forEach(u -> {
            if (datetimeMatrix != null) {
                userMap.get(u).stream()
                        .sorted(Comparator.comparing(UserRating::getDatetime).reversed())
                        .limit(maxUserPrefers)
                        .forEach(v -> newMap.put(u, v));
            } else {
                userMap.get(u).stream().limit(maxUserPrefers).forEach(v -> newMap.put(u, v));
            }
        });
        repository.saveUserRating(newMap);
    }

    private void saveScoreModel() {
        LOG.info("-------------------saveScoreModel:{}, hystrix:{}-------------------", this.itemIdMap.size(), itemCFHystrix);
        if (itemCFHystrix) {
            return;
        }
        CFSimilarity cfSimilarity = CFSimilarity.from(this.itemIdMap, symmMatrix);
        repository.saveCFScore(cfSimilarity);
    }

    @Override
    public void train() {
        isPredict = false;
        itemIdMap = dataModel.getItemMappingData();
        userIdMap = dataModel.getUserMappingData();
        trainMatrix = (SparseMatrix) dataModel.getTrainDataSet();
        testMatrix = (SparseMatrix) dataModel.getTestDataSet();
        datetimeMatrix = (SparseMatrix) dataModel.getDatetimeDataSet();
        symmMatrix = similarity.getSimilarityMatrix();

        int numItems = trainMatrix.numColumns;
        itemMeans = new DenseVector(numItems);
        int numRates = trainMatrix.size();
        double globalMean = trainMatrix.sum() / numRates;
        for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
            SparseVector userRatingVector = trainMatrix.column(itemIdx);
            itemMeans.set(itemIdx, userRatingVector.getCount() > 0 ? userRatingVector.mean() : globalMean);
        }
        this.globalMean = trainMatrix.mean();
    }

    @Override
    public void incrementalTrain() {
        isPredict = false;
        itemIdMap = dataModel.getItemMappingData();
        userIdMap = dataModel.getUserMappingData();
        trainMatrix = (SparseMatrix) dataModel.getTrainDataSet();
        datetimeMatrix = (SparseMatrix) dataModel.getDatetimeDataSet();
        symmMatrix = new ScoreSumMatrix();
        BiMap<Integer, String> items = this.itemIdMap.inverse();
        BiMap<Integer, String> users = this.userIdMap.inverse();
        SparseMatrix preferenceMatrix = trainMatrix;
        Multimap<String, UserRating> userMap = HashMultimap.create();
        // 遍历用户合并行为，计算评分
        String userId;
        List<Integer> userPreferItemIdxes;
        Collection<UserRating> userPrefers;
        Map<Integer, UserRating> userPreferMap;
        Map<Integer, Double> conflictItems;
        for (Integer userIdx : users.keySet()) {
            userId = users.get(userIdx);
            userPreferItemIdxes = preferenceMatrix.getColumns(userIdx);
            userPrefers = repository.getUserRating(userId);
            userPreferMap = Maps.newHashMap();
            conflictItems = Maps.newHashMap();
            if (!CollectionUtils.isEmpty(userPrefers)) {
                // check same items
                for (UserRating v : userPrefers) {
                    int col = itemIdMap.containsKey(v.getItemId()) ? itemIdMap.get(v.getItemId()) : itemIdMap.size();
                    itemIdMap.put(v.getItemId(), col);
                    userPreferMap.put(col, v);

                    if (userPreferItemIdxes.contains(col)) {
                        userPreferItemIdxes.remove(Integer.valueOf(col));
                        if (Double.compare(preferenceMatrix.get(userIdx, col), v.getRate()) > 0) {
                            conflictItems.put(col, v.getRate());
                            v.setRate(preferenceMatrix.get(userIdx, col));
                            if (datetimeMatrix != null) {
                                v.setDatetime(new Date(Double.valueOf(datetimeMatrix.get(userIdx, col)).longValue()));
                            }
                        }
                    }
                }
            }
            // step 1 multiply itself
            for (int i, ii = 0; ii < userPreferItemIdxes.size(); ii++) {
                i = userPreferItemIdxes.get(ii);
                if (datetimeMatrix == null) {
                    userMap.put(userId, new UserRating(items.get(i), preferenceMatrix.get(userIdx, i), null));
                } else {
                    userMap.put(userId, new UserRating(items.get(i), preferenceMatrix.get(userIdx, i),
                            new Date(Double.valueOf(datetimeMatrix.get(userIdx, i)).longValue())));
                }

                for (int j, jj = ii; jj < userPreferItemIdxes.size(); jj++) {
                    j = userPreferItemIdxes.get(jj);
                    this.symmMatrix.set(i, j,
                            preferenceMatrix.get(userIdx, i) * preferenceMatrix.get(userIdx, j) + this.symmMatrix.get(i, j));
                }
            }
            // step 2 multiply with UA
            for (Integer i : userPreferItemIdxes) {
                double v1 = preferenceMatrix.get(userIdx, i);
                for (Integer j : userPreferMap.keySet()) {
                    double v2 = Objects.requireNonNull(userPreferMap.get(j)).getRate();
                    this.symmMatrix.set(i, j, (v1 * v2) + this.symmMatrix.get(i, j));
                }
            }
            // step 3 adjust same items
            if (!CollectionUtils.isEmpty(conflictItems)) {
                for (Integer i : conflictItems.keySet()) {
                    double oldVal = conflictItems.get(i);
                    double newVal = userPreferMap.get(i).getRate();
                    for (Integer j : userPreferMap.keySet()) {
                        double rate = userPreferMap.get(j).getRate();
                        double diff;
                        if (i.equals(j)) {
                            diff = Math.pow(newVal - oldVal, 2) + 2 * oldVal * (newVal - oldVal);
                        } else {
                            diff = (newVal - oldVal) * rate;
                        }
                        this.symmMatrix.set(i, j, diff + this.symmMatrix.get(i, j));
                    }
                }
            }
            userMap.putAll(userId, userPrefers);
        }
        // save ua
        saveUserAction(userMap);
        // save score
        saveScoreModel();
    }

    @Override
    public void predict() {
        isPredict = true;
    }

    @Override
    public double predict(String userId, String itemId) {
        Assert.isTrue(isPredict);
        boolean existItem = itemIdMap.containsKey(itemId);
        Collection<UserRating> userActions = repository.getUserRating(userId);
        if (!existItem || CollectionUtils.isEmpty(userActions)) {
            return 0.0D;
        }
        return predict(userActions, itemId);
    }

    @Override
    public List<ItemEntry<String, Double>> batchPredict(String userId, Collection<String> items) {
        Assert.isTrue(isPredict);
        Collection<UserRating> userActions = repository.getUserRating(userId);
        List<ItemEntry<String, Double>> list = Lists.newArrayListWithExpectedSize(items.size());
        for (String itemId : items) {
            if (!itemIdMap.containsKey(itemId) || CollectionUtils.isEmpty(userActions)) {
                list.add(new ItemEntry<>(itemId, 0.0D));
            } else {
                list.add(new ItemEntry<>(itemId, predict(userActions, itemId)));
            }
        }
        return list;
    }

    @Override
    public void acceptEvaluators(List<RecEvaluator> evaluators) {
        Assert.isTrue(isPredict);
        int currentUserIdx = -1;
        Set<Integer> currentItemIdxSet = null;
        double predictRating;
        for (MatrixEntry matrixEntry : testMatrix) {
            int userIdx = matrixEntry.row();
            int itemIdx = matrixEntry.column();
            if (currentUserIdx != userIdx) {
                currentItemIdxSet = trainMatrix.getColumnsSet(userIdx);
                currentUserIdx = userIdx;
            }
            // PREDICT
            if (currentItemIdxSet == null || currentItemIdxSet.isEmpty()) {
                continue;
            }
            double sum = 0, ws = 0, sim;
            for (Integer iid : currentItemIdxSet) {
                double xy = symmMatrix.get(itemIdx, iid);
                double x = symmMatrix.get(itemIdx, itemIdx);
                double y = symmMatrix.get(iid, iid);
                sim = xy / (Math.sqrt(x) * Math.sqrt(y));
                sum += sim * (trainMatrix.get(userIdx, iid) - itemMeans.get(iid));
                ws += Math.abs(sim);
            }
            predictRating = ws > 0 ? itemMeans.get(itemIdx) + sum / ws : globalMean;

            if (Double.isNaN(predictRating)) {
                predictRating = globalMean;
            }
            for (RecEvaluator evaluator : evaluators) {
                evaluator.add(predictRating, testMatrix.get(userIdx, itemIdx));
            }
        }
    }

    /**
     * 已知用户偏好商品预测对未知商品的偏好
     *
     * @param userActions
     * @param itemId
     * @return double
     */
    public final double predict(Collection<UserRating> userActions, String itemId) {
        Date now = new Date();
        Integer itemIdx = itemIdMap.get(itemId);
        if (itemIdx == null) {
            return 0;
        }
        Integer otherIdx;
        // num / (Math.sqrt(thisPow2) * Math.sqrt(thatPow2))
        double sum = 0, ws = 0, sim;
        if (isRank) {
            for (UserRating ua : userActions) {
                otherIdx = itemIdMap.get(ua.getItemId());
                if (otherIdx == null) {
                    continue;
                }
                double xy = symmMatrix.get(itemIdx, otherIdx);
                double x = symmMatrix.get(itemIdx, itemIdx);
                double y = symmMatrix.get(otherIdx, otherIdx);
                if (xy != 0 && x != 0 && y != 0) {
                    sim = xy / (Math.sqrt(x) * Math.sqrt(y));
                    sum += ua.getRate() * sim * timeDecay(ua.getDatetime(), now);
                }
            }
            return sum;
        } else {
            for (UserRating ua : userActions) {
                otherIdx = itemIdMap.get(ua.getItemId());
                if (otherIdx == null) {
                    continue;
                }
                double xy = symmMatrix.get(itemIdx, otherIdx);
                double x = symmMatrix.get(itemIdx, itemIdx);
                double y = symmMatrix.get(otherIdx, otherIdx);
                if (xy != 0 && x != 0 && y != 0) {
                    sim = xy / (Math.sqrt(x) * Math.sqrt(y));
                    sum += ua.getRate() * sim;
                    ws += sim;
                }
            }
            return ws > 0 ? sum / ws : 0;
        }
    }

    private double timeDecay(Date actionTime, Date curr) {
        if (actionTime == null) {
            return 1;
        }
        long offset = TimeUnit.MILLISECONDS.toHours(Math.abs(curr.getTime() - actionTime.getTime()));
        return 0.5 + Math.pow(0.5, (1 + 0.2 * offset));
    }

    @Override
    public void sync() {
        Date begin = lastLoadTime;
        Date end = Calendar.getInstance().getTime();
        lastLoadTime = end;
        CFSimilarity cfSimilarity = repository.getCFScoreV2(begin, end);
        cfSimilarity.copyTo(itemIdMap, symmMatrix);
    }

    public static ItemCFModel initFrom(BiMap<String, Integer> items, SymmMatrix matrix) {
        ItemCFModel itemCFModel = new ItemCFModel();
        itemCFModel.itemIdMap = items;
        itemCFModel.symmMatrix = matrix;
        itemCFModel.predict();
        return itemCFModel;
    }
}