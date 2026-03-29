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
import com.ppwx.easysearch.admin.domain.model.ResourceVersionDO;
import com.ppwx.easysearch.admin.mapper.InterventionSentenceRuleMapper;
import com.ppwx.easysearch.admin.mapper.InterventionTermRuleMapper;
import com.ppwx.easysearch.admin.mapper.ResourceVersionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InterventionRuleService {
    private final InterventionSentenceRuleMapper sentenceRuleMapper;
    private final InterventionTermRuleMapper termRuleMapper;
    private final ResourceVersionMapper resourceVersionMapper;
    private final OperationLogService operationLogService;

    public InterventionRuleService(InterventionSentenceRuleMapper sentenceRuleMapper,
                                   InterventionTermRuleMapper termRuleMapper,
                                   ResourceVersionMapper resourceVersionMapper,
                                   OperationLogService operationLogService) {
        this.sentenceRuleMapper = sentenceRuleMapper;
        this.termRuleMapper = termRuleMapper;
        this.resourceVersionMapper = resourceVersionMapper;
        this.operationLogService = operationLogService;
    }

    @Transactional
    public void copyAllFromVersion(Long newVersionId, Long baseVersionId) {
        sentenceRuleMapper.copyFromVersion(newVersionId, baseVersionId);
        termRuleMapper.copyFromVersion(newVersionId, baseVersionId);
    }

    public PageResult<InterventionSentenceRuleDO> pageSentence(Long versionId, String q, long page, long pageSize) {
        Page<InterventionSentenceRuleDO> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<InterventionSentenceRuleDO> qw = new LambdaQueryWrapper<>();
        qw.eq(InterventionSentenceRuleDO::getVersionId, versionId);
        if (q != null && !q.trim().isEmpty()) {
            qw.and(x -> x.like(InterventionSentenceRuleDO::getSourceText, q).or().like(InterventionSentenceRuleDO::getTargetText, q));
        }
        qw.orderByDesc(InterventionSentenceRuleDO::getPriority).orderByDesc(InterventionSentenceRuleDO::getId);
        Page<InterventionSentenceRuleDO> out = sentenceRuleMapper.selectPage(p, qw);
        return PageResult.of(out.getCurrent(), out.getSize(), out.getTotal(), out.getRecords());
    }

    public PageResult<InterventionTermRuleDO> pageTerm(Long versionId, String q, long page, long pageSize) {
        Page<InterventionTermRuleDO> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<InterventionTermRuleDO> qw = new LambdaQueryWrapper<>();
        qw.eq(InterventionTermRuleDO::getVersionId, versionId);
        if (q != null && !q.trim().isEmpty()) {
            qw.and(x -> x.like(InterventionTermRuleDO::getSourceText, q).or().like(InterventionTermRuleDO::getTargetText, q));
        }
        qw.orderByDesc(InterventionTermRuleDO::getPriority).orderByDesc(InterventionTermRuleDO::getId);
        Page<InterventionTermRuleDO> out = termRuleMapper.selectPage(p, qw);
        return PageResult.of(out.getCurrent(), out.getSize(), out.getTotal(), out.getRecords());
    }

    public InterventionSentenceRuleDO createSentence(Long versionId, InterventionSentenceRuleDO in) {
        ResourceVersionDO v = ensureDraft(versionId);
        in.setId(null);
        in.setVersionId(versionId);
        normalizeSentence(in);
        sentenceRuleMapper.insert(in);
        operationLogService.log("create", v.getResourceSetId(), versionId, "intervention_sentence", in.getId(), null, in);
        return in;
    }

    public InterventionSentenceRuleDO updateSentence(Long versionId, Long ruleId, InterventionSentenceRuleDO in) {
        ResourceVersionDO v = ensureDraft(versionId);
        InterventionSentenceRuleDO before = sentenceRuleMapper.selectById(ruleId);
        if (before == null || !versionId.equals(before.getVersionId())) {
            throw new BizException(404, "rule not found");
        }
        in.setId(ruleId);
        in.setVersionId(versionId);
        normalizeSentence(in);
        sentenceRuleMapper.updateById(in);
        InterventionSentenceRuleDO after = sentenceRuleMapper.selectById(ruleId);
        operationLogService.log("update", v.getResourceSetId(), versionId, "intervention_sentence", ruleId, before, after);
        return after;
    }

    public void deleteSentence(Long versionId, Long ruleId) {
        ResourceVersionDO v = ensureDraft(versionId);
        InterventionSentenceRuleDO before = sentenceRuleMapper.selectById(ruleId);
        if (before == null || !versionId.equals(before.getVersionId())) {
            return;
        }
        sentenceRuleMapper.deleteById(ruleId);
        operationLogService.log("delete", v.getResourceSetId(), versionId, "intervention_sentence", ruleId, before, null);
    }

    public InterventionTermRuleDO createTerm(Long versionId, InterventionTermRuleDO in) {
        ResourceVersionDO v = ensureDraft(versionId);
        in.setId(null);
        in.setVersionId(versionId);
        normalizeTerm(in);
        termRuleMapper.insert(in);
        operationLogService.log("create", v.getResourceSetId(), versionId, "intervention_term", in.getId(), null, in);
        return in;
    }

    public InterventionTermRuleDO updateTerm(Long versionId, Long ruleId, InterventionTermRuleDO in) {
        ResourceVersionDO v = ensureDraft(versionId);
        InterventionTermRuleDO before = termRuleMapper.selectById(ruleId);
        if (before == null || !versionId.equals(before.getVersionId())) {
            throw new BizException(404, "rule not found");
        }
        in.setId(ruleId);
        in.setVersionId(versionId);
        normalizeTerm(in);
        termRuleMapper.updateById(in);
        InterventionTermRuleDO after = termRuleMapper.selectById(ruleId);
        operationLogService.log("update", v.getResourceSetId(), versionId, "intervention_term", ruleId, before, after);
        return after;
    }

    public void deleteTerm(Long versionId, Long ruleId) {
        ResourceVersionDO v = ensureDraft(versionId);
        InterventionTermRuleDO before = termRuleMapper.selectById(ruleId);
        if (before == null || !versionId.equals(before.getVersionId())) {
            return;
        }
        termRuleMapper.deleteById(ruleId);
        operationLogService.log("delete", v.getResourceSetId(), versionId, "intervention_term", ruleId, before, null);
    }

    public VersionService.ValidateReport validateIntervention(Long versionId) {
        List<InterventionSentenceRuleDO> sentence = sentenceRuleMapper.selectList(
                new LambdaQueryWrapper<InterventionSentenceRuleDO>().eq(InterventionSentenceRuleDO::getVersionId, versionId));
        for (InterventionSentenceRuleDO r : sentence) {
            String s = safe(r.getSourceText());
            String t = safe(r.getTargetText());
            if (s.isEmpty() || t.isEmpty()) {
                return VersionService.ValidateReport.fail("intervention sentence: source/target required");
            }
            if (!isMatchTypeValid(r.getMatchType())) {
                return VersionService.ValidateReport.fail("intervention sentence: invalid matchType " + r.getMatchType());
            }
            if (r.getPriority() != null && (r.getPriority() < -999 || r.getPriority() > 999)) {
                return VersionService.ValidateReport.fail("intervention sentence: priority out of range");
            }
        }

        List<InterventionTermRuleDO> term = termRuleMapper.selectList(
                new LambdaQueryWrapper<InterventionTermRuleDO>().eq(InterventionTermRuleDO::getVersionId, versionId));
        for (InterventionTermRuleDO r : term) {
            String s = safe(r.getSourceText());
            String t = safe(r.getTargetText());
            if (s.isEmpty() || t.isEmpty()) {
                return VersionService.ValidateReport.fail("intervention term: source/target required");
            }
            if (r.getPriority() != null && (r.getPriority() < -999 || r.getPriority() > 999)) {
                return VersionService.ValidateReport.fail("intervention term: priority out of range");
            }
        }

        return VersionService.ValidateReport.ok("intervention ok; sentence=" + sentence.size() + ", term=" + term.size());
    }

    @Transactional
    public BatchImportResult batchCreateSentence(Long versionId, List<InterventionSentenceRuleDO> items) {
        ResourceVersionDO v = ensureDraft(versionId);
        int ok = 0;
        int fail = 0;
        for (InterventionSentenceRuleDO in : items) {
            try {
                in.setId(null);
                in.setVersionId(versionId);
                normalizeSentence(in);
                sentenceRuleMapper.insert(in);
                ok++;
            } catch (Exception e) {
                fail++;
            }
        }
        operationLogService.log("batch_import", v.getResourceSetId(), versionId, "intervention_sentence", null, null, "ok=" + ok + ",fail=" + fail);
        return BatchImportResult.of(ok, fail);
    }

    @Transactional
    public BatchImportResult batchCreateTerm(Long versionId, List<InterventionTermRuleDO> items) {
        ResourceVersionDO v = ensureDraft(versionId);
        int ok = 0;
        int fail = 0;
        for (InterventionTermRuleDO in : items) {
            try {
                in.setId(null);
                in.setVersionId(versionId);
                normalizeTerm(in);
                termRuleMapper.insert(in);
                ok++;
            } catch (Exception e) {
                fail++;
            }
        }
        operationLogService.log("batch_import", v.getResourceSetId(), versionId, "intervention_term", null, null, "ok=" + ok + ",fail=" + fail);
        return BatchImportResult.of(ok, fail);
    }

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

    private ResourceVersionDO ensureDraft(Long versionId) {
        ResourceVersionDO v = resourceVersionMapper.selectById(versionId);
        if (v == null) {
            throw new BizException(404, "version not found");
        }
        if (!"draft".equals(v.getStatus())) {
            throw new BizException(400, "only draft can be modified");
        }
        return v;
    }

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
}

