package com.ppwx.easysearch.core.metrics;

import org.junit.Test;

import java.time.Clock;
import java.util.concurrent.TimeUnit;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className SlidingTimeWindowMetricsTest
 * @description todo
 * @date 2024/11/28 15:32
 **/
public class SlidingWindowMetricsTest {

    @Test
    public void testSlidingTimeWindowMetricsWorks() throws InterruptedException {
        SlidingTimeWindowMetrics metrics = new SlidingTimeWindowMetrics(3, Clock.systemUTC());
        for (int i = 0; i < 10; i++) {
            metrics.record(1, TimeUnit.MINUTES, Metrics.Outcome.SUCCESS);
        }
        Snapshot snapshot = metrics.getSnapshot();
        System.out.println(System.currentTimeMillis() + " : " + snapshot.getNumberOfSuccessfulCalls());

        Thread.sleep(1000);

        for (int i = 0; i < 10; i++) {
            metrics.record(1, TimeUnit.MINUTES, Metrics.Outcome.SUCCESS);
        }
        snapshot = metrics.getSnapshot();
        System.out.println(System.currentTimeMillis() + " : " + snapshot.getNumberOfSuccessfulCalls());

        Thread.sleep(1000);
        snapshot = metrics.getSnapshot();
        System.out.println(System.currentTimeMillis() + " : " + snapshot.getNumberOfSuccessfulCalls());

        Thread.sleep(1000);
        snapshot = metrics.getSnapshot();
        System.out.println(System.currentTimeMillis() + " : " + snapshot.getNumberOfSuccessfulCalls());
    }

    @Test
    public void testFixedSizeSlidingWindowMetricsWorks() {
        FixedSizeSlidingWindowMetrics metrics = new FixedSizeSlidingWindowMetrics(3);
        for (int i = 0; i < 8; i++) {
            metrics.record(1, TimeUnit.MINUTES, Metrics.Outcome.SUCCESS);
        }
        Snapshot snapshot = metrics.getSnapshot();
        System.out.println(snapshot.getNumberOfSuccessfulCalls());
    }
}
