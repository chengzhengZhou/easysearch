package com.ppwx.easysearch.core.common.bloom;

/**
 * 布隆过滤器
 * 对hash计算进行了调整，降低hash计算次数
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2024/01/29 10:27
 * @since 1.0.0
 */
public interface BloomFilter {

    /**
     * Adds element
     *
     * @param val - element to add
     * @return <code>true</code> if element has been added successfully
     *         <code>false</code> if element is already present
     */
    boolean add(String val);

    /**
     * Check for element present
     *
     * @param val - element
     * @return <code>true</code> if element is present
     *         <code>false</code> if element is not present
     */
    boolean contains(String val);
    /**
     * Returns expected amount of insertions per element.
     * Calculated during bloom filter initialization.
     *
     * @return expected amount of insertions per element
     */
    int getExpectedInsertions();

    /**
     * Returns false probability of element presence.
     * Calculated during bloom filter initialization.
     *
     * @return false probability of element presence
     */
    double getFalseProbability();

    /**
     * Returns number of bits in Redis memory required by this instance
     *
     * @return number of bits
     */
    int getSize();

    /**
     * Returns hash iterations amount used per element.
     * Calculated during bloom filter initialization.
     *
     * @return hash iterations amount
     */
    int getHashIterations();

    /**
     * Calculates probabilistic number of elements already added to Bloom filter.
     *
     * @return probabilistic number of elements
     */
    int count();

    /**
     * Return whether inserted elements equals to expected insertions.
     *
     * @return boolean
     */
    boolean isFull();
}
