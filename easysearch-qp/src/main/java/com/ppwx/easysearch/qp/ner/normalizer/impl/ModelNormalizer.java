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

package com.ppwx.easysearch.qp.ner.normalizer.impl;

import com.ppwx.easysearch.qp.ner.EntityType;
import com.ppwx.easysearch.qp.ner.normalizer.AbstractEntityTypeNormalizer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 型号归一化器
 * 归一化的主要目标：
 * 1. 统一大小写：将型号中的大写字母转换为小写字母
 * 2. 统一系列名称：确保同型号不同写法能统一到标准格式
 * 3. 统一空格：将紧凑的型号写法转换为标准格式
 * 4. 统一括号：将中文括号转换为英文括号
 * 5. 统一后缀：统一 pro、max、ultra、plus 等后缀格式
 * 
 */
public class ModelNormalizer extends AbstractEntityTypeNormalizer {
    
    public ModelNormalizer() {
        super(EntityType.MODEL);
    }

    @Override
    protected String doNormalize(String word) {
        if (word == null || word.isEmpty()) {
            return word;
        }
        
        String normalized = word;
        
        // 处理品牌前缀和型号之间的空格
        normalized = normalizeBrandSpacing(normalized);
        
        // 处理型号中的空格规则
        normalized = normalizeModelSpacing(normalized);
        
        // 去除首尾空格
        normalized = normalized.trim();
        
        return normalized;
    }
    
    /**
     * 归一化品牌名和型号之间的空格
     */
    private String normalizeBrandSpacing(String text) {
        // Apple 品牌系列：确保品牌后有空格
        text = normalizePattern(text, "iphone", "iphone ");
        text = normalizePattern(text, "ipad", "ipad ");
        text = normalizePattern(text, "macbook", "macbook ");
        text = normalizePattern(text, "apple\\s*watch", "apple watch ");
        text = normalizePattern(text, "airpods", "airpods ");
        text = normalizePattern(text, "apple\\s*pencil", "apple pencil ");
        text = normalizePattern(text, "homepod", "homepod ");
        
        // 华为品牌系列
        text = normalizePattern(text, "华为\\s*mate", "华为 mate ");
        text = normalizePattern(text, "华为\\s*p\\s*(\\d)", "华为 p$1");
        text = normalizePattern(text, "华为\\s*pura", "华为 pura ");
        text = normalizePattern(text, "华为\\s*nova", "华为 nova ");
        text = normalizePattern(text, "华为\\s*畅享", "华为 畅享 ");
        text = normalizePattern(text, "华为\\s*麦芒", "华为 麦芒 ");
        text = normalizePattern(text, "华为\\s*pocket", "华为 pocket ");
        text = normalizePattern(text, "华为\\s*matepad", "华为 matepad ");
        text = normalizePattern(text, "华为\\s*平板", "华为 平板 ");
        text = normalizePattern(text, "华为\\s*揽阅", "华为 揽阅 ");
        text = normalizePattern(text, "华为\\s*matebook", "华为 matebook ");
        text = normalizePattern(text, "华为\\s*擎云", "华为 擎云 ");
        text = normalizePattern(text, "华为\\s*watch", "华为 watch ");
        text = normalizePattern(text, "华为\\s*儿童手表", "华为 儿童手表 ");
        text = normalizePattern(text, "华为\\s*freebuds", "华为 freebuds ");
        text = normalizePattern(text, "华为\\s*freelace", "华为 freelace ");
        text = normalizePattern(text, "华为\\s*freeclip", "华为 freeclip");
        text = normalizePattern(text, "华为\\s*sound", "华为 sound ");
        
        // 小米品牌系列
        text = normalizePattern(text, "小米\\s*(\\d)", "小米 $1");
        text = normalizePattern(text, "小米\\s*mix", "小米 mix ");
        text = normalizePattern(text, "小米\\s*cc", "小米 cc ");
        text = normalizePattern(text, "小米\\s*civi", "小米 civi ");
        text = normalizePattern(text, "小米\\s*max", "小米 max ");
        text = normalizePattern(text, "小米\\s*play", "小米 play");
        text = normalizePattern(text, "小米\\s*note", "小米 note ");
        text = normalizePattern(text, "红米\\s*", "红米 ");
        text = normalizePattern(text, "redmi\\s*", "redmi ");
        text = normalizePattern(text, "小米\\s*平板", "小米 平板 ");
        text = normalizePattern(text, "小米\\s*pad", "小米 pad ");
        text = normalizePattern(text, "小米\\s*笔记本", "小米 笔记本 ");
        text = normalizePattern(text, "小米\\s*book", "小米 book ");
        text = normalizePattern(text, "小米\\s*游戏本", "小米 游戏本 ");
        text = normalizePattern(text, "redmibook", "redmi book ");
        text = normalizePattern(text, "小米\\s*watch", "小米 watch ");
        text = normalizePattern(text, "小米\\s*手表", "小米 手表 ");
        text = normalizePattern(text, "小米\\s*米兔", "小米 米兔");
        text = normalizePattern(text, "小米\\s*小寻", "小米 小寻");
        text = normalizePattern(text, "小米\\s*buds", "小米 buds ");
        text = normalizePattern(text, "小米\\s*flipbuds", "小米 flipbuds ");
        text = normalizePattern(text, "小米\\s*air\\s*(\\d)", "小米 air $1");
        text = normalizePattern(text, "小米\\s*sound", "小米 sound ");
        text = normalizePattern(text, "小米\\s*小爱", "小米 小爱");
        
        // OPPO 品牌系列
        text = normalizePattern(text, "oppo\\s*reno", "oppo reno ");
        text = normalizePattern(text, "oppo\\s*find", "oppo find ");
        text = normalizePattern(text, "oppo\\s*a\\s*(\\d)", "oppo a$1");
        text = normalizePattern(text, "oppo\\s*k\\s*(\\d)", "oppo k$1");
        text = normalizePattern(text, "oppo\\s*r\\s*(\\d)", "oppo r$1");
        text = normalizePattern(text, "oppo\\s*ace", "oppo ace ");
        
        // Vivo 品牌系列
        text = normalizePattern(text, "vivo\\s*x\\s*(\\d)", "vivo x$1");
        text = normalizePattern(text, "vivo\\s*s\\s*(\\d)", "vivo s$1");
        text = normalizePattern(text, "vivo\\s*y\\s*(\\d)", "vivo y$1");
        text = normalizePattern(text, "vivo\\s*z\\s*(\\d)", "vivo z$1");
        text = normalizePattern(text, "vivo\\s*nex", "vivo nex ");
        text = normalizePattern(text, "vivo\\s*t\\s*(\\d)", "vivo t$1");
        text = normalizePattern(text, "vivo\\s*u\\s*(\\d)", "vivo u$1");
        
        // 三星品牌系列
        text = normalizePattern(text, "三星\\s*galaxy", "三星 galaxy ");
        text = normalizePattern(text, "三星\\s*w\\s*(\\d)", "三星 w$1");
        
        // 一加品牌系列
        text = normalizePattern(text, "一加\\s*(\\d)", "一加 $1");
        text = normalizePattern(text, "一加\\s*ace", "一加 ace ");
        
        // 荣耀品牌系列
        text = normalizePattern(text, "荣耀\\s*(\\d)", "荣耀 $1");
        text = normalizePattern(text, "荣耀\\s*magic", "荣耀 magic ");
        text = normalizePattern(text, "荣耀\\s*x\\s*(\\d)", "荣耀 x$1");
        text = normalizePattern(text, "荣耀\\s*v\\s*(\\d)", "荣耀 v$1");
        text = normalizePattern(text, "荣耀\\s*畅玩", "荣耀 畅玩 ");
        text = normalizePattern(text, "荣耀\\s*play", "荣耀 play ");
        text = normalizePattern(text, "荣耀\\s*gt", "荣耀 gt ");
        text = normalizePattern(text, "荣耀\\s*power", "荣耀 power");
        text = normalizePattern(text, "荣耀\\s*magicbook", "荣耀 magicbook ");
        text = normalizePattern(text, "荣耀\\s*笔记本", "荣耀 笔记本 ");
        
        // IQOO 品牌系列
        text = normalizePattern(text, "iqoo\\s*", "iqoo ");
        
        // 真我/Realme 品牌系列
        text = normalizePattern(text, "真我\\s*", "真我 ");
        
        // 努比亚品牌系列
        text = normalizePattern(text, "努比亚\\s*红魔", "努比亚 红魔 ");
        text = normalizePattern(text, "努比亚\\s*z\\s*(\\d)", "努比亚 z$1");
        text = normalizePattern(text, "努比亚\\s*flip", "努比亚 flip ");
        text = normalizePattern(text, "努比亚\\s*小牛", "努比亚 小牛");
        
        // 笔记本品牌系列
        text = normalizePattern(text, "thinkpad\\s*", "thinkpad ");
        text = normalizePattern(text, "戴尔\\s*", "戴尔 ");
        text = normalizePattern(text, "alienware\\s*", "alienware ");
        text = normalizePattern(text, "华硕\\s*", "华硕 ");
        text = normalizePattern(text, "惠普\\s*", "惠普 ");
        text = normalizePattern(text, "联想\\s*", "联想 ");
        text = normalizePattern(text, "acer\\s*", "acer ");
        text = normalizePattern(text, "微软\\s*", "微软 ");
        text = normalizePattern(text, "三星\\s*", "三星 ");
        
        return text;
    }
    
