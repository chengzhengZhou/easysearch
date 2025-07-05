/**
 * Copyright (C), 2010-2024, 爱回收
 * FileName: UserRating
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2024/7/27 19:15
 * Description: 用户行为偏好记录
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