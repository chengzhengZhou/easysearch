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

    public DiffService(InterventionSentenceRuleMapper sentenceRuleMapper,
                       InterventionTermRuleMapper termRuleMapper,
                       SynonymRuleMapper synonymRuleMapper,
                       EntityRuleMapper entityRuleMapper,
                       TokenDictRuleMapper tokenDictRuleMapper,
                       MetaRuleMapper metaRuleMapper) {
        this.sentenceRuleMapper = sentenceRuleMapper;
        this.termRuleMapper = termRuleMapper;
        this.synonymRuleMapper = synonymRuleMapper;
        this.entityRuleMapper = entityRuleMapper;
        this.tokenDictRuleMapper = tokenDictRuleMapper;
        this.metaRuleMapper = metaRuleMapper;
    }

    public DiffResult diff(Long currentVersionId, Long baseVersionId, RuleModule module, InterventionMode mode) {
        if (module == RuleModule.intervention) {
            InterventionMode m = mode == null ? InterventionMode.sentence : mode;
            if (m == InterventionMode.sentence) {
                List<InterventionSentenceRuleDO> cur = sentenceRuleMapper.selectList(new LambdaQueryWrapper<InterventionSentenceRuleDO>().eq(InterventionSentenceRuleDO::getVersionId, currentVersionId));
                List<InterventionSentenceRuleDO> base = sentenceRuleMapper.selectList(new LambdaQueryWrapper<InterventionSentenceRuleDO>().eq(InterventionSentenceRuleDO::getVersionId, baseVersionId));
                return diffByKey(cur, base, r -> safe(r.getSourceText()));
            }
            List<InterventionTermRuleDO> cur = termRuleMapper.selectList(new LambdaQueryWrapper<InterventionTermRuleDO>().eq(InterventionTermRuleDO::getVersionId, currentVersionId));
            List<InterventionTermRuleDO> base = termRuleMapper.selectList(new LambdaQueryWrapper<InterventionTermRuleDO>().eq(InterventionTermRuleDO::getVersionId, baseVersionId));
            return diffByKey(cur, base, r -> safe(r.getSourceText()));
        }
        if (module == RuleModule.synonym) {
            List<SynonymRuleDO> cur = synonymRuleMapper.selectList(new LambdaQueryWrapper<SynonymRuleDO>().eq(SynonymRuleDO::getVersionId, currentVersionId));
            List<SynonymRuleDO> base = synonymRuleMapper.selectList(new LambdaQueryWrapper<SynonymRuleDO>().eq(SynonymRuleDO::getVersionId, baseVersionId));
            return diffByKey(cur, base, r -> safe(r.getSourceText()) + "|" + safe(r.getDirection()));
        }
        if (module == RuleModule.entity) {
            List<EntityRuleDO> cur = entityRuleMapper.selectList(new LambdaQueryWrapper<EntityRuleDO>().eq(EntityRuleDO::getVersionId, currentVersionId));
            List<EntityRuleDO> base = entityRuleMapper.selectList(new LambdaQueryWrapper<EntityRuleDO>().eq(EntityRuleDO::getVersionId, baseVersionId));
            return diffByKey(cur, base, r -> safe(r.getEntityText()) + "|" + safe(r.getEntityType()) + "|" + safe(r.getNormalizedValue()));
        }
        if (module == RuleModule.token) {
            List<TokenDictRuleDO> cur = tokenDictRuleMapper.selectList(new LambdaQueryWrapper<TokenDictRuleDO>().eq(TokenDictRuleDO::getVersionId, currentVersionId));
            List<TokenDictRuleDO> base = tokenDictRuleMapper.selectList(new LambdaQueryWrapper<TokenDictRuleDO>().eq(TokenDictRuleDO::getVersionId, baseVersionId));
            return diffByKey(cur, base, r -> safe(r.getWord()));
        }
        if (module == RuleModule.meta) {
            List<MetaRuleDO> cur = metaRuleMapper.selectList(new LambdaQueryWrapper<MetaRuleDO>().eq(MetaRuleDO::getVersionId, currentVersionId));
            List<MetaRuleDO> base = metaRuleMapper.selectList(new LambdaQueryWrapper<MetaRuleDO>().eq(MetaRuleDO::getVersionId, baseVersionId));
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
}

