/**
 * Copyright (C), 2010-2024, 爱回收
 * FileName: ScoreSumMatrix
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2024/7/31 13:46
 * Description: 相识矩阵
 */
package com.ppwx.easysearch.cf.data;

import net.librec.math.structure.SparseVector;
import net.librec.math.structure.SymmMatrix;

/**
 *
 * 相似矩阵
 * 实现dim参数的自动获取，在cf相似分中为分子部分积分
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2024/07/31 13:46
 * @since 1.0.0
 */
public class ScoreSumMatrix extends SymmMatrix {

    public ScoreSumMatrix() {
        super(0);
    }

    public ScoreSumMatrix(SymmMatrix mat) {
        super(mat);
    }

    @Override
    public int getDim() {
        return getData().rowMap().size();
    }

    @Override
    public SparseVector row(int row) {
        dim = getDim();
        return super.row(row);
    }
}