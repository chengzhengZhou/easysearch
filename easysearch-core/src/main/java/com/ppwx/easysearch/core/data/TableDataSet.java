/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: TableDataSet
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/12/7 17:00
 * Description: 数据Table
 */
package com.ppwx.easysearch.core.data;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.*;

import static com.ppwx.easysearch.core.data.model.Key.GLOBAL_ID;

/**
 *
 * 数据集Table实现
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/07 17:00
 * @since 1.0.0
 */
public class TableDataSet implements DataSet, Serializable {

    /**
     * id column name
     */
    private final String idColumn;
    /**
     * table name
     */
    private final List<String> columnNames;
    /**
     * data table
     */
    private final Table<Integer, Integer, Column> table;
    /**
     * 属性下标
     */
    private Map<String, Integer> colIdxMap;

    public TableDataSet(List<String> columnNames, Table<Integer, Integer, Column> table) {
        this(GLOBAL_ID, columnNames, table);
    }

    public TableDataSet(String idColumn, List<String> columnNames, Table<Integer, Integer, Column> table) {
        this.idColumn = idColumn;
        this.columnNames = columnNames;
        this.table = table;
        init();
    }

    public void init() {
        if (columnNames == null || table == null) {
            throw new IllegalArgumentException("columnNames and table can not be null.");
        }
        Map<String, Integer> colIdxMap = new HashMap<>(columnNames.size());
        for (int i = 0; i < columnNames.size(); i++) {
            colIdxMap.put(columnNames.get(i), i);
        }
        this.colIdxMap = colIdxMap;
    }

    @Override
    public int size() {
        return table.rowKeySet().size();
    }

    @Override
    public String idColumn() {
        return idColumn;
    }

    @Override
    public List<String> columnNames() {
        return this.columnNames;
    }

    @Override
    public Map<String, Column> row(int idx) throws IndexOutOfBoundsException {
        Map<String, Column> rowData = null;
        Map<Integer, Column> row = table.row(idx);
        if (row != null) {
            rowData = new HashMap<>(row.size());
            for (int i = 0; i < columnNames.size(); i++) {
                rowData.put(columnNames.get(i), row.get(i));
            }
        }
        return rowData;
    }

    @Override
    public List<Column> column(int idx) {
        Map<Integer, Column> column = table.column(idx);
        return Lists.newArrayList(column.values());
    }

    @Override
    public List<Column> column(String filed) {
        for (int i = 0; i < columnNames.size(); i++) {
            if (StringUtils.equals(columnNames.get(i), filed)) {
                Map<Integer, Column> column = table.column(i);
                return Lists.newArrayList(column.values());
            }
        }
        return null;
    }

    @Override
    public Map<String, Column> find(Object id) {
        Optional<Map.Entry<Integer, Column>> first = table.column(colIdxMap.get(idColumn)).entrySet().stream()
                .filter(entry -> Objects.equals(entry.getValue().getRawData(), id)).findFirst();
        return first.map(integerColumnEntry -> row(integerColumnEntry.getKey())).orElse(null);
    }

    @Override
    public Column set(int idx, String field, Column column) {
        if (table == null) {
            throw new IllegalStateException("Table is null");
        }
        if (idx >= table.size()) {
            throw new IndexOutOfBoundsException(String.format("idx:%s, size:%s", idx, table.size()));
        }
        Map<Integer, Column> row = table.row(idx);
        Integer colIdx = colIdxMap.get(field);
        return row.put(colIdx, column);
    }

    @Override
    public List<Map<String, Column>> all() {
        List<Map<String, Column>> list = new LinkedList<>();
        table.rowKeySet().forEach(rowId -> list.add(row(rowId)));
        return list;
    }

    @Override
    public Object clone() {
        String idColumn = this.idColumn;
        List<String> columnNames = null;
        if (this.columnNames != null) {
            columnNames = Lists.newArrayList(this.columnNames);
        }
        Table<Integer, Integer, Column> table = null;
        if (this.table != null) {
            table = HashBasedTable.create(this.table);
        }
        return new TableDataSet(idColumn, columnNames, table);
    }

    public List<String> getColumnNames() {
        return columnNames;
    }


    public Table<Integer, Integer, Column> getTable() {
        return table;
    }

    public String getIdColumn() {
        return idColumn;
    }
}