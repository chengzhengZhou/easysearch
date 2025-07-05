package com.ppwx.easysearch.cf.model;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import com.ppwx.easysearch.cf.data.CFSimilarity;
import com.ppwx.easysearch.cf.data.ScoreSumMatrix;
import com.ppwx.easysearch.cf.data.UserRating;
import com.ppwx.easysearch.cf.rank.ItemCFRankService;
import com.ppwx.easysearch.cf.rating.MAEEvaluator;
import com.ppwx.easysearch.cf.rating.RMSEEvaluator;
import com.ppwx.easysearch.cf.repository.CleanStrategy;
import com.ppwx.easysearch.cf.repository.JdbcModelDataRepository;
import com.ppwx.easysearch.cf.repository.ModelDataRepository;
import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.SymmMatrix;
import net.librec.recommender.item.ItemEntry;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SingleColumnRowMapper;

import java.text.ParseException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className ItemCFLocalTest
 * @description 线上9-10月数据分析测试
 * @date 2024/10/19 12:59
 **/
public class ItemCFLocalTest {

    private ModelDataRepository repository;

    private Map<String, ItemMapping> allItems;

    private JdbcTemplate jdbcTemplate;

    public void initJdbcRepository() {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setURL("jdbc:mysql://127.0.0.1:3306/rec?useSSL=false&useUnicode=true&characterEncoding=utf8&serverTimezone=Hongkong");
        dataSource.setUser("root");
        dataSource.setPassword("root");
        dataSource.setAutoClosePStmtStreams(true);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource, false);
        jdbcTemplate.afterPropertiesSet();
        this.jdbcTemplate = jdbcTemplate;

        JdbcModelDataRepository dataRepository = new JdbcModelDataRepository("rec_", jdbcTemplate);
        dataRepository.setScoreShards(2);
        this.repository = dataRepository;
    }

    public void loadProducts() {
        BeanPropertyRowMapper<ItemMapping> mapper = new BeanPropertyRowMapper<>();
        mapper.setMappedClass(ItemMapping.class);
        List<ItemMapping> result = jdbcTemplate.query("select * from `rec_item_mapping`", mapper);
        this.allItems = result.stream().collect(Collectors.toMap(v -> v.getId().toString(), Function.identity()));
    }

    @Before
    public void init() {
        initJdbcRepository();
        loadProducts();
    }

    @Test
    public void testJdbcProductPredict() {
        ItemCFRankService rankService = new ItemCFRankService(repository);
        String user = "2211101908309291";
        Collection<UserRating> userRating = repository.getUserRating(user);
        System.out.println("用户偏好：");
        userRating.stream().sorted(Comparator.comparing(UserRating::getDatetime).reversed())
                .forEach(ac -> System.out.printf("%s : %s : %s = %s > %s%n",
                allItems.get(ac.getItemId()).getProductName(),
                allItems.get(ac.getItemId()).getSkuPpvName(),
                allItems.get(ac.getItemId()).getQualityId(),
                ac.getRate(), DateFormatUtils.format(ac.getDatetime(), "yyyy-MM-dd HH:mm:ss")));

        Collection<ItemEntry<String, Double>> list = rankService.rank(user, 0, 20);
        for (ItemEntry<String, Double> entry : list) {
            ItemMapping itemMapping = allItems.get(entry.getKey());
            System.out.printf("%s : %s : %s = %s%n", itemMapping.getProductName(),
                    itemMapping.getQualityId(), itemMapping.getSkuPpvName(), entry.getValue());
        }
    }

    @Test
    public void testEvaluate() {
        // 核心数据
        BiMap<String, Integer> items = HashBiMap.create();
        SymmMatrix matrix = new ScoreSumMatrix();
        CFSimilarity sameItemCFScore = repository.getSameItemCFScore();
        sameItemCFScore.copyTo(items, matrix);
        ItemCFModel model = ItemCFModel.initFrom(items, matrix);
        model.setRank(false);
        // 预估
        MAEEvaluator maeEvaluator = new MAEEvaluator();
        RMSEEvaluator rmseEvaluator = new RMSEEvaluator();

        //List<String> users = Lists.newArrayList("2311261012862994", "2302261238195918", "2004271841652575", "2409111940846869", "2407062058255869", "2012080838937657");
        List<String> users = getUser();
        for (String user : users) {
            double ratio = 0.4;
            // 利用用户偏好的商品预估
            Collection<UserRating> userRatings = repository.getUserRating(user);
            if (Double.compare(userRatings.size() * ratio, 0.0d) <= 0) {
                continue;
            }
            Set<String> userPrefers = userRatings.stream().map(UserRating::getItemId).collect(Collectors.toSet());
            CFSimilarity cfScore = repository.getCFScore(userPrefers);
            cfScore.copyTo(items, matrix, true);
            // 切分数据集
            Collection<UserRating> trainData = Lists.newArrayList();
            Collection<UserRating> testData = Lists.newArrayList();
            for(UserRating ua: userRatings){
                double rdm = Randoms.uniform();
                if (rdm < ratio) {
                    testData.add(ua);
                } else {
                    trainData.add(ua);
                }
            }
            if (trainData.isEmpty()) {
                continue;
            }

            for (UserRating ua : testData) {
                double predict = model.predict(trainData, ua.getItemId());
                maeEvaluator.add(predict, ua.getRate());
                rmseEvaluator.add(predict, ua.getRate());
                System.out.println(String.format("predict:%s, act:%s", predict, ua.getRate()));
            }
        }
        System.out.println(String.format("MAE size: %s, eval: %s", maeEvaluator.getPredictSize(), maeEvaluator.evaluate()));
        System.out.println(String.format("RMSE size: %s, eval: %s", rmseEvaluator.getPredictSize(), rmseEvaluator.evaluate()));
    }

    public List<String> getUser() {
        return jdbcTemplate.query("select user_code from rec_user_action order by id desc limit 100", new SingleColumnRowMapper<>(String.class));
    }

    @Test
    public void testCleanSimilarity() throws ParseException {
        Date begin = DateUtils.parseDate("2024-09-10 00:00:00", "yyyy-MM-dd HH:mm:ss");
        Date end = DateUtils.parseDate("2024-09-12 00:00:00", "yyyy-MM-dd HH:mm:ss");
        CleanStrategy.TimeStrategy strategy = new CleanStrategy.TimeStrategy(100, begin, end);
        repository.cleanSimilarity(strategy);
    }

    @Test
    public void testTruncateStrategy() {
        CleanStrategy.TruncateStrategy strategy = new CleanStrategy.TruncateStrategy();
        repository.cleanSimilarity(strategy);
    }

    @Test
    public void testItemsStrategy() {
        CleanStrategy.ItemsStrategy strategy = new CleanStrategy.ItemsStrategy(new String[]{"108", "105", "103"});
        repository.cleanSimilarity(strategy);
    }

    @Test
    public void testSy() {
        SymmMatrix symmMatrix = new ScoreSumMatrix();
        System.out.println(symmMatrix.get(100, 100));;
    }
}