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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ppwx.easysearch.admin.domain.api.PageResult;
import com.ppwx.easysearch.admin.domain.exception.BizException;
import com.ppwx.easysearch.admin.domain.model.SynonymRuleDO;
import com.ppwx.easysearch.admin.mapper.SynonymRuleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 同义词规则服务
 * 简化版：直接操作当前规则表（归属 resourceSetId），无需 versionId
 */
@Service
public class SynonymRuleService {
    private final SynonymRuleMapper synonymRuleMapper;
    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    public SynonymRuleService(SynonymRuleMapper synonymRuleMapper,
                              OperationLogService operationLogService,
                              ObjectMapper objectMapper) {
        this.synonymRuleMapper = synonymRuleMapper;
        this.operationLogService = operationLogService;
        this.objectMapper = objectMapper;
    }

    public PageResult<SynonymRuleDO> page(Long resourceSetId, String q, long page, long pageSize) {
        Page<SynonymRuleDO> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<SynonymRuleDO> qw = new LambdaQueryWrapper<>();
        qw.eq(SynonymRuleDO::getResourceSetId, resourceSetId);
        if (q != null && !q.trim().isEmpty()) {
            qw.and(x -> x.like(SynonymRuleDO::getSourceText, q).or().like(SynonymRuleDO::getTargetsJson, q));
        }
        qw.orderByDesc(SynonymRuleDO::getId);
        Page<SynonymRuleDO> out = synonymRuleMapper.selectPage(p, qw);
        return PageResult.of(out.getCurrent(), out.getSize(), out.getTotal(), out.getRecords());
    }

    public SynonymRuleDO create(Long resourceSetId, SynonymRuleDO in) {
        in.setId(null);
        in.setResourceSetId(resourceSetId);
        normalize(in);
        synonymRuleMapper.insert(in);
        operationLogService.log("create", resourceSetId, null, "synonym", in.getId(), null, in);
        return in;
    }

    public SynonymRuleDO update(Long resourceSetId, Long ruleId, SynonymRuleDO in) {
        SynonymRuleDO before = synonymRuleMapper.selectById(ruleId);
        if (before == null || !resourceSetId.equals(before.getResourceSetId())) {
            throw new BizException(404, "rule not found");
        }
        in.setId(ruleId);
        in.setResourceSetId(resourceSetId);
        normalize(in);
        synonymRuleMapper.updateById(in);
        SynonymRuleDO after = synonymRuleMapper.selectById(ruleId);
        operationLogService.log("update", resourceSetId, null, "synonym", ruleId, before, after);
        return after;
    }

    public void delete(Long resourceSetId, Long ruleId) {
        SynonymRuleDO before = synonymRuleMapper.selectById(ruleId);
        if (before == null || !resourceSetId.equals(before.getResourceSetId())) {
            return;
        }
        synonymRuleMapper.deleteById(ruleId);
        operationLogService.log("delete", resourceSetId, null, "synonym", ruleId, before, null);
    }

    @Transactional
    public InterventionRuleService.BatchImportResult batchImport(Long resourceSetId, List<SynonymRuleDO> items) {
        int ok = 0;
        int fail = 0;
        for (SynonymRuleDO in : items) {
            try {
                in.setId(null);
                in.setResourceSetId(resourceSetId);
                normalize(in);
                synonymRuleMapper.insert(in);
                ok++;
            } catch (Exception e) {
                fail++;
            }
        }
        operationLogService.log("batch_import", resourceSetId, null, "synonym", null, null, "ok=" + ok + ",fail=" + fail);
        return InterventionRuleService.BatchImportResult.of(ok, fail);
    }

    @Transactional
    public void batchEnable(Long resourceSetId, List<Long> ids, boolean enabled) {
        for (Long id : ids) {
            SynonymRuleDO rule = synonymRuleMapper.selectById(id);
            if (rule != null && resourceSetId.equals(rule.getResourceSetId())) {
                rule.setEnabled(enabled ? 1 : 0);
                synonymRuleMapper.updateById(rule);
            }
        }
        operationLogService.log(enabled ? "batch_enable" : "batch_disable", resourceSetId, null, "synonym", null, null, "ids=" + ids);
    }

    @Transactional
    public void batchDelete(Long resourceSetId, List<Long> ids) {
        for (Long id : ids) {
            SynonymRuleDO rule = synonymRuleMapper.selectById(id);
            if (rule != null && resourceSetId.equals(rule.getResourceSetId())) {
                synonymRuleMapper.deleteById(id);
            }
        }
        operationLogService.log("batch_delete", resourceSetId, null, "synonym", null, null, "ids=" + ids);
    }

    public PublishService.ValidateReport validate(Long resourceSetId) {
        List<SynonymRuleDO> rules = synonymRuleMapper.selectList(
                new LambdaQueryWrapper<SynonymRuleDO>().eq(SynonymRuleDO::getResourceSetId, resourceSetId));
        for (SynonymRuleDO r : rules) {
            if (isBlank(r.getSourceText())) {
                return PublishService.ValidateReport.fail("synonym: source required");
            }
            String dir = safe(r.getDirection());
            if (!("=>".equals(dir) || "<=".equals(dir) || "SYM".equals(dir))) {
                return PublishService.ValidateReport.fail("synonym: invalid direction");
            }
            List<String> targets = parseTargets(r.getTargetsJson());
            if (targets.isEmpty()) {
                return PublishService.ValidateReport.fail("synonym: targets required");
            }
        }
        return PublishService.ValidateReport.ok("synonym ok; size=" + rules.size());
    }

    private void normalize(SynonymRuleDO in) {
        in.setSourceText(trimToNull(in.getSourceText()));
        in.setDirection(trimToNull(in.getDirection()));
        if (in.getEnabled() == null) in.setEnabled(1);
        if (in.getTargetsJson() == null || in.getTargetsJson().trim().isEmpty()) {
            in.setTargetsJson("[]");
        } else {
            parseTargets(in.getTargetsJson()); // validate json/normalize
        }
    }

    private List<String> parseTargets(String json) {
        try {
            List<String> arr = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            List<String> out = new ArrayList<>();
            for (String s : arr) {
                if (s != null) {
                    String t = s.trim();
                    if (!t.isEmpty()) out.add(t);
                }
            }
            return out;
        } catch (Exception e) {
            throw new BizException(400, "synonym targets_json invalid");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
