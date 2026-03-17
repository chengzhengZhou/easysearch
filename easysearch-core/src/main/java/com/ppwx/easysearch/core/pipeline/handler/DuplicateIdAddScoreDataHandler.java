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

import com.google.common.collect.Maps;
import com.ppwx.easysearch.core.data.Column;
import com.ppwx.easysearch.core.data.DataModel;
import com.ppwx.easysearch.core.data.DataSet;
import com.ppwx.easysearch.core.data.ListDataSet;
import com.ppwx.easysearch.core.data.element.DoubleColumn;
import com.ppwx.easysearch.core.pipeline.DataHandler;
import com.ppwx.easysearch.core.pipeline.DataHandlerAdapter;
import com.ppwx.easysearch.core.pipeline.DataHandlerContext;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.ppwx.easysearch.core.data.model.Key.GLOBAL_ID;
import static com.ppwx.easysearch.core.data.model.Key.GLOBAL_SCORE;
import static com.ppwx.easysearch.core.util.SearchLog.getLogger;

/**
 * 重复id叠加score Handler
 *
 * @since 1.0.0
 */
@DataHandler.Sharable
public class DuplicateIdAddScoreDataHandler extends DataHandlerAdapter {

    @Override
    public void dataComplete(DataHandlerContext ctx) throws Exception {
        getLogger().debug("---------------execute DuplicateIdAddScoreDataHandler-------------");
        DataModel dataModel = ctx.dataModel();
        DataSet dataset = dataModel.getDataset();
        List<String> list = dataset.columnNames();
        if (list.contains(GLOBAL_SCORE)) {
            getLogger().debug("---------------before duplicate id score {}-------------", dataModel.getDataset().size());
            Map<String, Map<String, Column>> duplicateIds = Maps.newHashMapWithExpectedSize(64);
            List<Map<String, Column>> all = dataModel.getDataset().all();
            Iterator<Map<String, Column>> iterator = all.iterator();
            Map<String, Column> next;
            Column id;
            Column score;
            while (iterator.hasNext()) {

                next = iterator.next();
                id = next.get(GLOBAL_ID);
                if (duplicateIds.containsKey(id.asString())) {
                    // add score
                    score = addScore(duplicateIds.get(id.asString()).get(GLOBAL_SCORE), next.get(GLOBAL_SCORE));
                    duplicateIds.get(id.asString()).put(GLOBAL_SCORE, score);

                    iterator.remove();
                } else {
                    duplicateIds.put(id.asString(), next);
                }

            }
            // reset data
            dataModel.setDataSet(new ListDataSet(dataset.columnNames(), all));
            getLogger().debug("---------------after duplicate id score {}-------------", dataModel.getDataset().size());
        }

        super.dataComplete(ctx);
    }

    private Column addScore(Column origin, Column target) {
        if (target.getRawData() == null) {
            return origin;
        } else if (origin.getRawData() == null) {
            return target;
        } else {
            return new DoubleColumn(origin.asDouble() + target.asDouble());
        }
    }
}