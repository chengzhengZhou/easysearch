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

package com.ppwx.easysearch.cf.data;

import net.librec.math.structure.SparseVector;
import net.librec.math.structure.SymmMatrix;

/**
 *
 * 相似矩阵
 * 实现dim参数的自动获取，在cf相似分中为分子部分积分
 *
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