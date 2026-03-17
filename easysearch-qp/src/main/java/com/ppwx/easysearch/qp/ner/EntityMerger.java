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

package com.ppwx.easysearch.qp.ner;

import java.util.*;

/**
 * 多识别器实体结果合并器
 * <p>
 * 合并 Dict 与 CRF 识别结果，根据 {@link MergeStrategy} 处理重叠与冲突：
 * <ul>
 *   <li>DICT_FIRST：以词典结果为主，CRF 与词典重叠的 span 被丢弃</li>
 *   <li>CRF_FIRST：以 CRF 结果为主，词典与 CRF 重叠的 span 被丢弃</li>
 * </ul>
 */
public final class EntityMerger {

    private final MergeStrategy strategy;

    public EntityMerger(MergeStrategy strategy) {
        this.strategy = strategy != null ? strategy : MergeStrategy.DICT_FIRST;
    }

    /**
     * 合并词典识别结果与 CRF 识别结果
     *
     * @param dictEntities 词典识别器结果
     * @param crfEntities  CRF 识别器结果
     * @return 合并去重后的实体列表（按 startOffset 排序）
     */
    public List<Entity> merge(Collection<Entity> dictEntities, Collection<Entity> crfEntities) {
        if (dictEntities == null && crfEntities == null) {
            return Collections.emptyList();
        }
        List<Entity> primary = strategy == MergeStrategy.DICT_FIRST
                ? toList(dictEntities)
                : toList(crfEntities);
        List<Entity> secondary = strategy == MergeStrategy.DICT_FIRST
                ? toList(crfEntities)
                : toList(dictEntities);

        List<Entity> result = new ArrayList<>(primary);

        for (Entity candidate : secondary) {
            if (!overlapsWithAny(candidate, result)) {
                result.add(candidate);
            }
        }

        result.sort(Comparator.comparingInt(Entity::getStartOffset).thenComparingInt(Entity::getEndOffset));
        return result;
    }

    private static List<Entity> toList(Collection<Entity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities instanceof List ? (List<Entity>) entities : new ArrayList<>(entities);
    }

    /**
     * 判断 entity 是否与 result 中任一实体存在 span 重叠
     */
    private static boolean overlapsWithAny(Entity entity, List<Entity> result) {
        for (Entity existing : result) {
            if (overlaps(entity, existing)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断两个实体 span 是否重叠
     * 重叠条件：start1 < end2 && start2 < end1
     */
    private static boolean overlaps(Entity a, Entity b) {
        if (a == null || b == null) {
            return false;
        }
        return a.getStartOffset() < b.getEndOffset() && b.getStartOffset() < a.getEndOffset();
    }
}
