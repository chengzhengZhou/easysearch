package com.ppwx.easysearch.core.similarity;

import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className JaccardSimilarity
 * @description 杰卡德相似度
 * @date 2024/10/10 14:33
 **/
public class JaccardSimilarity implements WordSimilarityScore<Double>, SequenceSimilarityScore<Double> {

    private static final JaccardSimilarity INSTANCE = new JaccardSimilarity();

    @Override
    public Double apply(Collection<String> var1, Collection<String> var2) {
        if (var1 != null && var2 != null) {
            return this.calculateJaccardSimilarity(var1, var2);
        } else {
            throw new IllegalArgumentException("Input cannot be null");
        }
    }

    @Override
    public Double apply(CharSequence var1, CharSequence var2, Function<Character, Boolean> charFilter) {
        if (var1 != null && var2 != null) {
            return this.calculateJaccardSimilarity(var1, var2, charFilter);
        } else {
            throw new IllegalArgumentException("Input cannot be null");
        }
    }

    private Double calculateJaccardSimilarity(CharSequence left, CharSequence right, Function<Character, Boolean> charFilter) {
        int leftLength = left.length();
        int rightLength = right.length();
        if (leftLength == 0 && rightLength == 0) {
            return 1.0;
        } else if (leftLength != 0 && rightLength != 0) {
            Set<Character> leftSet = new HashSet<>(leftLength);

            for(int i = 0; i < leftLength; ++i) {
                if (charFilter == null || !charFilter.apply(left.charAt(i))) {
                    leftSet.add(left.charAt(i));
                }
            }

            Set<Character> rightSet = new HashSet<>(rightLength);

            for(int i = 0; i < rightLength; ++i) {
                if (charFilter == null || !charFilter.apply(right.charAt(i))) {
                    rightSet.add(right.charAt(i));
                }
            }

            Set<Character> unionSet = new HashSet<>(leftSet);
            unionSet.addAll(rightSet);
            int intersectionSize = leftSet.size() + rightSet.size() - unionSet.size();
            return (double) intersectionSize / (double)unionSet.size();
        } else {
            return 0.0;
        }
    }

    private Double calculateJaccardSimilarity(Collection<String> var1, Collection<String> var2) {
        int leftLength = var1.size();
        int rightLength = var2.size();
        if (leftLength == 0 && rightLength == 0) {
            return 1.0;
        } else if (leftLength != 0 && rightLength != 0) {
            Set<String> leftSet = new HashSet<>(var1);
            Set<String> rightSet = new HashSet<>(var2);
            Set<String> unionSet = Sets.union(leftSet, rightSet);
            int intersectionSize = leftSet.size() + rightSet.size() - unionSet.size();
            return (double) intersectionSize / (double)unionSet.size();
        } else {
            return 0.0;
        }
    }
}
