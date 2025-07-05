/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: SlidingWindowInterleaveDataHandler
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/12/28 11:13
 * Description: 数据打散控制
 */
package com.ppwx.easysearch.core.pipeline.handler;

import com.ppwx.easysearch.core.data.Column;
import com.ppwx.easysearch.core.data.DataModel;
import com.ppwx.easysearch.core.data.DataSet;
import com.ppwx.easysearch.core.data.ListDataSet;
import com.ppwx.easysearch.core.pipeline.DataHandler;
import com.ppwx.easysearch.core.pipeline.DataHandlerAdapter;
import com.ppwx.easysearch.core.pipeline.DataHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.ppwx.easysearch.core.util.SearchLog.getLogger;

/**
 *
 * 数据打散控制
 * 基于滑动窗口，可配置前进步长和窗口大小
 * 默认每次步长为1，窗口为6
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/28 11:13
 * @since 1.0.0
 */
@DataHandler.Sharable
public class SlidingWindowInterleaveDataHandler extends DataHandlerAdapter {

    /**
     * 窗口大小
     */
    private int defaultWindowSize = 6;
    /**
     * 步长
     */
    private int step = 1;
    /**
     * 最大打散数量
     * 对于只需要部分打散的场景来看，减少打散量能提高性能
     */
    private int maxSize = 50;

    public SlidingWindowInterleaveDataHandler() {
    }

    public SlidingWindowInterleaveDataHandler(int defaultWindowSize, int step) {
        this.defaultWindowSize = defaultWindowSize;
        this.step = step;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public int getDefaultWindowSize() {
        return defaultWindowSize;
    }

    public void setDefaultWindowSize(int defaultWindowSize) {
        if (defaultWindowSize <= 2) {
            throw new IllegalArgumentException("window size must more than two.");
        }
        this.defaultWindowSize = defaultWindowSize;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        if (defaultWindowSize <= 1) {
            throw new IllegalArgumentException("step must more than one.");
        }
        this.step = step;
    }

    @Override
    public void dataComplete(DataHandlerContext ctx) throws Exception {
        getLogger().debug("---------------execute SlidingWindowInterleaveDataHandler-------------");
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
        while (l < r && r < size) {
            if (l == 0) {
                process(1, l, r, items);
            } else {
                process(Math.max(r - step, 0), l, r, items);
            }
            // scroll
            l += step;
            r += step;
        }
        // reset dataset
        dataModel.setDataSet(new ListDataSet(dataset.columnNames(), items));

        super.dataComplete(ctx);
    }

    /**
     * 对窗口内数据进行排序
     *
     * @param from 起始点
     * @param l 窗口左节点
     * @param r 窗口右节点
     * @param items 元数据
     * @return void
     */
    protected void process(int from, final int l, final int r, List<Map<String, Column>> items) {
        int limit = items.size();
        int idx;
        Map<String, Column> curr;
        Map<String, Column> temp;
        while (from <= r) {
            curr = items.get(from);
            if (isSame(l, from, curr, items)) {
                // find nearest item
                idx = (from + 1);
                boolean find = false;
                while (idx < limit) {
                    temp = items.get(idx);
                    if (isSame(l, Math.min(r, idx), temp, items)) {
                        ++idx;
                    } else {
                        swap(idx, from, items);
                        find = true;
                        break;
                    }
                }
                if (!find) {
                    // break advice
                    break;
                }
            }
            from++;
        }
    }

    /**
     * 交互顺序
     *
     * @param idx
     * @param cid
     * @param items
     * @return void
     */
    protected void swap(int idx, int cid, List<Map<String, Column>> items) {
        Map<String, Column> model = items.get(idx);
        items.set(idx, items.get(cid));
        items.set(cid, model);
    }


    protected boolean isSame(int l, int r, Map<String, Column> curr, List<Map<String, Column>> items) {
        for (; l < r; l++) {
            if (equalsResource(curr, items.get(l))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 子类通过重写该方法判断是否需要打散
     *
     * @param origin
     * @param target
     * @return boolean
     */
    protected boolean equalsResource(Map<String, Column> origin, Map<String, Column> target) {
        return Objects.equals(origin, target);
    }

}