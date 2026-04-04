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
    private final MetaRuleMapper metaRuleMapper;
    private final SnapshotInterventionSentenceMapper snapshotSentenceMapper;
    private final SnapshotInterventionTermMapper snapshotTermMapper;
    private final ResourceSetMapper resourceSetMapper;

    public DiffService(InterventionSentenceRuleMapper sentenceRuleMapper,
                       InterventionTermRuleMapper termRuleMapper,
                       SynonymRuleMapper synonymRuleMapper,
                       EntityRuleMapper entityRuleMapper,
                       TokenDictRuleMapper tokenDictRuleMapper,
                       MetaRuleMapper metaRuleMapper,
                       SnapshotInterventionSentenceMapper snapshotSentenceMapper,
                       SnapshotInterventionTermMapper snapshotTermMapper,
                       ResourceSetMapper resourceSetMapper) {
        this.sentenceRuleMapper = sentenceRuleMapper;
        this.termRuleMapper = termRuleMapper;
        this.synonymRuleMapper = synonymRuleMapper;
        this.entityRuleMapper = entityRuleMapper;
        this.tokenDictRuleMapper = tokenDictRuleMapper;
        this.metaRuleMapper = metaRuleMapper;
        this.snapshotSentenceMapper = snapshotSentenceMapper;
        this.snapshotTermMapper = snapshotTermMapper;
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
        if (module == RuleModule.meta) {
            List<MetaRuleDO> cur = metaRuleMapper.selectList(new LambdaQueryWrapper<MetaRuleDO>().eq(MetaRuleDO::getResourceSetId, currentResourceSetId));
            List<MetaRuleDO> base = new ArrayList<>();
            return diffByKey(cur, base, this::metaKey);
        }
        throw new BizException(400, "module not supported");
    }

    private String metaKey(MetaRuleDO r) {
        String tt = safe(r.getTermType());
        if ("category".equals(tt)) return "category:" + safe(firstNonBlank(r.getCategoryId(), r.getCategoryName()));
        if ("brand".equals(tt)) return "brand:" + safe(firstNonBlank(r.getBrandId(), r.getBrandName()));
        return "model:" + safe(firstNonBlank(r.getModelId(), r.getModelName()));
    }

    private String firstNonBlank(String a, String b) {
        if (a != null && !a.trim().isEmpty()) return a;
        return b;
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
     * 变更摘要：当前规则表 vs 线上快照的轻量统计
     */
    public DiffSummary diffSummary(Long resourceSetId, RuleModule module) {
        if (module != RuleModule.intervention) {
            throw new BizException(400, "diff-summary not supported for module: " + module);
        }

        ResourceSetDO rs = resourceSetMapper.selectById(resourceSetId);
        if (rs == null) {
            throw new BizException(404, "resource set not found");
        }

        Long snapshotId = rs.getCurrentSnapshotId();

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

