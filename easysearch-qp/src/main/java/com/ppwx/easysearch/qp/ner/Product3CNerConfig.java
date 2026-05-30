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

package com.ppwx.easysearch.qp.ner;

/**
 * 二手 3C NER 配置。
 */
public class Product3CNerConfig {

    public static final String PROP_PREFIX = "qp.ner.product3c.";

    private boolean enabled = true;
    private String modelPath;
    private String dictDir;
    private String spellingPath;
    private MergeStrategy mergeStrategy = MergeStrategy.DICT_FIRST;
    private boolean failFast = false;
    private double defaultConfidence = 0.60D;
    private String modelVersion = "unknown";

    public static Product3CNerConfig fromSystemProperties() {
        Product3CNerConfig config = new Product3CNerConfig();
        config.setEnabled(Boolean.parseBoolean(System.getProperty(PROP_PREFIX + "enabled", String.valueOf(config.isEnabled()))));
        config.setModelPath(System.getProperty(PROP_PREFIX + "modelPath"));
        config.setDictDir(System.getProperty(PROP_PREFIX + "dictDir"));
        config.setSpellingPath(System.getProperty(PROP_PREFIX + "spellingPath"));
        config.setFailFast(Boolean.parseBoolean(System.getProperty(PROP_PREFIX + "failFast", String.valueOf(config.isFailFast()))));
        config.setDefaultConfidence(Double.parseDouble(System.getProperty(PROP_PREFIX + "defaultConfidence", String.valueOf(config.getDefaultConfidence()))));
        config.setModelVersion(System.getProperty(PROP_PREFIX + "modelVersion", config.getModelVersion()));
        String strategy = System.getProperty(PROP_PREFIX + "mergeStrategy");
        if (strategy != null && !strategy.trim().isEmpty()) {
            config.setMergeStrategy(MergeStrategy.valueOf(strategy.trim().toUpperCase()));
        }
        return config;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Product3CNerConfig setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public String getModelPath() {
        return modelPath;
    }

    public Product3CNerConfig setModelPath(String modelPath) {
        this.modelPath = modelPath;
        return this;
    }

    public String getDictDir() {
        return dictDir;
    }

    public Product3CNerConfig setDictDir(String dictDir) {
        this.dictDir = dictDir;
        return this;
    }

    public String getSpellingPath() {
        return spellingPath;
    }

    public Product3CNerConfig setSpellingPath(String spellingPath) {
        this.spellingPath = spellingPath;
        return this;
    }

    public MergeStrategy getMergeStrategy() {
        return mergeStrategy;
    }

    public Product3CNerConfig setMergeStrategy(MergeStrategy mergeStrategy) {
        this.mergeStrategy = mergeStrategy != null ? mergeStrategy : MergeStrategy.DICT_FIRST;
        return this;
    }

    public boolean isFailFast() {
        return failFast;
    }

    public Product3CNerConfig setFailFast(boolean failFast) {
        this.failFast = failFast;
        return this;
    }

    public double getDefaultConfidence() {
        return defaultConfidence;
    }

    public Product3CNerConfig setDefaultConfidence(double defaultConfidence) {
        this.defaultConfidence = defaultConfidence;
        return this;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public Product3CNerConfig setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion != null ? modelVersion : "unknown";
        return this;
    }
}
