/**
 * Copyright (C), 2010-2024, 爱回收
 * FileName: ModifyColumnDataModelDecorator
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2024/2/20 13:46
 * Description: 添加字段装饰器
 */
package com.ppwx.easysearch.core.data.model;

import com.google.common.collect.Lists;
import com.ppwx.easysearch.core.data.*;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * 添加字段装饰器
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2024/02/20 13:46
 * @since 1.0.0
 */
public class ModifyColumnDataModelDecorator implements DataModel {
    /**
     * 装饰的对象实例
     */
    private DataModel target;
    /**
     * 延迟字段
     */
    private List<Pair<String, Column>> delayColumns;

    public ModifyColumnDataModelDecorator(DataModel target) {
        this.target = target;
    }

    /**
     * 添加字段，在真正获取数据时才会进行数据添加
     *
     * @param field
     * @param val
     * @return void
     */
    public void addColumnDelay(String field, Column val) {
        if (null == delayColumns) {
            delayColumns = new ArrayList<>();
        }
        delayColumns.add(Pair.of(field, val));
    }

    /**
     * 添加字段
     *
     * @param field
     * @param val
     * @return boolean
     */
    public boolean addColumn(String field, Column val) {
        DataSet dataset = target.getDataset();
        List<String> columnNames = dataset.columnNames();
        if (columnNames.contains(field)) {
            return false;
        }

        columnNames.add(field);

        if (dataset instanceof ListDataSet) {
            dataset.all().forEach(columnMap -> columnMap.put(field, val));
        } else if (dataset instanceof TableDataSet) {
            TableDataSet tableDataSet = (TableDataSet) dataset;
            int columnIdx = columnNames.size() - 1;
            tableDataSet.getTable().rowKeySet().forEach(rowIdx -> {
                tableDataSet.getTable().put(rowIdx, columnIdx, val);
            });
            tableDataSet.init();
        } else {
            List<Map<String, Column>> all = dataset.all();
            all.forEach(columnMap -> columnMap.put(field, val));
            setDataSet(new ListDataSet(Lists.newArrayList(columnNames), all));
        }
        return true;
    }

    /**
     * 移除字段
     *
     * @param field
     * @return boolean
     */
    public boolean removeColumn(String field) {
        DataSet dataset = target.getDataset();
        List<String> columnNames = dataset.columnNames();
        if (!columnNames.contains(field)) {
            return false;
        }

        if (dataset instanceof ListDataSet) {
            columnNames.removeIf(field::equals);
            for (int i = 0; i < dataset.size(); i++) {
                dataset.row(i).remove(field);
            }
        } else {
            columnNames = columnNames.stream().filter(v -> !field.equals(v)).collect(Collectors.toList());
            List<Map<String, Column>> all = dataset.all();
            all.forEach(columnMap -> columnMap.remove(field));
            setDataSet(new ListDataSet(columnNames, all));
        }
        return true;
    }

    @Override
    public void loadDataModel() {
        target.loadDataModel();
    }

    @Override
    public DataSet getDataset() {
        DataSet dataset = target.getDataset();
        if (!CollectionUtils.isEmpty(delayColumns)) {
            delayColumns.forEach(pair -> addColumn(pair.getLeft(), pair.getRight()));
            delayColumns.clear();
        }
        return dataset;
    }

    @Override
    public void setDataSet(DataSet dataSet) {
        target.setDataSet(dataSet);
    }

    @Override
    public <T> Attribute<T> attr(AttributeKey<T> attributeKey) {
        return target.attr(attributeKey);
    }

    @Override
    public <T> boolean hasAttr(AttributeKey<T> attributeKey) {
        return target.hasAttr(attributeKey);
    }
}