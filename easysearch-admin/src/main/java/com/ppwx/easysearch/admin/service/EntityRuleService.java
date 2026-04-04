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
import com.ppwx.easysearch.admin.mapper.EntityRuleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 实体规则服务
 * 简化版：直接操作当前规则表（归属 resourceSetId），无需 versionId
 */
@Service
public class EntityRuleService {
    private final EntityRuleMapper entityRuleMapper;
    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    public EntityRuleService(EntityRuleMapper entityRuleMapper,
                             OperationLogService operationLogService,
                             ObjectMapper objectMapper) {
        this.entityRuleMapper = entityRuleMapper;
        this.operationLogService = operationLogService;
        this.objectMapper = objectMapper;
    }

    public PageResult<EntityRuleDO> page(Long resourceSetId, String q, String entityType, long page, long pageSize) {
        Page<EntityRuleDO> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<EntityRuleDO> qw = new LambdaQueryWrapper<>();
        qw.eq(EntityRuleDO::getResourceSetId, resourceSetId);
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

    public EntityRuleDO create(Long resourceSetId, EntityRuleDO in) {
        in.setId(null);
        in.setResourceSetId(resourceSetId);
        normalize(in);
        entityRuleMapper.insert(in);
        operationLogService.log("create", resourceSetId, null, "entity", in.getId(), null, in);
        return in;
    }

    public EntityRuleDO update(Long resourceSetId, Long ruleId, EntityRuleDO in) {
        EntityRuleDO before = entityRuleMapper.selectById(ruleId);
        if (before == null || !resourceSetId.equals(before.getResourceSetId())) {
            throw new BizException(404, "rule not found");
        }
        in.setId(ruleId);
        in.setResourceSetId(resourceSetId);
        normalize(in);
        entityRuleMapper.updateById(in);
        EntityRuleDO after = entityRuleMapper.selectById(ruleId);
        operationLogService.log("update", resourceSetId, null, "entity", ruleId, before, after);
        return after;
    }

    public void delete(Long resourceSetId, Long ruleId) {
        EntityRuleDO before = entityRuleMapper.selectById(ruleId);
        if (before == null || !resourceSetId.equals(before.getResourceSetId())) {
            return;
        }
        entityRuleMapper.deleteById(ruleId);
        operationLogService.log("delete", resourceSetId, null, "entity", ruleId, before, null);
    }

    @Transactional
    public InterventionRuleService.BatchImportResult batchImport(Long resourceSetId, List<EntityRuleDO> items) {
        int ok = 0;
        int fail = 0;
        for (EntityRuleDO in : items) {
            try {
                in.setId(null);
                in.setResourceSetId(resourceSetId);
                normalize(in);
                entityRuleMapper.insert(in);
                ok++;
            } catch (Exception e) {
                fail++;
            }
        }
        operationLogService.log("batch_import", resourceSetId, null, "entity", null, null, "ok=" + ok + ",fail=" + fail);
        return InterventionRuleService.BatchImportResult.of(ok, fail);
    }

    public PublishService.ValidateReport validate(Long resourceSetId) {
        List<EntityRuleDO> rules = entityRuleMapper.selectList(
                new LambdaQueryWrapper<EntityRuleDO>().eq(EntityRuleDO::getResourceSetId, resourceSetId));
        for (EntityRuleDO r : rules) {
            if (isBlank(r.getEntityText()) || isBlank(r.getEntityType()) || isBlank(r.getNormalizedValue())) {
                return PublishService.ValidateReport.fail("entity: entity/type/normalizedValue required");
            }
            ensureJsonArray(r.getAliasesJson(), true);
            ensureJsonObject(r.getAttributesJson(), true);
            ensureJsonObject(r.getRelationsJson(), true);
            ensureJsonArray(r.getIdsJson(), true);
        }
        return PublishService.ValidateReport.ok("entity ok; size=" + rules.size());
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

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
