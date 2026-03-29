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
import com.ppwx.easysearch.admin.domain.enums.PublishStatus;
import com.ppwx.easysearch.admin.domain.enums.VersionStatus;
import com.ppwx.easysearch.admin.domain.exception.BizException;
import com.ppwx.easysearch.admin.domain.model.PublishRecordDO;
import com.ppwx.easysearch.admin.domain.model.ResourceSetDO;
import com.ppwx.easysearch.admin.domain.model.ResourceVersionDO;
import com.ppwx.easysearch.admin.mapper.PublishRecordMapper;
import com.ppwx.easysearch.admin.mapper.ResourceSetMapper;
import com.ppwx.easysearch.admin.mapper.ResourceVersionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class VersionService {
    private final ResourceSetMapper resourceSetMapper;
    private final ResourceVersionMapper resourceVersionMapper;
    private final PublishRecordMapper publishRecordMapper;
    private final InterventionRuleService interventionRuleService;
    private final SynonymRuleService synonymRuleService;
    private final EntityRuleService entityRuleService;
    private final TokenDictRuleService tokenDictRuleService;
    private final MetaRuleService metaRuleService;
    private final OperationLogService operationLogService;
    private final SecurityUserService securityUserService;

    public VersionService(ResourceSetMapper resourceSetMapper,
                          ResourceVersionMapper resourceVersionMapper,
                          PublishRecordMapper publishRecordMapper,
                          InterventionRuleService interventionRuleService,
                          SynonymRuleService synonymRuleService,
                          EntityRuleService entityRuleService,
                          TokenDictRuleService tokenDictRuleService,
                          MetaRuleService metaRuleService,
                          OperationLogService operationLogService,
                          SecurityUserService securityUserService) {
        this.resourceSetMapper = resourceSetMapper;
        this.resourceVersionMapper = resourceVersionMapper;
        this.publishRecordMapper = publishRecordMapper;
        this.interventionRuleService = interventionRuleService;
        this.synonymRuleService = synonymRuleService;
        this.entityRuleService = entityRuleService;
        this.tokenDictRuleService = tokenDictRuleService;
        this.metaRuleService = metaRuleService;
        this.operationLogService = operationLogService;
        this.securityUserService = securityUserService;
    }

    public PageResult<ResourceVersionDO> listByResourceSet(Long resourceSetId, long page, long pageSize) {
        Page<ResourceVersionDO> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<ResourceVersionDO> qw = new LambdaQueryWrapper<>();
        qw.eq(ResourceVersionDO::getResourceSetId, resourceSetId);
        qw.orderByDesc(ResourceVersionDO::getId);
        Page<ResourceVersionDO> out = resourceVersionMapper.selectPage(p, qw);
        return PageResult.of(out.getCurrent(), out.getSize(), out.getTotal(), out.getRecords());
    }

    @Transactional
    public ResourceVersionDO createDraft(Long resourceSetId, String changeLog, Long basedOnVersionId) {
        ResourceSetDO locked = resourceSetMapper.lockById(resourceSetId);
        if (locked == null) {
            throw new BizException(404, "resource set not found");
        }

        if (locked.getStagingVersionId() != null) {
            ResourceVersionDO byPointer = resourceVersionMapper.selectById(locked.getStagingVersionId());
            if (byPointer != null && VersionStatus.draft.name().equals(byPointer.getStatus())) {
                return byPointer;
            }
        }

        ResourceVersionDO existingDraft = resourceVersionMapper.selectDraft(resourceSetId);
        if (existingDraft != null) {
            locked.setStagingVersionId(existingDraft.getId());
            resourceSetMapper.updateById(locked);
            return existingDraft;
        }

        Long baseVersionId = basedOnVersionId;
        if (baseVersionId == null) {
            baseVersionId = locked.getCurrentVersionId();
        }

        Integer maxNo = resourceVersionMapper.selectMaxVersionNo(resourceSetId);
        int nextNo = maxNo == null ? 1 : (maxNo + 1);

        ResourceVersionDO v = new ResourceVersionDO();
        v.setResourceSetId(resourceSetId);
        v.setVersionNo(nextNo);
        v.setStatus(VersionStatus.draft.name());
        v.setProtectedFlag(0);
        v.setChangeLog(changeLog);
        v.setCreatedBy(securityUserService.currentUserOrSystem());
        resourceVersionMapper.insert(v);

        if (baseVersionId != null) {
            copyRulesByModuleType(locked.getModuleType(), v.getId(), baseVersionId);
        }

        locked.setStagingVersionId(v.getId());
        resourceSetMapper.updateById(locked);
        operationLogService.log("create_draft", resourceSetId, v.getId(), "resource_version", v.getId(), null, v);
        return v;
    }

    /**
     * Reset staging(draft) for a resource set.
     *
     * <p>Behavior:
     * <ul>
     *   <li>lock resource set row</li>
     *   <li>archive existing draft (if any)</li>
     *   <li>create a new draft versionNo=MAX+1</li>
     *   <li>copy rules from base version (default current_version_id) if present</li>
     * </ul>
     *
     * <p>This matches the prototype "重置工作区" semantics: discard unpublished changes and rebase staging on online.</p>
     */
    @Transactional
    public ResourceVersionDO resetStaging(Long resourceSetId, String changeLog, Long basedOnVersionId) {
        ResourceSetDO locked = resourceSetMapper.lockById(resourceSetId);
        if (locked == null) {
            throw new BizException(404, "resource set not found");
        }

        ResourceVersionDO existingDraft = null;
        if (locked.getStagingVersionId() != null) {
            ResourceVersionDO byPointer = resourceVersionMapper.selectById(locked.getStagingVersionId());
            if (byPointer != null && VersionStatus.draft.name().equals(byPointer.getStatus())) {
                existingDraft = byPointer;
            }
        }
        if (existingDraft == null) {
            existingDraft = resourceVersionMapper.selectDraft(resourceSetId);
        }
        if (existingDraft != null) {
            existingDraft.setStatus(VersionStatus.archived.name());
            existingDraft.setArchivedAt(LocalDateTime.now());
            resourceVersionMapper.updateById(existingDraft);
            operationLogService.log("archive_draft", resourceSetId, existingDraft.getId(), "resource_version", existingDraft.getId(), null, existingDraft);
        }

        Long baseVersionId = basedOnVersionId;
        if (baseVersionId == null) {
            baseVersionId = locked.getCurrentVersionId();
        }

        Integer maxNo = resourceVersionMapper.selectMaxVersionNo(resourceSetId);
        int nextNo = maxNo == null ? 1 : (maxNo + 1);

        ResourceVersionDO v = new ResourceVersionDO();
        v.setResourceSetId(resourceSetId);
        v.setVersionNo(nextNo);
        v.setStatus(VersionStatus.draft.name());
        v.setProtectedFlag(0);
        v.setChangeLog(changeLog);
        v.setCreatedBy(securityUserService.currentUserOrSystem());
        resourceVersionMapper.insert(v);

        if (baseVersionId != null) {
            copyRulesByModuleType(locked.getModuleType(), v.getId(), baseVersionId);
        }

        locked.setStagingVersionId(v.getId());
        resourceSetMapper.updateById(locked);
        operationLogService.log("reset_staging", resourceSetId, v.getId(), "resource_version", v.getId(), null, v);
        return v;
    }

    public ValidateReport validate(Long versionId) {
        ResourceVersionDO v = resourceVersionMapper.selectById(versionId);
        if (v == null) {
            throw new BizException(404, "version not found");
        }
        ResourceSetDO rs = resourceSetMapper.selectById(v.getResourceSetId());
        if (rs == null) {
            throw new BizException(404, "resource set not found");
        }
        String moduleType = rs.getModuleType() == null ? "" : rs.getModuleType().trim();
        if ("intervention".equals(moduleType)) return interventionRuleService.validateIntervention(versionId);
        if ("synonym".equals(moduleType)) return synonymRuleService.validate(versionId);
        if ("entity".equals(moduleType)) return entityRuleService.validate(versionId);
        if ("token".equals(moduleType)) return tokenDictRuleService.validate(versionId);
        if ("meta".equals(moduleType)) return metaRuleService.validate(versionId);
        return ValidateReport.fail("unknown module_type: " + moduleType);
    }

    public static class ValidateReport {
        private boolean ok;
        private String summary;

        public static ValidateReport ok(String summary) {
            ValidateReport r = new ValidateReport();
            r.ok = true;
            r.summary = summary;
            return r;
        }

        public static ValidateReport fail(String summary) {
            ValidateReport r = new ValidateReport();
            r.ok = false;
            r.summary = summary;
            return r;
        }

        public boolean isOk() {
            return ok;
        }

        public void setOk(boolean ok) {
            this.ok = ok;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }
    }

    public ApiPublishResult publish(Long versionId) {
        PublishRecordDO record = new PublishRecordDO();
        ResourceVersionDO v = resourceVersionMapper.selectById(versionId);
        if (v == null) {
            throw new BizException(404, "version not found");
        }

        ResourceSetDO rs = resourceSetMapper.selectById(v.getResourceSetId());
        if (rs == null) {
            throw new BizException(404, "resource set not found");
        }

        record.setResourceSetId(rs.getId());
        record.setVersionId(versionId);
        record.setEnv(rs.getEnv());
        record.setPublishStatus(PublishStatus.running.name());
        record.setOperator(securityUserService.currentUserOrSystem());
        publishRecordMapper.insert(record);

        try {
            ValidateReport report = validate(versionId);
            if (!report.isOk()) {
                throw new BizException(400, "validate failed: " + report.getSummary());
            }

            doPublishTxn(rs.getId(), versionId);

            record.setPublishStatus(PublishStatus.success.name());
            record.setPublishMsg("published; reload is reserved (qp not integrated in this phase)");
            record.setFinishedAt(LocalDateTime.now());
            publishRecordMapper.updateById(record);
            return ApiPublishResult.success(record.getId());
        } catch (Exception e) {
            record.setPublishStatus(PublishStatus.failed.name());
            record.setPublishMsg(e.getMessage());
            record.setFinishedAt(LocalDateTime.now());
            publishRecordMapper.updateById(record);
            if (e instanceof BizException) {
                throw (BizException) e;
            }
            throw new BizException(500, "publish failed");
        }
    }

    @Transactional
    protected void doPublishTxn(Long resourceSetId, Long versionId) {
        ResourceSetDO locked = resourceSetMapper.lockById(resourceSetId);
        if (locked == null) {
            throw new BizException(404, "resource set not found");
        }
        ResourceVersionDO v = resourceVersionMapper.selectById(versionId);
        if (v == null) {
            throw new BizException(404, "version not found");
        }
        if (!VersionStatus.draft.name().equals(v.getStatus())) {
            throw new BizException(400, "only draft can be published");
        }
        v.setStatus(VersionStatus.published.name());
        v.setPublishedBy(securityUserService.currentUserOrSystem());
        v.setPublishedAt(LocalDateTime.now());
        resourceVersionMapper.updateById(v);

        locked.setCurrentVersionId(versionId);
        ResourceVersionDO newDraft = createDraftInternalForPublish(locked, versionId);
        locked.setStagingVersionId(newDraft.getId());
        resourceSetMapper.updateById(locked);

        resourceVersionMapper.archiveOtherPublished(resourceSetId, versionId);
        operationLogService.log("publish", resourceSetId, versionId, "resource_version", versionId, null, v);
    }

    @Transactional
    public void rollback(Long resourceSetId, Long toVersionId) {
        ResourceSetDO locked = resourceSetMapper.lockById(resourceSetId);
        if (locked == null) {
            throw new BizException(404, "resource set not found");
        }
        ResourceVersionDO v = resourceVersionMapper.selectById(toVersionId);
        if (v == null || !resourceSetId.equals(v.getResourceSetId())) {
            throw new BizException(400, "invalid toVersion");
        }
        locked.setCurrentVersionId(toVersionId);
        ResourceVersionDO newDraft = resetStaging(resourceSetId, "rollback_sync", toVersionId);
        locked.setStagingVersionId(newDraft.getId());
        resourceSetMapper.updateById(locked);
        operationLogService.log("rollback", resourceSetId, toVersionId, "resource_set", resourceSetId, null, locked);
    }

    private void copyRulesByModuleType(String moduleTypeRaw, Long newVersionId, Long baseVersionId) {
        String moduleType = moduleTypeRaw == null ? "" : moduleTypeRaw.trim();
        if ("intervention".equals(moduleType)) {
            interventionRuleService.copyAllFromVersion(newVersionId, baseVersionId);
        } else if ("synonym".equals(moduleType)) {
            synonymRuleService.copyFromVersion(newVersionId, baseVersionId);
        } else if ("entity".equals(moduleType)) {
            entityRuleService.copyFromVersion(newVersionId, baseVersionId);
        } else if ("token".equals(moduleType)) {
            tokenDictRuleService.copyFromVersion(newVersionId, baseVersionId);
        } else if ("meta".equals(moduleType)) {
            metaRuleService.copyFromVersion(newVersionId, baseVersionId);
        }
    }

    private ResourceVersionDO createDraftInternalForPublish(ResourceSetDO locked, Long baseVersionId) {
        Integer maxNo = resourceVersionMapper.selectMaxVersionNo(locked.getId());
        int nextNo = maxNo == null ? 1 : (maxNo + 1);
        ResourceVersionDO draft = new ResourceVersionDO();
        draft.setResourceSetId(locked.getId());
        draft.setVersionNo(nextNo);
        draft.setStatus(VersionStatus.draft.name());
        draft.setProtectedFlag(0);
        draft.setChangeLog("auto: new staging after publish");
        draft.setCreatedBy(securityUserService.currentUserOrSystem());
        resourceVersionMapper.insert(draft);
        if (baseVersionId != null) {
            copyRulesByModuleType(locked.getModuleType(), draft.getId(), baseVersionId);
        }
        operationLogService.log("create_draft_after_publish", locked.getId(), draft.getId(), "resource_version", draft.getId(), null, draft);
        return draft;
    }

    public static class ApiPublishResult {
        private boolean success;
        private Long publishRecordId;

        public static ApiPublishResult success(Long publishRecordId) {
            ApiPublishResult r = new ApiPublishResult();
            r.success = true;
            r.publishRecordId = publishRecordId;
            return r;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public Long getPublishRecordId() {
            return publishRecordId;
        }

        public void setPublishRecordId(Long publishRecordId) {
            this.publishRecordId = publishRecordId;
        }
    }
}

