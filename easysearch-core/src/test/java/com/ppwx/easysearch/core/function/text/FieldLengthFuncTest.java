/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: FieldLengthFuncTest
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/12/27 11:07
 * Description:
 */
package com.ppwx.easysearch.core.function.text;

import com.ppwx.easysearch.core.data.element.StringColumn;
import org.junit.Test;

/**
 *
 * FieldLengthFunc
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/27 11:07
 * @since 1.0.0
 */
public class FieldLengthFuncTest {

    @Test
    public void testFieldLengthFuncWorks() {
        FieldLengthFunc fieldLengthFunc = new FieldLengthFunc();
        Integer score = fieldLengthFunc.apply(new StringColumn("我爱我的中国"));
        System.out.println(score);
    }

}