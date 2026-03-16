package com.ppwx.easysearch.qp.token;

import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.ppwx.easysearch.qp.support.FoundWord;
import com.ppwx.easysearch.qp.support.WordMatchTree;
import org.junit.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TokenHelperTest {

    @Test
    public void testDFA() {
        String text = "OPPO Reno 8Pro 5G版";
        WordMatchTree dicTree = new WordMatchTree();
        dicTree.addWords("iPhone15", "iPhone15Pro", "MacBook", "Air", "15年", "11寸", "Reno8","Pro","5G版");
        List<FoundWord> words = dicTree.matchAllWords(text);
        System.out.println(words);
    }

    @Test
    public void testJacrd() throws IOException {
        String text = "OPPO Reno8 Pro 5G版";
        List<String> segment = Splitter.on(" ").splitToList(text).stream().map(String::toLowerCase).collect(Collectors.toList());

        WordMatchTree dicTree = new WordMatchTree();
        dicTree.addWords("iphone15", "iphone15pro", "macbook", "air", "15年", "11寸", "reno8","pro","5g版");
        List<String> words = dicTree.matchAll("reno8pro");
        words.add("oppo");
        System.out.println(words);
        System.out.println(calculateJaccardSimilarity(words, segment));
    }

    /**
     * 杰卡德相关性计算
     * @param left
     * @param right
     * @return
     */
    private Double calculateJaccardSimilarity(List<String> left, List<String> right) {
        int leftLength = left.size();
        int rightLength = right.size();
        if (leftLength == 0 && rightLength == 0) {
            return 1.0;
        } else if (leftLength != 0 && rightLength != 0) {
            Set<String> leftSet = new HashSet<>(left);
            Set<String> rightSet = new HashSet<>(right);
            Set<String> unionSet = Sets.union(leftSet, rightSet);
            int intersectionSize = leftSet.size() + rightSet.size() - unionSet.size();
            return (double) intersectionSize / (double)unionSet.size();
        } else {
            return 0.0;
        }
    }

}
