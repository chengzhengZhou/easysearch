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
import com.ppwx.easysearch.admin.domain.model.TokenDictRuleDO;
import com.ppwx.easysearch.admin.mapper.TokenDictRuleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 分词词典规则服务
 * 简化版：直接操作当前规则表（归属 resourceSetId），无需 versionId
 */
@Service
public class TokenDictRuleService {
    private final TokenDictRuleMapper tokenDictRuleMapper;
    private final OperationLogService operationLogService;

    public TokenDictRuleService(TokenDictRuleMapper tokenDictRuleMapper,
                                OperationLogService operationLogService) {
        this.tokenDictRuleMapper = tokenDictRuleMapper;
        this.operationLogService = operationLogService;
    }

    public PageResult<TokenDictRuleDO> page(Long resourceSetId, String q, long page, long pageSize) {
        Page<TokenDictRuleDO> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<TokenDictRuleDO> qw = new LambdaQueryWrapper<>();
        qw.eq(TokenDictRuleDO::getResourceSetId, resourceSetId);
        if (q != null && !q.trim().isEmpty()) {
            qw.and(x -> x.like(TokenDictRuleDO::getWord, q).or().like(TokenDictRuleDO::getNature, q));
        }
        qw.orderByDesc(TokenDictRuleDO::getId);
        Page<TokenDictRuleDO> out = tokenDictRuleMapper.selectPage(p, qw);
        return PageResult.of(out.getCurrent(), out.getSize(), out.getTotal(), out.getRecords());
    }

    public TokenDictRuleDO create(Long resourceSetId, TokenDictRuleDO in) {
        in.setId(null);
        in.setResourceSetId(resourceSetId);
        normalize(in);
        tokenDictRuleMapper.insert(in);
        operationLogService.log("create", resourceSetId, null, "token", in.getId(), null, in);
        return in;
    }

    public TokenDictRuleDO update(Long resourceSetId, Long ruleId, TokenDictRuleDO in) {
        TokenDictRuleDO before = tokenDictRuleMapper.selectById(ruleId);
        if (before == null || !resourceSetId.equals(before.getResourceSetId())) {
            throw new BizException(404, "rule not found");
        }
        in.setId(ruleId);
        in.setResourceSetId(resourceSetId);
        normalize(in);
        tokenDictRuleMapper.updateById(in);
        TokenDictRuleDO after = tokenDictRuleMapper.selectById(ruleId);
        operationLogService.log("update", resourceSetId, null, "token", ruleId, before, after);
        return after;
    }

    public void delete(Long resourceSetId, Long ruleId) {
        TokenDictRuleDO before = tokenDictRuleMapper.selectById(ruleId);
        if (before == null || !resourceSetId.equals(before.getResourceSetId())) {
            return;
        }
        tokenDictRuleMapper.deleteById(ruleId);
        operationLogService.log("delete", resourceSetId, null, "token", ruleId, before, null);
    }

    @Transactional
    public InterventionRuleService.BatchImportResult batchImport(Long resourceSetId, List<TokenDictRuleDO> items) {
        int ok = 0;
        int fail = 0;
        for (TokenDictRuleDO in : items) {
            try {
                in.setId(null);
                in.setResourceSetId(resourceSetId);
                normalize(in);
                tokenDictRuleMapper.insert(in);
                ok++;
            } catch (Exception e) {
                fail++;
            }
        }
        operationLogService.log("batch_import", resourceSetId, null, "token", null, null, "ok=" + ok + ",fail=" + fail);
        return InterventionRuleService.BatchImportResult.of(ok, fail);
    }

    public PublishService.ValidateReport validate(Long resourceSetId) {
        List<TokenDictRuleDO> rules = tokenDictRuleMapper.selectList(
                new LambdaQueryWrapper<TokenDictRuleDO>().eq(TokenDictRuleDO::getResourceSetId, resourceSetId));
        for (TokenDictRuleDO r : rules) {
            if (isBlank(r.getWord()) || isBlank(r.getNature())) {
                return PublishService.ValidateReport.fail("token: word/nature required");
            }
            if (r.getFrequency() != null && r.getFrequency() < 0) {
                return PublishService.ValidateReport.fail("token: frequency must be non-negative");
            }
        }
        return PublishService.ValidateReport.ok("token ok; size=" + rules.size());
    }

    /**
     * 批量启用/停用规则
     */
    @Transactional
    public void batchEnable(Long resourceSetId, List<Long> ids, boolean enabled) {
        if (ids == null || ids.isEmpty()) return;
        for (Long id : ids) {
            TokenDictRuleDO rule = tokenDictRuleMapper.selectById(id);
            if (rule != null && resourceSetId.equals(rule.getResourceSetId())) {
                rule.setEnabled(enabled ? 1 : 0);
                tokenDictRuleMapper.updateById(rule);
            }
        }
        operationLogService.log(enabled ? "batch_enable" : "batch_disable", resourceSetId, null, "token", null, null, "ids=" + ids);
    }

    /**
     * 批量删除规则
     */
    @Transactional
    public void batchDelete(Long resourceSetId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        for (Long id : ids) {
            TokenDictRuleDO rule = tokenDictRuleMapper.selectById(id);
            if (rule != null && resourceSetId.equals(rule.getResourceSetId())) {
                tokenDictRuleMapper.deleteById(id);
            }
        }
        operationLogService.log("batch_delete", resourceSetId, null, "token", null, null, "ids=" + ids);
    }

    private void normalize(TokenDictRuleDO in) {
        in.setWord(trimToNull(in.getWord()));
        in.setNature(trimToNull(in.getNature()));
        if (in.getNature() == null) in.setNature("NN");
        if (in.getEnabled() == null) in.setEnabled(1);
        if (in.getFrequency() != null && in.getFrequency() < 0) {
            throw new BizException(400, "frequency must be non-negative");
        }
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
