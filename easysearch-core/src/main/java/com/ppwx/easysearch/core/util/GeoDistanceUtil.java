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

package com.ppwx.easysearch.core.util;

/**
 *
 * 获取两个点之间的球面距离
 *
 * @since 1.0.0
 */
public class GeoDistanceUtil {

    private final static double EARTH_RADIUS = 6378137.0;

    private GeoDistanceUtil() {

    }

    /**
     * 计算两点距离（米）
     *
     * @param lat1
     * @param lng1
     * @param lat2
     * @param lng2
     * @return double
     */
    public static double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double patm = 2;
        double radLat1 = rad(lat1);
        double radLat2 = rad(lat2);
        double difference = radLat1 - radLat2;
        double mdifference = rad(lng1) - rad(lng2);
        double distance = patm * Math.asin(
                Math.sqrt(
                        Math.pow(Math.sin(difference / patm), patm)
                                + Math.cos(radLat1)
                                * Math.cos(radLat2)
                                * Math.pow(Math.sin(mdifference / patm), patm)
                )
        );
        distance = distance * EARTH_RADIUS;
        return distance;
    }

    private static double rad(double d) {
        return d * Math.PI / 180.0;
    }
}
