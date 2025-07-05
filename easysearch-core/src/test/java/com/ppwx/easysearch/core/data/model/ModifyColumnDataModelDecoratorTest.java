/**
 * Copyright (C), 2010-2024, 爱回收
 * FileName: ModifyColumnDataModelDecoratorTest
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2024/2/20 14:32
 * Description: ModifyColumnDataModelDecorator测试类
 */
package com.ppwx.easysearch.core.data.model;

import com.google.common.collect.Maps;
import com.ppwx.easysearch.core.data.Column;
import com.ppwx.easysearch.core.data.DataSet;
import com.ppwx.easysearch.core.data.ListDataSet;
import com.ppwx.easysearch.core.data.element.LongColumn;
import com.ppwx.easysearch.core.data.element.StringColumn;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

/**
 *
 * ModifyColumnDataModelDecorator测试类
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2024/02/20 14:32
 * @since 1.0.0
 */
public class ModifyColumnDataModelDecoratorTest {

    private LocalDataModel localDataModel;

    @Before
    public void init() {
        List<String> columns = new ArrayList<>();
        columns.add("name");
        columns.add("age");

        List<Map<String, Column>> data = new LinkedList<>();
        Map<String, Column> d1 = Maps.newHashMap();
        d1.put("name", new StringColumn("sophiszhou"));
        d1.put("age", new LongColumn(18));
        data.add(d1);

        LocalDataModel localDataModel = new LocalDataModel();
        localDataModel.setDataSet(new ListDataSet(columns, data));
        this.localDataModel = localDataModel;
    }

    @Test
    public void testAddColumnWorks() {
        ModifyColumnDataModelDecorator dataModelDecorator = new ModifyColumnDataModelDecorator(localDataModel);
        dataModelDecorator.addColumn("chromosome", new StringColumn("guess"));

        Assert.assertTrue(dataModelDecorator.getDataset().columnNames().size() >= 3);
        System.out.println(dataModelDecorator.getDataset().column("chromosome").iterator().next().asString().equals("guess"));
    }

    @Test
    public void testRemoveColumnWorks() {
        ModifyColumnDataModelDecorator dataModelDecorator = new ModifyColumnDataModelDecorator(localDataModel);
        dataModelDecorator.removeColumn("age");
        Assert.assertEquals(1, dataModelDecorator.getDataset().columnNames().size());
        System.out.println(dataModelDecorator.getDataset().all());
    }

    @Test
    public void testMultiOptWorks() {
        ModifyColumnDataModelDecorator dataModelDecorator = new ModifyColumnDataModelDecorator(localDataModel);
        dataModelDecorator.addColumn("chromosome", new StringColumn("guess"));
        dataModelDecorator.removeColumn("age");
        Assert.assertEquals(2, dataModelDecorator.getDataset().columnNames().size());
        List<Column> column = dataModelDecorator.getDataset().column(1);
        System.out.println(column);
    }

    @Test
    public void testDelayWorks() {
        ModifyColumnDataModelDecorator dataModelDecorator = new ModifyColumnDataModelDecorator(localDataModel);
        dataModelDecorator.addColumnDelay("chromosome", new StringColumn("guess"));

        dataModelDecorator.getDataset();
        Assert.assertTrue(dataModelDecorator.getDataset().columnNames().size() >= 3);
        System.out.println(dataModelDecorator.getDataset().column("chromosome").iterator().next().asString().equals("guess"));
    }

}