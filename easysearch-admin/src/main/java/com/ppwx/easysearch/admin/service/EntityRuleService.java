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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ppwx.easysearch.admin.domain.api.PageResult;
import com.ppwx.easysearch.admin.domain.exception.BizException;
import com.ppwx.easysearch.admin.domain.model.EntityRuleDO;
import com.ppwx.easysearch.admin.domain.model.ResourceVersionDO;
import com.ppwx.easysearch.admin.mapper.EntityRuleMapper;
import com.ppwx.easysearch.admin.mapper.ResourceVersionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EntityRuleService {
    private final EntityRuleMapper entityRuleMapper;
    private final ResourceVersionMapper resourceVersionMapper;
    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    public EntityRuleService(EntityRuleMapper entityRuleMapper,
                             ResourceVersionMapper resourceVersionMapper,
                             OperationLogService operationLogService,
                             ObjectMapper objectMapper) {
        this.entityRuleMapper = entityRuleMapper;
        this.resourceVersionMapper = resourceVersionMapper;
        this.operationLogService = operationLogService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void copyFromVersion(Long newVersionId, Long baseVersionId) {
        entityRuleMapper.copyFromVersion(newVersionId, baseVersionId);
    }

    public PageResult<EntityRuleDO> page(Long versionId, String q, String entityType, long page, long pageSize) {
        Page<EntityRuleDO> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<EntityRuleDO> qw = new LambdaQueryWrapper<>();
        qw.eq(EntityRuleDO::getVersionId, versionId);
        if (entityType != null && !entityType.trim().isEmpty()) {
            qw.eq(EntityRuleDO::getEntityType, entityType);
        }
        if (q != null && !q.trim().isEmpty()) {
            qw.and(x -> x.like(EntityRuleDO::getEntityText, q)
                    .or().like(EntityRuleDO::getNormalizedValue, q)
                    .or().like(EntityRuleDO::getAliasesJson, q));
        }
        qw.orderByDesc(EntityRuleDO::getId);
        Page<EntityRuleDO> out = entityRuleMapper.selectPage(p, qw);
        return PageResult.of(out.getCurrent(), out.getSize(), out.getTotal(), out.getRecords());
    }

    public EntityRuleDO create(Long versionId, EntityRuleDO in) {
        ResourceVersionDO v = ensureDraft(versionId);
        in.setId(null);
        in.setVersionId(versionId);
        normalize(in);
        entityRuleMapper.insert(in);
        operationLogService.log("create", v.getResourceSetId(), versionId, "entity", in.getId(), null, in);
        return in;
    }

    public EntityRuleDO update(Long versionId, Long ruleId, EntityRuleDO in) {
        ResourceVersionDO v = ensureDraft(versionId);
        EntityRuleDO before = entityRuleMapper.selectById(ruleId);
        if (before == null || !versionId.equals(before.getVersionId())) throw new BizException(404, "rule not found");
        in.setId(ruleId);
        in.setVersionId(versionId);
        normalize(in);
        entityRuleMapper.updateById(in);
        EntityRuleDO after = entityRuleMapper.selectById(ruleId);
        operationLogService.log("update", v.getResourceSetId(), versionId, "entity", ruleId, before, after);
        return after;
    }

    public void delete(Long versionId, Long ruleId) {
        ResourceVersionDO v = ensureDraft(versionId);
        EntityRuleDO before = entityRuleMapper.selectById(ruleId);
        if (before == null || !versionId.equals(before.getVersionId())) return;
        entityRuleMapper.deleteById(ruleId);
        operationLogService.log("delete", v.getResourceSetId(), versionId, "entity", ruleId, before, null);
    }

    @Transactional
    public InterventionRuleService.BatchImportResult batchImport(Long versionId, List<EntityRuleDO> items) {
        ResourceVersionDO v = ensureDraft(versionId);
        int ok = 0;
        int fail = 0;
        for (EntityRuleDO in : items) {
            try {
                in.setId(null);
                in.setVersionId(versionId);
                normalize(in);
                entityRuleMapper.insert(in);
                ok++;
            } catch (Exception e) {
                fail++;
            }
        }
        operationLogService.log("batch_import", v.getResourceSetId(), versionId, "entity", null, null, "ok=" + ok + ",fail=" + fail);
        return InterventionRuleService.BatchImportResult.of(ok, fail);
    }

    public VersionService.ValidateReport validate(Long versionId) {
        List<EntityRuleDO> rules = entityRuleMapper.selectList(new LambdaQueryWrapper<EntityRuleDO>().eq(EntityRuleDO::getVersionId, versionId));
        for (EntityRuleDO r : rules) {
            if (isBlank(r.getEntityText()) || isBlank(r.getEntityType()) || isBlank(r.getNormalizedValue())) {
                return VersionService.ValidateReport.fail("entity: entity/type/normalizedValue required");
            }
            ensureJsonArray(r.getAliasesJson(), true);
            ensureJsonObject(r.getAttributesJson(), true);
            ensureJsonObject(r.getRelationsJson(), true);
            ensureJsonArray(r.getIdsJson(), true);
        }
        return VersionService.ValidateReport.ok("entity ok; size=" + rules.size());
    }

    private void normalize(EntityRuleDO in) {
        in.setEntityText(trimToNull(in.getEntityText()));
        in.setEntityType(trimToNull(in.getEntityType()));
        in.setNormalizedValue(trimToNull(in.getNormalizedValue()));
        if (in.getEnabled() == null) in.setEnabled(1);
        if (in.getAliasesJson() == null || in.getAliasesJson().trim().isEmpty()) in.setAliasesJson("[]");
        if (in.getAttributesJson() == null || in.getAttributesJson().trim().isEmpty()) in.setAttributesJson("{}");
        if (in.getRelationsJson() == null || in.getRelationsJson().trim().isEmpty()) in.setRelationsJson("{}");
        if (in.getIdsJson() == null || in.getIdsJson().trim().isEmpty()) in.setIdsJson("[]");
        ensureJsonArray(in.getAliasesJson(), false);
        ensureJsonObject(in.getAttributesJson(), false);
        ensureJsonObject(in.getRelationsJson(), false);
        ensureJsonArray(in.getIdsJson(), false);
    }

    private void ensureJsonArray(String json, boolean allowNull) {
        if (json == null) {
            if (allowNull) return;
            throw new BizException(400, "json array required");
        }
        try {
            JsonNode n = objectMapper.readTree(json);
            if (!n.isArray()) throw new BizException(400, "json array required");
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(400, "json array invalid");
        }
    }

    private void ensureJsonObject(String json, boolean allowNull) {
        if (json == null) {
            if (allowNull) return;
            throw new BizException(400, "json object required");
        }
        try {
            JsonNode n = objectMapper.readTree(json);
            if (!n.isObject()) throw new BizException(400, "json object required");
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(400, "json object invalid");
        }
    }

    private ResourceVersionDO ensureDraft(Long versionId) {
        ResourceVersionDO v = resourceVersionMapper.selectById(versionId);
        if (v == null) throw new BizException(404, "version not found");
        if (!"draft".equals(v.getStatus())) throw new BizException(400, "only draft can be modified");
        return v;
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

