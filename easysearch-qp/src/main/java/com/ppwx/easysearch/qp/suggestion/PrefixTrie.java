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
import java.util.function.Consumer;
import java.util.function.ToLongFunction;

/**
 * 支持高效前缀子树遍历的泛型 Trie 数据结构，带剪枝优化。
 * <p>
 * 核心能力：通过逐字符 transition 走到前缀节点后，DFS 遍历子树收集所有匹配值，
 * 配合小顶堆保留按指定 {@link Comparator} 排序的 Top-N 结果。
 * <p>
 * <b>剪枝优化</b>：
 * <ul>
 *   <li>每个节点预存子树最大权重 {@code maxWeight}，DFS 时跳过无效子树</li>
 *   <li>子节点按 {@code maxWeight} 降序排列，优先遍历高权重子树，使堆更快填满</li>
 * </ul>
 * <p>
 * 复杂度：
 * <ul>
 *   <li>{@code put} / {@code get}：O(key.length)</li>
 *   <li>{@code prefixSearch}：O(prefix.length + K·log(limit))，K 为有效匹配数（剪枝后远小于总匹配数）</li>
 *   <li>{@code collectAllValues}：O(总节点数)</li>
 *   <li>{@code buildMaxWeight}：O(总节点数)，构建完成后调用一次</li>
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

        /** 子树最大权重，用于剪枝优化（方案一） */
        long maxWeight = Long.MIN_VALUE;

        /** 按 maxWeight 降序排列的子节点数组，用于优先遍历高权重子树（方案二） */
        Node<V>[] sortedChildren;
    }

    private final Node<V> root = new Node<>();
    private int size;

    /** 权重提取器，用于剪枝优化（buildMaxWeight 时设置） */
    private ToLongFunction<V> weightExtractor;

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

    // ==================== 方案一：maxWeight 剪枝优化 ====================

    /**
     * 构建子树最大权重索引，启用剪枝优化（方案一 + 方案二）。
     * <p>
     * 在 put 完成后调用一次，后序遍历计算每个节点的 maxWeight，
     * 并按 maxWeight 降序构建子节点排序数组。
     *
     * @param weightExtractor 从值 V 中提取权重的函数
     */
    public void buildMaxWeight(ToLongFunction<V> weightExtractor) {
        this.weightExtractor = weightExtractor;
        computeMaxWeight(root, weightExtractor);
        buildSortedChildren(root);
    }

    /**
     * 后序遍历计算每个节点的子树最大权重。
     */
    private long computeMaxWeight(Node<V> node, ToLongFunction<V> weightExtractor) {
        long max = (node.value != null) ? weightExtractor.applyAsLong(node.value) : Long.MIN_VALUE;
        for (Node<V> child : node.children.values()) {
            max = Math.max(max, computeMaxWeight(child, weightExtractor));
        }
        node.maxWeight = max;
        return max;
    }

    // ==================== 方案二：子节点按 maxWeight 排序 ====================

    /**
     * 构建按 maxWeight 降序排列的子节点数组。
     * 在 computeMaxWeight 完成后调用。
     */
    @SuppressWarnings("unchecked")
    private void buildSortedChildren(Node<V> node) {
        if (!node.children.isEmpty()) {
            List<Node<V>> childList = new ArrayList<>(node.children.values());
            childList.sort((a, b) -> Long.compare(b.maxWeight, a.maxWeight));
            node.sortedChildren = childList.toArray(new Node[0]);
            for (Node<V> child : node.sortedChildren) {
                buildSortedChildren(child);
            }
        }
    }

    // ==================== 方案三：prefixVisit 遍历接口 ====================

    /**
     * 遍历前缀子树的所有值，通过 Consumer 回调处理。
     * <p>
     * 适用于拼音 Trie（值类型为 List），避免创建中间 List 收集所有匹配项。
     * 支持通过 maxWeight 提前终止无效子树遍历。
     *
     * @param prefix  前缀字符串
     * @param visitor 值处理回调
     */
    public void prefixVisit(String prefix, Consumer<V> visitor) {
        if (prefix == null || prefix.isEmpty() || visitor == null) {
            return;
        }
        Node<V> node = prefixNode(prefix);
        if (node == null) {
            return;
        }
        dfsVisit(node, visitor);
    }

    /**
     * 带剪枝的前缀遍历，支持通过 PruneContext 提前终止无效子树。
     * <p>
     * PruneContext 提供当前堆顶权重阈值，当子树 maxWeight <= 阈值时跳过该子树。
     *
     * @param prefix  前缀字符串
     * @param visitor 值处理回调
     * @param context 剪枝上下文，提供动态权重阈值
     */
    public void prefixVisitWithPrune(String prefix, Consumer<V> visitor, PruneContext context) {
        if (prefix == null || prefix.isEmpty() || visitor == null) {
            return;
        }
        Node<V> node = prefixNode(prefix);
        if (node == null) {
            return;
        }
        dfsVisitWithPrune(node, visitor, context);
    }

    /**
     * 剪枝上下文接口，提供动态权重阈值用于剪枝判断。
     */
    public interface PruneContext {
        /**
         * 获取当前的权重阈值（通常是堆顶元素的权重）。
         * 当子树 maxWeight <= 此阈值时，可以跳过整棵子树。
         *
         * @return 当前权重阈值，返回 Long.MIN_VALUE 表示不剪枝
         */
        long getThreshold();

        /**
         * 堆是否已满（已达到 limit）。
         * 只有堆满时才需要剪枝。
         *
         * @return 是否已满
         */
        boolean isFull();
    }

    private void dfsVisit(Node<V> node, Consumer<V> visitor) {
        if (node.value != null) {
            visitor.accept(node.value);
        }
        // 优先使用排序后的子节点数组（方案二）
        if (node.sortedChildren != null) {
            for (Node<V> child : node.sortedChildren) {
                dfsVisit(child, visitor);
            }
        } else {
            for (Node<V> child : node.children.values()) {
                dfsVisit(child, visitor);
            }
        }
    }

    private void dfsVisitWithPrune(Node<V> node, Consumer<V> visitor, PruneContext context) {
        // 方案一：剪枝 - 堆已满且子树最大权重 <= 阈值时跳过
        if (context.isFull() && node.maxWeight <= context.getThreshold()) {
            return;
        }
        if (node.value != null) {
            visitor.accept(node.value);
        }
        // 方案二：优先遍历高权重子树
        if (node.sortedChildren != null) {
            for (Node<V> child : node.sortedChildren) {
                dfsVisitWithPrune(child, visitor, context);
            }
        } else {
            for (Node<V> child : node.children.values()) {
                dfsVisitWithPrune(child, visitor, context);
            }
        }
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
     * <p>
     * 优化：
     * <ul>
     *   <li>方案一：当堆已满且子树 maxWeight <= 堆顶权重时，跳过整棵子树</li>
     *   <li>方案二：优先遍历 maxWeight 大的子树，使堆更快填满高权重值</li>
     * </ul>
     */
    private void dfsCollectTopN(Node<V> node, PriorityQueue<V> heap, int limit) {
        // 方案一：剪枝优化 - 堆已满且子树最大权重不超过堆顶时跳过
        if (weightExtractor != null && heap.size() >= limit && !heap.isEmpty()) {
            long heapTopWeight = weightExtractor.applyAsLong(heap.peek());
            if (node.maxWeight <= heapTopWeight) {
                return;
            }
        }

        if (node.value != null) {
            heap.offer(node.value);
            if (heap.size() > limit) {
                heap.poll();
            }
        }

        // 方案二：优先使用按 maxWeight 降序排列的子节点数组
        if (node.sortedChildren != null) {
            for (Node<V> child : node.sortedChildren) {
                dfsCollectTopN(child, heap, limit);
            }
        } else {
            for (Node<V> child : node.children.values()) {
                dfsCollectTopN(child, heap, limit);
            }
        }
    }

    /**
     * DFS 遍历子树，收集所有值。
     */
    private void dfsCollectAll(Node<V> node, List<V> result) {
        if (node.value != null) {
            result.add(node.value);
        }
        // 优先使用排序后的子节点数组
        if (node.sortedChildren != null) {
            for (Node<V> child : node.sortedChildren) {
                dfsCollectAll(child, result);
            }
        } else {
            for (Node<V> child : node.children.values()) {
                dfsCollectAll(child, result);
            }
        }
    }
}
