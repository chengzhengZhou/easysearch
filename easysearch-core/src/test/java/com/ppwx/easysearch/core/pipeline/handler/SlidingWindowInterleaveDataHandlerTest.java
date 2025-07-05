/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: SlidingWindowInterleaveDataHandlerTest
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/12/28 12:36
 * Description: SlidingWindowInterleaveDataHandler测试类
 */
package com.ppwx.easysearch.core.pipeline.handler;

import com.google.common.collect.Lists;
import com.ppwx.easysearch.core.data.Column;
import com.ppwx.easysearch.core.data.DataModel;
import com.ppwx.easysearch.core.data.ListDataSet;
import com.ppwx.easysearch.core.data.element.LongColumn;
import com.ppwx.easysearch.core.data.element.StringColumn;
import com.ppwx.easysearch.core.data.model.LocalDataModel;
import com.ppwx.easysearch.core.pipeline.DataHandlerContext;
import io.netty.util.AttributeKey;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.ppwx.easysearch.core.data.model.Key.GLOBAL_ID;

/**
 *
 * SlidingWindowInterleaveDataHandler测试类
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/28 12:36
 * @since 1.0.0
 */
public class SlidingWindowInterleaveDataHandlerTest {

    private DataHandlerContext getContext() {
        List<String> columnNames = Lists.newArrayList(GLOBAL_ID, "productName");
        List<Map<String, Column>> data = Lists.newArrayList();

        Map<String, Column> i1 = new HashMap<>();
        i1.put(GLOBAL_ID, new LongColumn(1));
        i1.put("productName", new StringColumn("苹果 iphone 13"));
        data.add(i1);
        Map<String, Column> i2 = new HashMap<>();
        i2.put(GLOBAL_ID, new LongColumn(2));
        i2.put("productName", new StringColumn("苹果 pod 1"));
        data.add(i2);
        Map<String, Column> i3 = new HashMap<>();
        i3.put(GLOBAL_ID, new LongColumn(3));
        i3.put("productName", new StringColumn("苹果 iphone 13"));
        data.add(i3);
        Map<String, Column> i4 = new HashMap<>();
        i4.put(GLOBAL_ID, new LongColumn(4));
        i4.put("productName", new StringColumn("苹果 iphone 16"));
        data.add(i4);
        Map<String, Column> i5 = new HashMap<>();
        i5.put(GLOBAL_ID, new LongColumn(5));
        i5.put("productName", new StringColumn("苹果 MackBook"));
        data.add(i5);
        Map<String, Column> i6 = new HashMap<>();
        i6.put(GLOBAL_ID, new LongColumn(6));
        i6.put("productName", new StringColumn("苹果 MackBook air"));
        data.add(i6);
        Map<String, Column> i7 = new HashMap<>();
        i7.put(GLOBAL_ID, new LongColumn(6));
        i7.put("productName", new StringColumn("苹果 watch pro"));
        data.add(i7);

        DataModel dataModel = new LocalDataModel();
        dataModel.setDataSet(new ListDataSet(columnNames, data));
        DataHandlerContext context = Mockito.mock(DataHandlerContext.class);
        Mockito.when(context.dataModel()).thenReturn(dataModel);
        return context;
    }

    @Test
    public void testSlidingWindowInterleaveDataHandlerWorks() throws Exception {
        DataHandlerContext context = getContext();
        SlidingWindowInterleaveDataHandler handler = new SlidingWindowInterleaveDataHandler() {
            @Override
            protected boolean equalsResource(Map<String, Column> origin, Map<String, Column> target) {
                return Objects.equals(origin.get("productName").getRawData(), target.get("productName").getRawData());
            }
        };
        handler.setDefaultWindowSize(5);
        handler.setStep(1);
        handler.dataComplete(context);

        context.dataModel().getDataset().all().forEach(item -> {
            System.out.println(item.get(GLOBAL_ID).asString() + ": " + item.get("productName").asString());
        });
    }

    @Test
    public void testMMRInterleaveDataHandlerWorks() throws Exception {
        DataHandlerContext context = getContext();
        MMRInterleaveDataHandler handler = new MMRInterleaveDataHandler();
        handler.setQuery("苹果");
        handler.dataComplete(context);

        context.dataModel().getDataset().all().forEach(
                item -> System.out.println(item.get(GLOBAL_ID).asString() + ": " + item.get("productName").asString()));
    }

}