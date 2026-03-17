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

import com.google.common.collect.Sets;
import com.ppwx.easysearch.core.data.Column;
import com.ppwx.easysearch.core.data.DataModel;
import com.ppwx.easysearch.core.data.DataSet;
import com.ppwx.easysearch.core.data.ListDataSet;
import com.ppwx.easysearch.core.pipeline.DataHandler;
import com.ppwx.easysearch.core.pipeline.DataHandlerAdapter;
import com.ppwx.easysearch.core.pipeline.DataHandlerContext;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.ppwx.easysearch.core.data.model.Key.GLOBAL_ID;
import static com.ppwx.easysearch.core.util.SearchLog.getLogger;

/**
 *
 * 重复id剔除Handler
 *
 * @since 1.0.0
 */
@DataHandler.Sharable
public class DuplicateIdSkipDataHandler extends DataHandlerAdapter {

    @Override
    public void dataComplete(DataHandlerContext ctx) throws Exception {
        getLogger().debug("---------------execute DuplicateIdSkipDataHandler-------------");
        DataModel dataModel = ctx.dataModel();
        DataSet dataset = dataModel.getDataset();
        List<String> list = dataset.columnNames();
        if (list.contains(GLOBAL_ID)) {
            getLogger().debug("---------------before duplicate id skip {}-------------", dataModel.getDataset().size());
            Set<String> duplicateIds = Sets.newHashSetWithExpectedSize(64);
            List<Map<String, Column>> all = dataModel.getDataset().all();
            Iterator<Map<String, Column>> iterator = all.iterator();
            Map<String, Column> next;
            Column column;
            while (iterator.hasNext()) {
                next = iterator.next();
                column = next.get(GLOBAL_ID);
                if (duplicateIds.contains(column.asString())) {
                    iterator.remove();
                } else {
                    duplicateIds.add(column.asString());
                }
            }
            // reset data
            dataModel.setDataSet(new ListDataSet(dataset.columnNames(), all));
            getLogger().debug("---------------after duplicate id skip {}-------------", dataModel.getDataset().size());
        }

        super.dataComplete(ctx);
    }
}