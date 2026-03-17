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

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className WordFormatTruncate
 * @description 截断
 * @date 2024/11/1 19:47
 **/
public class WordFormatTruncate implements WordFormat {

    private static final WordFormatTruncate INSTANCE = new WordFormatTruncate(60);

    public static WordFormat getInstance() {
        return INSTANCE;
    }

    private final int maxLen;

    public WordFormatTruncate(int maxLen) {
        this.maxLen = maxLen;
    }

    @Override
    public StringBuilder format(StringBuilder original) {
        if (original.length() > maxLen) {
            original.setLength(maxLen);
        }
        return original;
    }
}
