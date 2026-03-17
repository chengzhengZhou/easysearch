/*
 * Copyright 2026 chengzhengZhou
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ppwx.easysearch.core.pipeline.handler;

import com.ppwx.easysearch.core.data.Column;
import com.ppwx.easysearch.core.data.DataModel;
import com.ppwx.easysearch.core.data.DataSet;
import com.ppwx.easysearch.core.data.ListDataSet;
import com.ppwx.easysearch.core.pipeline.DataHandler;
import com.ppwx.easysearch.core.pipeline.DataHandlerAdapter;
import com.ppwx.easysearch.core.pipeline.DataHandlerContext;

import java.util.List;
import java.util.Map;

import static com.ppwx.easysearch.core.data.model.Key.GLOBAL_SCORE;
import static com.ppwx.easysearch.core.util.SearchLog.getLogger;

/**
 *
 * 分值排序处理器
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/27 13:41
 * @since 1.0.0
 */
@DataHandler.Sharable
public class ScoreSortDataHandler extends DataHandlerAdapter {

    @Override
    public void dataComplete(DataHandlerContext ctx) throws Exception {
        getLogger().debug("---------------execute ScoreSortDataHandler-------------");
        DataModel dataModel = ctx.dataModel();
        DataSet dataset = dataModel.getDataset();
        if (dataset.size() <= 1) {
            return;
        }
        List<Map<String, Column>> all = dataset.all();
        all.sort((o1, o2) -> {
            if (o1 == null || o1.get(GLOBAL_SCORE) == null) {
                return 1;
            } else if (o2 == null || o2.get(GLOBAL_SCORE) == null) {
                return -1;
            }
            Column col1 = o1.get(GLOBAL_SCORE);
            Column col2 = o2.get(GLOBAL_SCORE);
            if (col1 == null) {
                return 1;
            } else if (col2 == null) {
                return -1;
            }
            return - Double.compare(col1.asDouble(), col2.asDouble());
        });
        // reset data
        dataModel.setDataSet(new ListDataSet(dataset.columnNames(), all));

        super.dataComplete(ctx);
    }
}