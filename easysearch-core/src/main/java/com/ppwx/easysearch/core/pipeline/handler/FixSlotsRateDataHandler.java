/**
 * Copyright (C), 2010-2024, 爱回收
 * FileName: FixSlotsRateDataHandler
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2024/3/19 19:13
 * Description: 固出比例控制
 */
package com.ppwx.easysearch.core.pipeline.handler;

import com.google.common.collect.Lists;
import com.ppwx.easysearch.core.data.Column;
import com.ppwx.easysearch.core.data.DataModel;
import com.ppwx.easysearch.core.data.DataSet;
import com.ppwx.easysearch.core.data.ListDataSet;
import com.ppwx.easysearch.core.pipeline.DataHandler;
import com.ppwx.easysearch.core.pipeline.DataHandlerAdapter;
import com.ppwx.easysearch.core.pipeline.DataHandlerContext;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.ppwx.easysearch.core.util.SearchLog.getLogger;

/**
 *
 * 固出比例控制
 * 控制窗口内某类标的物的出现比例
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2024/03/19 19:13
 * @since 1.0.0
 */
@DataHandler.Sharable
public class FixSlotsRateDataHandler extends DataHandlerAdapter {

    /**
     * 固定窗口大小
     */
    private int defaultWindowSize = 10;
    /**
     * 控制露出数
     */
    private int limitSize = 5;
    /**
     * 最大打散数量
     * 对于只需要部分打散的场景来看，减少打散量能提高性能
     */
    private int maxSize = 30;
    /**
     * Constructor
     *
     */
    public FixSlotsRateDataHandler() {

    }
    /**
     * Args constructor
     *
     * @param windowSize
     * @param rate 控制比例[1-100)
     */
    public FixSlotsRateDataHandler(int windowSize, int rate) {
        setDefaultWindowSize(windowSize);
        setDefaultRate(rate);
    }

    public void setDefaultRate(int defaultRate) {
        int size = (int)(this.defaultWindowSize * defaultRate * 0.01D);
        if (size <= 0 || size >= defaultWindowSize) {
            throw new IllegalArgumentException("rate must nether small than one nor bigger than hundred");
        }
        this.limitSize = size;
    }

    public void setDefaultWindowSize(int defaultWindowSize) {
        if (defaultWindowSize <= 1) {
            throw new IllegalArgumentException("step must more than one.");
        }
        this.defaultWindowSize = defaultWindowSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public void dataComplete(DataHandlerContext ctx) throws Exception {
        getLogger().debug("---------------execute FixSlotsRateDataHandler-------------");
        DataModel dataModel = ctx.dataModel();
        DataSet dataset = dataModel.getDataset();
        List<Map<String, Column>> items = dataset.all();
        int size = dataset.size();
        if (this.maxSize > 0) {
            size = Math.min(size, maxSize);
        }
        int windowSize = Math.min(defaultWindowSize, size);
        int l = 0;
        int r = windowSize - 1;
        List<Integer> sameIds = Lists.newArrayListWithCapacity(windowSize);
        List<Integer> diffIds = Lists.newArrayListWithCapacity(windowSize);
        while (l < r && r < size) {
            process(l, r, items, sameIds, diffIds);
            // scroll
            l += windowSize;
            r += windowSize;
            sameIds.clear();
            diffIds.clear();
        }
        // reset dataset
        dataModel.setDataSet(new ListDataSet(dataset.columnNames(), items));

        super.dataComplete(ctx);
    }

    /**
     * 调控该窗口内的相同类别的标的物出现频次
     *
     * @param l
     * @param r
     * @param items
     * @param sameIds
     * @param diffIds
     * @return void
     */
    private void process(int l, int r, List<Map<String, Column>> items, List<Integer> sameIds, List<Integer> diffIds) {
        int count = 0;
        while (l <= r) {
            if (isSame(items.get(l))) {
                sameIds.add(l);
                count++;
            } else {
                diffIds.add(l);
            }
            l++;
        }

        if (count > limitSize) {
            // more
            handlerMore(sameIds, (count - limitSize), (r + 1), items);
        } else if (count < limitSize) {
            // less
            handlerLess(diffIds, (limitSize - count), (r + 1), items);
        } else {
            // equals
        }
    }

    /**
     * 从后续数据中查找可替代的标的物
     *
     * @param diffIds
     * @param count
     * @param r
     * @param items
     * @return void
     */
    protected void handlerLess(List<Integer> diffIds, int count, int r, List<Map<String, Column>> items) {
        Collections.shuffle(diffIds);
        Map<String, Column> columnMap;
        while (count > 0 && r < items.size()) {
            columnMap = items.get(r);
            if (isSame(columnMap)) {
                Integer from = diffIds.remove(0);
                items.set(r, items.get(from));
                items.set(from, columnMap);
                count--;
            }
            r++;
        }
    }

    /**
     * 转移多余的标的物
     *
     * @param sameIds
     * @param count
     * @param r
     * @param items
     * @return void
     */
    protected void handlerMore(List<Integer> sameIds, int count, int r, List<Map<String, Column>> items) {
        Collections.shuffle(sameIds);
        Map<String, Column> columnMap;
        while (count > 0 && r < items.size()) {
            columnMap = items.get(r);
            if (!isSame(columnMap)) {
                Integer from = sameIds.remove(0);
                items.set(r, items.get(from));
                items.set(from, columnMap);
                count--;
            }
            r++;
        }
    }


    /**
     * 由子类进行判断处理
     *
     * @param curr
     * @return boolean
     */
    protected boolean isSame(Map<String, Column> curr) {
        return false;
    }


}