/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: DataModelComposite
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/12/10 16:03
 * Description: 聚合多个数据模型
 */
package com.ppwx.easysearch.core.data;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ppwx.easysearch.core.data.model.AbstractDataModel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.util.*;

/**
 *
 * 聚合多个数据模型
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/10 16:03
 * @since 1.0.0
 */
public class DataModelComposite extends AbstractDataModel {

    private final List<DataModel> dataModelList;

    public DataModelComposite(List<DataModel> dataModelList) {
        this.dataModelList = dataModelList;
    }

    public List<DataModel> getDataModelList() {
        return dataModelList;
    }

    @Override
    public <T> Attribute<T> attr(AttributeKey<T> attributeKey) {
        // 从顶层向下
        boolean bool = super.hasAttr(attributeKey);
        if (!bool) {
            Optional<DataModel> first = dataModelList.stream().filter(model -> model.hasAttr(attributeKey)).findFirst();
            if (first.isPresent()) {
                return first.get().attr(attributeKey);
            }
        }
        return super.attr(attributeKey);
    }

    @Override
    public <T> boolean hasAttr(AttributeKey<T> attributeKey) {
        // 从顶层向下
        boolean bool = super.hasAttr(attributeKey);
        if (!bool) {
            bool = dataModelList.stream().anyMatch(model -> model.hasAttr(attributeKey));
        }
        return bool;
    }

    @Override
    public void loadDataModel() {
        dataModelList.forEach(DataModel::loadDataModel);
    }

    @Override
    public DataSet getDataset() {
        if (this.dataSet == null) {
            // merge multi dataset
            DataSetComposite dataSetComposite = new DataSetComposite();
            dataModelList.forEach(mode -> dataSetComposite.merge(mode.getDataset()));
            this.dataSet = dataSetComposite;
        }
        return this.dataSet;
    }

    static class DataSetComposite implements DataSet {

        private List<DataSet> dataSets = new LinkedList<>();

        void merge(DataSet dataSet) {
            if (dataSet != null) {
                dataSets.add(dataSet);
            }
        }

        @Override
        public int size() {
            int size = 0;
            for (DataSet dataSet : dataSets) {
                size += dataSet.size();
            }
            return size;
        }

        @Override
        public String idColumn() {
            if (!dataSets.isEmpty()) {
                return dataSets.get(0).idColumn();
            }
            return null;
        }

        @Override
        public List<String> columnNames() {
            if (!dataSets.isEmpty()) {
                return dataSets.get(0).columnNames();
            }
            return Collections.emptyList();
        }

        @Override
        public Map<String, Column> row(int idx) {
            for (DataSet dataSet : dataSets) {
                int size = dataSet.size();
                if (idx >= size) {
                    idx -= size;
                    continue;
                }
                return dataSet.row(idx);
            }
            return null;
        }

        @Override
        public List<Column> column(int idx) {
            if (dataSets.size() == 1) {
                return dataSets.get(0).column(idx);
            }
            List<Column> list = new LinkedList<>();
            for (DataSet dataSet : dataSets) {
                list.addAll(dataSet.column(idx));
            }
            return list;
        }

        @Override
        public List<Column> column(String filed) {
            if (dataSets.size() == 1) {
                return dataSets.get(0).column(filed);
            }
            List<Column> list = new LinkedList<>();
            for (DataSet dataSet : dataSets) {
                list.addAll(dataSet.column(filed));
            }
            return list;
        }

        @Override
        public Map<String, Column> find(Object id) {
            Map<String, Column> map = null;
            for (DataSet dataSet : dataSets) {
                map = dataSet.find(id);
                if (map != null) {
                    break;
                }
            }
            return map;
        }

        @Override
        public Column set(int idx, String field, Column column) {
            for (DataSet dataSet : dataSets) {
                int size = dataSet.size();
                if (idx >= size) {
                    idx -= size;
                    continue;
                }
                return dataSet.set(idx, field, column);
            }
            return null;
        }

        @Override
        public List<Map<String, Column>> all() {
            if (dataSets.size() == 1) {
                return dataSets.get(0).all();
            }
            List<Map<String, Column>> list = new LinkedList<>();
            for (DataSet dataSet : dataSets) {
                list.addAll(dataSet.all());
            }
            return list;
        }

        @Override
        public Object clone() {
            List<String> columnNames = null;
            if (dataSets.size() > 0) {
                columnNames = Lists.newArrayList(dataSets.iterator().next().columnNames());
            }
            List<Map<String, Column>> data = Lists.newLinkedList();
            HashMap<String, Column> newMap;
            for (Map<String, Column> map : all()) {
                newMap = Maps.newHashMapWithExpectedSize(map.size());
                newMap.putAll(map);
                data.add(newMap);
            }
            return new ListDataSet(columnNames, data);
        }
    }
}