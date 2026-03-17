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