/**
 * Copyright (C), 2010-2024, 爱回收
 * FileName: MetaBloomFilter
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2024/1/29 10:35
 * Description: 布隆过滤器实现
 */
package com.ppwx.easysearch.core.common.bloom;

import com.google.common.base.Objects;
import com.google.common.hash.Funnel;
import com.google.common.hash.Funnels;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.ppwx.easysearch.core.util.CurrentTimeUtil;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;

/**
 *
 * 布隆过滤器实现
 * 每一个小bloomfilter块，分为meta区和data区，data区好理解，就是构成bloomfiter的bitmap，meta区作为小bloomfilter的描述信息，一般需要包括这几个信息：
 * magic 校验值，读取端和写入端对齐，防止内容被改写、损坏。（16字节）
 * count 个数，保存了多少个item。（4字节）
 * start_time 开始时间，即该bloomfilter块的创建时间。(8字节）
 * last_time 最后修改时间，每次更新bloomfilter块时更新该值。（8字节）
 * resv 保留区，前12字节用于存储bitSetSize、expectedInsertions、hashIterations（16字节）,剩余4个字节未用
 * 共52个字节
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2024/01/29 10:35
 * @since 1.0.0
 */
public class MetaBloomFilter implements BloomFilter {
    /**
     * 检验值，16字节
     */
    private static final int MAGIC_OFFSET = 0;
    /**
     * 个数，4字节
     */
    private static final int COUNT_OFFSET = MAGIC_OFFSET + 16;
    /**
     * meta end
     */
    private static final int META_END_OFFSET = 52;
    /**
     * 存储位字节
     */
    private final BitSet bitSet;
    /**
     * 元信息
     */
    private final Meta meta;
    /**
     * {@link Funnel}
     */
    private final Funnel<CharSequence> funnel = Funnels.stringFunnel(StandardCharsets.UTF_8);
    /**
     * 从元数据构造一个初始化的布隆过滤器
     *
     * @param meta
     * @return
     */
    private MetaBloomFilter(Meta meta) {
        this.meta = meta;
        this.bitSet = new BitSet(meta.bitSetSize);
    }

    /**
     * 从元数据和字节数据生成一个布隆过滤器
     *
     * @param meta
     * @param bytes
     * @return
     */
    private MetaBloomFilter(Meta meta, byte[] bytes) {
        this.meta = meta;
        this.bitSet = BitSet.valueOf(bytes);
    }

    /**
     * 从外部数据生成布隆过滤器
     *
     * @param bytes
     * @return com.ppwx.easysearch.core.common.bloom.MetaBloomFilter
     */
    public static MetaBloomFilter valueOf(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byte[] magicBytes = new byte[COUNT_OFFSET];
        byteBuffer.get(magicBytes, 0, magicBytes.length);

        HashCode hashCode = Hashing.md5().hashBytes(bytes, COUNT_OFFSET, bytes.length - COUNT_OFFSET);
        byte[] digest = hashCode.asBytes();
        if (!compareBytes(magicBytes, digest)) {
            throw new IllegalArgumentException("magic not match");
        }

        byteBuffer.position(COUNT_OFFSET);
        Meta meta = new Meta();
        meta.count = byteBuffer.getInt();
        meta.startTime = byteBuffer.getLong();
        meta.lastTime = byteBuffer.getLong();
        meta.bitSetSize = byteBuffer.getInt();
        meta.expectedInsertions = byteBuffer.getInt();
        meta.hashIterations = byteBuffer.getInt();

        byte[] bitSet = new byte[bytes.length - META_END_OFFSET];
        byteBuffer.position(META_END_OFFSET);
        byteBuffer.get(bitSet, 0, bitSet.length);
        return new MetaBloomFilter(meta, bitSet);
    }

    private static boolean compareBytes(byte[] magicBytes, byte[] digest) {
        if (magicBytes.length != digest.length) {
            return false;
        }
        for (int i = 0; i < magicBytes.length; i++) {
            int cmp = Byte.compare(magicBytes[i], digest[i]);
            if (cmp != 0)
                return false;
        }
        return true;
    }

    /**
     * 创建一个布隆过滤器
     * 该类型的过滤器不宜存储过大的数据，因为需要持久化
     *
     * @param expectedInsertions
     * @param falseProbability
     * @return
     */
    public MetaBloomFilter(int expectedInsertions, double falseProbability) {
        if (falseProbability > 1) {
            throw new IllegalArgumentException("Bloom filter false probability can't be greater than 1");
        }
        if (falseProbability < 0) {
            throw new IllegalArgumentException("Bloom filter false probability can't be negative");
        }
        int size = optimalNumOfBits(expectedInsertions, falseProbability);
        if (size == 0) {
            throw new IllegalArgumentException("Bloom filter calculated size is " + size);
        }
        if (size > getMaxSize()) {
            throw new IllegalArgumentException("Bloom filter size can't be greater than " + getMaxSize() + ". But calculated size is " + size);
        }
        int hashIterations = optimalNumOfHashFunctions(expectedInsertions, size);

        Meta meta = new Meta();
        meta.bitSetSize = size;
        meta.expectedInsertions = expectedInsertions;
        meta.hashIterations = hashIterations;

        long now = CurrentTimeUtil.currentTimeMillis();
        meta.startTime = now;
        meta.lastTime = now;
        this.meta = meta;
        this.bitSet = new BitSet(size);
    }

