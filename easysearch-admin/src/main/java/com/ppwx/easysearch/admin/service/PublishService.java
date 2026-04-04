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
import com.ppwx.easysearch.admin.domain.exception.BizException;
import com.ppwx.easysearch.admin.domain.model.PublishRecordDO;
import com.ppwx.easysearch.admin.domain.model.ResourceSetDO;
import com.ppwx.easysearch.admin.domain.model.SnapshotDO;
import com.ppwx.easysearch.admin.mapper.PublishRecordMapper;
import com.ppwx.easysearch.admin.mapper.ResourceSetMapper;
import com.ppwx.easysearch.admin.mapper.SnapshotMapper;
import com.ppwx.easysearch.admin.mapper.SnapshotInterventionSentenceMapper;
import com.ppwx.easysearch.admin.mapper.SnapshotInterventionTermMapper;
import com.ppwx.easysearch.admin.mapper.SnapshotSynonymMapper;
import com.ppwx.easysearch.admin.mapper.SnapshotEntityMapper;
import com.ppwx.easysearch.admin.mapper.InterventionSentenceRuleMapper;
import com.ppwx.easysearch.admin.mapper.InterventionTermRuleMapper;
import com.ppwx.easysearch.admin.mapper.SynonymRuleMapper;
import com.ppwx.easysearch.admin.mapper.EntityRuleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 发布与快照服务
 * 简化版：去掉 staging/draft 概念，改为就地编辑 + 发布快照
 */
@Service
public class PublishService {
    private final ResourceSetMapper resourceSetMapper;
    private final SnapshotMapper snapshotMapper;
    private final PublishRecordMapper publishRecordMapper;
    private final SnapshotInterventionSentenceMapper snapshotSentenceMapper;
    private final SnapshotInterventionTermMapper snapshotTermMapper;
    private final SnapshotSynonymMapper snapshotSynonymMapper;
    private final SnapshotEntityMapper snapshotEntityMapper;
    private final InterventionSentenceRuleMapper sentenceRuleMapper;
    private final InterventionTermRuleMapper termRuleMapper;
    private final SynonymRuleMapper synonymRuleMapper;
    private final EntityRuleMapper entityRuleMapper;
    private final InterventionRuleService interventionRuleService;
    private final SynonymRuleService synonymRuleService;
    private final EntityRuleService entityRuleService;
    private final OperationLogService operationLogService;
    private final SecurityUserService securityUserService;

    public PublishService(ResourceSetMapper resourceSetMapper,
                          SnapshotMapper snapshotMapper,
                          PublishRecordMapper publishRecordMapper,
                          SnapshotInterventionSentenceMapper snapshotSentenceMapper,
                          SnapshotInterventionTermMapper snapshotTermMapper,
                          SnapshotSynonymMapper snapshotSynonymMapper,
                          SnapshotEntityMapper snapshotEntityMapper,
                          InterventionSentenceRuleMapper sentenceRuleMapper,
                          InterventionTermRuleMapper termRuleMapper,
                          SynonymRuleMapper synonymRuleMapper,
                          EntityRuleMapper entityRuleMapper,
                          InterventionRuleService interventionRuleService,
                          SynonymRuleService synonymRuleService,
                          EntityRuleService entityRuleService,
                          OperationLogService operationLogService,
                          SecurityUserService securityUserService) {
        this.resourceSetMapper = resourceSetMapper;
        this.snapshotMapper = snapshotMapper;
        this.publishRecordMapper = publishRecordMapper;
        this.snapshotSentenceMapper = snapshotSentenceMapper;
        this.snapshotTermMapper = snapshotTermMapper;
        this.snapshotSynonymMapper = snapshotSynonymMapper;
        this.snapshotEntityMapper = snapshotEntityMapper;
        this.sentenceRuleMapper = sentenceRuleMapper;
        this.termRuleMapper = termRuleMapper;
        this.synonymRuleMapper = synonymRuleMapper;
        this.entityRuleMapper = entityRuleMapper;
        this.interventionRuleService = interventionRuleService;
        this.synonymRuleService = synonymRuleService;
        this.entityRuleService = entityRuleService;
        this.operationLogService = operationLogService;
        this.securityUserService = securityUserService;
    }

    /**
     * 列出历史快照
     */
    public PageResult<SnapshotDO> listSnapshots(Long resourceSetId, long page, long pageSize) {
        Page<SnapshotDO> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<SnapshotDO> qw = new LambdaQueryWrapper<>();
        qw.eq(SnapshotDO::getResourceSetId, resourceSetId);
        qw.orderByDesc(SnapshotDO::getSnapshotNo);
        Page<SnapshotDO> out = snapshotMapper.selectPage(p, qw);
        return PageResult.of(out.getCurrent(), out.getSize(), out.getTotal(), out.getRecords());
    }

