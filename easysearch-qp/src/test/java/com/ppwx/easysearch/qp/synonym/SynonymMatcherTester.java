package com.ppwx.easysearch.qp.synonym;

import com.ppwx.easysearch.qp.support.FoundWord;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className SynonymMatcherTester
 * @description SynonymMatcher测试类
 * @date 2024/11/1 15:33
 **/
public class SynonymMatcherTester {

    @Test
    public void testSynonymMatcherWorks() {
        SynonymMatcher matcher = new SynonymMatcher("synonym/synonym_yx.arff");
        Map<FoundWord, SynonymMatcher.Synonym> matched = matcher.match("苹果 12");
        Map<String, List<String>> map = matched.keySet().stream()
                .collect(Collectors.toMap(FoundWord::getKeyWord, k -> {
                    SynonymMatcher.Synonym synonym = matched.get(k);
                    return synonym.getSynonyms();
                }));
        System.out.println(map);

        String re = matcher.matchAndReplace("我要 苹果15和苹果16 还有折叠手机  可以吗", null);
        System.out.println(re);
    }


}
