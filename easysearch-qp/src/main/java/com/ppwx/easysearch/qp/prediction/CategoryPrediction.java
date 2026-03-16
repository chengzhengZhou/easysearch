package com.ppwx.easysearch.qp.prediction;

import java.util.List;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className CategoryPrediction
 * @description 类目预测
 * @date 2024/11/1 15:47
 **/
public interface CategoryPrediction {

    /**
     * @description 类目预测
     * @author ext.ahs.zhouchzh1@jd.com
     * @date 2024/11/1 16:01
     * @param query  查询词
     * @return List<Category>
     */
    List<Category> predict(String query);

}
