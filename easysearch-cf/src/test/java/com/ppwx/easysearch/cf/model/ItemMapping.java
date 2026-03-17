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