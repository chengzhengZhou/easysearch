package com.ppwx.easysearch.qp;

import com.ppwx.easysearch.qp.data.ArffSourceConvertor;
import org.junit.Test;
import org.springframework.util.StopWatch;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className ProductMappingTest
 * @description 映射测试
 * @date 2024/10/28 10:22
 **/
public class ProductMappingTest {

    @Test
    public void testQueryProcessWorks() throws IOException {
        QueryProcess queryProcess = new QueryProcess();
        queryProcess.init();
        queryProcess.setConfidence(0.8);
        // load data
        AtomicInteger count = new AtomicInteger();
        AtomicInteger badCount = new AtomicInteger();
        ArffSourceConvertor convertor = new ArffSourceConvertor("meta/product_meta.arff");
        convertor.readData();
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        convertor.getInstances().forEach(arffInstance -> {
            String name = (String) arffInstance.getValueByAttrName("product_name");
            List<QueryProcess.QPMeta> metas = queryProcess.queryProcessParalled(name);
            metas = metas.stream().sorted(Comparator.comparing(QueryProcess.QPMeta::getScore).reversed()).collect(Collectors.toList());
            if (metas.isEmpty() || metas.get(0).getScore().compareTo(0.92) < 0) {
                if (metas.isEmpty()) {
                    System.out.printf("竟然没匹配上. query:%s", name);
                } else {
                    System.out.printf("竟然没匹配上. query:%s, matched:%s, score:%s", name,
                            metas.get(0).getProductName(), metas.get(0).getScore());
                }
                System.out.println();
                badCount.incrementAndGet();
            } else {
                count.incrementAndGet();
            }
        });
        stopWatch.stop();
        System.out.println("耗时：" + stopWatch.getTotalTimeMillis());
        System.out.println("未匹配上的数量：" + badCount.get());
        System.out.println("匹配上的数量：" + count.get());
    }

}