    protected int getMaxSize() {
        return Short.MAX_VALUE * 2;
    }

    private int optimalNumOfHashFunctions(long n, long m) {
        return Math.max(1, (int) Math.round((double) m / n * Math.log(2)));
    }

    private int optimalNumOfBits(int n, double p) {
        if (p == 0) {
            p = Integer.MIN_VALUE;
        }
        return (int) (-n * Math.log(p) / (Math.log(2) * Math.log(2)));
    }

    @Override
    public boolean add(String val) {
        int bitSize = meta.bitSetSize;
        long hash64 = Hashing.murmur3_128().hashObject(val, funnel).asLong();
        int hash1 = (int) hash64;
        int hash2 = (int) (hash64 >>> 32);

        boolean bitsChanged = false;
        for (int i = 1; i <= meta.hashIterations; i++) {
            int combinedHash = hash1 + (i * hash2);
            // Flip all the bits if it's negative (guaranteed positive number)
            if (combinedHash < 0) {
                combinedHash = ~combinedHash;
            }
            int position = combinedHash % bitSize;
            if (!bitSet.get(position)) {
                bitsChanged = true;
                bitSet.set(position);
            }
        }

        if (bitsChanged) {
            ++this.meta.count;
            this.meta.lastTime = CurrentTimeUtil.currentTimeMillis();
        }
        return bitsChanged;
    }

    @Override
    public boolean contains(String val) {
        int bitSize = meta.bitSetSize;
        long hash64 = Hashing.murmur3_128().hashObject(val, funnel).asLong();
        int hash1 = (int) hash64;
        int hash2 = (int) (hash64 >>> 32);

        for (int i = 1; i <= meta.hashIterations; i++) {
            int combinedHash = hash1 + (i * hash2);
            // Flip all the bits if it's negative (guaranteed positive number)
            if (combinedHash < 0) {
                combinedHash = ~combinedHash;
            }
            if (!bitSet.get(combinedHash % bitSize)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int getExpectedInsertions() {
        return this.meta.expectedInsertions;
    }

    @Override
    public double getFalseProbability() {
        // (1 - e^(-k * n / m)) ^ k
        return Math.pow((1 - Math.exp(-meta.hashIterations * (double) meta.expectedInsertions / meta.bitSetSize)), meta.hashIterations);
    }

    @Override
    public int getSize() {
        return this.meta.bitSetSize;
    }

    @Override
    public int getHashIterations() {
        return this.meta.hashIterations;
    }

    @Override
    public int count() {
        return this.meta.count;
    }

    @Override
    public boolean isFull() {
        return this.meta.count >= this.meta.expectedInsertions;
    }

    /**
     * 转换为字节输出
     *
     * @param
     * @return byte[]
     */
    public byte[] toByteArray() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(META_END_OFFSET + bitSet.size());
        byteBuffer.position(COUNT_OFFSET);
        byteBuffer.putInt(this.meta.count)
                .putLong(this.meta.startTime)
                .putLong(this.meta.lastTime)
                .putInt(this.meta.bitSetSize)
                .putInt(this.meta.expectedInsertions)
                .putInt(this.meta.hashIterations)
                // rest 4 bits fill zero
                .putInt(0)
                .put(bitSet.toByteArray());

        // magic
        byte[] digest = Hashing.md5()
                .hashBytes(byteBuffer.array(), COUNT_OFFSET, byteBuffer.capacity() - COUNT_OFFSET)
                .asBytes();

        byteBuffer.position(0);
        byteBuffer.put(digest);
        return byteBuffer.array();
    }

    public long getStartTime() {
        return meta.startTime;
    }

    public long getLastTime() {
        return meta.lastTime;
    }

    @Override
    protected Object clone() {
        Meta meta = new Meta();
        long now = CurrentTimeUtil.currentTimeMillis();
        meta.startTime = now;
        meta.lastTime = now;
        meta.bitSetSize = this.meta.bitSetSize;
        meta.expectedInsertions = this.meta.expectedInsertions;
        meta.hashIterations = this.meta.hashIterations;
        return new MetaBloomFilter(meta);
    }

    private static class Meta {
        /**
         * 计数
         */
        private int count;
        /**
         * 创建时间
         */
        private long startTime;
        /**
         * 修改时间
         */
        private long lastTime;

        /**
         * 字节大小
         */
        private int bitSetSize;
        /**
         * 预计包含的记录数
         */
        private int expectedInsertions;
        /**
         * hash函数个数
         */
        private int hashIterations;

        @Override
        public String toString() {
            return "Meta{" +
                    "count=" + count +
                    ", startTime=" + startTime +
                    ", lastTime=" + lastTime +
                    ", bitSetSize=" + bitSetSize +
                    ", expectedInsertions=" + expectedInsertions +
                    ", hashIterations=" + hashIterations +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "MetaBloomFilter{" +
                "meta=" + meta +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.meta, this.funnel, this.bitSet);
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        } else if (!(object instanceof MetaBloomFilter)) {
            return false;
        } else {
            MetaBloomFilter that = (MetaBloomFilter) object;
            return this.meta.expectedInsertions == that.meta.expectedInsertions
                    && this.meta.hashIterations == that.meta.hashIterations
                    && this.funnel.equals(that.funnel) && this.bitSet.equals(that.bitSet);
        }
    }
}