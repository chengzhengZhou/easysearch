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