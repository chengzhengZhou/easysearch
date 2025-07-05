/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: DefaultDataPipelineTest
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/12/10 0:11
 * Description:
 */
package com.ppwx.easysearch.core.pipeline;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.ppwx.easysearch.core.data.*;
import com.ppwx.easysearch.core.data.element.DoubleColumn;
import com.ppwx.easysearch.core.data.element.StringColumn;
import com.ppwx.easysearch.core.data.model.AbstractDataModel;
import com.ppwx.easysearch.core.data.model.DataModelProxy;
import com.ppwx.easysearch.core.data.model.LocalDataModel;
import com.ppwx.easysearch.core.pipeline.handler.DuplicateIdAddScoreDataHandler;
import com.ppwx.easysearch.core.pipeline.handler.DuplicateIdSkipDataHandler;
import com.ppwx.easysearch.core.pipeline.handler.SlidingWindowInterleaveDataHandler;
import com.ppwx.easysearch.core.util.DataModelThreadHolder;
import io.netty.util.AttributeKey;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static com.ppwx.easysearch.core.data.model.Key.GLOBAL_ID;
import static com.ppwx.easysearch.core.data.model.Key.GLOBAL_SCORE;

/**
 *
 *
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/10 00:11
 * @since 1.0.0
 */
public class DefaultDataPipelineTest {

    static class InnerHandler extends DataHandlerAdapter {
        @Override
        public void dataPrepare(DataHandlerContext ctx) throws Exception {
            System.out.println("dataPrepared:" + ctx.name());
            super.dataPrepare(ctx);
        }

        @Override
        public void dataComplete(DataHandlerContext ctx) throws Exception {
            System.out.println("dataComplete:" + ctx.name() + " > " + ctx.attr(AttributeKey.valueOf("num")).get());
            super.dataComplete(ctx);
        }

        @Override
        public void exceptionCaught(DataHandlerContext ctx, Throwable cause) throws Exception {
            System.out.println("exceptionCaught:" + ctx.name());
            super.exceptionCaught(ctx, cause);
        }
    }

    static class InnerModel extends AbstractDataModel {

        @Override
        public void loadDataModel() {

        }
    }

    @Test
    public void testDuplicateIdSkipDataHandlerWorks() {
        Table<Integer, Integer, Column> table = HashBasedTable.create();
        table.put(0, 0, new StringColumn("1"));
        table.put(0, 1, new StringColumn("sophiszhou"));
        table.put(1, 0, new StringColumn("1"));
        table.put(1, 1, new StringColumn("sophiszhouCopy"));
        table.put(2, 0, new StringColumn("2"));
        table.put(2, 1, new StringColumn("xuqin"));

        TableDataSet dataSet = new TableDataSet(Arrays.asList(GLOBAL_ID, "name"), table);

        DataModel dataModel = new InnerModel();
        dataModel.setDataSet(dataSet);
        DefaultDataPipeline pipeline = new DefaultDataPipeline(dataModel);
        pipeline.addLast("duplicateIdSkip", new DuplicateIdSkipDataHandler());
        pipeline.fireDataComplete();

        System.out.println(dataModel.getDataset().all());
    }

    @Test
    public void testPipelineWorks() {
        Table<Integer, Integer, Column> table = HashBasedTable.create();
        table.put(0, 0, new StringColumn("1"));
        table.put(0, 1, new StringColumn("sophiszhou"));
        table.put(1, 0, new StringColumn("1"));
        table.put(1, 1, new StringColumn("sophiszhouCopy"));
        table.put(2, 0, new StringColumn("2"));
        table.put(2, 1, new StringColumn("xuqin"));

        DataSet dataSet = new TableDataSet(Arrays.asList(GLOBAL_ID, "name"), table);
        DataModel dataModel = new InnerModel();
        dataModel.setDataSet(dataSet);

        DataPipelineBuilder pipelineBuilder = new DataPipelineBuilder();
        pipelineBuilder.dataModel(dataModel);
        DataPipeline pipeline = pipelineBuilder.build();
        pipeline.addLast("duplicateIdSkip", new DuplicateIdSkipDataHandler());
        pipeline.fireDataPrepare();
        pipeline.fireDataComplete();
        System.out.println(dataModel.getDataset().all());
    }

    @Test
    public void testDataModelProxyWorks() {
        DataPipelineBuilder builder = new DataPipelineBuilder();
        builder.dataModel(new DataModelProxy());
        builder.handler(dataPipeline -> dataPipeline.addLast("innerHandler", new InnerHandler()));
        builder.mode(false);
        DataPipeline pipeline = builder.build();

        /*InnerModel model = new InnerModel();
        model.attr(AttributeKey.valueOf("num")).set(0);
        DataModelThreadHolder.set(model);
        pipeline.fireDataComplete();*/

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++) {
            int finalI = i;
            executorService.execute(() -> {
                InnerModel model = new InnerModel();
                model.attr(AttributeKey.valueOf("num")).set(finalI);
                DataModelThreadHolder.set(model);
                pipeline.fireDataComplete();
            });
        }
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
    }

    @Test
    public void testAddFirst() {
        ListDataSet listDataSet = new ListDataSet(Collections.emptyList(), Collections.emptyList());
        LocalDataModel localDataModel = new LocalDataModel();
        localDataModel.setDataSet(listDataSet);
        // build pipeline
        DataPipelineBuilder builder = new DataPipelineBuilder().dataModel(localDataModel);
        // add base handler
        builder.handler(pipeline -> {
            pipeline.addFirst(new DuplicateIdSkipDataHandler());
            pipeline.addLast(new SlidingWindowInterleaveDataHandler());
        });
        builder.build();

        DataPipeline pipeline = builder.build();
        // prepare
        //pipeline.fireDataPrepare();
        // complete
        pipeline.fireDataComplete();
        System.out.println("hh");
    }

    @Test
    public void testDuplicateIdAddScoreDataHandlerWorks() {
        Table<Integer, Integer, Column> table = HashBasedTable.create();
        table.put(0, 0, new StringColumn("1"));
        table.put(0, 1, new DoubleColumn(10.0));
        table.put(0, 2, new StringColumn("sophiszhou"));

        table.put(1, 0, new StringColumn("1"));
        table.put(1, 1, new DoubleColumn(11.0));
        table.put(1, 2, new StringColumn("sophiszhouCopy"));

        table.put(2, 0, new StringColumn("2"));
        table.put(2, 1, new DoubleColumn(12.0));
        table.put(2, 2, new StringColumn("xuqin"));

        TableDataSet dataSet = new TableDataSet(Arrays.asList(GLOBAL_ID, GLOBAL_SCORE, "name"), table);

        DataModel dataModel = new InnerModel();
        dataModel.setDataSet(dataSet);
        DefaultDataPipeline pipeline = new DefaultDataPipeline(dataModel);
        pipeline.addLast("duplicateIdAddScore", new DuplicateIdAddScoreDataHandler());
        pipeline.fireDataComplete();

        System.out.println(dataModel.getDataset().all());
    }
}