    /**
     * 校验当前规则
     */
    public ValidateReport validate(Long resourceSetId) {
        ResourceSetDO rs = resourceSetMapper.selectById(resourceSetId);
        if (rs == null) {
            throw new BizException(404, "resource set not found");
        }
        String moduleType = rs.getModuleType() == null ? "" : rs.getModuleType().trim();
        if ("intervention".equals(moduleType)) {
            return interventionRuleService.validateIntervention(resourceSetId);
        }
        if ("synonym".equals(moduleType)) {
            return synonymRuleService.validate(resourceSetId);
        }
        if ("entity".equals(moduleType)) {
            return entityRuleService.validate(resourceSetId);
        }
        // 其他模块可扩展...
        return ValidateReport.fail("unknown module_type: " + moduleType);
    }

    /**
     * 发布：将当前规则表打快照推线上
     */
    public ApiPublishResult publish(Long resourceSetId, String changeLog) {
        ResourceSetDO rs = resourceSetMapper.selectById(resourceSetId);
        if (rs == null) {
            throw new BizException(404, "resource set not found");
        }

        // 先校验
        ValidateReport report = validate(resourceSetId);
        if (!report.isOk()) {
            throw new BizException(400, "validate failed: " + report.getSummary());
        }

        // 创建发布记录
        PublishRecordDO record = new PublishRecordDO();
        record.setResourceSetId(resourceSetId);
        record.setEnv(rs.getEnv());
        record.setPublishStatus(PublishStatus.running.name());
        record.setOperator(securityUserService.currentUserOrSystem());
        publishRecordMapper.insert(record);

        try {
            SnapshotDO snapshot = doPublishTxn(rs, changeLog);
            record.setSnapshotId(snapshot.getId());
            record.setPublishStatus(PublishStatus.success.name());
            record.setPublishMsg("published; snapshot_no=" + snapshot.getSnapshotNo() + "; reload is reserved");
            record.setFinishedAt(LocalDateTime.now());
            publishRecordMapper.updateById(record);
            return ApiPublishResult.success(record.getId(), snapshot.getId());
        } catch (Exception e) {
            record.setPublishStatus(PublishStatus.failed.name());
            record.setPublishMsg(e.getMessage());
            record.setFinishedAt(LocalDateTime.now());
            publishRecordMapper.updateById(record);
            if (e instanceof BizException) {
                throw (BizException) e;
            }
            throw new BizException(500, "publish failed: " + e.getMessage());
        }
    }

    @Transactional
    protected SnapshotDO doPublishTxn(ResourceSetDO rs, String changeLog) {
        Long resourceSetId = rs.getId();
        ResourceSetDO locked = resourceSetMapper.lockById(resourceSetId);
        if (locked == null) {
            throw new BizException(404, "resource set not found");
        }

        // 1. 创建快照元信息
        Integer maxNo = snapshotMapper.selectMaxSnapshotNo(resourceSetId);
        int nextNo = (maxNo == null ? 0 : maxNo) + 1;

        SnapshotDO snapshot = new SnapshotDO();
        snapshot.setResourceSetId(resourceSetId);
        snapshot.setSnapshotNo(nextNo);
        snapshot.setChangeLog(changeLog);
        snapshot.setPublishedBy(securityUserService.currentUserOrSystem());
        snapshot.setPublishedAt(LocalDateTime.now());
        snapshot.setProtectedFlag(0);

        // 2. 按模块复制规则到快照表
        int ruleCount = copyRulesToSnapshot(locked.getModuleType(), resourceSetId, snapshot);
        snapshot.setRuleCount(ruleCount);
        // 注意：copyRulesToSnapshot 内部对于 intervention 模块已经执行过 insert，
        // 只有当 snapshot.getId() 为 null 时才需要再次 insert（其他模块场景）
        if (snapshot.getId() == null) {
            snapshotMapper.insert(snapshot);
        }

        // 3. 更新生效指针（放在最后）
        locked.setCurrentSnapshotId(snapshot.getId());
        resourceSetMapper.updateById(locked);

        operationLogService.log("publish", resourceSetId, snapshot.getId(), "snapshot", snapshot.getId(), null, snapshot);
        return snapshot;
    }

