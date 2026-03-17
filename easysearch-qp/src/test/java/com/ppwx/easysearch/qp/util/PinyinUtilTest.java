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

package com.ppwx.easysearch.qp.util;

import org.junit.Test;

public class PinyinUtilTest {

    @Test
    public void testPinyinConversion() {
        // 测试多音字和词组
        String result1 = PinyinUtil.getPinyin("重载不是重任");
        System.out.println("多音字测试 - 重载不是重任: " + result1);
        
        // 测试混合文本（中文+英文+数字）
        String result2 = PinyinUtil.getPinyin("重载12不是重任");
        System.out.println("混合文本测试 - 重载12不是重任: " + result2);
        
        // 测试带分隔符的情况
        String result3 = PinyinUtil.getPinyin("北京大学", " ");
        System.out.println("带分隔符测试 - 北京大学: " + result3);
        
        // 测试纯英文
        String result4 = PinyinUtil.getPinyin("Hello World");
        System.out.println("纯英文测试 - Hello World: " + result4);
        
        // 测试空字符串和null
        String result5 = PinyinUtil.getPinyin("");
        System.out.println("空字符串测试: '" + result5 + "'");
        
        String result6 = PinyinUtil.getPinyin(null);
        System.out.println("null测试: " + result6);
        
        // 测试复杂混合文本
        String result7 = PinyinUtil.getPinyin("iPhone14手机价格");
        System.out.println("复杂混合文本测试 - iPhone14手机价格: " + result7);
        
        System.out.println("\n=== 测试拼音首字母获取方法 ===");
        
        // 测试纯汉字
        String initials1 = PinyinUtil.getPinyinInitials("北京大学");
        System.out.println("纯汉字测试 - 北京大学: " + initials1);
        
        // 测试混合文本（应该忽略非汉字）
        String initials2 = PinyinUtil.getPinyinInitials("iPhone14手机价格");
        System.out.println("混合文本测试 - iPhone14手机价格: " + initials2);
        
        // 测试多音字
        String initials3 = PinyinUtil.getPinyinInitials("重载不是重任");
        System.out.println("多音字测试 - 重载不是重任: " + initials3);
        
        // 测试纯英文（应该返回空字符串）
        String initials4 = PinyinUtil.getPinyinInitials("Hello World");
        System.out.println("纯英文测试 - Hello World: '" + initials4 + "'");
        
        // 测试空字符串和null
        String initials5 = PinyinUtil.getPinyinInitials("");
        System.out.println("空字符串测试: '" + initials5 + "'");
        
        String initials6 = PinyinUtil.getPinyinInitials(null);
        System.out.println("null测试: " + initials6);
        
        // 测试数字和符号（应该忽略）
        String initials7 = PinyinUtil.getPinyinInitials("123@#$%^&*()中文");
        System.out.println("数字符号测试 - 123@#$%^&*()中文: " + initials7);
    }

    @Test
    public void test() {
        
    }
}
