/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: ListDataSet
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/12/10 17:58
 * Description: 集合结构的数据集
 */
package com.ppwx.easysearch.core.data;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.*;
import java.util.stream.Collectors;

import static com.ppwx.easysearch.core.data.model.Key.GLOBAL_ID;

/**
 *
 * 集合结构的数据集
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/10 17:58
 * @since 1.0.0
 */
public class ListDataSet implements DataSet {

    /**
     * id column name
     */
    private final String idColumn;
    /**
     * table name
     */
    private final List<String> columnNames;
    /**
     * raw data
     */
    private final List<Map<String, Column>> data;

    public ListDataSet(List<String> columnNames, List<Map<String, Column>> data) {
        this(GLOBAL_ID, columnNames, data);
    }

    public ListDataSet(String idColumn, List<String> columnNames, List<Map<String, Column>> data) {
        this.columnNames = columnNames;
        this.data = data;
        this.idColumn = idColumn;
    }

    public void init() {
        if (columnNames == null || data == null) {
            throw new IllegalArgumentException("columnNames and table can not be null.");
        }
    }

    @Override
    public int size() {
        return data == null ? 0 : data.size();
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
    public Map<String, Column> row(int idx) {
        return data == null ? null : data.get(idx);
    }

    @Override
    public List<Column> column(int idx) {
        if (data == null) {
            return null;
        }
        String columnName = columnNames.get(idx);
        return this.data.stream().map(item -> item.get(columnName)).collect(Collectors.toList());
    }

    @Override
    public List<Column> column(String filed) {
        if (data == null) {
            return null;
        }
        return this.data.stream().map(item -> item.get(filed)).collect(Collectors.toList());
    }

    @Override
    public Map<String, Column> find(Object id) {
        if (data == null) {
            return null;
        }
        Optional<Map<String, Column>> first = data.stream()
                .filter(map -> Objects.equals(id, map.get(idColumn).getRawData())).findFirst();
        return first.orElse(null);
    }

    @Override
    public Column set(int idx, String field, Column column) {
        if (data == null) {
            throw new IllegalStateException("Data is null");
        }
        if (idx >= data.size()) {
            throw new IndexOutOfBoundsException(String.format("idx:%s, size:%s", idx, data.size()));
        }
        Map<String, Column> map = row(idx);
        return map.put(field, column);
    }

    @Override
    public List<Map<String, Column>> all() {
        return Lists.newLinkedList(this.data);
    }

    @Override
    public Object clone() {
        String idColumn = this.idColumn;
        List<String> columnNames = null;
        if (this.columnNames != null) {
            columnNames = Lists.newArrayList(this.columnNames);
        }
        List<Map<String, Column>> data = null;
        if (this.data != null) {
            data = Lists.newLinkedList();
            HashMap<String, Column> newMap;
            for (Map<String, Column> map : this.data) {
                newMap = Maps.newHashMapWithExpectedSize(map.size());
                newMap.putAll(map);
                data.add(newMap);
            }
        }
        return new ListDataSet(idColumn, columnNames, data);
    }
}