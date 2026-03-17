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

import java.util.HashMap;
import java.util.Map;

/**
 * @className WordFormatHalfWidth
 * @description 半角
 **/
public class WordFormatHalfWidth implements WordFormat {

    // 全角转半角映射
    private static final Map<Character, Character> FULL_TO_HALF_MAP = new HashMap<>();

    static {
        initFullToHalfMapping();
    }

    @Override
    public StringBuilder format(StringBuilder original) {
        if (original.length() <= 0) {
            return original;
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < original.length(); i++) {
            char c = original.charAt(i);
            if (FULL_TO_HALF_MAP.containsKey(c)) {
                result.append(FULL_TO_HALF_MAP.get(c));
            } else {
                result.append(c);
            }
        }
        return result;
    }

    /**
     * 初始化全角转半角映射
     */
    private static void initFullToHalfMapping() {
        // 数字
        for (int i = 0; i < 10; i++) {
            FULL_TO_HALF_MAP.put((char) ('０' + i), (char) ('0' + i));
        }

        // 英文字母
        for (int i = 0; i < 26; i++) {
            FULL_TO_HALF_MAP.put((char) ('Ａ' + i), (char) ('A' + i));
            FULL_TO_HALF_MAP.put((char) ('ａ' + i), (char) ('a' + i));
        }

        // 常用符号
        //FULL_TO_HALF_MAP.put('（', '(');
        //FULL_TO_HALF_MAP.put('）', ')');
        //FULL_TO_HALF_MAP.put('【', '[');
        //FULL_TO_HALF_MAP.put('】', ']');
        FULL_TO_HALF_MAP.put('，', ',');
        FULL_TO_HALF_MAP.put('。', '.');
        FULL_TO_HALF_MAP.put('：', ':');
        FULL_TO_HALF_MAP.put('；', ';');
        FULL_TO_HALF_MAP.put('？', '?');
        FULL_TO_HALF_MAP.put('！', '!');
        // 引号字符映射（跳过有问题的字符）
        FULL_TO_HALF_MAP.put('－', '-');
        FULL_TO_HALF_MAP.put('—', '-');
        FULL_TO_HALF_MAP.put('　', ' '); // 全角空格转半角空格
    }
}
