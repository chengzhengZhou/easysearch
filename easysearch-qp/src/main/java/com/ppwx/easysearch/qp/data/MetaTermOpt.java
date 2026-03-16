package com.ppwx.easysearch.qp.data;

public class MetaTermOpt extends TermOpt {

    public static final String CATEGORY = "category";

    public static final String BRAND = "brand";

    public static final String MODEL = "model";

    private String modelId;

    private String modelName;

    private String brandId;

    private String brandName;

    private String brandNameEn;

    private String categoryId;

    private String categoryName;

    public String getModelId() {
        return modelId;
    }

    public String getModelName() {
        return modelName;
    }

    public String getBrandId() {
        return brandId;
    }

    public String getBrandName() {
        return brandName;
    }

    public String getBrandNameEn() {
        return brandNameEn;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public static class Builder {
        private String modelId;
        private String modelName;
        private String brandId;
        private String brandName;
        private String brandNameEn;
        private String categoryId;
        private String categoryName;
        private String termType;
        private int opt;

        public Builder modelId(String modelId) {
            this.modelId = modelId;
            return this;
        }

        public Builder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public Builder brandId(String brandId) {
            this.brandId = brandId;
            return this;
        }

        public Builder brandName(String brandName) {
            this.brandName = brandName;
            return this;
        }

        public Builder categoryId(String categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        public Builder categoryName(String categoryName) {
            this.categoryName = categoryName;
            return this;
        }

        public Builder brandNameEn(String brandNameEn) {
            this.brandNameEn = brandNameEn;
            return this;
        }

        public Builder termType(String termType) {
            this.termType = termType;
            return this;
        }

        public Builder opt(int opt) {
            this.opt = opt;
            return this;
        }

        public MetaTermOpt build() {
            MetaTermOpt metaTermOpt = new MetaTermOpt();
            metaTermOpt.modelId = (this.modelId);
            metaTermOpt.modelName = (this.modelName);
            metaTermOpt.brandId = (this.brandId);
            metaTermOpt.brandName = (this.brandName);
            metaTermOpt.categoryId = (this.categoryId);
            metaTermOpt.categoryName = (this.categoryName);
            metaTermOpt.termType = (this.termType);
            metaTermOpt.opt = (this.opt);
            metaTermOpt.brandNameEn = (this.brandNameEn);
            return metaTermOpt;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "MetaTermOpt{" +
                "termType='" + termType + '\'' +
                ", opt=" + opt +
                ", categoryName='" + categoryName + '\'' +
                ", categoryId='" + categoryId + '\'' +
                ", brandName='" + brandName + '\'' +
                ", brandId='" + brandId + '\'' +
                ", modelName='" + modelName + '\'' +
                ", modelId=" + modelId +
                '}';
    }
}
