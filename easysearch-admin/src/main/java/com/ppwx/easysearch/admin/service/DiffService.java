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

package com.ppwx.easysearch.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ppwx.easysearch.admin.domain.enums.InterventionMode;
import com.ppwx.easysearch.admin.domain.enums.RuleModule;
import com.ppwx.easysearch.admin.domain.exception.BizException;
import com.ppwx.easysearch.admin.domain.model.*;
import com.ppwx.easysearch.admin.mapper.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DiffService {
    private final InterventionSentenceRuleMapper sentenceRuleMapper;
    private final InterventionTermRuleMapper termRuleMapper;
    private final SynonymRuleMapper synonymRuleMapper;
    private final EntityRuleMapper entityRuleMapper;
    private final TokenDictRuleMapper tokenDictRuleMapper;
    private final SnapshotInterventionSentenceMapper snapshotSentenceMapper;
    private final SnapshotInterventionTermMapper snapshotTermMapper;
    private final SnapshotSynonymMapper snapshotSynonymMapper;
    private final SnapshotEntityMapper snapshotEntityMapper;
    private final SnapshotTokenDictMapper snapshotTokenDictMapper;
    private final ResourceSetMapper resourceSetMapper;

    public DiffService(InterventionSentenceRuleMapper sentenceRuleMapper,
                       InterventionTermRuleMapper termRuleMapper,
                       SynonymRuleMapper synonymRuleMapper,
                       EntityRuleMapper entityRuleMapper,
                       TokenDictRuleMapper tokenDictRuleMapper,
                       SnapshotInterventionSentenceMapper snapshotSentenceMapper,
                       SnapshotInterventionTermMapper snapshotTermMapper,
                       SnapshotSynonymMapper snapshotSynonymMapper,
                       SnapshotEntityMapper snapshotEntityMapper,
                       SnapshotTokenDictMapper snapshotTokenDictMapper,
                       ResourceSetMapper resourceSetMapper) {
        this.sentenceRuleMapper = sentenceRuleMapper;
        this.termRuleMapper = termRuleMapper;
        this.synonymRuleMapper = synonymRuleMapper;
        this.entityRuleMapper = entityRuleMapper;
        this.tokenDictRuleMapper = tokenDictRuleMapper;
        this.snapshotSentenceMapper = snapshotSentenceMapper;
        this.snapshotTermMapper = snapshotTermMapper;
        this.snapshotSynonymMapper = snapshotSynonymMapper;
        this.snapshotEntityMapper = snapshotEntityMapper;
        this.snapshotTokenDictMapper = snapshotTokenDictMapper;
        this.resourceSetMapper = resourceSetMapper;
    }

    public DiffResult diff(Long currentResourceSetId, Long baseSnapshotId, RuleModule module, InterventionMode mode) {
        if (module == RuleModule.intervention) {
            InterventionMode m = mode == null ? InterventionMode.sentence : mode;
            if (m == InterventionMode.sentence) {
                List<InterventionSentenceRuleDO> cur = sentenceRuleMapper.selectList(new LambdaQueryWrapper<InterventionSentenceRuleDO>().eq(InterventionSentenceRuleDO::getResourceSetId, currentResourceSetId));
                List<InterventionSentenceRuleDO> base = new ArrayList<>();
                if (baseSnapshotId != null) {
                    List<SnapshotInterventionSentenceDO> snapRules = snapshotSentenceMapper.selectList(
                            new LambdaQueryWrapper<SnapshotInterventionSentenceDO>().eq(SnapshotInterventionSentenceDO::getSnapshotId, baseSnapshotId));
                    for (SnapshotInterventionSentenceDO s : snapRules) {
                        InterventionSentenceRuleDO r = new InterventionSentenceRuleDO();
                        r.setId(s.getSourceRuleId());
                        r.setSourceText(s.getSourceText());
                        r.setTargetText(s.getTargetText());
                        r.setMatchType(s.getMatchType());
                        r.setPriority(s.getPriority());
                        r.setEnabled(s.getEnabled());
                        r.setRemark(s.getRemark());
                        base.add(r);
                    }
                }
                return diffSentenceByKey(cur, base);
            }
            List<InterventionTermRuleDO> cur = termRuleMapper.selectList(new LambdaQueryWrapper<InterventionTermRuleDO>().eq(InterventionTermRuleDO::getResourceSetId, currentResourceSetId));
            List<InterventionTermRuleDO> base = new ArrayList<>();
            if (baseSnapshotId != null) {
                List<SnapshotInterventionTermDO> snapRules = snapshotTermMapper.selectList(
                        new LambdaQueryWrapper<SnapshotInterventionTermDO>().eq(SnapshotInterventionTermDO::getSnapshotId, baseSnapshotId));
                for (SnapshotInterventionTermDO s : snapRules) {
                    InterventionTermRuleDO r = new InterventionTermRuleDO();
                    r.setId(s.getSourceRuleId());
                    r.setSourceText(s.getSourceText());
                    r.setTargetText(s.getTargetText());
                    r.setPriority(s.getPriority());
                    r.setEnabled(s.getEnabled());
                    r.setRemark(s.getRemark());
                    base.add(r);
                }
            }
            return diffTermByKey(cur, base);
        }
        if (module == RuleModule.synonym) {
            List<SynonymRuleDO> cur = synonymRuleMapper.selectList(new LambdaQueryWrapper<SynonymRuleDO>().eq(SynonymRuleDO::getResourceSetId, currentResourceSetId));
            List<SynonymRuleDO> base = new ArrayList<>();
            return diffByKey(cur, base, r -> safe(r.getSourceText()) + "|" + safe(r.getDirection()));
        }
        if (module == RuleModule.entity) {
            List<EntityRuleDO> cur = entityRuleMapper.selectList(new LambdaQueryWrapper<EntityRuleDO>().eq(EntityRuleDO::getResourceSetId, currentResourceSetId));
            List<EntityRuleDO> base = new ArrayList<>();
            return diffByKey(cur, base, r -> safe(r.getEntityText()) + "|" + safe(r.getEntityType()) + "|" + safe(r.getNormalizedValue()));
        }
        if (module == RuleModule.token) {
            List<TokenDictRuleDO> cur = tokenDictRuleMapper.selectList(new LambdaQueryWrapper<TokenDictRuleDO>().eq(TokenDictRuleDO::getResourceSetId, currentResourceSetId));
            List<TokenDictRuleDO> base = new ArrayList<>();
            return diffByKey(cur, base, r -> safe(r.getWord()));
        }
        throw new BizException(400, "module not supported");
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private interface KeyFn<T> {
        String key(T t);
    }

    private <T> DiffResult diffByKey(List<T> current, List<T> base, KeyFn<T> keyFn) {
        Map<String, T> curMap = toMap(current, keyFn);
        Map<String, T> baseMap = toMap(base, keyFn);

        List<Object> added = new ArrayList<>();
        List<Object> deleted = new ArrayList<>();
        List<ModifiedPair> modified = new ArrayList<>();

        for (Map.Entry<String, T> e : curMap.entrySet()) {
            if (!baseMap.containsKey(e.getKey())) {
                added.add(e.getValue());
            } else {
                T b = baseMap.get(e.getKey());
                if (!Objects.equals(e.getValue(), b)) {
                    modified.add(ModifiedPair.of(e.getKey(), b, e.getValue()));
                }
            }
        }
        for (Map.Entry<String, T> e : baseMap.entrySet()) {
            if (!curMap.containsKey(e.getKey())) {
                deleted.add(e.getValue());
            }
        }
        return DiffResult.of(added, deleted, modified);
    }

    private <T> Map<String, T> toMap(List<T> list, KeyFn<T> keyFn) {
        Map<String, T> m = new LinkedHashMap<>();
        for (T t : list) {
            String k = keyFn.key(t);
            if (!m.containsKey(k)) m.put(k, t);
        }
        return m;
    }

    /**
     * 整句规则的字段级比较
     */
    private DiffResult diffSentenceByKey(List<InterventionSentenceRuleDO> current, List<InterventionSentenceRuleDO> base) {
        Map<Long, InterventionSentenceRuleDO> curMap = new LinkedHashMap<>();
        for (InterventionSentenceRuleDO r : current) {
            if (r.getId() != null) curMap.put(r.getId(), r);
        }
        Map<Long, InterventionSentenceRuleDO> baseMap = new LinkedHashMap<>();
        for (InterventionSentenceRuleDO r : base) {
            if (r.getId() != null) baseMap.put(r.getId(), r);
        }

        List<Object> added = new ArrayList<>();
        List<Object> deleted = new ArrayList<>();
        List<ModifiedPair> modified = new ArrayList<>();

        for (Map.Entry<Long, InterventionSentenceRuleDO> e : curMap.entrySet()) {
            if (!baseMap.containsKey(e.getKey())) {
                added.add(e.getValue());
            } else {
                InterventionSentenceRuleDO c = e.getValue();
                InterventionSentenceRuleDO b = baseMap.get(e.getKey());
                if (!sentenceFieldsEqual(c, b)) {
                    modified.add(ModifiedPair.of(String.valueOf(e.getKey()), b, c));
                }
            }
        }
        for (Map.Entry<Long, InterventionSentenceRuleDO> e : baseMap.entrySet()) {
            if (!curMap.containsKey(e.getKey())) {
                deleted.add(e.getValue());
            }
        }
        return DiffResult.of(added, deleted, modified);
    }

    /**
     * 比较整句规则的业务字段是否相同
     */
    private boolean sentenceFieldsEqual(InterventionSentenceRuleDO a, InterventionSentenceRuleDO b) {
        return Objects.equals(a.getSourceText(), b.getSourceText())
                && Objects.equals(a.getTargetText(), b.getTargetText())
                && Objects.equals(a.getMatchType(), b.getMatchType())
                && Objects.equals(a.getPriority(), b.getPriority())
                && Objects.equals(a.getEnabled(), b.getEnabled())
                && Objects.equals(a.getRemark(), b.getRemark());
    }

    /**
     * 词表规则的字段级比较
     */
    private DiffResult diffTermByKey(List<InterventionTermRuleDO> current, List<InterventionTermRuleDO> base) {
        Map<Long, InterventionTermRuleDO> curMap = new LinkedHashMap<>();
        for (InterventionTermRuleDO r : current) {
            if (r.getId() != null) curMap.put(r.getId(), r);
        }
        Map<Long, InterventionTermRuleDO> baseMap = new LinkedHashMap<>();
        for (InterventionTermRuleDO r : base) {
            if (r.getId() != null) baseMap.put(r.getId(), r);
        }

        List<Object> added = new ArrayList<>();
        List<Object> deleted = new ArrayList<>();
        List<ModifiedPair> modified = new ArrayList<>();

        for (Map.Entry<Long, InterventionTermRuleDO> e : curMap.entrySet()) {
            if (!baseMap.containsKey(e.getKey())) {
                added.add(e.getValue());
            } else {
                InterventionTermRuleDO c = e.getValue();
                InterventionTermRuleDO b = baseMap.get(e.getKey());
                if (!termFieldsEqual(c, b)) {
                    modified.add(ModifiedPair.of(String.valueOf(e.getKey()), b, c));
                }
            }
        }
        for (Map.Entry<Long, InterventionTermRuleDO> e : baseMap.entrySet()) {
            if (!curMap.containsKey(e.getKey())) {
                deleted.add(e.getValue());
            }
        }
        return DiffResult.of(added, deleted, modified);
    }

    /**
     * 比较词表规则的业务字段是否相同
     */
    private boolean termFieldsEqual(InterventionTermRuleDO a, InterventionTermRuleDO b) {
        return Objects.equals(a.getSourceText(), b.getSourceText())
                && Objects.equals(a.getTargetText(), b.getTargetText())
                && Objects.equals(a.getPriority(), b.getPriority())
                && Objects.equals(a.getEnabled(), b.getEnabled())
                && Objects.equals(a.getRemark(), b.getRemark());
    }

    public static class ModifiedPair {
        private String key;
        private Object before;
        private Object after;

        public static ModifiedPair of(String key, Object before, Object after) {
            ModifiedPair p = new ModifiedPair();
            p.key = key;
            p.before = before;
            p.after = after;
            return p;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public Object getBefore() {
            return before;
        }

        public void setBefore(Object before) {
            this.before = before;
        }

        public Object getAfter() {
            return after;
        }

        public void setAfter(Object after) {
            this.after = after;
        }
    }

    public static class DiffResult {
        private List<Object> added;
        private List<Object> deleted;
        private List<ModifiedPair> modified;

        public static DiffResult of(List<Object> added, List<Object> deleted, List<ModifiedPair> modified) {
            DiffResult r = new DiffResult();
            r.added = added;
            r.deleted = deleted;
            r.modified = modified;
            return r;
        }

        public List<Object> getAdded() {
            return added;
        }

        public void setAdded(List<Object> added) {
            this.added = added;
        }

        public List<Object> getDeleted() {
            return deleted;
        }

        public void setDeleted(List<Object> deleted) {
            this.deleted = deleted;
        }

        public List<ModifiedPair> getModified() {
            return modified;
        }

        public void setModified(List<ModifiedPair> modified) {
            this.modified = modified;
        }
    }

    /**
     * 快照间对比（或当前编辑 vs 快照）。
     * snapshotA 为 null 表示"当前编辑版本"，此时从当前规则表查询数据。
     * snapshotB 为 null 表示"当前编辑版本"。
     * 结果语义：added = A 有 B 没有，deleted = B 有 A 没有，modified = 两边都有但字段不同。
     */
    public DiffResult diffSnapshots(Long resourceSetId, Long snapshotA, Long snapshotB, InterventionMode mode) {
        InterventionMode m = mode == null ? InterventionMode.sentence : mode;
        if (m == InterventionMode.sentence) {
            List<InterventionSentenceRuleDO> listA = loadSentenceRules(resourceSetId, snapshotA);
            List<InterventionSentenceRuleDO> listB = loadSentenceRules(resourceSetId, snapshotB);
            return diffSentenceByKey(listA, listB);
        }
        List<InterventionTermRuleDO> listA = loadTermRules(resourceSetId, snapshotA);
        List<InterventionTermRuleDO> listB = loadTermRules(resourceSetId, snapshotB);
        return diffTermByKey(listA, listB);
    }

    /**
     * 从快照表或当前规则表加载整句规则。snapshotId 为 null 则从当前规则表加载。
     */
    private List<InterventionSentenceRuleDO> loadSentenceRules(Long resourceSetId, Long snapshotId) {
        if (snapshotId == null) {
            return sentenceRuleMapper.selectList(
                    new LambdaQueryWrapper<InterventionSentenceRuleDO>()
                            .eq(InterventionSentenceRuleDO::getResourceSetId, resourceSetId));
        }
        List<SnapshotInterventionSentenceDO> snapRules = snapshotSentenceMapper.selectList(
                new LambdaQueryWrapper<SnapshotInterventionSentenceDO>()
                        .eq(SnapshotInterventionSentenceDO::getSnapshotId, snapshotId));
        List<InterventionSentenceRuleDO> result = new ArrayList<>();
        for (SnapshotInterventionSentenceDO s : snapRules) {
            InterventionSentenceRuleDO r = new InterventionSentenceRuleDO();
            r.setId(s.getSourceRuleId());
            r.setSourceText(s.getSourceText());
            r.setTargetText(s.getTargetText());
            r.setMatchType(s.getMatchType());
            r.setPriority(s.getPriority());
            r.setEnabled(s.getEnabled());
            r.setRemark(s.getRemark());
            result.add(r);
        }
        return result;
    }

    /**
     * 从快照表或当前规则表加载词表规则。snapshotId 为 null 则从当前规则表加载。
     */
    private List<InterventionTermRuleDO> loadTermRules(Long resourceSetId, Long snapshotId) {
        if (snapshotId == null) {
            return termRuleMapper.selectList(
                    new LambdaQueryWrapper<InterventionTermRuleDO>()
                            .eq(InterventionTermRuleDO::getResourceSetId, resourceSetId));
        }
        List<SnapshotInterventionTermDO> snapRules = snapshotTermMapper.selectList(
                new LambdaQueryWrapper<SnapshotInterventionTermDO>()
                        .eq(SnapshotInterventionTermDO::getSnapshotId, snapshotId));
        List<InterventionTermRuleDO> result = new ArrayList<>();
        for (SnapshotInterventionTermDO s : snapRules) {
            InterventionTermRuleDO r = new InterventionTermRuleDO();
            r.setId(s.getSourceRuleId());
            r.setSourceText(s.getSourceText());
            r.setTargetText(s.getTargetText());
            r.setPriority(s.getPriority());
            r.setEnabled(s.getEnabled());
            r.setRemark(s.getRemark());
            result.add(r);
        }
        return result;
    }

    /**
     * 变更摘要：当前规则表 vs 线上快照的轻量统计
     */
    public DiffSummary diffSummary(Long resourceSetId, RuleModule module) {
        ResourceSetDO rs = resourceSetMapper.selectById(resourceSetId);
        if (rs == null) {
            throw new BizException(404, "resource set not found");
        }

        Long snapshotId = rs.getCurrentSnapshotId();

        if (module == RuleModule.intervention) {
            // sentence diff
            DiffResult sentenceDiff = diff(resourceSetId, snapshotId, module, InterventionMode.sentence);
            // term diff
            DiffResult termDiff = diff(resourceSetId, snapshotId, module, InterventionMode.term);

            int addedCount = sentenceDiff.getAdded().size() + termDiff.getAdded().size();
            int deletedCount = sentenceDiff.getDeleted().size() + termDiff.getDeleted().size();
            int modifiedCount = sentenceDiff.getModified().size() + termDiff.getModified().size();

            int currentSentenceCount = sentenceRuleMapper.countByResourceSetId(resourceSetId);
            int currentTermCount = termRuleMapper.countByResourceSetId(resourceSetId);

            return DiffSummary.of(
                    addedCount > 0 || deletedCount > 0 || modifiedCount > 0,
                    addedCount, deletedCount, modifiedCount,
                    currentSentenceCount + currentTermCount,
                    snapshotId == null
            );
        }

        if (module == RuleModule.synonym) {
            DiffResult synonymDiff = diffSynonym(resourceSetId, snapshotId);

            int addedCount = synonymDiff.getAdded().size();
            int deletedCount = synonymDiff.getDeleted().size();
            int modifiedCount = synonymDiff.getModified().size();

            int currentCount = synonymRuleMapper.countByResourceSetId(resourceSetId);

            return DiffSummary.of(
                    addedCount > 0 || deletedCount > 0 || modifiedCount > 0,
                    addedCount, deletedCount, modifiedCount,
                    currentCount,
                    snapshotId == null
            );
        }

        if (module == RuleModule.entity) {
            DiffResult entityDiff = diffEntity(resourceSetId, snapshotId);

            int addedCount = entityDiff.getAdded().size();
            int deletedCount = entityDiff.getDeleted().size();
            int modifiedCount = entityDiff.getModified().size();

            int currentCount = entityRuleMapper.countByResourceSetId(resourceSetId);

            return DiffSummary.of(
                    addedCount > 0 || deletedCount > 0 || modifiedCount > 0,
                    addedCount, deletedCount, modifiedCount,
                    currentCount,
                    snapshotId == null
            );
        }

        if (module == RuleModule.token) {
            DiffResult tokenDiff = diffToken(resourceSetId, snapshotId);

            int addedCount = tokenDiff.getAdded().size();
            int deletedCount = tokenDiff.getDeleted().size();
            int modifiedCount = tokenDiff.getModified().size();

            int currentCount = tokenDictRuleMapper.countByResourceSetId(resourceSetId);

            return DiffSummary.of(
                    addedCount > 0 || deletedCount > 0 || modifiedCount > 0,
                    addedCount, deletedCount, modifiedCount,
                    currentCount,
                    snapshotId == null
            );
        }

        throw new BizException(400, "diff-summary not supported for module: " + module);
    }

    /**
     * 实体快照对比
     */
    public DiffResult diffEntity(Long resourceSetId, Long snapshotId) {
        List<EntityRuleDO> current = entityRuleMapper.selectList(
                new LambdaQueryWrapper<EntityRuleDO>().eq(EntityRuleDO::getResourceSetId, resourceSetId));

        List<EntityRuleDO> base = new ArrayList<>();
        if (snapshotId != null) {
            List<SnapshotEntityDO> snapRules = snapshotEntityMapper.selectList(
                    new LambdaQueryWrapper<SnapshotEntityDO>().eq(SnapshotEntityDO::getSnapshotId, snapshotId));
            for (SnapshotEntityDO s : snapRules) {
                EntityRuleDO r = new EntityRuleDO();
                r.setId(s.getSourceRuleId());
                r.setEntityText(s.getEntityText());
                r.setEntityType(s.getEntityType());
                r.setNormalizedValue(s.getNormalizedValue());
                r.setAliasesJson(s.getAliasesJson());
                r.setAttributesJson(s.getAttributesJson());
                r.setRelationsJson(s.getRelationsJson());
                r.setIdsJson(s.getIdsJson());
                r.setEnabled(s.getEnabled());
                base.add(r);
            }
        }

        return diffEntityByKey(current, base);
    }

    /**
     * 实体规则的字段级比较
     */
    private DiffResult diffEntityByKey(List<EntityRuleDO> current, List<EntityRuleDO> base) {
        Map<Long, EntityRuleDO> curMap = new LinkedHashMap<>();
        for (EntityRuleDO r : current) {
            if (r.getId() != null) curMap.put(r.getId(), r);
        }
        Map<Long, EntityRuleDO> baseMap = new LinkedHashMap<>();
        for (EntityRuleDO r : base) {
            if (r.getId() != null) baseMap.put(r.getId(), r);
        }

        List<Object> added = new ArrayList<>();
        List<Object> deleted = new ArrayList<>();
        List<ModifiedPair> modified = new ArrayList<>();

        for (Map.Entry<Long, EntityRuleDO> e : curMap.entrySet()) {
            if (!baseMap.containsKey(e.getKey())) {
                added.add(e.getValue());
            } else {
                EntityRuleDO c = e.getValue();
                EntityRuleDO b = baseMap.get(e.getKey());
                if (!entityFieldsEqual(c, b)) {
                    modified.add(ModifiedPair.of(String.valueOf(e.getKey()), b, c));
                }
            }
        }
        for (Map.Entry<Long, EntityRuleDO> e : baseMap.entrySet()) {
            if (!curMap.containsKey(e.getKey())) {
                deleted.add(e.getValue());
            }
        }
        return DiffResult.of(added, deleted, modified);
    }

    /**
     * 比较实体规则的业务字段是否相同
     */
    private boolean entityFieldsEqual(EntityRuleDO a, EntityRuleDO b) {
        return Objects.equals(a.getEntityText(), b.getEntityText())
                && Objects.equals(a.getEntityType(), b.getEntityType())
                && Objects.equals(a.getNormalizedValue(), b.getNormalizedValue())
                && Objects.equals(a.getAliasesJson(), b.getAliasesJson())
                && Objects.equals(a.getAttributesJson(), b.getAttributesJson())
                && Objects.equals(a.getRelationsJson(), b.getRelationsJson())
                && Objects.equals(a.getIdsJson(), b.getIdsJson())
                && Objects.equals(a.getEnabled(), b.getEnabled());
    }

    /**
     * 实体快照间对比
     */
    public DiffResult diffEntitySnapshots(Long resourceSetId, Long snapshotA, Long snapshotB) {
        List<EntityRuleDO> listA = loadEntityRules(resourceSetId, snapshotA);
        List<EntityRuleDO> listB = loadEntityRules(resourceSetId, snapshotB);
        return diffEntityByKey(listA, listB);
    }

    /**
     * 从快照表或当前规则表加载实体规则
     */
    private List<EntityRuleDO> loadEntityRules(Long resourceSetId, Long snapshotId) {
        if (snapshotId == null) {
            return entityRuleMapper.selectList(
                    new LambdaQueryWrapper<EntityRuleDO>()
                            .eq(EntityRuleDO::getResourceSetId, resourceSetId));
        }
        List<SnapshotEntityDO> snapRules = snapshotEntityMapper.selectList(
                new LambdaQueryWrapper<SnapshotEntityDO>()
                        .eq(SnapshotEntityDO::getSnapshotId, snapshotId));
        List<EntityRuleDO> result = new ArrayList<>();
        for (SnapshotEntityDO s : snapRules) {
            EntityRuleDO r = new EntityRuleDO();
            r.setId(s.getSourceRuleId());
            r.setEntityText(s.getEntityText());
            r.setEntityType(s.getEntityType());
            r.setNormalizedValue(s.getNormalizedValue());
            r.setAliasesJson(s.getAliasesJson());
            r.setAttributesJson(s.getAttributesJson());
            r.setRelationsJson(s.getRelationsJson());
            r.setIdsJson(s.getIdsJson());
            r.setEnabled(s.getEnabled());
            result.add(r);
        }
        return result;
    }

    /**
     * 同义词快照对比
     */
    public DiffResult diffSynonym(Long resourceSetId, Long snapshotId) {
        List<SynonymRuleDO> current = synonymRuleMapper.selectList(
                new LambdaQueryWrapper<SynonymRuleDO>().eq(SynonymRuleDO::getResourceSetId, resourceSetId));

        List<SynonymRuleDO> base = new ArrayList<>();
        if (snapshotId != null) {
            List<SnapshotSynonymDO> snapRules = snapshotSynonymMapper.selectList(
                    new LambdaQueryWrapper<SnapshotSynonymDO>().eq(SnapshotSynonymDO::getSnapshotId, snapshotId));
            for (SnapshotSynonymDO s : snapRules) {
                SynonymRuleDO r = new SynonymRuleDO();
                r.setId(s.getSourceRuleId());
                r.setSourceText(s.getSourceText());
                r.setDirection(s.getDirection());
                r.setTargetsJson(s.getTargetsJson());
                r.setEnabled(s.getEnabled());
                r.setRemark(s.getRemark());
                base.add(r);
            }
        }

        return diffSynonymByKey(current, base);
    }

    /**
     * 同义词规则的字段级比较
     */
    private DiffResult diffSynonymByKey(List<SynonymRuleDO> current, List<SynonymRuleDO> base) {
        Map<Long, SynonymRuleDO> curMap = new LinkedHashMap<>();
        for (SynonymRuleDO r : current) {
            if (r.getId() != null) curMap.put(r.getId(), r);
        }
        Map<Long, SynonymRuleDO> baseMap = new LinkedHashMap<>();
        for (SynonymRuleDO r : base) {
            if (r.getId() != null) baseMap.put(r.getId(), r);
        }

        List<Object> added = new ArrayList<>();
        List<Object> deleted = new ArrayList<>();
        List<ModifiedPair> modified = new ArrayList<>();

        for (Map.Entry<Long, SynonymRuleDO> e : curMap.entrySet()) {
            if (!baseMap.containsKey(e.getKey())) {
                added.add(e.getValue());
            } else {
                SynonymRuleDO c = e.getValue();
                SynonymRuleDO b = baseMap.get(e.getKey());
                if (!synonymFieldsEqual(c, b)) {
                    modified.add(ModifiedPair.of(String.valueOf(e.getKey()), b, c));
                }
            }
        }
        for (Map.Entry<Long, SynonymRuleDO> e : baseMap.entrySet()) {
            if (!curMap.containsKey(e.getKey())) {
                deleted.add(e.getValue());
            }
        }
        return DiffResult.of(added, deleted, modified);
    }

    /**
     * 比较同义词规则的业务字段是否相同
     */
    private boolean synonymFieldsEqual(SynonymRuleDO a, SynonymRuleDO b) {
        return Objects.equals(a.getSourceText(), b.getSourceText())
                && Objects.equals(a.getDirection(), b.getDirection())
                && Objects.equals(a.getTargetsJson(), b.getTargetsJson())
                && Objects.equals(a.getEnabled(), b.getEnabled())
                && Objects.equals(a.getRemark(), b.getRemark());
    }

    /**
     * 同义词快照间对比
     */
    public DiffResult diffSynonymSnapshots(Long resourceSetId, Long snapshotA, Long snapshotB) {
        List<SynonymRuleDO> listA = loadSynonymRules(resourceSetId, snapshotA);
        List<SynonymRuleDO> listB = loadSynonymRules(resourceSetId, snapshotB);
        return diffSynonymByKey(listA, listB);
    }

    /**
     * 从快照表或当前规则表加载同义词规则
     */
    private List<SynonymRuleDO> loadSynonymRules(Long resourceSetId, Long snapshotId) {
        if (snapshotId == null) {
            return synonymRuleMapper.selectList(
                    new LambdaQueryWrapper<SynonymRuleDO>()
                            .eq(SynonymRuleDO::getResourceSetId, resourceSetId));
        }
        List<SnapshotSynonymDO> snapRules = snapshotSynonymMapper.selectList(
                new LambdaQueryWrapper<SnapshotSynonymDO>()
                        .eq(SnapshotSynonymDO::getSnapshotId, snapshotId));
        List<SynonymRuleDO> result = new ArrayList<>();
        for (SnapshotSynonymDO s : snapRules) {
            SynonymRuleDO r = new SynonymRuleDO();
            r.setId(s.getSourceRuleId());
            r.setSourceText(s.getSourceText());
            r.setDirection(s.getDirection());
            r.setTargetsJson(s.getTargetsJson());
            r.setEnabled(s.getEnabled());
            r.setRemark(s.getRemark());
            result.add(r);
        }
        return result;
    }

    /**
     * 分词词典快照对比
     */
    public DiffResult diffToken(Long resourceSetId, Long snapshotId) {
        List<TokenDictRuleDO> current = tokenDictRuleMapper.selectList(
                new LambdaQueryWrapper<TokenDictRuleDO>().eq(TokenDictRuleDO::getResourceSetId, resourceSetId));

        List<TokenDictRuleDO> base = new ArrayList<>();
        if (snapshotId != null) {
            List<SnapshotTokenDictDO> snapRules = snapshotTokenDictMapper.selectList(
                    new LambdaQueryWrapper<SnapshotTokenDictDO>().eq(SnapshotTokenDictDO::getSnapshotId, snapshotId));
            for (SnapshotTokenDictDO s : snapRules) {
                TokenDictRuleDO r = new TokenDictRuleDO();
                r.setId(s.getSourceRuleId());
                r.setWord(s.getWord());
                r.setNature(s.getNature());
                r.setFrequency(s.getFrequency());
                r.setBizId(s.getBizId());
                r.setEnabled(s.getEnabled());
                base.add(r);
            }
        }

        return diffTokenByKey(current, base);
    }

    /**
     * 分词词典规则的字段级比较
     */
    private DiffResult diffTokenByKey(List<TokenDictRuleDO> current, List<TokenDictRuleDO> base) {
        Map<Long, TokenDictRuleDO> curMap = new LinkedHashMap<>();
        for (TokenDictRuleDO r : current) {
            if (r.getId() != null) curMap.put(r.getId(), r);
        }
        Map<Long, TokenDictRuleDO> baseMap = new LinkedHashMap<>();
        for (TokenDictRuleDO r : base) {
            if (r.getId() != null) baseMap.put(r.getId(), r);
        }

        List<Object> added = new ArrayList<>();
        List<Object> deleted = new ArrayList<>();
        List<ModifiedPair> modified = new ArrayList<>();

        for (Map.Entry<Long, TokenDictRuleDO> e : curMap.entrySet()) {
            if (!baseMap.containsKey(e.getKey())) {
                added.add(e.getValue());
            } else {
                TokenDictRuleDO c = e.getValue();
                TokenDictRuleDO b = baseMap.get(e.getKey());
                if (!tokenFieldsEqual(c, b)) {
                    modified.add(ModifiedPair.of(String.valueOf(e.getKey()), b, c));
                }
            }
        }
        for (Map.Entry<Long, TokenDictRuleDO> e : baseMap.entrySet()) {
            if (!curMap.containsKey(e.getKey())) {
                deleted.add(e.getValue());
            }
        }
        return DiffResult.of(added, deleted, modified);
    }

    /**
     * 比较分词词典规则的业务字段是否相同
     */
    private boolean tokenFieldsEqual(TokenDictRuleDO a, TokenDictRuleDO b) {
        return Objects.equals(a.getWord(), b.getWord())
                && Objects.equals(a.getNature(), b.getNature())
                && Objects.equals(a.getFrequency(), b.getFrequency())
                && Objects.equals(a.getBizId(), b.getBizId())
                && Objects.equals(a.getEnabled(), b.getEnabled());
    }

    /**
     * 分词词典快照间对比
     */
    public DiffResult diffTokenSnapshots(Long resourceSetId, Long snapshotA, Long snapshotB) {
        List<TokenDictRuleDO> listA = loadTokenRules(resourceSetId, snapshotA);
        List<TokenDictRuleDO> listB = loadTokenRules(resourceSetId, snapshotB);
        return diffTokenByKey(listA, listB);
    }

    /**
     * 从快照表或当前规则表加载分词词典规则
     */
    private List<TokenDictRuleDO> loadTokenRules(Long resourceSetId, Long snapshotId) {
        if (snapshotId == null) {
            return tokenDictRuleMapper.selectList(
                    new LambdaQueryWrapper<TokenDictRuleDO>()
                            .eq(TokenDictRuleDO::getResourceSetId, resourceSetId));
        }
        List<SnapshotTokenDictDO> snapRules = snapshotTokenDictMapper.selectList(
                new LambdaQueryWrapper<SnapshotTokenDictDO>()
                        .eq(SnapshotTokenDictDO::getSnapshotId, snapshotId));
        List<TokenDictRuleDO> result = new ArrayList<>();
        for (SnapshotTokenDictDO s : snapRules) {
            TokenDictRuleDO r = new TokenDictRuleDO();
            r.setId(s.getSourceRuleId());
            r.setWord(s.getWord());
            r.setNature(s.getNature());
            r.setFrequency(s.getFrequency());
            r.setBizId(s.getBizId());
            r.setEnabled(s.getEnabled());
            result.add(r);
        }
        return result;
    }

    public static class DiffSummary {
        private boolean hasChanges;
        private int addedCount;
        private int deletedCount;
        private int modifiedCount;
        private int currentRuleCount;
        private boolean noSnapshot;

        public static DiffSummary of(boolean hasChanges, int addedCount, int deletedCount, int modifiedCount, int currentRuleCount, boolean noSnapshot) {
            DiffSummary s = new DiffSummary();
            s.hasChanges = hasChanges;
            s.addedCount = addedCount;
            s.deletedCount = deletedCount;
            s.modifiedCount = modifiedCount;
            s.currentRuleCount = currentRuleCount;
            s.noSnapshot = noSnapshot;
            return s;
        }

        public boolean isHasChanges() { return hasChanges; }
        public void setHasChanges(boolean hasChanges) { this.hasChanges = hasChanges; }
        public int getAddedCount() { return addedCount; }
        public void setAddedCount(int addedCount) { this.addedCount = addedCount; }
        public int getDeletedCount() { return deletedCount; }
        public void setDeletedCount(int deletedCount) { this.deletedCount = deletedCount; }
        public int getModifiedCount() { return modifiedCount; }
        public void setModifiedCount(int modifiedCount) { this.modifiedCount = modifiedCount; }
        public int getCurrentRuleCount() { return currentRuleCount; }
        public void setCurrentRuleCount(int currentRuleCount) { this.currentRuleCount = currentRuleCount; }
        public boolean isNoSnapshot() { return noSnapshot; }
        public void setNoSnapshot(boolean noSnapshot) { this.noSnapshot = noSnapshot; }
    }
}

