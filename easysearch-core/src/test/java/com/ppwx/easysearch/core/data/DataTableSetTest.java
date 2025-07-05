/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: DataTableSetTest
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/12/7 18:06
 * Description: DataTableSet测试类
 */
package com.ppwx.easysearch.core.data;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.ppwx.easysearch.core.data.element.DynamicColumn;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 *
 * DataTableSet测试类
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/07 18:06
 * @since 1.0.0
 */
public class DataTableSetTest {

    @Test
    public void testDataTableSetWorks() {
        List<String> columnNames = Arrays.asList("id", "name", "age", "createTime");
        Table<Integer, Integer ,Column> table = HashBasedTable.create();
        table.put(0, 0, new DynamicColumn(1));
        table.put(0, 1, new DynamicColumn("sophiszhou"));
        table.put(0, 2, new DynamicColumn(18));
        table.put(0, 3, new DynamicColumn(new Date()));

        TableDataSet dataTableSet = new TableDataSet("id", columnNames, table);
        Map<String, Column> row = dataTableSet.row(0);
        Assert.assertNotNull(row);

        row = dataTableSet.find(1);
        Assert.assertNotNull(row);

        List<Column> column = dataTableSet.column(1);
        Assert.assertNotNull(column);
    }

}