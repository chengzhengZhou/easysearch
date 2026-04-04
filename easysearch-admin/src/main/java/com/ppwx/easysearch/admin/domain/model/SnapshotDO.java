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

package com.ppwx.easysearch.admin.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 快照元信息（取代原 qp_resource_version）
 * 每次发布时创建一条记录
 */
@TableName("qp_snapshot")
public class SnapshotDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long resourceSetId;
    private Integer snapshotNo;              // 递增编号
    private String checksum;
    private String changeLog;
    private Integer ruleCount;
    private String publishedBy;
    private LocalDateTime publishedAt;
    @TableField("protected_flag")
    private Integer protectedFlag;

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

    public Integer getSnapshotNo() {
        return snapshotNo;
    }

    public void setSnapshotNo(Integer snapshotNo) {
        this.snapshotNo = snapshotNo;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getChangeLog() {
        return changeLog;
    }

    public void setChangeLog(String changeLog) {
        this.changeLog = changeLog;
    }

    public Integer getRuleCount() {
        return ruleCount;
    }

    public void setRuleCount(Integer ruleCount) {
        this.ruleCount = ruleCount;
    }

    public String getPublishedBy() {
        return publishedBy;
    }

    public void setPublishedBy(String publishedBy) {
        this.publishedBy = publishedBy;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Integer getProtectedFlag() {
        return protectedFlag;
    }

    public void setProtectedFlag(Integer protectedFlag) {
        this.protectedFlag = protectedFlag;
    }
}
