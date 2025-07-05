/**
 * Copyright (C), 2010-2024, 爱回收
 * FileName: CFDataModel
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2024/8/1 11:15
 * Description:
 */
package com.ppwx.easysearch.cf.data;

import net.librec.data.DataModel;
import net.librec.math.structure.SparseMatrix;

/**
 * 拓展{{@link net.librec.data.DataModel}}
 *
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2024/08/01 11:15
 * @since 1.0.0
 */
public interface CFDataModel extends DataModel {

    /**
     * 获取全量偏好矩阵
     *
     * @param
     * @return net.librec.math.structure.SparseMatrix
     */
    SparseMatrix getPreferenceMatrix();
    /**
     * 偏好时间矩阵
     *
     * @param
     * @return net.librec.math.structure.SparseMatrix
     */
    SparseMatrix getDatetimeMatrix();

}