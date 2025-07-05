/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: AbstractElasticSearchDataModel
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/12/7 19:55
 * Description: ElasticSearch作为数据源加载的数据集
 */
package com.ppwx.easysearch.core.data.model;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ppwx.easysearch.core.common.DataException;
import com.ppwx.easysearch.core.data.Column;
import com.ppwx.easysearch.core.data.DataSet;
import com.ppwx.easysearch.core.data.ListDataSet;
import com.ppwx.easysearch.core.data.element.*;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.search.SearchHits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.ppwx.easysearch.core.data.model.Key.*;

/**
 *
 * ElasticSearch作为数据源模型
 * 该抽象类实现了索引配置解析，子类通过实现doQuery方法进行实际的调用
 * mapping和实际返回字段最好吻合统一，避免出现类型转换出错
 * 默认会在字段添加字段：GLOBAL_ID, GLOBAL_SCORE, GLOBAL_STAINING, GLOBAL_SCORE_DETAIL
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/07 19:55
 * @since 1.0.0
 */
public abstract class AbstractElasticSearchDataModel extends AbstractDataModel {
    /**
     * logger
     */
    protected static final Logger logger = LoggerFactory.getLogger(AbstractElasticSearchDataModel.class);

    private static final String DEFAULT_ID_COL = "_id";

    private static final String DEFAULT_SCORE_COL = "_score";
    /**
     * 默认id字段
     */
    private String idCol = DEFAULT_ID_COL;

    /**
     * 字段类型
     */
    protected Map<String, ElasticSearchFieldType> mappingTypes;
    /**
     * 构造函数
     *
     * @param mappingTypes
     * @return
     */
    public AbstractElasticSearchDataModel(Map<String, ElasticSearchFieldType> mappingTypes) {
        this.mappingTypes = mappingTypes;
    }

    /**
     * 添加字段
     *
     * @param field
     * @param type
     * @return void
     */
    public void addField(String field, ElasticSearchFieldType type) {
        if (this.mappingTypes == null) {
            this.mappingTypes = new LinkedHashMap<>();
        }
        this.mappingTypes.put(field, type);
    }

    public void setIdCol(String idCol) {
        this.idCol = idCol;
    }

    @Override
    public void loadDataModel() {
        if (this.mappingTypes == null) {
            if (!StringUtils.equals(idCol, DEFAULT_ID_COL)) {
                logger.warn("ignore idCol:{} because of empty mappings", idCol);
            }
        } else {
            // remove default col
            if (this.mappingTypes.containsKey(DEFAULT_ID_COL)) {
                mappingTypes.remove(DEFAULT_ID_COL);
            }
            if (this.mappingTypes.containsKey(DEFAULT_SCORE_COL)) {
                mappingTypes.remove(DEFAULT_SCORE_COL);
            }
            if (!StringUtils.equals(idCol, DEFAULT_ID_COL) && !mappingTypes.containsKey(idCol)) {
                throw new IllegalArgumentException("Can not find mapping field:" + idCol);
            }
        }
    }

    @Override
    public DataSet getDataset() {
        if (dataSet == null) {
            List<String> columnNames = Lists.newArrayList(GLOBAL_ID, GLOBAL_SCORE, GLOBAL_STAINING, GLOBAL_SCORE_DETAIL);
            if (this.mappingTypes != null) {
                columnNames.addAll(mappingTypes.keySet());
            }

            List<Map<String, Column>> data = Lists.newLinkedList();
            try {
                SearchHits hits = doQuery();
                if (hits != null) {
                    hits.forEach(hit -> {
                        Map<String, Column> map = Maps.newLinkedHashMap();
                        map.put(GLOBAL_ID, new StringColumn(hit.getId()));
                        if (Float.isNaN(hit.getScore())) {
                            map.put(GLOBAL_SCORE, new DoubleColumn(0));
                        } else {
                            map.put(GLOBAL_SCORE, new DoubleColumn(hit.getScore()));
                        }
                        map.put(GLOBAL_STAINING, getRecallMark());
                        map.put(GLOBAL_SCORE_DETAIL, initScoreDetail());

                        if (!CollectionUtils.isEmpty(this.mappingTypes)) {
                            JSONObject jsonObject = JSONObject.parseObject(hit.getSourceAsString());
                            for (String field : mappingTypes.keySet()) {
                                map.put(field, getResultColumn(jsonObject, field, this.mappingTypes.get(field)));
                            }
                            if (!StringUtils.equals(idCol, DEFAULT_ID_COL)) {
                                map.put(GLOBAL_ID, getResultColumn(jsonObject, idCol, this.mappingTypes.get(idCol)));
                            }
                        }
                        data.add(map);
                    });
                }
            } catch (Throwable throwable) {
                throw new DataException(throwable);
            }
            this.dataSet = postProcess(columnNames, data);
        }
        return this.dataSet;
    }

    protected Column initScoreDetail() {
        return new JsonColumn(new JSONObject());
    }

    protected Column getRecallMark() {
        return new StringColumn("default");
    }

    protected DataSet postProcess(List<String> columnNames, List<Map<String, Column>> data) {
        return new ListDataSet(columnNames, data);
    }

    protected Column getResultColumn(JSONObject jsonObject, String field, ElasticSearchFieldType type) {
        Column column;
        if (type == null) {
            throw new DataException(String.format("Type error: null type for column %s", field));
        }
        switch (type) {
            case ID:
            case PARENT:
            case ROUTING:
            case VERSION:
                column = new StringColumn(jsonObject.getString(field));
                break;
            case DATE:
                column = new DateColumn(jsonObject.getDate(field));
                break;
            case KEYWORD:
            case STRING:
            case TEXT:
            case IP:
                column = new StringColumn(jsonObject.getString(field));
                break;
            case BOOLEAN:
                column = new BoolColumn(jsonObject.getBoolean(field));
                break;
            case BYTE:
            case BINARY:
                column = new BytesColumn(jsonObject.getBytes(field));
                break;
            case LONG:
                column = new LongColumn(jsonObject.getLong(field));
                break;
            case INTEGER:
            case SHORT:
                column = new LongColumn(jsonObject.getInteger(field));
                break;
            case FLOAT:
            case DOUBLE:
                column = new DoubleColumn(jsonObject.getDouble(field));
                break;
            case GEO_SHAPE:
            case DATE_RANGE:
            case INTEGER_RANGE:
            case FLOAT_RANGE:
            case LONG_RANGE:
            case DOUBLE_RANGE:
            case NESTED:
            case OBJECT:
            case GEO_POINT:
            case IP_RANGE:
                column = new JsonColumn(jsonObject.getJSONObject(field));
                break;
            default:
                throw new DataException(String.format(
                        "Type error: unsupported type %s for column %s", type, field));
        }
        return column;
    }

    /**
     * 子类自定义实现
     *
     * @param
     * @return org.elasticsearch.search.SearchHits
     */
    protected abstract SearchHits doQuery() throws IOException;
}