package com.ppwx.easysearch.core.similarity;

import com.google.common.collect.Sets;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className CosineSimilarity
 * @description 余弦相似度
 * @date 2024/10/10 14:40
 **/
public class CosineSimilarity implements WordSimilarityScore<Double>, SequenceSimilarityScore<Double> {

    private static final CosineSimilarity INSTANCE = new CosineSimilarity();

    @Override
    public Double apply(Collection<String> var1, Collection<String> var2) {
        if (var1 != null && var2 != null) {
            Map<CharSequence, Long> leftVector = var1.stream().collect(Collectors.groupingBy(v -> v, Collectors.counting()));
            Map<CharSequence, Long> rightVector = var2.stream().collect(Collectors.groupingBy(v -> v, Collectors.counting()));
            return this.calculateCosineSimilarity(leftVector, rightVector);
        } else {
            throw new IllegalArgumentException("Input cannot be null");
        }
    }

    @Override
    public Double apply(CharSequence var1, CharSequence var2, Function<Character, Boolean> charFilter) {
        if (var1 != null && var2 != null) {
            Map<CharSequence, Long> leftVector = new HashMap<>(var1.length());
            for (int i = 0; i < var1.length(); i++) {
                if (charFilter == null || !charFilter.apply(var1.charAt(i))) {
                    leftVector.compute(String.valueOf(var1.charAt(i)), (k,v) -> v == null ? 1 : ++v);
                }
            }
            Map<CharSequence, Long> rightVector = new HashMap<>(var2.length());
            for (int i = 0; i < var2.length(); i++) {
                if (charFilter == null || !charFilter.apply(var2.charAt(i))) {
                    rightVector.compute(String.valueOf(var2.charAt(i)), (k,v) -> v == null ? 1 : ++v);
                }
            }
            return this.calculateCosineSimilarity(leftVector, rightVector);
        } else {
            throw new IllegalArgumentException("Input cannot be null");
        }
    }

    private Double calculateCosineSimilarity(Map<CharSequence, Long> leftVector, Map<CharSequence, Long> rightVector) {
        Set<CharSequence> intersection = this.getIntersection(leftVector, rightVector);
        double dotProduct = this.dot(leftVector, rightVector, intersection);
        double d1 = 0.0;

        Long value;
        for(Iterator<Long> var8 = leftVector.values().iterator(); var8.hasNext(); d1 += Math.pow((double)value, 2.0)) {
            value = var8.next();
        }

        double d2 = 0.0;

        for(Iterator<Long> var10 = rightVector.values().iterator(); var10.hasNext(); d2 += Math.pow((double)value, 2.0)) {
            value = var10.next();
        }

        double cosineSimilarity;
        if (!(d1 <= 0.0) && !(d2 <= 0.0)) {
            cosineSimilarity = dotProduct / (Math.sqrt(d1) * Math.sqrt(d2));
        } else {
            cosineSimilarity = 0.0;
        }

        return cosineSimilarity;
    }

    private double dot(Map<CharSequence, Long> leftVector, Map<CharSequence, Long> rightVector, Set<CharSequence> intersection) {
        long dotProduct = 0L;

        CharSequence key;
        for(Iterator<CharSequence> var6 = intersection.iterator(); var6.hasNext(); dotProduct += leftVector.get(key) * rightVector.get(key)) {
            key = var6.next();
        }

        return (double)dotProduct;
    }

    private Set<CharSequence> getIntersection(Map<CharSequence, Long> leftVector, Map<CharSequence, Long> rightVector) {
        Set<CharSequence> intersection = Sets.newHashSet(leftVector.keySet());
        intersection.retainAll(rightVector.keySet());
        return intersection;
    }
}
