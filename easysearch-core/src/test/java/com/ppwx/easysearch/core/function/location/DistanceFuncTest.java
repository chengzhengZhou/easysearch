/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: DistanceFuncTest
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/12/27 11:30
 * Description: DistanceFunc测试类
 */
package com.ppwx.easysearch.core.function.location;

import com.alibaba.fastjson.JSONObject;
import com.ppwx.easysearch.core.data.element.JsonColumn;
import org.junit.Test;

import static com.ppwx.easysearch.core.data.model.Key.COLUMN_LATITUDE;
import static com.ppwx.easysearch.core.data.model.Key.COLUMN_LONGITUDE;

/**
 *
 * DistanceFunc测试类
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/27 11:30
 * @since 1.0.0
 */
public class DistanceFuncTest {

    @Test
    public void testDistanceFuncWorks() {
        JSONObject location = new JSONObject();
        location.put(COLUMN_LATITUDE, "31.2959273848");
        location.put(COLUMN_LONGITUDE, "121.5191009209");

        DistanceFunc distanceFunc = new DistanceFunc(31.2959273848D, 121.5502498225D);
        Double apply = distanceFunc.apply(new JsonColumn(location));
        System.out.println(apply);
    }

}