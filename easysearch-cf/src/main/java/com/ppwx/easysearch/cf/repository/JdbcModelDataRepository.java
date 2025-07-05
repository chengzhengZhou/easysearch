/**
 * Copyright (C), 2010-2024, 爱回收
 * FileName: JdbcModelDataRepository
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2024/8/2 17:51
 * Description: 数据库存储模型数据
 */
package com.ppwx.easysearch.cf.repository;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.ppwx.easysearch.cf.data.CFSimilarity;
import com.ppwx.easysearch.cf.data.UserRating;
import com.ppwx.easysearch.cf.utils.TableShardUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StopWatch;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.ppwx.easysearch.cf.utils.TableShardUtils.splitRange;

/**
 *
 * 数据库存储模型数据
 * 用户数据和评分矩阵往往都较大，一般为百万到亿级别，需要考虑存储和加载的效率
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2024/08/02 17:51
 * @since 1.0.0
 */
public class JdbcModelDataRepository extends AbstractPersistentDataRepository {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcModelDataRepository.class);

    /**
     * 表前缀
     */
    private String tablePre;
    /**
     * 使用spring数据库模板
     */
    private JdbcTemplate jdbcTemplate;
    /**
     * 用户数据分片大小
     */
    private int userShards = 1;
    /**
     * 评分数据分片
     */
    private int scoreShards = 1;

    public JdbcModelDataRepository(String tablePre, JdbcTemplate jdbcTemplate) {
        this.tablePre = tablePre;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setUserShards(int userShards) {
        this.userShards = userShards;
    }

    public void setScoreShards(int scoreShards) {
        this.scoreShards = scoreShards;
    }

    @Override
    public Collection<UserRating> getUserRating(String userId) {
        String sql = "SELECT `actions` FROM %s WHERE user_code = ?";
        List<String> result = jdbcTemplate.query(String.format(sql, getUserActionTable(userId)),
                new Object[]{userId}, new SingleColumnRowMapper<>());
        if (CollectionUtils.isEmpty(result)) {
            return Collections.emptyList();
        }
        return decompress(result.iterator().next());
    }

    @Override
    public CFSimilarity getCFScoreV2() {
        return getCF(null);
    }

    @Override
    public CFSimilarity getCFScoreV2(Date beginTime, Date endTime) {
        Assert.isTrue(beginTime != null && endTime != null);
        StringBuilder whereSql = new StringBuilder("(")
                .append("'").append(DateFormatUtils.format(beginTime, "yyyy-MM-dd HH:mm:ss")).append("' <= `update_dt`").append(" AND ")
                .append("`update_dt` < '").append(DateFormatUtils.format(endTime, "yyyy-MM-dd HH:mm:ss")).append("'")
                .append(")");
        return getCF(whereSql.toString());
    }

    @Override
    public CFSimilarity getCFScore(Collection<String> items) {
        String whereSql = "item_id_h in(%s)";
        String whereSql1 = "item_id_l in(%s)";
        String val = items.stream().map(this::surround).collect(Collectors.joining(","));
        CFSimilarity cf = getCF(String.format(whereSql, val));
        CFSimilarity cf1 = getCF(String.format(whereSql1, val));
        Map<String, Double> scores = cf.getScores();
        Map<String, Double> scores1 = cf1.getScores();
        Map<String, Double> all = Maps.newHashMapWithExpectedSize(scores.size() + scores1.size());
        all.putAll(scores);
        all.putAll(scores1);
        return new CFSimilarity(all);
    }

    @Override
    public CFSimilarity getSameItemCFScore() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        String querySql = "SELECT `item_id_h`,`item_id_l`,`similarity_score` FROM %s WHERE item_id_h=item_id_l LIMIT 100000";
        CFSimilarity cfSimilarity = new CFSimilarity();
        String sql;
        for (String table : getCFTables()) {
            sql = String.format(querySql, table);
            jdbcTemplate.query(sql, rs -> {
                cfSimilarity.setScore(rs.getString(1), rs.getString(2), rs.getDouble(3));
            });
        }
        stopWatch.stop();
        LOG.info("finish getSameItemCFScore elapse:{}", stopWatch.getTotalTimeSeconds());
        return cfSimilarity;
    }

    private String surround(String item) {
        return "'" + item + "'";
    }

    private CFSimilarity getCF(String where) {
        CFSimilarity cfSimilarity = new CFSimilarity();
        String countSql = "SELECT count(*) count,MIN(id) min,MAX(id) max FROM %s";
        String querySql = "SELECT `item_id_h`,`item_id_l`,`similarity_score` FROM %s";
        if (where != null) {
            countSql = new StringBuilder(countSql).append(" WHERE ").append(where).toString();
            querySql = new StringBuilder(querySql).append(" WHERE ").append(where).toString();
        }
        String sql;
        BeanPropertyRowMapper<InnerAgg> mapper = new BeanPropertyRowMapper<>();
        mapper.setMappedClass(InnerAgg.class);
        AtomicLong sum = new AtomicLong();
        long total = 0;
        StringBuilder appender;
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        for (String table : getCFTables()) {
            // handle per table
            sql = String.format(countSql, table);
            InnerAgg innerAgg = jdbcTemplate.queryForObject(sql, mapper);
            total += innerAgg.count;
            if (innerAgg.count <= 0) {
                continue;
            }
            List<String> conditions = TableShardUtils.wrapRange(splitRange(innerAgg.count, innerAgg.min, innerAgg.max, getFetchSize()),
                    "id", "");
            sql = String.format(querySql, table);

            for (String condition : conditions) {
                if (where == null) {
                    appender = new StringBuilder(sql).append(" WHERE ").append(condition);
                } else {
                    appender = new StringBuilder(sql).append(" AND ").append(condition);
                }
                //LOG.info("query sql:{}", appender.toString());
                jdbcTemplate.query(appender.toString(), rs -> {
                    sum.incrementAndGet();
                    cfSimilarity.setScore(rs.getString(1), rs.getString(2), rs.getDouble(3));
                });
                //LOG.info("process getCF total:{}, progress:{}", total, sum);
            }
        }
        stopWatch.stop();
        LOG.info("finish getCF total:{}, progress:{}, elapse:{}", total, sum.get(), stopWatch.getTotalTimeSeconds());
        return cfSimilarity;
    }

    @Override
    public void saveUserRating(String userId, List<UserRating> ratings) {
        String sql = "INSERT INTO %s(`user_code`, `actions`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `actions` = ?";
        String actions = compress(ratings);
        jdbcTemplate.update(String.format(sql, getUserActionTable(userId)), new Object[]{userId, actions, actions});
    }

    @Override
    public void saveUserRating(Multimap<String, UserRating> userMap) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        String sql = "INSERT INTO %s(`user_code`, `actions`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `actions` = ?";
        Multimap<String, String> tableMap = HashMultimap.create();
        Set<String> keys = userMap.keySet();
        for (String k : keys) {
            tableMap.put(getUserActionTable(k), k);
        }
        int total = keys.size();
        int sum = 0;
        String actions;
        List<Object[]> args = Lists.newLinkedList();
        int batch = 10000;
        int count = 0;
        String insertSql;
        for (String table : tableMap.keySet()) {
            insertSql = String.format(sql, table);
            for (String k : tableMap.get(table)) {
                if (count >= batch) {
                    jdbcTemplate.batchUpdate(insertSql, args);
                    sum += count;
                    count = 0;
                    args = Lists.newLinkedList();
                    LOG.info("process saveUserRating total:{}, progress:{}", total, sum);
                }
                actions = compress(userMap.get(k));
                args.add(new Object[]{k, actions, actions});
                count++;
            }
            // save rest
            if (!args.isEmpty()) {
                jdbcTemplate.batchUpdate(insertSql, args);
            }
            sum += count;
            count = 0;
            args = Lists.newLinkedList();
        }
        stopWatch.stop();
        LOG.info("finish saveUserRating total:{}, progress:{}, elapse:{}", total, sum, stopWatch.getTotalTimeSeconds());
    }

    @Override
    public void saveCFScore(CFSimilarity similarity) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        String sql = "INSERT INTO %s(`item_id_h`,`item_id_l`,`similarity_score`) VALUES(?,?,?) " +
                "ON DUPLICATE KEY UPDATE `similarity_score`=`similarity_score` + ?";
        Map<String, Double> scores = similarity.getScores();
        Multimap<String, String> tableMap = HashMultimap.create();
        for (String k : scores.keySet()) {
            tableMap.put(getCFTable(k), k);
        }
        int sum = 0;
        String[] ids;
        List<Object[]> args = Lists.newLinkedList();
        int batch = 10000;
        int count = 0;
        String insertSql;
        for (String table : tableMap.keySet()) {
            insertSql = String.format(sql, table);
            for (String k : tableMap.get(table)) {
                if (count >= batch) {
                    jdbcTemplate.batchUpdate(insertSql, args);
                    sum += count;
                    count = 0;
                    args = Lists.newLinkedList();
                    LOG.info("process saveCFScore total:{}, progress:{}", scores.size(), sum);
                }
                ids = CFSimilarity.getBiItemId(k);
                args.add(new Object[]{ids[0], ids[1], scores.get(k), scores.get(k)});
                count++;
            }
            // save rest
            if (!args.isEmpty()) {
                jdbcTemplate.batchUpdate(insertSql, args);
            }
            sum += count;
            count = 0;
            args = Lists.newLinkedList();
        }
        stopWatch.stop();
        LOG.info("finish saveCFScore total:{}, progress:{}, elapse:{}", scores.size(), sum, stopWatch.getTotalTimeSeconds());
    }

    @Override
    public void cleanSimilarity(CleanStrategy strategy) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        int count = 0;
        if (strategy instanceof CleanStrategy.TruncateStrategy) {
            for (String table : getCFTables()) {
                jdbcTemplate.execute("truncate table " + table);
            }
        } else if (strategy instanceof CleanStrategy.TimeStrategy) {
            CleanStrategy.TimeStrategy cleanStrategy = (CleanStrategy.TimeStrategy) strategy;
            StringBuilder sqlBuilder = new StringBuilder("SELECT DISTINCT(item_id_l) FROM ")
                    .append(getCFTable(String.valueOf(strategy.hashCode())))
                    .append(" WHERE update_dt BETWEEN ? AND ? ")
                    .append(" LIMIT ").append(cleanStrategy.getSize());
            String begin = DateFormatUtils.format(cleanStrategy.getBegin(), "yyyy-MM-dd HH:mm:ss");
            String end = DateFormatUtils.format(cleanStrategy.getEnd(), "yyyy-MM-dd HH:mm:ss");
            List<String> re = jdbcTemplate.query(sqlBuilder.toString(), new Object[]{begin, end}, new SingleColumnRowMapper<>(String.class));
            LOG.info("clean model items:{}", re);
            if (!CollectionUtils.isEmpty(re)) {
                List<String> cfTables = getCFTables();
                for (String id : re) {
                    for (String table : cfTables) {
                        count += jdbcTemplate.update(String.format("DELETE FROM %s WHERE item_id_h = ?", table), id);
                        count += jdbcTemplate.update(String.format("DELETE FROM %s WHERE item_id_l = ?", table), id);
                    }
                }
            }
        } else if (strategy instanceof CleanStrategy.ItemsStrategy) {
            CleanStrategy.ItemsStrategy itemsStrategy = (CleanStrategy.ItemsStrategy) strategy;
            String[] items = itemsStrategy.getItems();
            if (items != null && items.length > 0) {
                List<String> cfTables = getCFTables();
                for (String table : cfTables) {
                    String args = StringUtils.repeat("?", ",", items.length);
                    count += jdbcTemplate.update(String.format("DELETE FROM %s WHERE item_id_h in (%s)", table, args), items);
                    count += jdbcTemplate.update(String.format("DELETE FROM %s WHERE item_id_l in (%s)", table, args), items);
                }
            }
        } else {
            // not support
            throw new RuntimeException("暂不支持的策略");
        }
        stopWatch.stop();
        LOG.info("finish cleanSimilarity count:{}, elapse:{}", count, stopWatch.getTotalTimeSeconds());
    }

    private String getCFTable(String key) {
        if (scoreShards > 1) {
            return tablePre + CF + "_" + TableShardUtils.getShardNum(key, scoreShards);
        } else {
            return tablePre + CF;
        }
    }

    private String getUserActionTable(String key) {
        if (userShards > 1) {
            return tablePre + UA + "_" + TableShardUtils.getShardNum(key, userShards);
        } else {
            return tablePre + UA;
        }
    }

    private List<String> getCFTables() {
        if (scoreShards > 1) {
            return IntStream.range(0, scoreShards).mapToObj(i -> tablePre + CF + "_" + i).collect(Collectors.toList());
        } else {
            return Lists.newArrayList(tablePre + CF);
        }
    }

    public static class InnerAgg {
        private long count;
        private BigInteger min;
        private BigInteger max;

        public void setCount(long count) {
            this.count = count;
        }

        public void setMin(BigInteger min) {
            this.min = min;
        }

        public void setMax(BigInteger max) {
            this.max = max;
        }
    }
}