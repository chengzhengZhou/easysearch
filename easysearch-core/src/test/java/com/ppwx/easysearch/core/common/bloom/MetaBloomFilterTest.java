/**
 * Copyright (C), 2010-2024, 爱回收
 * FileName: MetaBloomFilterTest
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2024/1/26 18:08
 * Description: MetaBloomFilter测试类
 */
package com.ppwx.easysearch.core.common.bloom;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.util.BitSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 *
 * MetaBloomFilter测试类
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2024/01/26 18:08
 * @since 1.0.0
 */
public class MetaBloomFilterTest {

    @Test
    public void testBloomFilterWorksV2() {
        MetaBloomFilter filter = new MetaBloomFilter(200, 0.01);
        System.out.println(filter.getFalseProbability());
        System.out.println(filter.getHashIterations());
        System.out.println(filter.getSize());
        int count = 0;
        Stopwatch stopWatch = Stopwatch.createStarted();
        for (int i = 0; i < 10000; i++) {
            String s = String.valueOf(i);
            if (filter.contains(s)) {
                ++count;
            } else {
                filter.add(s);
            }
        }
        stopWatch.stop();
        System.out.println("repeat:" + count + " elp:" + stopWatch.elapsed(TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSerializeWorks() {
        MetaBloomFilter filter = new MetaBloomFilter(200, 0.01);
        filter.add("sophiszhou");
        System.out.println("size:" + filter.getSize());
        System.out.println("funcs:" + filter.getHashIterations());
        byte[] bytes = filter.toByteArray();
        System.out.println(bytes.length);

        MetaBloomFilter metaBloomFilterV2 = MetaBloomFilter.valueOf(bytes);
        boolean exists = metaBloomFilterV2.contains("sophiszhou");
        System.out.println("exists:" + exists);
    }

    @Test
    public void testBloomFilterChunkWorks() {
        MetaBloomFilter metaBloomFilter = new MetaBloomFilter(200, 0.01);
        MetaBloomFilter metaBloomFilter2 = new MetaBloomFilter(200, 0.01);
        BloomFilterChunk chunk = new BloomFilterChunk(10, 1, TimeUnit.MINUTES,
                Lists.newArrayList(metaBloomFilter, metaBloomFilter2));
        System.out.println("expectedInsertions:" + chunk.getExpectedInsertions());
        System.out.println("probablity:" + chunk.getFalseProbability());
        System.out.println("funcs:" + chunk.getHashIterations());
        System.out.println("size:" + chunk.getSize());
        Assert.assertTrue(metaBloomFilter.add("nihao"));
        Assert.assertFalse(metaBloomFilter.add("nihao"));
        Assert.assertTrue(metaBloomFilter.add("nihaO"));
    }

    @Test
    public void testOverSizeChunkWorks() {
        MetaBloomFilter metaBloomFilter = new MetaBloomFilter(20, 0.01);
        BloomFilterChunk chunk = new BloomFilterChunk(1, 1, TimeUnit.MINUTES,
                Lists.newArrayList(metaBloomFilter));
        for (int i = 0; i < 99; i++) {
            chunk.add(String.valueOf(i));
        }
        System.out.println(chunk.getBloomFilters());
        System.out.println("------------------------------------");
        System.out.println(chunk.getRemovedFilters());
    }

    @Test
    public void testOverTimeChunkWorks() throws InterruptedException {
        MetaBloomFilter metaBloomFilter = new MetaBloomFilter(20, 0.01);
        for (int i = 0; i < 2; i++) {
            metaBloomFilter.add(String.valueOf(i));
        }
        Thread.sleep(10000);

        BloomFilterChunk chunk = new BloomFilterChunk(2, 3, TimeUnit.SECONDS,
                Lists.newArrayList(metaBloomFilter));
        chunk.add("end");

        System.out.println(chunk.getBloomFilters());
        System.out.println("------------------------------------");
        System.out.println(chunk.getRemovedFilters());
    }

    @Test
    public void testSingleWorks() {
        int defaultChunkSize = 10;
        int defaultExpireTime = 1;
        MetaBloomFilter metaBloomFilter = new MetaBloomFilter(100, 0.01);
        BloomFilterChunk chunk = new BloomFilterChunk(defaultChunkSize, defaultExpireTime, TimeUnit.HOURS,
                Lists.newArrayList(metaBloomFilter));
        System.out.println("expectedInsertions:" + chunk.getExpectedInsertions());
        System.out.println("probablity:" + chunk.getFalseProbability());
        System.out.println("funcs:" + chunk.getHashIterations());
        System.out.println("size:" + chunk.getSize());
        Assert.assertFalse(chunk.contains("43172001"));
        Assert.assertTrue(chunk.add("43172001"));
        Assert.assertTrue(chunk.contains("43172001"));

        // desc
        List<byte[]> bloomBytes = Lists.newArrayList();
        for (int i = 0; i < chunk.getBloomFilters().size(); i++) {
            bloomBytes.add(chunk.getBloomFilters().get(i).toByteArray());
        }
        List<MetaBloomFilter> bloomFilters = bloomBytes.stream().map(MetaBloomFilter::valueOf).collect(Collectors.toList());
        chunk = new BloomFilterChunk(defaultChunkSize, defaultExpireTime, TimeUnit.HOURS, bloomFilters);
        System.out.println(chunk.count());
        Assert.assertTrue(chunk.contains("43172001"));
    }
}