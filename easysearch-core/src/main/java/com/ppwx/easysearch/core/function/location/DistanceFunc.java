/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: DistanceFunc
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/12/27 11:20
 * Description: 获取两个点之间的球面距离。一般用于LBS的距离计算
 */
package com.ppwx.easysearch.core.function.location;

import com.alibaba.fastjson.JSONObject;
import com.ppwx.easysearch.core.data.Column;
import com.ppwx.easysearch.core.function.Function;
import com.ppwx.easysearch.core.util.GeoDistanceUtil;

import static com.ppwx.easysearch.core.data.Column.Type.DYNAMIC;
import static com.ppwx.easysearch.core.data.model.Key.COLUMN_LATITUDE;
import static com.ppwx.easysearch.core.data.model.Key.COLUMN_LONGITUDE;

/**
 *
 * 获取两个点之间的球面距离。一般用于LBS的距离计算
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/27 11:20
 * @since 1.0.0
 */
public class DistanceFunc implements Function<Double> {

    private double originLat;

    private double originLon;

    public DistanceFunc(double lat, double lon) {
        this.originLat = lat;
        this.originLon = lon;
    }

    @Override
    public Double apply(Column column) {
        if (column.getType() == DYNAMIC) {
            JSONObject location = (JSONObject) column.getRawData();
            Double lat = location.getDouble(COLUMN_LATITUDE);
            Double lon = location.getDouble(COLUMN_LONGITUDE);
            return GeoDistanceUtil.calculateDistance(lat, lon, originLat, originLon);
        }
        return 0.0;
    }

}