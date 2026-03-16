package com.ppwx.easysearch.qp.prediction;

import java.util.Objects;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className Category
 * @description 类目信息
 * @date 2024/11/1 15:57
 **/
public class Category {

    private Category parent;

    private final double score;

    private final String categoryName;

    private final Integer categoryId;
    /**
     * 分类类型
     */
    private final String type;

    public Category(double score, String categoryName, Integer categoryId, String type) {
        this.score = score;
        this.categoryName = categoryName;
        this.categoryId = categoryId;
        this.type = type;
    }

    public void setParent(Category parent) {
        this.parent = parent;
    }

    public Category getParent() {
        return parent;
    }

    public double getScore() {
        return score;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public String getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return Double.compare(score, category.score) == 0 && Objects.equals(categoryName, category.categoryName) && Objects.equals(categoryId, category.categoryId) && Objects.equals(type, category.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(score, categoryName, categoryId, type);
    }

    @Override
    public String toString() {
        return "Category{" +
                "parent=" + parent +
                ", score=" + score +
                ", categoryName='" + categoryName + '\'' +
                ", categoryId=" + categoryId +
                ", type='" + type + '\'' +
                '}';
    }
}
