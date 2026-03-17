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
 * @className WordFormatIgnoreCase
 * @description 忽略大小写
 **/
public class WordFormatIgnoreCase implements WordFormat {

    private static final WordFormatIgnoreCase INSTANCE = new WordFormatIgnoreCase();

    public static WordFormat getInstance() {
        return INSTANCE;
    }

    @Override
    public StringBuilder format(StringBuilder original) {
        for (int i = 0; i < original.length(); i++) {
            original.setCharAt(i, Character.toLowerCase(original.charAt(i)));
        }
        return original;
    }
}
