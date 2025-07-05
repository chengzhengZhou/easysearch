/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: CurrentTimeUtil
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/12/26 15:48
 * Description: 当前时间毫秒获取工具
 */
package com.ppwx.easysearch.core.util;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 *
 * 当前时间毫秒获取工具
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/26 15:48
 * @since 1.0.0
 */
public final class CurrentTimeUtil {

    private static volatile long CURRENT_TIME = System.currentTimeMillis();

    static {
        Executors.newSingleThreadScheduledExecutor().
                scheduleAtFixedRate(CurrentTimeUtil::update, 0L, 1, TimeUnit.SECONDS);
    }

    public static long currentTimeMillis() {
        return CURRENT_TIME;
    }

    public static long currentTimeSeconds() {
        return CURRENT_TIME / 1000;
    }

    public static long currentTImeNanos() {
        return System.nanoTime();
    }

    public static void update() {
        CURRENT_TIME = System.currentTimeMillis();
    }


}