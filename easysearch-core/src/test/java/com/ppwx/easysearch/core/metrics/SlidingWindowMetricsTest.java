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

package com.ppwx.easysearch.core.metrics;

import org.junit.Test;

import java.time.Clock;
import java.util.concurrent.TimeUnit;

/**
 * @className SlidingTimeWindowMetricsTest
 * @description todo
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
