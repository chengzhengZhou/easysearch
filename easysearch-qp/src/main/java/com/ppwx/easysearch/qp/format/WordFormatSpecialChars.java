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

package com.ppwx.easysearch.qp.format;

import java.util.regex.Pattern;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className WordFormatSpecialChars
 * @description 特殊字符清理
 * @date 2024/11/1 19:47
 **/
public class WordFormatSpecialChars implements WordFormat{

    // 表情符号
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[\\x{1F600}-\\x{1F64F}\\x{1F300}-\\x{1F5FF}\\x{1F680}-\\x{1F6FF}\\x{1F1E0}-\\x{1F1FF}]");

    @Override
    public StringBuilder format(StringBuilder original) {
        if (original.length() <= 0) {
            return original;
        }
        // 清理特殊字符
        String normalized = SPECIAL_CHARS_PATTERN.matcher(original).replaceAll("");
        // 清理多余空格
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return new StringBuilder(normalized);
    }
}
