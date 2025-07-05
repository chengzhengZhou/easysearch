/**
 * Copyright (C), 2010-2024, 爱回收
 * FileName: FixSlotsRateDataHandlerTest
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2024/3/19 22:41
 * Description: FixSlotsRateDataHandler测试类
 */
package com.ppwx.easysearch.core.pipeline.handler;

import com.google.common.collect.Lists;
import com.ppwx.easysearch.core.data.Column;
import com.ppwx.easysearch.core.data.DataModel;
import com.ppwx.easysearch.core.data.ListDataSet;
import com.ppwx.easysearch.core.data.element.LongColumn;
import com.ppwx.easysearch.core.data.element.StringColumn;
import com.ppwx.easysearch.core.data.model.LocalDataModel;
import com.ppwx.easysearch.core.pipeline.*;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.ppwx.easysearch.core.data.model.Key.GLOBAL_ID;

/**
 *
 * FixSlotsRateDataHandler测试类
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2024/03/19 22:41
 * @since 1.0.0
 */
public class FixSlotsRateDataHandlerTest {

    @Test
    public void testFixSlotsRateDataHandlerWorks() throws Exception {
        List<String> columnNames = Lists.newArrayList(GLOBAL_ID, "name");
        List<Map<String, Column>> data = Lists.newArrayList();

        Map<String, Column> i1 = new HashMap<>();
        i1.put(GLOBAL_ID, new LongColumn(1));
        i1.put("name", new StringColumn("Ni"));
        data.add(i1);
        Map<String, Column> i2 = new HashMap<>();
        i2.put(GLOBAL_ID, new LongColumn(2));
        i2.put("name", new StringColumn("Zhou"));
        data.add(i2);
        Map<String, Column> i3 = new HashMap<>();
        i3.put(GLOBAL_ID, new LongColumn(3));
        i3.put("name", new StringColumn("Xu"));
        data.add(i3);
        Map<String, Column> i4 = new HashMap<>();
        i4.put(GLOBAL_ID, new LongColumn(4));
        i4.put("name", new StringColumn("You"));
        data.add(i4);
        Map<String, Column> i5 = new HashMap<>();
        i5.put(GLOBAL_ID, new LongColumn(5));
        i5.put("name", new StringColumn("Xu"));
        data.add(i5);
        Map<String, Column> i6 = new HashMap<>();
        i6.put(GLOBAL_ID, new LongColumn(6));
        i6.put("name", new StringColumn("Zhou"));
        data.add(i6);

        DataModel dataModel = new LocalDataModel();
        dataModel.setDataSet(new ListDataSet(columnNames, data));
        DataHandlerContext context = PowerMockito.mock(DataHandlerContext.class);
        Mockito.when(context.dataModel()).thenReturn(dataModel);
        FixSlotsRateDataHandler handler = new FixSlotsRateDataHandler() {
            @Override
            protected boolean isSame(Map<String, Column> curr) {
                return curr.get("name").asString().equals("Zhou");
            }
        };
        handler.setDefaultWindowSize(5);
        handler.setDefaultRate(50);
        handler.dataComplete(context);

        dataModel.getDataset().all().forEach(item -> {
            System.out.println(item.get(GLOBAL_ID).asString() + ": " + item.get("name").asString());
        });
    }

    @Test
    public void testMoreFixSlotsRateDataHandlerWorks() throws Exception {
        List<String> columnNames = Lists.newArrayList(GLOBAL_ID, "name");
        List<Map<String, Column>> data = Lists.newArrayList();

        Map<String, Column> i1 = new HashMap<>();
        i1.put(GLOBAL_ID, new LongColumn(1));
        i1.put("name", new StringColumn("Zhou"));
        data.add(i1);
        Map<String, Column> i2 = new HashMap<>();
        i2.put(GLOBAL_ID, new LongColumn(2));
        i2.put("name", new StringColumn("Zhou"));
        data.add(i2);
        Map<String, Column> i3 = new HashMap<>();
        i3.put(GLOBAL_ID, new LongColumn(3));
        i3.put("name", new StringColumn("Zhou"));
        data.add(i3);
        Map<String, Column> i4 = new HashMap<>();
        i4.put(GLOBAL_ID, new LongColumn(4));
        i4.put("name", new StringColumn("You"));
        data.add(i4);
        Map<String, Column> i5 = new HashMap<>();
        i5.put(GLOBAL_ID, new LongColumn(5));
        i5.put("name", new StringColumn("Xu"));
        data.add(i5);
        Map<String, Column> i6 = new HashMap<>();
        i6.put(GLOBAL_ID, new LongColumn(6));
        i6.put("name", new StringColumn("Zhou"));
        data.add(i6);

        DataModel dataModel = new LocalDataModel();
        dataModel.setDataSet(new ListDataSet(columnNames, data));
        DataHandlerContext context = PowerMockito.mock(DataHandlerContext.class);
        Mockito.when(context.dataModel()).thenReturn(dataModel);
        FixSlotsRateDataHandler handler = new FixSlotsRateDataHandler() {
            @Override
            protected boolean isSame(Map<String, Column> curr) {
                return curr.get("name").asString().equals("Zhou");
            }
        };
        handler.setDefaultWindowSize(2);
        handler.setDefaultRate(50);
        handler.setMaxSize(3);
        handler.dataComplete(context);

        dataModel.getDataset().all().forEach(item -> {
            System.out.println(item.get(GLOBAL_ID).asString() + ": " + item.get("name").asString());
        });
    }

    @Test
    public void testEmptyFixSlotsRateDataHandlerWorks() throws Exception {
        List<String> columnNames = Lists.newArrayList(GLOBAL_ID, "name");
        List<Map<String, Column>> data = Lists.newArrayList();

        DataModel dataModel = new LocalDataModel();
        dataModel.setDataSet(new ListDataSet(columnNames, data));
        DataHandlerContext context = PowerMockito.mock(DataHandlerContext.class);
        Mockito.when(context.dataModel()).thenReturn(dataModel);
        FixSlotsRateDataHandler handler = new FixSlotsRateDataHandler() {
            @Override
            protected boolean isSame(Map<String, Column> curr) {
                return curr.get("name").asString().equals("Zhou");
            }
        };
        handler.setDefaultWindowSize(2);
        handler.setDefaultRate(99);
        handler.dataComplete(context);

        dataModel.getDataset().all().forEach(item -> {
            System.out.println(item.get(GLOBAL_ID).asString() + ": " + item.get("name").asString());
        });
    }
}