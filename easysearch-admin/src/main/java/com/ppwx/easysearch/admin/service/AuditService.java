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
import com.ppwx.easysearch.admin.domain.model.OperationLogDO;
import com.ppwx.easysearch.admin.domain.model.ResourceSetDO;
import com.ppwx.easysearch.admin.domain.model.SnapshotDO;
import com.ppwx.easysearch.admin.domain.vo.OperationLogVO;
import com.ppwx.easysearch.admin.mapper.OperationLogMapper;
import com.ppwx.easysearch.admin.mapper.ResourceSetMapper;
import com.ppwx.easysearch.admin.mapper.SnapshotMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AuditService {
    private final OperationLogMapper operationLogMapper;
    private final ResourceSetMapper resourceSetMapper;
    private final SnapshotMapper snapshotMapper;

    public AuditService(OperationLogMapper operationLogMapper,
                        ResourceSetMapper resourceSetMapper,
                        SnapshotMapper snapshotMapper) {
        this.operationLogMapper = operationLogMapper;
        this.resourceSetMapper = resourceSetMapper;
        this.snapshotMapper = snapshotMapper;
    }

    public PageResult<OperationLogVO> page(Long resourceSetId, Long snapshotId, String action, String entityType,
                                           long page, long pageSize) {
        Page<OperationLogDO> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<OperationLogDO> qw = new LambdaQueryWrapper<>();
        if (resourceSetId != null) {
            qw.eq(OperationLogDO::getResourceSetId, resourceSetId);
        }
        if (snapshotId != null) {
            qw.eq(OperationLogDO::getSnapshotId, snapshotId);
        }
        if (StringUtils.hasText(action)) {
            qw.eq(OperationLogDO::getAction, action);
        }
        if (StringUtils.hasText(entityType)) {
            qw.eq(OperationLogDO::getEntityType, entityType);
        }
        qw.orderByDesc(OperationLogDO::getId);
        Page<OperationLogDO> out = operationLogMapper.selectPage(p, qw);

        List<OperationLogDO> records = out.getRecords();

        // 批量查询关联的资源集名称
        List<Long> rsIds = records.stream()
                .map(OperationLogDO::getResourceSetId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, ResourceSetDO> rsMap = rsIds.isEmpty() ? Collections.emptyMap()
                : resourceSetMapper.selectBatchIds(rsIds).stream()
                        .collect(Collectors.toMap(ResourceSetDO::getId, Function.identity()));

        // 批量查询关联的快照编号
        List<Long> snapIds = records.stream()
                .map(OperationLogDO::getSnapshotId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, SnapshotDO> snapMap = snapIds.isEmpty() ? Collections.emptyMap()
                : snapshotMapper.selectBatchIds(snapIds).stream()
                        .collect(Collectors.toMap(SnapshotDO::getId, Function.identity()));

        // 组装 VO
        List<OperationLogVO> voList = records.stream().map(r -> {
            OperationLogVO vo = OperationLogVO.from(r);
            ResourceSetDO rs = r.getResourceSetId() != null ? rsMap.get(r.getResourceSetId()) : null;
            if (rs != null) {
                vo.setResourceSetName(rs.getName());
            }
            SnapshotDO snap = r.getSnapshotId() != null ? snapMap.get(r.getSnapshotId()) : null;
            if (snap != null) {
                vo.setSnapshotNo(snap.getSnapshotNo());
            }
            return vo;
        }).collect(Collectors.toList());

        return PageResult.of(out.getCurrent(), out.getSize(), out.getTotal(), voList);
    }
}

