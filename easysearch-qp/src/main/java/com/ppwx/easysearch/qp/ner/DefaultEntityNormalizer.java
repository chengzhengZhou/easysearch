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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultEntityNormalizer implements EntityNormalizer{

    // CPU归一化映射
    private static final Map<String, String> CPU_NORMALIZATION = new HashMap<>();

    // 成色归一化映射
    private static final Map<String, String> CONDITION_NORMALIZATION = new HashMap<>();

    // 单位归一化正则
    private static final Pattern UNIT_PATTERN = Pattern.compile("(\\d+)\\s*([GgTtMmKk][BbGg]?)");

    static {
        initCPUNormalization();
        initConditionNormalization();
    }


    @Override
    public String normalize(EntityType entityType, String word) {
        switch (entityType) {
            case BRAND:
                return normalizeBrand(word);
            case MODEL:
                return normalizeModel(word);
            case CPU:
                return normalizeCPU(word);
            case CONDITION:
                return normalizeCondition(word);
        }
        return word;
    }

    /**
     * 归一化品牌
     */
    public String normalizeBrand(String brand) {
        // do nothing
        return brand;
    }

    /**
     * 归一化型号
     */
    public String normalizeModel(String model) {
        if (model == null || model.trim().isEmpty()) {
            return model;
        }

        String normalized = model.trim();

        // iPhone型号特殊处理
        if (normalized.toLowerCase().contains("iphone") || normalized.toLowerCase().contains("苹果")) {
            return normalizePhoneModel(normalized);
        }

        // MacBook型号特殊处理
        if (normalized.toLowerCase().contains("macbook")) {
            return normalizeMacBookModel(normalized);
        }

        // 华为型号特殊处理
        if (normalized.toLowerCase().contains("mate") || normalized.toLowerCase().contains("p")) {
            return normalizeHuaweiModel(normalized);
        }

        // 小米型号特殊处理
        if (normalized.toLowerCase().contains("mi") || normalized.toLowerCase().contains("redmi")) {
            return normalizeXiaomiModel(normalized);
        }

        return normalized;
    }

    /**
     * 归一化CPU
     */
    public String normalizeCPU(String cpu) {
        if (cpu == null || cpu.trim().isEmpty()) {
            return cpu;
        }

        String normalized = cpu.trim();

        // 查找CPU映射
        for (Map.Entry<String, String> entry : CPU_NORMALIZATION.entrySet()) {
            if (normalized.toLowerCase().contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }

        return normalized;
    }

    /**
     * 归一化成色
     */
    public String normalizeCondition(String condition) {
        if (condition == null || condition.trim().isEmpty()) {
            return condition;
        }

        String normalized = condition.trim();

        // 查找成色映射
        for (Map.Entry<String, String> entry : CONDITION_NORMALIZATION.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return normalized;
    }

    /**
     * 归一化存储容量
     */
    public String normalizeStorage(String storage) {
        if (storage == null || storage.trim().isEmpty()) {
            return storage;
        }

        Matcher matcher = UNIT_PATTERN.matcher(storage);
        if (matcher.find()) {
            String number = matcher.group(1);
            String unit = matcher.group(2).toUpperCase();

            // 统一单位格式
            if (unit.startsWith("G")) {
                unit = "GB";
            } else if (unit.startsWith("T")) {
                unit = "TB";
            } else if (unit.startsWith("M")) {
                unit = "MB";
            } else if (unit.startsWith("K")) {
                unit = "KB";
            }

            return number + unit;
        }

        return storage;
    }

    /**
     * 归一化内存容量
     */
    public String normalizeRAM(String ram) {
        if (ram == null || ram.trim().isEmpty()) {
            return ram;
        }

        Matcher matcher = UNIT_PATTERN.matcher(ram);
        if (matcher.find()) {
            String number = matcher.group(1);
            return number + "GB RAM";
        }

        return ram;
    }

    /**
     * 归一化iPhone型号
     */
    private String normalizePhoneModel(String model) {
        String normalized = model.toLowerCase();

        // 提取数字和可能的修饰词
        Pattern pattern = Pattern.compile("(iphone|苹果)\\s*(\\d{1,2})\\s*(pro max|pro|plus|mini|max|ultra|air)*");
        Matcher matcher = pattern.matcher(normalized);
        if (matcher.find()) {
            String prefix = "iphone";
            String number = matcher.group(2);
            String suffix = matcher.group(3) != null ? " " + matcher.group(3) : "";
            return prefix + " " + number + suffix;
        }

        return model;
    }

    /**
     * 归一化MacBook型号
     */
    private String normalizeMacBookModel(String model) {
        String normalized = model.toLowerCase();

        if (normalized.contains("air")) {
            return "macbook air";
        } else if (normalized.contains("pro")) {
            return "macbook pro";
        } else {
            return model;
        }
    }

    /**
     * 归一化华为型号
     */
    private String normalizeHuaweiModel(String model) {
        String normalized = model.toLowerCase();

        if (normalized.contains("mate")) {
            Pattern pattern = Pattern.compile("mate\\s*(\\d+)");
            Matcher matcher = pattern.matcher(normalized);
            if (matcher.find()) {
                return "华为 mate " + matcher.group(1);
            }
        } else if (normalized.contains("p")) {
            Pattern pattern = Pattern.compile("p\\s*(\\d+)");
            Matcher matcher = pattern.matcher(normalized);
            if (matcher.find()) {
                return "华为 p" + matcher.group(1);
            }
        }

        return "华为 " + model;
    }

    /**
     * 归一化小米型号
     */
    private String normalizeXiaomiModel(String model) {
        String normalized = model.toLowerCase();

        if (normalized.contains("redmi")) {
            return "redmi " + model.replaceAll("(?i)redmi\\s*", "");
        } else if (normalized.contains("mi")) {
            return "mi " + model.replaceAll("(?i)mi\\s*", "");
        }

        return "xiaomi " + model;
    }

    /**
     * 初始化CPU归一化映射
     */
    private static void initCPUNormalization() {
        CPU_NORMALIZATION.put("i3", "intel core i3");
        CPU_NORMALIZATION.put("i5", "intel core i5");
        CPU_NORMALIZATION.put("i7", "intel core i7");
        CPU_NORMALIZATION.put("i9", "intel core i9");
        CPU_NORMALIZATION.put("麒麟", "kirin");
        CPU_NORMALIZATION.put("天玑", "dimensity");
        CPU_NORMALIZATION.put("联发科", "mediatek");
        CPU_NORMALIZATION.put("高通", "qualcomm");
    }

    /**
     * 初始化成色归一化映射
     */
    private static void initConditionNormalization() {
        CONDITION_NORMALIZATION.put("全新", "全新");
        CONDITION_NORMALIZATION.put("九九新", "99新");
        CONDITION_NORMALIZATION.put("九五新", "95新");
        CONDITION_NORMALIZATION.put("九成新", "90新");
        CONDITION_NORMALIZATION.put("八成新", "80新");
        CONDITION_NORMALIZATION.put("七成新", "70新");
        CONDITION_NORMALIZATION.put("六成新", "60新");
        CONDITION_NORMALIZATION.put("五成新", "50新");
        CONDITION_NORMALIZATION.put("旧", "二手");
    }
}
