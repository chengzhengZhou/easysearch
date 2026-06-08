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

package com.hankcs.hanlp.model.crf;

import com.hankcs.hanlp.model.crf.crfpp.FeatureIndex;
import com.hankcs.hanlp.model.perceptron.PerceptronNERecognizer;
import com.hankcs.hanlp.model.perceptron.feature.FeatureMap;
import com.hankcs.hanlp.model.perceptron.instance.NERInstance;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * 支持 6 列字符特征输入的二手 3C CRF NER 推理适配器。
 * 列序：0 小写字符 / 1 字类 / 2 品牌命中 / 3 品类命中 / 4 参数模式 / 5 系列命中
 */
public class Product3CCRFRecognizer extends CRFNERecognizer {

    public static final int NUM_COLUMNS = 6;

    private final PerceptronNERecognizer decoder;

    public Product3CCRFRecognizer(String modelPath) throws IOException {
        super(modelPath);
        this.decoder = new PerceptronNERecognizer(this.model);
    }

    /**
     * @param columns columns[col][i] 表示第 i 个字符的第 col 列特征
     * @return BIO 标签序列
     */
    public String[] recognize(String[][] columns) {
        validateColumns(columns);
        return decoder.recognize(createInstance(columns));
    }

    private static void validateColumns(String[][] columns) {
        if (columns == null || columns.length != NUM_COLUMNS || columns[0] == null) {
            throw new IllegalArgumentException("Product3C CRF requires exactly 6 feature columns");
        }
        int length = columns[0].length;
        for (int i = 1; i < columns.length; i++) {
            if (columns[i] == null || columns[i].length != length) {
                throw new IllegalArgumentException("Product3C CRF feature columns must have the same length");
            }
        }
    }

    private NERInstance createInstance(final String[][] columns) {
        final FeatureTemplate[] templates = model.getFeatureTemplateArray();
        final int n = columns[0].length;
        return new NERInstance(columns[0], columns[1], model.featureMap) {
            @Override
            protected int[] extractFeature(String[] wordArray, String[] posArray, FeatureMap featureMap, int position) {
                StringBuilder sb = new StringBuilder();
                List<Integer> vec = new LinkedList<>();
                for (int i = 0; i < templates.length; i++) {
                    Iterator<int[]> offIt = templates[i].offsetList.iterator();
                    Iterator<String> delIt = templates[i].delimiterList.iterator();
                    delIt.next();
                    while (offIt.hasNext()) {
                        int[] off = offIt.next();
                        int t = off[0] + position;
                        int col = off[1];
                        if (t < 0) {
                            sb.append(FeatureIndex.BOS[-(t + 1)]);
                        } else if (t >= n) {
                            sb.append(FeatureIndex.EOS[t - n]);
                        } else {
                            sb.append(columns[col][t]);
                        }
                        if (delIt.hasNext()) {
                            sb.append(delIt.next());
                        } else {
                            sb.append(i);
                        }
                    }
                    addFeatureThenClear(sb, vec, featureMap);
                }
                return toFeatureArray(vec);
            }
        };
    }
}