    /**
     * 归一化型号内部的空格
     */
    private String normalizeModelSpacing(String text) {
        // 统一后缀前的空格：pro, max, ultra, plus, mini, se, air, pro+, ultra 等
        text = normalizePattern(text, "\\s*(pro\\+)", " $1");
        text = normalizePattern(text, "\\s*(pro)", " $1");
        text = normalizePattern(text, "\\s*(max)", " $1");
        text = normalizePattern(text, "\\s*(ultra)", " $1");
        text = normalizePattern(text, "\\s*(plus)", " $1");
        text = normalizePattern(text, "\\s*(mini)", " $1");
        text = normalizePattern(text, "\\s*(se)", " $1");
        text = normalizePattern(text, "\\s*(air)", " $1");
        text = normalizePattern(text, "\\s*(lite)", " $1");
        text = normalizePattern(text, "\\s*(edge)", " $1");
        text = normalizePattern(text, "\\s*(fe)", " $1");
        text = normalizePattern(text, "\\s*(gt)", " $1");
        text = normalizePattern(text, "\\s*(turbo)", " $1");
        text = normalizePattern(text, "\\s*(neo)", " $1");
        text = normalizePattern(text, "\\s*(flip)", " $1");
        text = normalizePattern(text, "\\s*(fold)", " $1");
        text = normalizePattern(text, "\\s*(note)", " $1");
        
        // 版本标识前的空格
        text = normalizePattern(text, "\\s*\\((5g版|4g版|青春版|活力版|标准版|至尊版|竞速版|电竞版)", " ($1");
        
        // 去除多余空格
        text = text.replaceAll("\\s+", " ");
        
        return text;
    }
    
    /**
     * 使用正则表达式替换模式，忽略大小写
     */
    private String normalizePattern(String text, String pattern, String replacement) {
        Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        return m.replaceAll(replacement);
    }

}

