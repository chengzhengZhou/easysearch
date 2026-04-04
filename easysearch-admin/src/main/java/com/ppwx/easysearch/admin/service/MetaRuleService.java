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
import com.ppwx.easysearch.admin.domain.api.PageResult;
import com.ppwx.easysearch.admin.domain.exception.BizException;
import com.ppwx.easysearch.admin.domain.model.MetaRuleDO;
import com.ppwx.easysearch.admin.mapper.MetaRuleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Meta规则服务（品牌/品类/型号）
 * 简化版：直接操作当前规则表（归属 resourceSetId），无需 versionId
 */
@Service
public class MetaRuleService {
    private final MetaRuleMapper metaRuleMapper;
    private final OperationLogService operationLogService;

    public MetaRuleService(MetaRuleMapper metaRuleMapper,
                           OperationLogService operationLogService) {
        this.metaRuleMapper = metaRuleMapper;
        this.operationLogService = operationLogService;
    }

    public PageResult<MetaRuleDO> page(Long resourceSetId, String q, String termType, long page, long pageSize) {
        Page<MetaRuleDO> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<MetaRuleDO> qw = new LambdaQueryWrapper<>();
        qw.eq(MetaRuleDO::getResourceSetId, resourceSetId);
        if (termType != null && !termType.trim().isEmpty()) {
            qw.eq(MetaRuleDO::getTermType, termType);
        }
        if (q != null && !q.trim().isEmpty()) {
            qw.and(x -> x.like(MetaRuleDO::getCategoryName, q)
                    .or().like(MetaRuleDO::getBrandName, q)
                    .or().like(MetaRuleDO::getBrandNameEn, q)
                    .or().like(MetaRuleDO::getModelName, q)
                    .or().like(MetaRuleDO::getCategoryId, q)
                    .or().like(MetaRuleDO::getBrandId, q)
                    .or().like(MetaRuleDO::getModelId, q));
        }
        qw.orderByDesc(MetaRuleDO::getId);
        Page<MetaRuleDO> out = metaRuleMapper.selectPage(p, qw);
        return PageResult.of(out.getCurrent(), out.getSize(), out.getTotal(), out.getRecords());
    }

    public MetaRuleDO create(Long resourceSetId, MetaRuleDO in) {
        in.setId(null);
        in.setResourceSetId(resourceSetId);
        normalize(in);
        metaRuleMapper.insert(in);
        operationLogService.log("create", resourceSetId, null, "meta", in.getId(), null, in);
        return in;
    }

    public MetaRuleDO update(Long resourceSetId, Long ruleId, MetaRuleDO in) {
        MetaRuleDO before = metaRuleMapper.selectById(ruleId);
        if (before == null || !resourceSetId.equals(before.getResourceSetId())) {
            throw new BizException(404, "rule not found");
        }
        in.setId(ruleId);
        in.setResourceSetId(resourceSetId);
        normalize(in);
        metaRuleMapper.updateById(in);
        MetaRuleDO after = metaRuleMapper.selectById(ruleId);
        operationLogService.log("update", resourceSetId, null, "meta", ruleId, before, after);
        return after;
    }

    public void delete(Long resourceSetId, Long ruleId) {
        MetaRuleDO before = metaRuleMapper.selectById(ruleId);
        if (before == null || !resourceSetId.equals(before.getResourceSetId())) {
            return;
        }
        metaRuleMapper.deleteById(ruleId);
        operationLogService.log("delete", resourceSetId, null, "meta", ruleId, before, null);
    }

    @Transactional
    public InterventionRuleService.BatchImportResult batchImport(Long resourceSetId, List<MetaRuleDO> items) {
        int ok = 0;
        int fail = 0;
        for (MetaRuleDO in : items) {
            try {
                in.setId(null);
                in.setResourceSetId(resourceSetId);
                normalize(in);
                metaRuleMapper.insert(in);
                ok++;
            } catch (Exception e) {
                fail++;
            }
        }
        operationLogService.log("batch_import", resourceSetId, null, "meta", null, null, "ok=" + ok + ",fail=" + fail);
        return InterventionRuleService.BatchImportResult.of(ok, fail);
    }

    public PublishService.ValidateReport validate(Long resourceSetId) {
        List<MetaRuleDO> rules = metaRuleMapper.selectList(
                new LambdaQueryWrapper<MetaRuleDO>().eq(MetaRuleDO::getResourceSetId, resourceSetId));
        for (MetaRuleDO r : rules) {
            String tt = safe(r.getTermType());
            if (!("category".equals(tt) || "brand".equals(tt) || "model".equals(tt))) {
                return PublishService.ValidateReport.fail("meta: invalid term_type");
            }
            if ("category".equals(tt)) {
                if (isBlank(r.getCategoryId()) && isBlank(r.getCategoryName())) {
                    return PublishService.ValidateReport.fail("meta: category fields required");
                }
            }
            if ("brand".equals(tt)) {
                if (isBlank(r.getBrandId()) && isBlank(r.getBrandName())) {
                    return PublishService.ValidateReport.fail("meta: brand fields required");
                }
            }
            if ("model".equals(tt)) {
                if (isBlank(r.getModelId()) && isBlank(r.getModelName())) {
                    return PublishService.ValidateReport.fail("meta: model fields required");
                }
            }
        }
        return PublishService.ValidateReport.ok("meta ok; size=" + rules.size());
    }

    private void normalize(MetaRuleDO in) {
        in.setTermType(trimToNull(in.getTermType()));
        in.setCategoryId(trimToNull(in.getCategoryId()));
        in.setCategoryName(trimToNull(in.getCategoryName()));
        in.setBrandId(trimToNull(in.getBrandId()));
        in.setBrandName(trimToNull(in.getBrandName()));
        in.setBrandNameEn(trimToNull(in.getBrandNameEn()));
        in.setModelId(trimToNull(in.getModelId()));
        in.setModelName(trimToNull(in.getModelName()));
        if (in.getEnabled() == null) in.setEnabled(1);
        String tt = safe(in.getTermType());
        if (!("category".equals(tt) || "brand".equals(tt) || "model".equals(tt))) {
            throw new BizException(400, "invalid term_type");
        }
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private String safe(String s) { return s == null ? "" : s.trim(); }
    private String trimToNull(String s) { if (s == null) return null; String t = s.trim(); return t.isEmpty() ? null : t; }
}
