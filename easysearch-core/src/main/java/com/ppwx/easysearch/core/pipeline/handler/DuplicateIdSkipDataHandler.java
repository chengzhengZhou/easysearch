/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: DuplicateIdSkipDataHandler
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/12/10 16:52
 * Description: 重复key剔除Handler
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
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/10 16:52
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