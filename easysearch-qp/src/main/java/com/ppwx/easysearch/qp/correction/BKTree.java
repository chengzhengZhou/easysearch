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

package com.ppwx.easysearch.qp.correction;

import java.util.*;

/**
 * BK-Tree（Burkhard-Keller Tree），基于编辑距离的近似字符串搜索数据结构。
 * <p>
 * 将词典构建为 BK-Tree 后，查找编辑距离 ≤ k 的候选词的时间复杂度为 O(log N)，
 * 相比暴力遍历的 O(N) 显著提升。
 * <p>
 * 线程安全：构建完成后不可变，可安全并发读取。
 *
 * @param <T> 词条类型，需通过 DistanceFunction 计算距离
 */
public class BKTree<T> {

    /**
     * 距离函数接口。
     */
    public interface DistanceFunction<T> {
        int distance(T a, T b);
    }

    /** 树节点 */
    private static class Node<T> {
        final T item;
        final Map<Integer, Node<T>> children = new HashMap<>();

        Node(T item) {
            this.item = item;
        }
    }

    private final DistanceFunction<T> distanceFunction;
    private Node<T> root;
    private int size;

    public BKTree(DistanceFunction<T> distanceFunction) {
        this.distanceFunction = distanceFunction;
    }

    /**
     * 构建索引：清空已有数据并批量插入。
     *
     * @param items 待索引的词条集合
     */
    public void build(Collection<T> items) {
        this.root = null;
        this.size = 0;
        for (T item : items) {
            insert(item);
        }
    }

    private void insert(T item) {
        if (root == null) {
            root = new Node<>(item);
            size = 1;
            return;
        }

        Node<T> current = root;
        while (true) {
            int d = distanceFunction.distance(current.item, item);
            if (d == 0) {
                // 完全相同的项不重复插入
                return;
            }
            Node<T> child = current.children.get(d);
            if (child == null) {
                current.children.put(d, new Node<>(item));
                size++;
                return;
            }
            current = child;
        }
    }

    /**
     * 搜索编辑距离 ≤ maxDistance 的所有候选。
     *
     * @param query         查询词
     * @param maxDistance    最大编辑距离
     * @return 匹配的候选列表，不为 null
     */
    public List<T> search(T query, int maxDistance) {
        if (root == null) {
            return Collections.emptyList();
        }

        List<T> result = new ArrayList<>();
        Deque<Node<T>> stack = new ArrayDeque<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            Node<T> node = stack.pop();
            int d = distanceFunction.distance(node.item, query);

            if (d <= maxDistance) {
                result.add(node.item);
            }

            // BK-Tree 剪枝：只需搜索距离在 [d - maxDistance, d + maxDistance] 范围内的子树
            int low = d - maxDistance;
            int high = d + maxDistance;
            for (Map.Entry<Integer, Node<T>> entry : node.children.entrySet()) {
                int edge = entry.getKey();
                if (edge >= low && edge <= high) {
                    stack.push(entry.getValue());
                }
            }
        }

        return result;
    }

    /** 索引中的词条数量 */
    public int size() {
        return size;
    }

    /** 索引是否为空 */
    public boolean isEmpty() {
        return root == null;
    }
}
