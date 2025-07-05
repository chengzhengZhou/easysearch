/**
 * Copyright (C), 2010-2024, 爱回收
 * FileName: CFTextDataModel
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2024/8/1 11:06
 * Description: 从文本文件中加载用户行为数据
 */
package com.ppwx.easysearch.cf.data;

import net.librec.conf.Configuration;
import net.librec.data.model.TextDataModel;
import net.librec.math.structure.SparseMatrix;

/**
 *
 * 从文本文件中加载用户行为数据
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2024/08/01 11:06
 * @since 1.0.0
 */
public class CFTextDataModel extends TextDataModel implements CFDataModel {

    public CFTextDataModel() {
    }

    public CFTextDataModel(Configuration conf) {
        super(conf);
    }

    @Override
    public SparseMatrix getPreferenceMatrix() {
        return this.dataConvertor.getPreferenceMatrix();
    }

    @Override
    public SparseMatrix getDatetimeMatrix() {
        return this.dataConvertor.getDatetimeMatrix();
    }
}