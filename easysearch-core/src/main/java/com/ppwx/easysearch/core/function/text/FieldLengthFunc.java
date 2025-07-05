/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: FieldLengthFunc
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/12/27 10:21
 * Description: 获取字段上的分词词组个数
 */
package com.ppwx.easysearch.core.function.text;

import com.ppwx.easysearch.core.data.Column;
import com.ppwx.easysearch.core.function.Function;
import com.ppwx.easysearch.core.util.IKAnalyzerUtil;

/**
 *
 * 获取字段上的分词词组个数
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/27 10:21
 * @since 1.0.0
 */
public class FieldLengthFunc implements Function<Integer> {

    @Override
    public Integer apply(Column column) {
        if (column == null) {
            return 0;
        }
        return IKAnalyzerUtil.extractWords(column.asString(), 0).size();
    }
}