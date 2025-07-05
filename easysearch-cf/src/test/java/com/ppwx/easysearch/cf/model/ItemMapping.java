/**
 * Copyright (C), 2010-2024, 爱回收
 * FileName: RecItemMapping
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2024/9/3 16:53
 * Description: 物品 - 属性映射
 */
package com.ppwx.easysearch.cf.model;

import java.io.Serializable;

/**
 *
 * 物品 - 属性映射
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2024/09/03 16:53
 * @since 1.0.0
 */
public class ItemMapping implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    /**
     * 型号
     */
    private Integer productId;
    /**
     * 机型名称
     */
    private String productName;
    /**
     * 成色
     */
    private Integer qualityId;
    /**
     * sku属性值
     */
    private String skuPpvIds;
    /**
     * sku属性值描述
     */
    private String skuPpvName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getQualityId() {
        return qualityId;
    }

    public void setQualityId(Integer qualityId) {
        this.qualityId = qualityId;
    }

    public String getSkuPpvIds() {
        return skuPpvIds;
    }

    public void setSkuPpvIds(String skuPpvIds) {
        this.skuPpvIds = skuPpvIds;
    }

    public String getSkuPpvName() {
        return skuPpvName;
    }

    public void setSkuPpvName(String skuPpvName) {
        this.skuPpvName = skuPpvName;
    }

    @Override
    public String toString() {
        return "ItemMapping{" +
                "id=" + id +
                ", productId=" + productId +
                ", productName='" + productName + '\'' +
                ", qualityId=" + qualityId +
                ", skuPpvIds='" + skuPpvIds + '\'' +
                ", skuPpvName='" + skuPpvName + '\'' +
                '}';
    }
}