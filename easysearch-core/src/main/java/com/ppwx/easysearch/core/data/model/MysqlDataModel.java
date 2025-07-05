/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: MysqlDataModel
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/12/11 16:48
 * Description: Mysql作为数据源加载的数据集
 */
package com.ppwx.easysearch.core.data.model;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.ppwx.easysearch.core.common.DataException;
import com.ppwx.easysearch.core.data.Column;
import com.ppwx.easysearch.core.data.DataSet;
import com.ppwx.easysearch.core.data.TableDataSet;
import com.ppwx.easysearch.core.data.element.*;
import io.netty.util.internal.ObjectUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.ppwx.easysearch.core.data.model.Key.GLOBAL_ID;
import static com.ppwx.easysearch.core.data.model.Key.GLOBAL_SCORE;

/**
 *
 * Mysql作为数据源模型
 *
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/11 16:48
 * @since 1.0.0
 */
public class MysqlDataModel extends AbstractDataModel {

    protected static Logger logger = LoggerFactory.getLogger(MysqlDataModel.class);

    private JdbcTemplate jdbcTemplate;

    private String querySql;

    protected final byte[] EMPTY_CHAR_ARRAY = new byte[0];

    public MysqlDataModel(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String getQuerySql() {
        return querySql;
    }

    public void setQuerySql(String querySql) {
        this.querySql = querySql;
    }

    @Override
    public void loadDataModel() {
        ObjectUtil.checkNotNull(querySql, "querySql");
        if (!querySql.contains(GLOBAL_ID) || !querySql.contains(GLOBAL_SCORE)) {
            logger.warn("查询Sql中未包含{}或{}，若不想看到该告警，请调整Sql！", GLOBAL_ID, GLOBAL_SCORE);
        }
    }

    @Override
    public DataSet getDataset() {
        if (this.dataSet == null) {
            Table<Integer, Integer, Column> table = HashBasedTable.create();
            List<String> columns = new ArrayList<>();
            jdbcTemplate.query(querySql, (ResultSetExtractor<List<Void>>) resultSet -> {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();
                for (int i = 0; i < columnCount; i++) {
                    columns.add(metaData.getColumnLabel(i + 1));
                }

                int row = 0;
                while(resultSet.next()) {
                    for(int i = 1; i <= columnCount; ++i) {
                        Column column = getResultSetColumn(resultSet, metaData, i, "");
                        table.put(row, i - 1, column);
                    }
                    row++;
                }
                return Collections.emptyList();
            });

            this.dataSet = new TableDataSet(columns, table);
        }
        return this.dataSet;
    }

    protected Column getResultSetColumn(ResultSet rs, ResultSetMetaData metaData, int columnNumber, String mandatoryEncoding) {
        Column column = null;
        try {
            for (int i = 1; i <= columnNumber; i++) {
                switch (metaData.getColumnType(i)) {

                    case Types.CHAR:
                    case Types.NCHAR:
                    case Types.VARCHAR:
                    case Types.LONGVARCHAR:
                    case Types.NVARCHAR:
                    case Types.LONGNVARCHAR:
                        String rawData;
                        if(StringUtils.isBlank(mandatoryEncoding)){
                            rawData = rs.getString(i);
                        }else{
                            rawData = new String((rs.getBytes(i) == null ? EMPTY_CHAR_ARRAY :
                                    rs.getBytes(i)), mandatoryEncoding);
                        }
                        column = new StringColumn(rawData);
                        break;

                    case Types.CLOB:
                    case Types.NCLOB:
                        column = new StringColumn(rs.getString(i));
                        break;

                    case Types.SMALLINT:
                    case Types.TINYINT:
                    case Types.INTEGER:
                    case Types.BIGINT:
                        column = new LongColumn(rs.getString(i));
                        break;

                    case Types.NUMERIC:
                    case Types.DECIMAL:
                    case Types.FLOAT:
                    case Types.REAL:
                    case Types.DOUBLE:
                        column = new DoubleColumn(rs.getString(i));
                        break;

                    case Types.TIME:
                        column = new DateColumn(rs.getTime(i));
                        break;

                    // for mysql bug, see http://bugs.mysql.com/bug.php?id=35115
                    case Types.DATE:
                        if (metaData.getColumnTypeName(i).equalsIgnoreCase("year")) {
                            column = new LongColumn(rs.getInt(i));
                        } else {
                            column = new DateColumn(rs.getDate(i));
                        }
                        break;

                    case Types.TIMESTAMP:
                        column = new DateColumn(rs.getTimestamp(i));
                        break;

                    case Types.BINARY:
                    case Types.VARBINARY:
                    case Types.BLOB:
                    case Types.LONGVARBINARY:
                        column = new BytesColumn(rs.getBytes(i));
                        break;

                    // warn: bit(1) -> Types.BIT 可使用BoolColumn
                    // warn: bit(>1) -> Types.VARBINARY 可使用BytesColumn
                    case Types.BOOLEAN:
                    case Types.BIT:
                        column = new BoolColumn(rs.getBoolean(i));
                        break;

                    case Types.NULL:
                        String stringData = null;
                        if(rs.getObject(i) != null) {
                            stringData = rs.getObject(i).toString();
                        }
                        column = new StringColumn(stringData);
                        break;

                    default:
                        throw new DataException(String.format(
                                "您的配置文件中的列配置信息有误. 不支持数据库读取这种字段类型. 字段名:[%s], 字段名称:[%s], 字段Java类型:[%s]. 请尝试使用数据库函数将其转换datax支持的类型 或者不同步该字段 .",
                                metaData.getColumnName(i),
                                metaData.getColumnType(i),
                                metaData.getColumnClassName(i)));
                }
            }
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("read data occur exception:", e);
            }
            if (e instanceof DataException) {
                throw (DataException) e;
            }
        }
        return column;
    }
}