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

package com.ppwx.easysearch.qp.support;

/**
 * 查询处理选项，控制各可选阶段的启用/禁用。
 * <p>
 * 所有开关默认启用，同义词默认使用 rewrite 模式。
 * 通过 {@link Builder} 构建，或使用 {@link #defaults()} 获取默认配置。
 */
public class ProcessOptions {

    /** 是否启用同义词改写（生成单条主查询） */
    private final boolean enableSynonymRewrite;

    /** 是否启用同义词扩展（生成多条候选 query） */
    private final boolean enableSynonymExpand;

    /** 是否启用拼写纠错 */
    private final boolean enableSpellCorrection;

    /** 是否启用实体识别 */
    private final boolean enableNer;

    /** 是否启用实体归一化 */
    private final boolean enableNormalization;

    private ProcessOptions(Builder builder) {
        this.enableSynonymRewrite = builder.enableSynonymRewrite;
        this.enableSynonymExpand = builder.enableSynonymExpand;
        this.enableSpellCorrection = builder.enableSpellCorrection;
        this.enableNer = builder.enableNer;
        this.enableNormalization = builder.enableNormalization;
    }

    /**
     * 返回默认选项：所有功能启用，同义词使用 rewrite 模式。
     */
    public static ProcessOptions defaults() {
        return new Builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isEnableSynonymRewrite() {
        return enableSynonymRewrite;
    }

    public boolean isEnableSynonymExpand() {
        return enableSynonymExpand;
    }

    public boolean isEnableSpellCorrection() {
        return enableSpellCorrection;
    }

    public boolean isEnableNer() {
        return enableNer;
    }

    public boolean isEnableNormalization() {
        return enableNormalization;
    }

    @Override
    public String toString() {
        return "ProcessOptions{"
                + "synonymRewrite=" + enableSynonymRewrite
                + ", synonymExpand=" + enableSynonymExpand
                + ", spellCorrection=" + enableSpellCorrection
                + ", ner=" + enableNer
                + ", normalization=" + enableNormalization
                + '}';
    }

    /**
     * ProcessOptions 构建器。
     */
    public static class Builder {

        private boolean enableSynonymRewrite = true;
        private boolean enableSynonymExpand = false;
        private boolean enableSpellCorrection = true;
        private boolean enableNer = true;
        private boolean enableNormalization = true;

        public Builder enableSynonymRewrite(boolean enable) {
            this.enableSynonymRewrite = enable;
            return this;
        }

        public Builder enableSynonymExpand(boolean enable) {
            this.enableSynonymExpand = enable;
            return this;
        }

        public Builder enableSpellCorrection(boolean enable) {
            this.enableSpellCorrection = enable;
            return this;
        }

        public Builder enableNer(boolean enable) {
            this.enableNer = enable;
            return this;
        }

        public Builder enableNormalization(boolean enable) {
            this.enableNormalization = enable;
            return this;
        }

        public ProcessOptions build() {
            return new ProcessOptions(this);
        }
    }
}
