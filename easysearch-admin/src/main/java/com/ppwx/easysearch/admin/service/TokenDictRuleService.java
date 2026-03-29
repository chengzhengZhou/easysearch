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
import com.ppwx.easysearch.admin.domain.model.ResourceVersionDO;
import com.ppwx.easysearch.admin.domain.model.TokenDictRuleDO;
import com.ppwx.easysearch.admin.mapper.ResourceVersionMapper;
import com.ppwx.easysearch.admin.mapper.TokenDictRuleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TokenDictRuleService {
    private final TokenDictRuleMapper tokenDictRuleMapper;
    private final ResourceVersionMapper resourceVersionMapper;
    private final OperationLogService operationLogService;

    public TokenDictRuleService(TokenDictRuleMapper tokenDictRuleMapper,
                                ResourceVersionMapper resourceVersionMapper,
                                OperationLogService operationLogService) {
        this.tokenDictRuleMapper = tokenDictRuleMapper;
        this.resourceVersionMapper = resourceVersionMapper;
        this.operationLogService = operationLogService;
    }

    @Transactional
    public void copyFromVersion(Long newVersionId, Long baseVersionId) {
        tokenDictRuleMapper.copyFromVersion(newVersionId, baseVersionId);
    }

    public PageResult<TokenDictRuleDO> page(Long versionId, String q, long page, long pageSize) {
        Page<TokenDictRuleDO> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<TokenDictRuleDO> qw = new LambdaQueryWrapper<>();
        qw.eq(TokenDictRuleDO::getVersionId, versionId);
        if (q != null && !q.trim().isEmpty()) {
            qw.and(x -> x.like(TokenDictRuleDO::getWord, q).or().like(TokenDictRuleDO::getNature, q));
        }
        qw.orderByDesc(TokenDictRuleDO::getId);
        Page<TokenDictRuleDO> out = tokenDictRuleMapper.selectPage(p, qw);
        return PageResult.of(out.getCurrent(), out.getSize(), out.getTotal(), out.getRecords());
    }

    public TokenDictRuleDO create(Long versionId, TokenDictRuleDO in) {
        ResourceVersionDO v = ensureDraft(versionId);
        in.setId(null);
        in.setVersionId(versionId);
        normalize(in);
        tokenDictRuleMapper.insert(in);
        operationLogService.log("create", v.getResourceSetId(), versionId, "token", in.getId(), null, in);
        return in;
    }

    public TokenDictRuleDO update(Long versionId, Long ruleId, TokenDictRuleDO in) {
        ResourceVersionDO v = ensureDraft(versionId);
        TokenDictRuleDO before = tokenDictRuleMapper.selectById(ruleId);
        if (before == null || !versionId.equals(before.getVersionId())) throw new BizException(404, "rule not found");
        in.setId(ruleId);
        in.setVersionId(versionId);
        normalize(in);
        tokenDictRuleMapper.updateById(in);
        TokenDictRuleDO after = tokenDictRuleMapper.selectById(ruleId);
        operationLogService.log("update", v.getResourceSetId(), versionId, "token", ruleId, before, after);
        return after;
    }

    public void delete(Long versionId, Long ruleId) {
        ResourceVersionDO v = ensureDraft(versionId);
        TokenDictRuleDO before = tokenDictRuleMapper.selectById(ruleId);
        if (before == null || !versionId.equals(before.getVersionId())) return;
        tokenDictRuleMapper.deleteById(ruleId);
        operationLogService.log("delete", v.getResourceSetId(), versionId, "token", ruleId, before, null);
    }

    @Transactional
    public InterventionRuleService.BatchImportResult batchImport(Long versionId, List<TokenDictRuleDO> items) {
        ResourceVersionDO v = ensureDraft(versionId);
        int ok = 0;
        int fail = 0;
        for (TokenDictRuleDO in : items) {
            try {
                in.setId(null);
                in.setVersionId(versionId);
                normalize(in);
                tokenDictRuleMapper.insert(in);
                ok++;
            } catch (Exception e) {
                fail++;
            }
        }
        operationLogService.log("batch_import", v.getResourceSetId(), versionId, "token", null, null, "ok=" + ok + ",fail=" + fail);
        return InterventionRuleService.BatchImportResult.of(ok, fail);
    }

    public VersionService.ValidateReport validate(Long versionId) {
        List<TokenDictRuleDO> rules = tokenDictRuleMapper.selectList(new LambdaQueryWrapper<TokenDictRuleDO>().eq(TokenDictRuleDO::getVersionId, versionId));
        for (TokenDictRuleDO r : rules) {
            if (isBlank(r.getWord()) || isBlank(r.getNature())) return VersionService.ValidateReport.fail("token: word/nature required");
            if (r.getFrequency() != null && r.getFrequency() < 0) return VersionService.ValidateReport.fail("token: frequency must be non-negative");
            if (isBlank(r.getDictType()) || !("dic".equals(r.getDictType()) || "id".equals(r.getDictType()))) {
                return VersionService.ValidateReport.fail("token: dictType must be dic/id");
            }
        }
        return VersionService.ValidateReport.ok("token ok; size=" + rules.size());
    }

    private void normalize(TokenDictRuleDO in) {
        in.setWord(trimToNull(in.getWord()));
        in.setNature(trimToNull(in.getNature()));
        if (in.getNature() == null) in.setNature("NN");
        if (in.getEnabled() == null) in.setEnabled(1);
        if (in.getDictType() == null || in.getDictType().trim().isEmpty()) in.setDictType("dic");
        if (in.getFrequency() != null && in.getFrequency() < 0) throw new BizException(400, "frequency must be non-negative");
        if (!("dic".equals(in.getDictType()) || "id".equals(in.getDictType()))) throw new BizException(400, "dictType must be dic/id");
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

