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
import com.ppwx.easysearch.admin.domain.model.ResourceVersionDO;
import com.ppwx.easysearch.admin.domain.model.SynonymRuleDO;
import com.ppwx.easysearch.admin.mapper.ResourceVersionMapper;
import com.ppwx.easysearch.admin.mapper.SynonymRuleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class SynonymRuleService {
    private final SynonymRuleMapper synonymRuleMapper;
    private final ResourceVersionMapper resourceVersionMapper;
    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    public SynonymRuleService(SynonymRuleMapper synonymRuleMapper,
                              ResourceVersionMapper resourceVersionMapper,
                              OperationLogService operationLogService,
                              ObjectMapper objectMapper) {
        this.synonymRuleMapper = synonymRuleMapper;
        this.resourceVersionMapper = resourceVersionMapper;
        this.operationLogService = operationLogService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void copyFromVersion(Long newVersionId, Long baseVersionId) {
        synonymRuleMapper.copyFromVersion(newVersionId, baseVersionId);
    }

    public PageResult<SynonymRuleDO> page(Long versionId, String q, long page, long pageSize) {
        Page<SynonymRuleDO> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<SynonymRuleDO> qw = new LambdaQueryWrapper<>();
        qw.eq(SynonymRuleDO::getVersionId, versionId);
        if (q != null && !q.trim().isEmpty()) {
            qw.and(x -> x.like(SynonymRuleDO::getSourceText, q).or().like(SynonymRuleDO::getTargetsJson, q));
        }
        qw.orderByDesc(SynonymRuleDO::getId);
        Page<SynonymRuleDO> out = synonymRuleMapper.selectPage(p, qw);
        return PageResult.of(out.getCurrent(), out.getSize(), out.getTotal(), out.getRecords());
    }

    public SynonymRuleDO create(Long versionId, SynonymRuleDO in) {
        ResourceVersionDO v = ensureDraft(versionId);
        in.setId(null);
        in.setVersionId(versionId);
        normalize(in);
        synonymRuleMapper.insert(in);
        operationLogService.log("create", v.getResourceSetId(), versionId, "synonym", in.getId(), null, in);
        return in;
    }

    public SynonymRuleDO update(Long versionId, Long ruleId, SynonymRuleDO in) {
        ResourceVersionDO v = ensureDraft(versionId);
        SynonymRuleDO before = synonymRuleMapper.selectById(ruleId);
        if (before == null || !versionId.equals(before.getVersionId())) {
            throw new BizException(404, "rule not found");
        }
        in.setId(ruleId);
        in.setVersionId(versionId);
        normalize(in);
        synonymRuleMapper.updateById(in);
        SynonymRuleDO after = synonymRuleMapper.selectById(ruleId);
        operationLogService.log("update", v.getResourceSetId(), versionId, "synonym", ruleId, before, after);
        return after;
    }

    public void delete(Long versionId, Long ruleId) {
        ResourceVersionDO v = ensureDraft(versionId);
        SynonymRuleDO before = synonymRuleMapper.selectById(ruleId);
        if (before == null || !versionId.equals(before.getVersionId())) {
            return;
        }
        synonymRuleMapper.deleteById(ruleId);
        operationLogService.log("delete", v.getResourceSetId(), versionId, "synonym", ruleId, before, null);
    }

    @Transactional
    public InterventionRuleService.BatchImportResult batchImport(Long versionId, List<SynonymRuleDO> items) {
        ResourceVersionDO v = ensureDraft(versionId);
        int ok = 0;
        int fail = 0;
        for (SynonymRuleDO in : items) {
            try {
                in.setId(null);
                in.setVersionId(versionId);
                normalize(in);
                synonymRuleMapper.insert(in);
                ok++;
            } catch (Exception e) {
                fail++;
            }
        }
        operationLogService.log("batch_import", v.getResourceSetId(), versionId, "synonym", null, null, "ok=" + ok + ",fail=" + fail);
        return InterventionRuleService.BatchImportResult.of(ok, fail);
    }

    public VersionService.ValidateReport validate(Long versionId) {
        List<SynonymRuleDO> rules = synonymRuleMapper.selectList(new LambdaQueryWrapper<SynonymRuleDO>().eq(SynonymRuleDO::getVersionId, versionId));
        for (SynonymRuleDO r : rules) {
            if (isBlank(r.getSourceText())) return VersionService.ValidateReport.fail("synonym: source required");
            String dir = safe(r.getDirection());
            if (!("=>".equals(dir) || "<=".equals(dir) || "SYM".equals(dir))) return VersionService.ValidateReport.fail("synonym: invalid direction");
            List<String> targets = parseTargets(r.getTargetsJson());
            if (targets.isEmpty()) return VersionService.ValidateReport.fail("synonym: targets required");
        }
        return VersionService.ValidateReport.ok("synonym ok; size=" + rules.size());
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

    private ResourceVersionDO ensureDraft(Long versionId) {
        ResourceVersionDO v = resourceVersionMapper.selectById(versionId);
        if (v == null) throw new BizException(404, "version not found");
        if (!"draft".equals(v.getStatus())) throw new BizException(400, "only draft can be modified");
        return v;
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

