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

import com.ppwx.easysearch.admin.domain.model.OperationLogDO;

import java.time.LocalDateTime;

public class OperationLogVO {
    private Long id;
    private String userName;
    private String action;
    private Long resourceSetId;
    private Long snapshotId;
    private String batchId;
    private String entityType;
    private Long entityId;
    private String beforeJson;
    private String afterJson;
    private LocalDateTime createdAt;

    // 展示字段
    private String resourceSetName;
    private Integer snapshotNo;

    public static OperationLogVO from(OperationLogDO src) {
        OperationLogVO vo = new OperationLogVO();
        vo.id = src.getId();
        vo.userName = src.getUserName();
        vo.action = src.getAction();
        vo.resourceSetId = src.getResourceSetId();
        vo.snapshotId = src.getSnapshotId();
        vo.batchId = src.getBatchId();
        vo.entityType = src.getEntityType();
        vo.entityId = src.getEntityId();
        vo.beforeJson = src.getBeforeJson();
        vo.afterJson = src.getAfterJson();
        vo.createdAt = src.getCreatedAt();
        return vo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
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

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getBeforeJson() {
        return beforeJson;
    }

    public void setBeforeJson(String beforeJson) {
        this.beforeJson = beforeJson;
    }

    public String getAfterJson() {
        return afterJson;
    }

    public void setAfterJson(String afterJson) {
        this.afterJson = afterJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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
