/**
 * Copyright (C), 2010-2024, 爱回收
 * FileName: ItemCFRankService
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2024/8/9 11:32
 * Description: 基于物品的协同过滤排序
 */
package com.ppwx.easysearch.cf.rank;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import com.ppwx.easysearch.cf.data.CFSimilarity;
import com.ppwx.easysearch.cf.data.ScoreSumMatrix;
import com.ppwx.easysearch.cf.data.UserRating;
import com.ppwx.easysearch.cf.model.ItemCFModel;
import com.ppwx.easysearch.cf.repository.ModelDataRepository;
import net.librec.math.structure.SymmMatrix;
import net.librec.recommender.item.ItemEntry;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * 基于物品的协同过滤排序
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2024/08/09 11:32
 * @since 1.0.0
 */
public class ItemCFRankService implements RankService {

    protected ModelDataRepository repository;

    /**
     * 是否跳过用户已浏览的物品
     */
    private boolean skipUser = true;

    public ItemCFRankService(ModelDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public Collection<UserRating> getUA(String userId) {
        return repository.getUserRating(userId);
    }

    @Override
    public Collection<ItemEntry<String, Double>> rank(String userId, int topK) {
        return rank(userId, Double.MIN_VALUE, topK);
    }

    @Override
    public Collection<ItemEntry<String, Double>> rank(String userId, double minScore, int topK) {
        Collection<UserRating> userActions = getUA(userId);
        return rank(userActions, minScore, topK);
    }

    @Override
    public Collection<ItemEntry<String, Double>> rank(Collection<UserRating> userActions, double minScore, int topK) {
        if (CollectionUtils.isEmpty(userActions)) {
            return Collections.emptyList();
        }
        Set<String> userPrefers = userActions.stream().map(UserRating::getItemId).collect(Collectors.toSet());
        CFSimilarity cfScore = repository.getCFScore(userPrefers);
        BiMap<String, Integer> items = HashBiMap.create();
        SymmMatrix matrix = new ScoreSumMatrix();
        cfScore.copyTo(items, matrix);
        Set<String> itemSet = items.keySet();
        // filter not exits
        CFSimilarity sameItemCFScore = getSameItemCFScore();
        sameItemCFScore.copyTo(items, matrix, true);

        ItemCFModel model = ItemCFModel.initFrom(items, matrix);
        List<ItemEntry<String, Double>> itemEntries = Lists.newArrayListWithCapacity(itemSet.size());
        for (String item : itemSet) {
            if (skipUser && userPrefers.contains(item)) {
                continue;
            }
            double predict = model.predict(userActions, item);
            if (!Double.isNaN(predict) && predict >= minScore) {
                itemEntries.add(new ItemEntry<>(item, model.predict(userActions, item)));
            }
        }

        itemEntries = net.librec.util.Lists.sortItemEntryListTopK(itemEntries, true, topK);
        return itemEntries;
    }

    protected CFSimilarity getSameItemCFScore() {
        return repository.getSameItemCFScore();
    }

    public void setSkipUser(boolean skipUser) {
        this.skipUser = skipUser;
    }
}