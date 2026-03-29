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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BatchOperationService {
    private final ResourceVersionMapper resourceVersionMapper;
    private final OperationLogService operationLogService;
    private final InterventionSentenceRuleMapper sentenceRuleMapper;
    private final InterventionTermRuleMapper termRuleMapper;
    private final SynonymRuleMapper synonymRuleMapper;
    private final EntityRuleMapper entityRuleMapper;
    private final TokenDictRuleMapper tokenDictRuleMapper;
    private final MetaRuleMapper metaRuleMapper;

    public BatchOperationService(ResourceVersionMapper resourceVersionMapper,
                                 OperationLogService operationLogService,
                                 InterventionSentenceRuleMapper sentenceRuleMapper,
                                 InterventionTermRuleMapper termRuleMapper,
                                 SynonymRuleMapper synonymRuleMapper,
                                 EntityRuleMapper entityRuleMapper,
                                 TokenDictRuleMapper tokenDictRuleMapper,
                                 MetaRuleMapper metaRuleMapper) {
        this.resourceVersionMapper = resourceVersionMapper;
        this.operationLogService = operationLogService;
        this.sentenceRuleMapper = sentenceRuleMapper;
        this.termRuleMapper = termRuleMapper;
        this.synonymRuleMapper = synonymRuleMapper;
        this.entityRuleMapper = entityRuleMapper;
        this.tokenDictRuleMapper = tokenDictRuleMapper;
        this.metaRuleMapper = metaRuleMapper;
    }

    @Transactional
    public BatchOpResult enable(Long versionId, RuleModule module, InterventionMode mode, List<Long> ids) {
        return setEnabled(versionId, module, mode, ids, 1);
    }

    @Transactional
    public BatchOpResult disable(Long versionId, RuleModule module, InterventionMode mode, List<Long> ids) {
        return setEnabled(versionId, module, mode, ids, 0);
    }

    @Transactional
    public BatchOpResult delete(Long versionId, RuleModule module, InterventionMode mode, List<Long> ids) {
        ResourceVersionDO v = ensureDraft(versionId);
        int affected = 0;
        if (module == RuleModule.intervention) {
            InterventionMode m = mode == null ? InterventionMode.sentence : mode;
            if (m == InterventionMode.sentence) {
                affected = sentenceRuleMapper.delete(new LambdaQueryWrapper<InterventionSentenceRuleDO>()
                        .eq(InterventionSentenceRuleDO::getVersionId, versionId).in(InterventionSentenceRuleDO::getId, ids));
            } else {
                affected = termRuleMapper.delete(new LambdaQueryWrapper<InterventionTermRuleDO>()
                        .eq(InterventionTermRuleDO::getVersionId, versionId).in(InterventionTermRuleDO::getId, ids));
            }
        } else if (module == RuleModule.synonym) {
            affected = synonymRuleMapper.delete(new LambdaQueryWrapper<SynonymRuleDO>()
                    .eq(SynonymRuleDO::getVersionId, versionId).in(SynonymRuleDO::getId, ids));
        } else if (module == RuleModule.entity) {
            affected = entityRuleMapper.delete(new LambdaQueryWrapper<EntityRuleDO>()
                    .eq(EntityRuleDO::getVersionId, versionId).in(EntityRuleDO::getId, ids));
        } else if (module == RuleModule.token) {
            affected = tokenDictRuleMapper.delete(new LambdaQueryWrapper<TokenDictRuleDO>()
                    .eq(TokenDictRuleDO::getVersionId, versionId).in(TokenDictRuleDO::getId, ids));
        } else if (module == RuleModule.meta) {
            affected = metaRuleMapper.delete(new LambdaQueryWrapper<MetaRuleDO>()
                    .eq(MetaRuleDO::getVersionId, versionId).in(MetaRuleDO::getId, ids));
        } else {
            throw new BizException(400, "module not supported");
        }
        operationLogService.log("batch_delete", v.getResourceSetId(), versionId, module.name(), null, null, "ids=" + ids);
        return BatchOpResult.of(affected);
    }

    private BatchOpResult setEnabled(Long versionId, RuleModule module, InterventionMode mode, List<Long> ids, int enabled) {
        ResourceVersionDO v = ensureDraft(versionId);
        int affected = 0;
        if (module == RuleModule.intervention) {
            InterventionMode m = mode == null ? InterventionMode.sentence : mode;
            if (m == InterventionMode.sentence) {
                InterventionSentenceRuleDO upd = new InterventionSentenceRuleDO();
                upd.setEnabled(enabled);
                affected = sentenceRuleMapper.update(upd, new LambdaQueryWrapper<InterventionSentenceRuleDO>()
                        .eq(InterventionSentenceRuleDO::getVersionId, versionId).in(InterventionSentenceRuleDO::getId, ids));
            } else {
                InterventionTermRuleDO upd = new InterventionTermRuleDO();
                upd.setEnabled(enabled);
                affected = termRuleMapper.update(upd, new LambdaQueryWrapper<InterventionTermRuleDO>()
                        .eq(InterventionTermRuleDO::getVersionId, versionId).in(InterventionTermRuleDO::getId, ids));
            }
        } else if (module == RuleModule.synonym) {
            SynonymRuleDO upd = new SynonymRuleDO();
            upd.setEnabled(enabled);
            affected = synonymRuleMapper.update(upd, new LambdaQueryWrapper<SynonymRuleDO>()
                    .eq(SynonymRuleDO::getVersionId, versionId).in(SynonymRuleDO::getId, ids));
        } else if (module == RuleModule.entity) {
            EntityRuleDO upd = new EntityRuleDO();
            upd.setEnabled(enabled);
            affected = entityRuleMapper.update(upd, new LambdaQueryWrapper<EntityRuleDO>()
                    .eq(EntityRuleDO::getVersionId, versionId).in(EntityRuleDO::getId, ids));
        } else if (module == RuleModule.token) {
            TokenDictRuleDO upd = new TokenDictRuleDO();
            upd.setEnabled(enabled);
            affected = tokenDictRuleMapper.update(upd, new LambdaQueryWrapper<TokenDictRuleDO>()
                    .eq(TokenDictRuleDO::getVersionId, versionId).in(TokenDictRuleDO::getId, ids));
        } else if (module == RuleModule.meta) {
            MetaRuleDO upd = new MetaRuleDO();
            upd.setEnabled(enabled);
            affected = metaRuleMapper.update(upd, new LambdaQueryWrapper<MetaRuleDO>()
                    .eq(MetaRuleDO::getVersionId, versionId).in(MetaRuleDO::getId, ids));
        } else {
            throw new BizException(400, "module not supported");
        }
        operationLogService.log(enabled == 1 ? "batch_enable" : "batch_disable", v.getResourceSetId(), versionId, module.name(), null, null, "ids=" + ids);
        return BatchOpResult.of(affected);
    }

    private ResourceVersionDO ensureDraft(Long versionId) {
        ResourceVersionDO v = resourceVersionMapper.selectById(versionId);
        if (v == null) throw new BizException(404, "version not found");
        if (!"draft".equals(v.getStatus())) throw new BizException(400, "only draft can be modified");
        return v;
    }

    public static class BatchOpResult {
        private int affected;

        public static BatchOpResult of(int affected) {
            BatchOpResult r = new BatchOpResult();
            r.affected = affected;
            return r;
        }

        public int getAffected() {
            return affected;
        }

        public void setAffected(int affected) {
            this.affected = affected;
        }
    }
}

