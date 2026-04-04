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

package com.ppwx.easysearch.admin.domain.vo;

import com.ppwx.easysearch.admin.domain.model.PublishRecordDO;

import java.time.LocalDateTime;

public class PublishRecordVO {
    private Long id;
    private Long resourceSetId;
    private Long snapshotId;
    private String env;
    private String publishStatus;
    private String publishMsg;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String operator;

    // 展示字段
    private String resourceSetName;
    private Integer snapshotNo;

    public static PublishRecordVO from(PublishRecordDO src) {
        PublishRecordVO vo = new PublishRecordVO();
        vo.id = src.getId();
        vo.resourceSetId = src.getResourceSetId();
        vo.snapshotId = src.getSnapshotId();
        vo.env = src.getEnv();
        vo.publishStatus = src.getPublishStatus();
        vo.publishMsg = src.getPublishMsg();
        vo.startedAt = src.getStartedAt();
        vo.finishedAt = src.getFinishedAt();
        vo.operator = src.getOperator();
        return vo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getResourceSetId() {
        return resourceSetId;
    }

    public void setResourceSetId(Long resourceSetId) {
        this.resourceSetId = resourceSetId;
    }

    public Long getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(Long snapshotId) {
        this.snapshotId = snapshotId;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public String getPublishStatus() {
        return publishStatus;
    }

    public void setPublishStatus(String publishStatus) {
        this.publishStatus = publishStatus;
    }

    public String getPublishMsg() {
        return publishMsg;
    }

    public void setPublishMsg(String publishMsg) {
        this.publishMsg = publishMsg;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getResourceSetName() {
        return resourceSetName;
    }

    public void setResourceSetName(String resourceSetName) {
        this.resourceSetName = resourceSetName;
    }

    public Integer getSnapshotNo() {
        return snapshotNo;
    }

    public void setSnapshotNo(Integer snapshotNo) {
        this.snapshotNo = snapshotNo;
    }
}
