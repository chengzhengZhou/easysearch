/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: TimelinessFuncTest
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/12/26 17:28
 * Description: TimelinessFunc测试类
 */
package com.ppwx.easysearch.core.function.timeliness;

import com.ppwx.easysearch.core.TestLogger;
import com.ppwx.easysearch.core.data.Column;
import com.ppwx.easysearch.core.data.element.DateColumn;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Test;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 *
 * TimelinessFunc测试类
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/26 17:28
 * @since 1.0.0
 */
public class TimelinessFuncTest {

    private List<Column> columns;

    @Before
    public void init() throws ParseException {
        List<Column> columns = new ArrayList<>();

        this.columns = columns;
    }

    @Test
    public void testTimelinessFuncWorks() throws ParseException {
        TimelinessFunc func = new TimelinessFunc(columns);
        Date d0 = DateUtils.parseDate("2020-00-00 00:00:00", "yyyy-MM-dd HH:mm:ss");
        Date d1 = DateUtils.parseDate("2022-12-25 00:00:00", "yyyy-MM-dd HH:mm:ss");
        Date d2 = DateUtils.parseDate("2023-05-25 11:00:00", "yyyy-MM-dd HH:mm:ss");
        Date d3 = DateUtils.parseDate("2023-12-26 00:00:00", "yyyy-MM-dd HH:mm:ss");
        Date d4 = DateUtils.parseDate("2023-12-26 11:00:00", "yyyy-MM-dd HH:mm:ss");
        columns.add(new DateColumn(TimeUnit.MILLISECONDS.toSeconds(d0.getTime())));

        Double value = func.score(TimeUnit.MILLISECONDS.toSeconds(d1.getTime()));
        TestLogger.info("分值：{}", value);
        value = func.score(TimeUnit.MILLISECONDS.toSeconds(d2.getTime()));
        TestLogger.info("分值：{}", value);
        value = func.score(TimeUnit.MILLISECONDS.toSeconds(d3.getTime()));
        TestLogger.info("分值：{}", value);
        value = func.score(TimeUnit.MILLISECONDS.toSeconds(d4.getTime()));
        TestLogger.info("分值：{}", value);
    }

    @Test
    public void testTimelinessMsFuncWorks() throws ParseException {
        TimelinessMsFunc func = new TimelinessMsFunc(columns);
        Date d1 = DateUtils.parseDate("2022-12-25 00:00:00", "yyyy-MM-dd HH:mm:ss");
        Date d2 = DateUtils.parseDate("2023-05-25 11:00:00", "yyyy-MM-dd HH:mm:ss");
        Date d3 = DateUtils.parseDate("2023-12-26 00:00:00", "yyyy-MM-dd HH:mm:ss");
        Date d4 = DateUtils.parseDate("2023-12-26 11:00:00", "yyyy-MM-dd HH:mm:ss");
        columns.add(new DateColumn(d1.getTime()));

        Double value = func.score(d1.getTime());
        TestLogger.info("分值：{}", value);
        value = func.score(d2.getTime());
        TestLogger.info("分值：{}", value);
        value = func.score(d3.getTime());
        TestLogger.info("分值：{}", value);
        value = func.score(d4.getTime());
        TestLogger.info("分值：{}", value);
    }
}