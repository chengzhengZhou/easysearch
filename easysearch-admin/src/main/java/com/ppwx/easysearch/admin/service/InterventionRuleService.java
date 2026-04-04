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
import com.ppwx.easysearch.admin.domain.enums.InterventionMatchType;
import com.ppwx.easysearch.admin.domain.exception.BizException;
import com.ppwx.easysearch.admin.domain.model.InterventionSentenceRuleDO;
import com.ppwx.easysearch.admin.domain.model.InterventionTermRuleDO;
import com.ppwx.easysearch.admin.mapper.InterventionSentenceRuleMapper;
import com.ppwx.easysearch.admin.mapper.InterventionTermRuleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 干预规则服务
 * 简化版：直接操作当前规则表（归属 resourceSetId），无需 versionId
 */
@Service
public class InterventionRuleService {
    private final InterventionSentenceRuleMapper sentenceRuleMapper;
    private final InterventionTermRuleMapper termRuleMapper;
    private final OperationLogService operationLogService;

    public InterventionRuleService(InterventionSentenceRuleMapper sentenceRuleMapper,
                                   InterventionTermRuleMapper termRuleMapper,
                                   OperationLogService operationLogService) {
        this.sentenceRuleMapper = sentenceRuleMapper;
        this.termRuleMapper = termRuleMapper;
        this.operationLogService = operationLogService;
    }

    // ========== 整句干预规则 ==========

    public PageResult<InterventionSentenceRuleDO> pageSentence(Long resourceSetId, String q, long page, long pageSize) {
        Page<InterventionSentenceRuleDO> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<InterventionSentenceRuleDO> qw = new LambdaQueryWrapper<>();
        qw.eq(InterventionSentenceRuleDO::getResourceSetId, resourceSetId);
        if (q != null && !q.trim().isEmpty()) {
            qw.and(x -> x.like(InterventionSentenceRuleDO::getSourceText, q).or().like(InterventionSentenceRuleDO::getTargetText, q));
        }
        qw.orderByDesc(InterventionSentenceRuleDO::getPriority).orderByDesc(InterventionSentenceRuleDO::getId);
        Page<InterventionSentenceRuleDO> out = sentenceRuleMapper.selectPage(p, qw);
        return PageResult.of(out.getCurrent(), out.getSize(), out.getTotal(), out.getRecords());
    }

    public InterventionSentenceRuleDO createSentence(Long resourceSetId, InterventionSentenceRuleDO in) {
        in.setId(null);
        in.setResourceSetId(resourceSetId);
        normalizeSentence(in);
        sentenceRuleMapper.insert(in);
        operationLogService.log("create", resourceSetId, null, "intervention_sentence", in.getId(), null, in);
        return in;
    }

    public InterventionSentenceRuleDO updateSentence(Long resourceSetId, Long ruleId, InterventionSentenceRuleDO in) {
        InterventionSentenceRuleDO before = sentenceRuleMapper.selectById(ruleId);
        if (before == null || !resourceSetId.equals(before.getResourceSetId())) {
            throw new BizException(404, "rule not found");
        }
        in.setId(ruleId);
        in.setResourceSetId(resourceSetId);
        normalizeSentence(in);
        sentenceRuleMapper.updateById(in);
        InterventionSentenceRuleDO after = sentenceRuleMapper.selectById(ruleId);
        operationLogService.log("update", resourceSetId, null, "intervention_sentence", ruleId, before, after);
        return after;
    }

    public void deleteSentence(Long resourceSetId, Long ruleId) {
        InterventionSentenceRuleDO before = sentenceRuleMapper.selectById(ruleId);
        if (before == null || !resourceSetId.equals(before.getResourceSetId())) {
            return;
        }
        sentenceRuleMapper.deleteById(ruleId);
        operationLogService.log("delete", resourceSetId, null, "intervention_sentence", ruleId, before, null);
    }

    // ========== 词表干预规则 ==========

    public PageResult<InterventionTermRuleDO> pageTerm(Long resourceSetId, String q, long page, long pageSize) {
        Page<InterventionTermRuleDO> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<InterventionTermRuleDO> qw = new LambdaQueryWrapper<>();
        qw.eq(InterventionTermRuleDO::getResourceSetId, resourceSetId);
        if (q != null && !q.trim().isEmpty()) {
            qw.and(x -> x.like(InterventionTermRuleDO::getSourceText, q).or().like(InterventionTermRuleDO::getTargetText, q));
        }
        qw.orderByDesc(InterventionTermRuleDO::getPriority).orderByDesc(InterventionTermRuleDO::getId);
        Page<InterventionTermRuleDO> out = termRuleMapper.selectPage(p, qw);
        return PageResult.of(out.getCurrent(), out.getSize(), out.getTotal(), out.getRecords());
    }

