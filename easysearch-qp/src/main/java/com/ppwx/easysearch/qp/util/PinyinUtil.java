package com.ppwx.easysearch.qp.util;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.dictionary.py.Pinyin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PinyinUtil {

    private static final Logger log = LoggerFactory.getLogger(PinyinUtil.class);

    /**
     * 将汉字转换为全拼
     *
     * @param text 文本
     * @param separator 分隔符(默认空格)
     * @return {@link String}
     */
    public static String getPinyin(String text, String separator) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        separator = separator == null ? " " : separator;
        StringBuilder sb = new StringBuilder();
        
        try {
            // 使用HanLP进行分词，这样可以正确处理多字词组和多音字
            List<Pinyin> pinyinList = HanLP.convertToPinyinList(text);
            
            for (int i = 0; i < text.length(); i++) {
                char ch = text.charAt(i);
                
                // 判断是否为汉字字符
                if (ch >= '\u4E00' && ch <= '\u9FA5') {
                    // 从分词结果中获取对应的拼音
                    if (i < pinyinList.size()) {
                        String pinyin = pinyinList.get(i).getPinyinWithoutTone();
                        sb.append(pinyin);
                        
                        // 添加分隔符：如果下一个字符是汉字或者当前是最后一个字符
                        if (i == text.length() - 1 || isChineseChar(text.charAt(i + 1))) {
                            sb.append(separator);
                        }
                    } else {
                        // 如果分词结果不足，直接添加原字符
                        sb.append(ch);
                        if (i == text.length() - 1 || isChineseChar(text.charAt(i + 1))) {
                            sb.append(separator);
                        }
                    }
                } else {
                    // 非汉字字符直接添加
                    sb.append(ch);
                    // 非汉字后添加分隔符，除非下一个字符也是非汉字
                    if (i < text.length() - 1 && isChineseChar(text.charAt(i + 1))) {
                        sb.append(separator);
                    }
                }
            }
        } catch (Exception e) {
            log.error("转换失败: {}", text, e);
            return text;
        }
        
        // 移除末尾的分隔符
        String result = sb.toString();
        if (!separator.isEmpty() && result.endsWith(separator)) {
            result = result.substring(0, result.length() - separator.length());
        }
        
        return result;
    }
    
    /**
     * 判断字符是否为汉字
     *
     * @param ch 字符
     * @return true if 汉字
     */
    private static boolean isChineseChar(char ch) {
        return ch >= '\u4E00' && ch <= '\u9FA5';
    }

    /**
     * 将汉字转换为全拼
     *
     * @param text 文本
     * @return {@link String}
     */
    public static String getPinyin(String text) {
        return getPinyin(text, "");
    }

    /**
     * 获取汉字首字母，非汉字部分忽略
     *
     * @param text 文本
     * @return {@link String}
     */
    public static String getPinyinInitials(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        StringBuilder sb = new StringBuilder();
        try {
            // 使用HanLP对整个文本进行拼音转换
            List<Pinyin> pinyinList = HanLP.convertToPinyinList(text);
            
            for (int i = 0; i < text.length(); i++) {
                char ch = text.charAt(i);
                
                // 只处理汉字字符，非汉字部分忽略
                if (isChineseChar(ch)) {
                    // 从拼音列表中获取对应位置的拼音首字母
                    if (i < pinyinList.size()) {
                        char firstChar = pinyinList.get(i).getFirstChar();
                        sb.append(firstChar);
                    }
                    // 如果拼音列表长度不足，忽略该字符
                }
                // 非汉字字符直接忽略，不添加到结果中
            }
        } catch (Exception e) {
            log.error("获取拼音首字母失败: {}", text, e);
            return "";
        }
        
        return sb.toString();
    }

}
