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

package com.ppwx.easysearch.core.data.codec;

import com.ppwx.easysearch.core.data.Column;
import com.ppwx.easysearch.core.data.DataSet;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className CodecDataSetDecorator
 * @description 支持序数据集的序列化
 * @date 2024/11/29 14:56
 **/
public class DataSetMeta implements Serializable {

    /**
     * id column name
     */
    private String idColumn;
    /**
     * table name
     */
    private List<String> columnNames;
    /**
     * raw data
     */
    private List<Map<String, Column>> data;

    public void decorate(DataSet dataSet) {
        this.idColumn = dataSet.idColumn();
        this.columnNames = dataSet.columnNames();
        this.data = dataSet.all();
    }

    public String getIdColumn() {
        return idColumn;
    }

    public void setIdColumn(String idColumn) {
        this.idColumn = idColumn;
    }

    public List<String> getColumnNames() {
        return columnNames;
    }

    public void setColumnNames(List<String> columnNames) {
        this.columnNames = columnNames;
    }

    public List<Map<String, Column>> getData() {
        return data;
    }

    public void setData(List<Map<String, Column>> data) {
        this.data = data;
    }


}