    public InterventionTermRuleDO createTerm(Long resourceSetId, InterventionTermRuleDO in) {
        in.setId(null);
        in.setResourceSetId(resourceSetId);
        normalizeTerm(in);
        termRuleMapper.insert(in);
        operationLogService.log("create", resourceSetId, null, "intervention_term", in.getId(), null, in);
        return in;
    }

    public InterventionTermRuleDO updateTerm(Long resourceSetId, Long ruleId, InterventionTermRuleDO in) {
        InterventionTermRuleDO before = termRuleMapper.selectById(ruleId);
        if (before == null || !resourceSetId.equals(before.getResourceSetId())) {
            throw new BizException(404, "rule not found");
        }
        in.setId(ruleId);
        in.setResourceSetId(resourceSetId);
        normalizeTerm(in);
        termRuleMapper.updateById(in);
        InterventionTermRuleDO after = termRuleMapper.selectById(ruleId);
        operationLogService.log("update", resourceSetId, null, "intervention_term", ruleId, before, after);
        return after;
    }

    public void deleteTerm(Long resourceSetId, Long ruleId) {
        InterventionTermRuleDO before = termRuleMapper.selectById(ruleId);
        if (before == null || !resourceSetId.equals(before.getResourceSetId())) {
            return;
        }
        termRuleMapper.deleteById(ruleId);
        operationLogService.log("delete", resourceSetId, null, "intervention_term", ruleId, before, null);
    }

    // ========== 校验 ==========

    public PublishService.ValidateReport validateIntervention(Long resourceSetId) {
        List<InterventionSentenceRuleDO> sentence = sentenceRuleMapper.selectList(
                new LambdaQueryWrapper<InterventionSentenceRuleDO>().eq(InterventionSentenceRuleDO::getResourceSetId, resourceSetId));
        for (InterventionSentenceRuleDO r : sentence) {
            String s = safe(r.getSourceText());
            String t = safe(r.getTargetText());
            if (s.isEmpty() || t.isEmpty()) {
                return PublishService.ValidateReport.fail("intervention sentence: source/target required");
            }
            if (!isMatchTypeValid(r.getMatchType())) {
                return PublishService.ValidateReport.fail("intervention sentence: invalid matchType " + r.getMatchType());
            }
            if (r.getPriority() != null && (r.getPriority() < -999 || r.getPriority() > 999)) {
                return PublishService.ValidateReport.fail("intervention sentence: priority out of range");
            }
        }

        List<InterventionTermRuleDO> term = termRuleMapper.selectList(
                new LambdaQueryWrapper<InterventionTermRuleDO>().eq(InterventionTermRuleDO::getResourceSetId, resourceSetId));
        for (InterventionTermRuleDO r : term) {
            String s = safe(r.getSourceText());
            String t = safe(r.getTargetText());
            if (s.isEmpty() || t.isEmpty()) {
                return PublishService.ValidateReport.fail("intervention term: source/target required");
            }
            if (r.getPriority() != null && (r.getPriority() < -999 || r.getPriority() > 999)) {
                return PublishService.ValidateReport.fail("intervention term: priority out of range");
            }
        }

        return PublishService.ValidateReport.ok("intervention ok; sentence=" + sentence.size() + ", term=" + term.size());
    }

    // ========== 批量操作 ==========

    @Transactional
    public BatchImportResult batchCreateSentence(Long resourceSetId, List<InterventionSentenceRuleDO> items) {
        int ok = 0;
        int fail = 0;
        for (InterventionSentenceRuleDO in : items) {
            try {
                in.setId(null);
                in.setResourceSetId(resourceSetId);
                normalizeSentence(in);
                sentenceRuleMapper.insert(in);
                ok++;
            } catch (Exception e) {
                fail++;
            }
        }
        operationLogService.log("batch_import", resourceSetId, null, "intervention_sentence", null, null, "ok=" + ok + ",fail=" + fail);
        return BatchImportResult.of(ok, fail);
    }

