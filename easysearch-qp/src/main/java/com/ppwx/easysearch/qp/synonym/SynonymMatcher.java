package com.ppwx.easysearch.qp.synonym;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ppwx.easysearch.core.common.DataException;
import com.ppwx.easysearch.qp.data.ArffSourceConvertor;
import com.ppwx.easysearch.qp.support.CustomStopChar;
import com.ppwx.easysearch.qp.support.DFAMatcher;
import com.ppwx.easysearch.qp.support.FoundWord;
import com.ppwx.easysearch.qp.support.WordMatchTree;
import com.ppwx.easysearch.qp.util.StrTrimUtil;
import org.springframework.core.Ordered;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className SynonymMatcher
 * @description 同义词匹配
 * @date 2024/11/1 13:59
 **/
public class SynonymMatcher {
    private static final String separate = ",";
    /**
     * 匹配词
     */
    private static final String WORD = "word";
    /**
     * 同义词
     */
    private static final String SYNONYM = "synonym";
    /**
     * 同义词路径
     */
    private final String sourcePath;
    /**
     * 词典匹配
     */
    private DFAMatcher matcher;
    /**
     * 词典hash映射
     */
    private Map<String, Synonym> hashMap;

    public SynonymMatcher(String sourcePath) {
        this.sourcePath = sourcePath;
        this.load();
    }

    private void load() {
        ArffSourceConvertor convertor = new ArffSourceConvertor(sourcePath);
        try {
            convertor.readData();
        } catch (IOException e) {
            throw new DataException(e);
        }

        WordMatchTree dicTree = new WordMatchTree();
        Map<String, Synonym> hashMap = Maps.newHashMapWithExpectedSize(convertor.getInstances().size());

        convertor.getInstances().forEach(arffInstance -> {
            String word = StrTrimUtil.trim((String) arffInstance.getValueByAttrName(WORD), CustomStopChar::isNotStopChar);
            String synonym = (String) arffInstance.getValueByAttrName(SYNONYM);
            dicTree.addWord(word);
            hashMap.put(word, new Synonym(synonym));
        });

        this.matcher = new DFAMatcher(sourcePath, dicTree, Ordered.HIGHEST_PRECEDENCE);
        this.hashMap = hashMap;
    }

    /**
     * @description 匹配同义词
     * @author ext.ahs.zhouchzh1@jd.com
     * @date 2024/11/1 14:58
     * @param str 匹配字符串
     * @return Map<FoundWord,Synonym>
     */
    public Map<FoundWord, Synonym> match(String str) {
        Map<FoundWord, Synonym> map = Maps.newLinkedHashMap();
        if (hashMap.containsKey(str)) {
            map.put(new FoundWord(str, str, 0, str.length() - 1), hashMap.get(str));
        } else {
            List<FoundWord> words = matcher.matchAll(str);
            for (FoundWord word : words) {
                if (hashMap.containsKey(word.getKeyWord())) {
                    map.put(word, hashMap.get(word.getKeyWord()));
                }
            }
        }
        return map;
    }

    /**
     * @description 匹配并替换
     * @author ext.ahs.zhouchzh1@jd.com
     * @date 2024/11/1 15:00
     * @param str 匹配字符串
     * @param splitChar  若存在多个同义词的分隔符
     * @return String
     */
    public String matchAndReplace(String str, String splitChar) {
        Map<FoundWord, Synonym> matched = match(str);
        if (matched.isEmpty()) {
            return str;
        }
        StringBuilder builder = new StringBuilder();
        Iterator<FoundWord> iterator = matched.keySet().iterator();
        FoundWord next = iterator.next();
        for (int i = 0; i < str.length(); i++) {
            if (i < next.getStartIndex()) {
                builder.append(str.charAt(i));
            } else if (i >= next.getStartIndex() && i <= next.getEndIndex()) {
                if (splitChar == null) {
                    builder.append(matched.get(next).getSynonym());
                } else {
                    builder.append(matched.get(next).getSynonym().replaceAll(separate, splitChar));
                }
                i = next.getEndIndex();
                if (iterator.hasNext()) {
                    next = iterator.next();
                }
            } else {
                builder.append(str.charAt(i));
            }
        }

        return builder.toString();
    }

    public static class Synonym {
        private final String synonym;

        public Synonym(String synonym) {
            this.synonym = synonym;
        }

        public String getSynonym() {
            return synonym;
        }

        public List<String> getSynonyms() {
            if (synonym != null && synonym.indexOf(separate) > 0) {
                return Lists.newArrayList(synonym.split(separate));
            } else if (synonym != null) {
                return Lists.newArrayList(synonym);
            }
            return Collections.emptyList();
        }

        @Override
        public String toString() {
            return "Synonym{" +
                    "synonym='" + synonym + '\'' +
                    '}';
        }
    }
}
