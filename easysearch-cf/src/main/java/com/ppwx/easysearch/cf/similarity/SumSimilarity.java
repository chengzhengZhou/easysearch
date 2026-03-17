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

package com.ppwx.easysearch.cf.similarity;

import net.librec.data.DataModel;
import net.librec.math.structure.SparseMatrix;
import net.librec.math.structure.SparseVector;
import net.librec.math.structure.SymmMatrix;
import net.librec.similarity.AbstractRecommenderSimilarity;
import org.apache.commons.lang.StringUtils;

import java.util.List;

/**
 *
 * 累加分值计算
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2024/07/31 16:25
 * @since 1.0.0
 */
public class SumSimilarity extends AbstractRecommenderSimilarity {

    @Override
    public void buildSimilarityMatrix(DataModel dataModel) {
        conf = dataModel.getContext().getConf();
        String similarityKey = conf.get("rec.recommender.similarity.key", "user");
        if(StringUtils.isNotBlank(similarityKey)){
            // calculate the similarity between users, or the similarity between
            // items.
            boolean isUser = StringUtils.equals(similarityKey, "user");
            SparseMatrix trainMatrix = dataModel.getDataSplitter().getTrainData();
            int numUsers = trainMatrix.numRows();
            int numItems = trainMatrix.numColumns();
            int count = isUser ? numUsers : numItems;

            similarityMatrix = new SymmMatrix(count);

            for (int i = 0; i < count; i++) {
                SparseVector thisVector = isUser ? trainMatrix.row(i) : trainMatrix.column(i);
                if (thisVector.getCount() == 0) {
                    continue;
                }
                // user/item itself include
                for (int j = i; j < count; j++) {
                    SparseVector thatVector = isUser ? trainMatrix.row(j) : trainMatrix.column(j);
                    if (thatVector.getCount() == 0) {
                        continue;
                    }

                    double sim = getCorrelation(thisVector, thatVector);
                    if (!Double.isNaN(sim) && sim != 0) {
                        similarityMatrix.set(i, j, sim);
                    }
                }
        }
        }
    }

    @Override
    protected double getSimilarity(List<? extends Number> thisList, List<? extends Number> thatList) {
        if (thisList == null || thatList == null || thisList.size() != thatList.size()) {
            return Double.NaN;
        }

        double num = 0.0;
        for (int i = 0; i < thisList.size(); i++) {
            double thisMinusMu = thisList.get(i).doubleValue();
            double thatMinusMu = thatList.get(i).doubleValue();

            num += thisMinusMu * thatMinusMu;
        }

        return num;
    }
}