    private int copyRulesToSnapshot(String moduleTypeRaw, Long resourceSetId, SnapshotDO snapshot) {
        String moduleType = moduleTypeRaw == null ? "" : moduleTypeRaw.trim();
        int count = 0;

        if ("intervention".equals(moduleType)) {
            // 先插入快照记录以获取 ID
            snapshotMapper.insert(snapshot);
            Long snapshotId = snapshot.getId();

            // 复制整句干预规则
            int sentenceCount = snapshotSentenceMapper.copyFromRuleTable(snapshotId, resourceSetId);
            // 复制词表干预规则
            int termCount = snapshotTermMapper.copyFromRuleTable(snapshotId, resourceSetId);
            count = sentenceCount + termCount;

            // 由于已经 insert，后面不需要再 insert
            snapshot.setId(snapshotId);
            snapshot.setRuleCount(count);
            snapshotMapper.updateById(snapshot);
            return count;
        }

        if ("synonym".equals(moduleType)) {
            // 先插入快照记录以获取 ID
            snapshotMapper.insert(snapshot);
            Long snapshotId = snapshot.getId();

            // 复制同义词规则
            count = snapshotSynonymMapper.copyFromRuleTable(snapshotId, resourceSetId);

            snapshot.setId(snapshotId);
            snapshot.setRuleCount(count);
            snapshotMapper.updateById(snapshot);
            return count;
        }

        if ("entity".equals(moduleType)) {
            // 先插入快照记录以获取 ID
            snapshotMapper.insert(snapshot);
            Long snapshotId = snapshot.getId();

            // 复制实体规则
            count = snapshotEntityMapper.copyFromRuleTable(snapshotId, resourceSetId);

            snapshot.setId(snapshotId);
            snapshot.setRuleCount(count);
            snapshotMapper.updateById(snapshot);
            return count;
        }

        // 其他模块可扩展...
        return count;
    }

    /**
     * 回滚：用历史快照覆盖当前规则并切换指针
     */
    @Transactional
    public void rollback(Long resourceSetId, Long toSnapshotId) {
        ResourceSetDO locked = resourceSetMapper.lockById(resourceSetId);
        if (locked == null) {
            throw new BizException(404, "resource set not found");
        }

        SnapshotDO snapshot = snapshotMapper.selectById(toSnapshotId);
        if (snapshot == null || !resourceSetId.equals(snapshot.getResourceSetId())) {
            throw new BizException(400, "invalid snapshot");
        }

        // 1. 清空当前规则表
        // 2. 从快照表还原规则
        restoreRulesFromSnapshot(locked.getModuleType(), resourceSetId, toSnapshotId);

        // 3. 切换指针
        locked.setCurrentSnapshotId(toSnapshotId);
        resourceSetMapper.updateById(locked);

        operationLogService.log("rollback", resourceSetId, toSnapshotId, "snapshot", toSnapshotId, null, snapshot);
    }

    private void restoreRulesFromSnapshot(String moduleTypeRaw, Long resourceSetId, Long snapshotId) {
        String moduleType = moduleTypeRaw == null ? "" : moduleTypeRaw.trim();

        if ("intervention".equals(moduleType)) {
            // 清空当前规则
            sentenceRuleMapper.deleteByResourceSetId(resourceSetId);
            termRuleMapper.deleteByResourceSetId(resourceSetId);

            // 从快照还原
            snapshotSentenceMapper.restoreToRuleTable(snapshotId, resourceSetId);
            snapshotTermMapper.restoreToRuleTable(snapshotId, resourceSetId);
        }

        if ("synonym".equals(moduleType)) {
            // 清空当前规则
            synonymRuleMapper.deleteByResourceSetId(resourceSetId);

            // 从快照还原
            snapshotSynonymMapper.restoreToRuleTable(snapshotId, resourceSetId);
        }

        if ("entity".equals(moduleType)) {
            // 清空当前规则
            entityRuleMapper.deleteByResourceSetId(resourceSetId);

            // 从快照还原
            snapshotEntityMapper.restoreToRuleTable(snapshotId, resourceSetId);
        }

        // 其他模块可扩展...
    }

    // ========== 内部类 ==========

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

    public static class ApiPublishResult {
        private boolean success;
        private Long publishRecordId;
        private Long snapshotId;

        public static ApiPublishResult success(Long publishRecordId, Long snapshotId) {
            ApiPublishResult r = new ApiPublishResult();
            r.success = true;
            r.publishRecordId = publishRecordId;
            r.snapshotId = snapshotId;
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

        public Long getSnapshotId() {
            return snapshotId;
        }

        public void setSnapshotId(Long snapshotId) {
            this.snapshotId = snapshotId;
        }
    }
}
