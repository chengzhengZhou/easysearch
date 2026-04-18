/*
 * Copyright 2026 chengzhengZhou
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ppwx.easysearch.qp.suggestion;

import java.util.*;

/**
 * 支持高效前缀子树遍历的泛型 Trie 数据结构。
 * <p>
 * 核心能力：通过逐字符 transition 走到前缀节点后，DFS 遍历子树收集所有匹配值，
 * 配合小顶堆保留按指定 {@link Comparator} 排序的 Top-N 结果。
 * <p>
 * 复杂度：
 * <ul>
 *   <li>{@code put} / {@code get}：O(key.length)</li>
 *   <li>{@code prefixSearch}：O(prefix.length + M·log(limit))，M 为匹配数</li>
 *   <li>{@code collectAllValues}：O(总节点数)</li>
 * </ul>
 * <p>
 * 线程安全说明：本类不是线程安全的。预期使用方式为"构建完成后只读"，
 * 通过 {@code volatile} 引用原子替换来保证可见性。
 *
 * @param <V> 值类型
 */
public class PrefixTrie<V> {

    /**
     * Trie 节点。
     *
     * @param <V> 值类型
     */
    static class Node<V> {
        /** 节点关联的值，null 表示非终止节点 */
        V value;

        /** 子节点映射（字符 → 子节点），初始容量 4 以节省中文场景下的内存 */
        final Map<Character, Node<V>> children = new HashMap<>(4);
    }

    private final Node<V> root = new Node<>();
    private int size;

    /**
     * 插入键值对。如果 key 已存在，覆盖其值。
     *
     * @param key   键（不能为 null 或空）
     * @param value 值（不能为 null）
     */
    public void put(String key, V value) {
        if (key == null || key.isEmpty() || value == null) {
            return;
        }
        Node<V> current = root;
        for (int i = 0; i < key.length(); i++) {
            current = current.children.computeIfAbsent(key.charAt(i), c -> new Node<>());
        }
        if (current.value == null) {
            size++;
        }
        current.value = value;
    }

    /**
     * 精确查找。
     *
     * @param key 键
     * @return 对应的值，不存在则返回 null
     */
    public V get(String key) {
        Node<V> node = prefixNode(key);
        return node != null ? node.value : null;
    }

    /**
     * 前缀搜索：返回以 prefix 开头的所有值中，按 comparator "最大"的 Top-N。
     * <p>
     * 实现：走到前缀节点后 DFS 遍历子树，用容量为 limit 的小顶堆实时维护 Top-N。
     * 小顶堆使用传入的 comparator，堆顶是"最小"的元素，当堆满时淘汰堆顶。
     * 最终结果按 comparator 降序排列（即"最大"在前）。
     *
     * @param prefix     前缀（null 或空返回空列表）
     * @param limit      最大返回条数
     * @param comparator 排序比较器（定义"大小"关系，堆顶为最小）
     * @return 按 comparator 降序排列的结果列表
     */
    public List<V> prefixSearch(String prefix, int limit, Comparator<V> comparator) {
        if (prefix == null || prefix.isEmpty() || limit <= 0) {
            return Collections.emptyList();
        }
        Node<V> node = prefixNode(prefix);
        if (node == null) {
            return Collections.emptyList();
        }

        // 避免 limit + 1 整数溢出（Integer.MAX_VALUE + 1 = 负数）
        int initialCapacity = (limit < Integer.MAX_VALUE) ? Math.min(limit + 1, 16) : 16;
        PriorityQueue<V> heap = new PriorityQueue<>(initialCapacity, comparator);
        dfsCollectTopN(node, heap, limit);

        List<V> result = new ArrayList<>(heap.size());
        while (!heap.isEmpty()) {
            result.add(heap.poll());
        }
        // 小顶堆 poll 出来是升序，反转为降序
        Collections.reverse(result);
        return result;
    }

    /**
     * 收集 Trie 中所有的值。
     *
     * @return 所有值的列表（无特定顺序）
     */
    public List<V> collectAllValues() {
        List<V> result = new ArrayList<>(size);
        dfsCollectAll(root, result);
        return result;
    }

    /**
     * 返回已存储的键值对数量。
     */
    public int size() {
        return size;
    }

    /**
     * 走到 prefix 对应的节点。
     *
     * @param prefix 前缀字符串
     * @return prefix 末尾字符对应的节点，不存在返回 null
     */
    private Node<V> prefixNode(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return null;
        }
        Node<V> current = root;
        for (int i = 0; i < prefix.length(); i++) {
            current = current.children.get(prefix.charAt(i));
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    /**
     * DFS 遍历子树，使用小顶堆收集 Top-N 值。
     */
    private void dfsCollectTopN(Node<V> node, PriorityQueue<V> heap, int limit) {
        if (node.value != null) {
            heap.offer(node.value);
            if (heap.size() > limit) {
                heap.poll();
            }
        }
        for (Node<V> child : node.children.values()) {
            dfsCollectTopN(child, heap, limit);
        }
    }

    /**
     * DFS 遍历子树，收集所有值。
     */
    private void dfsCollectAll(Node<V> node, List<V> result) {
        if (node.value != null) {
            result.add(node.value);
        }
        for (Node<V> child : node.children.values()) {
            dfsCollectAll(child, result);
        }
    }
}
