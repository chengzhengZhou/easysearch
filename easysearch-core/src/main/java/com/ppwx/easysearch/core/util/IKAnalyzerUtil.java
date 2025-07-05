package com.ppwx.easysearch.core.util;

import com.ppwx.easysearch.core.common.DataException;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.wltea.analyzer.lucene.IKAnalyzer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * 中文分词器工具
 * 扩展词典：ext.dic
 * 扩展停止词典：stopword.dic
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/27 10:21
 * @since 1.0.0
 */
public final class IKAnalyzerUtil {

    /**
     * 提取词组
     *
     * @param str
     * @param minLen
     * @return java.util.List<java.lang.String>
     */
    public static List<String> extractWords(String str, int minLen) {
        List<String> re = new ArrayList<>();
        if (StringUtils.isBlank(str)) {
            return re;
        }
        //创建一个标准分析器对象
        Analyzer analyzer = InnerInstance.analyzer;
        //获取tokenStream对象
        //参数1域名 2要分析的文本内容
        try (TokenStream tokenStream = analyzer.tokenStream("", str)) {
            //添加引用,用于获取每个关键词
            CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
            //添加一个偏移量的引用，记录了关键词的开始位置以及结束位置
            //OffsetAttribute offsetAttribute = tokenStream.addAttribute(OffsetAttribute.class);
            //TermFrequencyAttribute termFrequencyAttribute = tokenStream.addAttribute(TermFrequencyAttribute.class);
            //KeywordAttribute keywordAttribute = tokenStream.addAttribute(KeywordAttribute.class);
            //将指针调整到列表的头部
            tokenStream.reset();
            //遍历关键词列表,incrementToken判断是否结束
            String val;
            while (tokenStream.incrementToken()) {
                val = charTermAttribute.toString();
                if (val.length() >= minLen) {
                    re.add(val);
                }
            }
        } catch (IOException e) {
            throw new DataException(e);
        }
        return re;
    }

    private static class InnerInstance {
        private final static Analyzer analyzer = new IKAnalyzer(true);
    }

}
