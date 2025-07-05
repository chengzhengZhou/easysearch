/**
 * Copyright (C), 2010-2024, 爱回收
 * FileName: ColumnFactoryTest
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2024/6/27 16:07
 * Description: ColumnFactory测试类
 */
package com.ppwx.easysearch.core.data.element;

import com.ppwx.easysearch.core.data.Column;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * ColumnFactory测试类
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2024/06/27 16:07
 * @since 1.0.0
 */
public class ColumnFactoryTest {

    @Test
    public void testEmptyColumnWorks() {
        Column column = ColumnFactory.emptyColumn(StringColumn.class);
        Column column2 = ColumnFactory.emptyColumn(StringColumn.class);
        Assert.assertEquals(column, column2);

        column = ColumnFactory.emptyColumn(DateColumn.class);
        column2 = ColumnFactory.emptyColumn(DateColumn.class);
        Assert.assertEquals(column, column2);
    }

    @Test
    public void testCreateWorks() {
        StringColumn column = ColumnFactory.createString(null);
        StringColumn column2 = ColumnFactory.createString(null);
        StringColumn column3 = ColumnFactory.createString("123");
        Assert.assertEquals(column, column2);
        Assert.assertNotEquals(column2, column3);
    }

}