    @Transactional
    public BatchImportResult batchCreateTerm(Long resourceSetId, List<InterventionTermRuleDO> items) {
        int ok = 0;
        int fail = 0;
        for (InterventionTermRuleDO in : items) {
            try {
                in.setId(null);
                in.setResourceSetId(resourceSetId);
                normalizeTerm(in);
                termRuleMapper.insert(in);
                ok++;
            } catch (Exception e) {
                fail++;
            }
        }
        operationLogService.log("batch_import", resourceSetId, null, "intervention_term", null, null, "ok=" + ok + ",fail=" + fail);
        return BatchImportResult.of(ok, fail);
    }

    @Transactional
    public void batchEnableSentence(Long resourceSetId, List<Long> ids, boolean enabled) {
        for (Long id : ids) {
            InterventionSentenceRuleDO rule = sentenceRuleMapper.selectById(id);
            if (rule != null && resourceSetId.equals(rule.getResourceSetId())) {
                rule.setEnabled(enabled ? 1 : 0);
                sentenceRuleMapper.updateById(rule);
            }
        }
        operationLogService.log(enabled ? "batch_enable" : "batch_disable", resourceSetId, null, "intervention_sentence", null, null, "ids=" + ids);
    }

    @Transactional
    public void batchEnableTerm(Long resourceSetId, List<Long> ids, boolean enabled) {
        for (Long id : ids) {
            InterventionTermRuleDO rule = termRuleMapper.selectById(id);
            if (rule != null && resourceSetId.equals(rule.getResourceSetId())) {
                rule.setEnabled(enabled ? 1 : 0);
                termRuleMapper.updateById(rule);
            }
        }
        operationLogService.log(enabled ? "batch_enable" : "batch_disable", resourceSetId, null, "intervention_term", null, null, "ids=" + ids);
    }

    @Transactional
    public void batchDeleteSentence(Long resourceSetId, List<Long> ids) {
        for (Long id : ids) {
            InterventionSentenceRuleDO rule = sentenceRuleMapper.selectById(id);
            if (rule != null && resourceSetId.equals(rule.getResourceSetId())) {
                sentenceRuleMapper.deleteById(id);
            }
        }
        operationLogService.log("batch_delete", resourceSetId, null, "intervention_sentence", null, null, "ids=" + ids);
    }

    @Transactional
    public void batchDeleteTerm(Long resourceSetId, List<Long> ids) {
        for (Long id : ids) {
            InterventionTermRuleDO rule = termRuleMapper.selectById(id);
            if (rule != null && resourceSetId.equals(rule.getResourceSetId())) {
                termRuleMapper.deleteById(id);
            }
        }
        operationLogService.log("batch_delete", resourceSetId, null, "intervention_term", null, null, "ids=" + ids);
    }

    // ========== 内部方法 ==========

    private void normalizeSentence(InterventionSentenceRuleDO in) {
        in.setSourceText(trimToNull(in.getSourceText()));
        in.setTargetText(trimToNull(in.getTargetText()));
        if (in.getMatchType() == null || in.getMatchType().trim().isEmpty()) {
            in.setMatchType(InterventionMatchType.EXACT.name());
        }
        if (!isMatchTypeValid(in.getMatchType())) {
            throw new BizException(400, "invalid matchType");
        }
        if (in.getPriority() == null) {
            in.setPriority(0);
        }
        if (in.getEnabled() == null) {
            in.setEnabled(1);
        }
    }

    private void normalizeTerm(InterventionTermRuleDO in) {
        in.setSourceText(trimToNull(in.getSourceText()));
        in.setTargetText(trimToNull(in.getTargetText()));
        if (in.getPriority() == null) {
            in.setPriority(0);
        }
        if (in.getEnabled() == null) {
            in.setEnabled(1);
        }
    }

    private boolean isMatchTypeValid(String mt) {
        if (mt == null) {
            return false;
        }
        for (InterventionMatchType t : InterventionMatchType.values()) {
            if (t.name().equals(mt)) {
                return true;
            }
        }
        return false;
    }

    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    // ========== 结果类 ==========

    public static class BatchImportResult {
        private int success;
        private int failed;

        public static BatchImportResult of(int success, int failed) {
            BatchImportResult r = new BatchImportResult();
            r.success = success;
            r.failed = failed;
            return r;
        }

        public int getSuccess() {
            return success;
        }

        public void setSuccess(int success) {
            this.success = success;
        }

        public int getFailed() {
            return failed;
        }

        public void setFailed(int failed) {
            this.failed = failed;
        }
    }
}
