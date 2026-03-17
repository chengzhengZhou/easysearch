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

import java.util.Date;

/**
 *
 * 用户对标的物的频分
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2024/07/27 19:15
 * @since 1.0.0
 */
public class UserRating {
    /**
     * itemId
     */
    private String itemId;
    /**
     * 评分
     */
    private Double rate;
    /**
     * 评分时间
     */
    private Date datetime;

    public UserRating() {
    }

    public UserRating(String itemId, Double rate, Date datetime) {
        this.itemId = itemId;
        this.rate = rate;
        this.datetime = datetime;
    }

    public String getItemId() {
        return itemId;
    }

    public Double getRate() {
        return rate;
    }

    public Date getDatetime() {
        return datetime;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public void setRate(Double rate) {
        this.rate = rate;
    }

    public void setDatetime(Date datetime) {
        this.datetime = datetime;
    }
}