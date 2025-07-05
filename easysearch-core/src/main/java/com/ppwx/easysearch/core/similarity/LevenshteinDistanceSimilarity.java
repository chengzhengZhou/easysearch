package com.ppwx.easysearch.core.similarity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.function.Function;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className LevenshteinDistance
 * @description 莱文斯坦距离(Levenshtein distance)算法计算相似度
 * @date 2024/10/12 15:42
 **/
public class LevenshteinDistanceSimilarity implements SequenceSimilarityScore<Double> {

    private static final LevenshteinDistanceSimilarity INSTANCE = new LevenshteinDistanceSimilarity();
    private final Integer threshold;

    public static LevenshteinDistanceSimilarity getDefaultInstance() {
        return INSTANCE;
    }

    private static int limitedCompare(CharSequence left, CharSequence right, int threshold) {
        if (left != null && right != null) {
            if (threshold < 0) {
                throw new IllegalArgumentException("Threshold must not be negative");
            } else {
                int n = left.length();
                int m = right.length();
                if (n == 0) {
                    return m <= threshold ? m : -1;
                } else if (m == 0) {
                    return n <= threshold ? n : -1;
                } else {
                    if (n > m) {
                        CharSequence tmp = left;
                        left = right;
                        right = tmp;
                        n = m;
                        m = tmp.length();
                    }

                    if (m - n > threshold) {
                        return -1;
                    } else {
                        int[] p = new int[n + 1];
                        int[] d = new int[n + 1];
                        int boundary = Math.min(n, threshold) + 1;

                        int j;
                        for(j = 0; j < boundary; p[j] = j++) {
                        }

                        Arrays.fill(p, boundary, p.length, Integer.MAX_VALUE);
                        Arrays.fill(d, Integer.MAX_VALUE);

                        for(j = 1; j <= m; ++j) {
                            char rightJ = right.charAt(j - 1);
                            d[0] = j;
                            int min = Math.max(1, j - threshold);
                            int max = j > Integer.MAX_VALUE - threshold ? n : Math.min(n, j + threshold);
                            if (min > 1) {
                                d[min - 1] = Integer.MAX_VALUE;
                            }

                            int lowerBound = Integer.MAX_VALUE;

                            for(int i = min; i <= max; ++i) {
                                if (left.charAt(i - 1) == rightJ) {
                                    d[i] = p[i - 1];
                                } else {
                                    d[i] = 1 + Math.min(Math.min(d[i - 1], p[i]), p[i - 1]);
                                }

                                lowerBound = Math.min(lowerBound, d[i]);
                            }

                            if (lowerBound > threshold) {
                                return -1;
                            }

                            int[] tempD = p;
                            p = d;
                            d = tempD;
                        }

                        if (p[n] <= threshold) {
                            return p[n];
                        } else {
                            return -1;
                        }
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("CharSequences must not be null");
        }
    }

    private static int unlimitedCompare(CharSequence left, CharSequence right) {
        if (left != null && right != null) {
            int n = left.length();
            int m = right.length();
            if (n == 0) {
                return m;
            } else if (m == 0) {
                return n;
            } else {
                if (n > m) {
                    CharSequence tmp = left;
                    left = right;
                    right = tmp;
                    n = m;
                    m = tmp.length();
                }

                int[] p = new int[n + 1];

                int i;
                for(i = 0; i <= n; p[i] = i++) {
                }

                for(int j = 1; j <= m; ++j) {
                    int upperLeft = p[0];
                    char rightJ = right.charAt(j - 1);
                    p[0] = j;

                    for(i = 1; i <= n; ++i) {
                        int upper = p[i];
                        int cost = left.charAt(i - 1) == rightJ ? 0 : 1;
                        p[i] = Math.min(Math.min(p[i - 1] + 1, p[i] + 1), upperLeft + cost);
                        upperLeft = upper;
                    }
                }

                return p[n];
            }
        } else {
            throw new IllegalArgumentException("CharSequences must not be null");
        }
    }

    public LevenshteinDistanceSimilarity() {
        this((Integer)null);
    }

    public LevenshteinDistanceSimilarity(Integer threshold) {
        if (threshold != null && threshold < 0) {
            throw new IllegalArgumentException("Threshold must not be negative");
        } else {
            this.threshold = threshold;
        }
    }

    @Override
    public Double apply(CharSequence left, CharSequence right, Function<Character, Boolean> charFilter) {
        // 用较大的字符串长度作为分母，相似子串作为分子计算出字串相似度
        int temp = Math.max(left.length(), right.length());
        if(0 == temp) {
            // 两个都是空串相似度为1，被认为是相同的串
            return 1.0;
        }

        if (charFilter != null) {
            left = removeSign(left, charFilter);
            right = removeSign(right, charFilter);
            temp = Math.max(left.length(), right.length());
        }

        int distance = this.threshold != null ? limitedCompare(left, right, this.threshold) : unlimitedCompare(left, right);
        return BigDecimal.valueOf(temp - distance).divide(BigDecimal.valueOf(temp), 10, RoundingMode.HALF_UP).doubleValue();
    }

    public Integer getThreshold() {
        return this.threshold;
    }

    /**
     * 将字符串的所有数据依次写成一行，去除无意义字符串
     *
     * @param str 字符串
     * @return 处理后的字符串
     */
    private static CharSequence removeSign(CharSequence str, Function<Character, Boolean> charFilter) {
        int length = str.length();
        StringBuilder sb = new StringBuilder(length);
        // 遍历字符串str,如果是汉字数字或字母，则追加到ab上面
        char c;
        for (int i = 0; i < length; i++) {
            c = str.charAt(i);
            if(!charFilter.apply(c)) {
                sb.append(c);
            }
        }

        return sb.toString();
    }
}
