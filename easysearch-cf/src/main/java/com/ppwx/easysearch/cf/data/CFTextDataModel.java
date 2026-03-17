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