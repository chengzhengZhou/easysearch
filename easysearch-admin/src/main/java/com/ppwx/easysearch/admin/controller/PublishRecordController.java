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

package com.ppwx.easysearch.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ppwx.easysearch.admin.domain.api.ApiResponse;
import com.ppwx.easysearch.admin.domain.api.PageResult;
import com.ppwx.easysearch.admin.domain.model.PublishRecordDO;
import com.ppwx.easysearch.admin.domain.model.ResourceSetDO;
import com.ppwx.easysearch.admin.domain.model.SnapshotDO;
import com.ppwx.easysearch.admin.domain.vo.PublishRecordVO;
import com.ppwx.easysearch.admin.mapper.PublishRecordMapper;
import com.ppwx.easysearch.admin.mapper.ResourceSetMapper;
import com.ppwx.easysearch.admin.mapper.SnapshotMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/publish-records")
public class PublishRecordController {
    private final PublishRecordMapper publishRecordMapper;
    private final ResourceSetMapper resourceSetMapper;
    private final SnapshotMapper snapshotMapper;

    public PublishRecordController(PublishRecordMapper publishRecordMapper,
                                   ResourceSetMapper resourceSetMapper,
                                   SnapshotMapper snapshotMapper) {
        this.publishRecordMapper = publishRecordMapper;
        this.resourceSetMapper = resourceSetMapper;
        this.snapshotMapper = snapshotMapper;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','PUBLISHER','ADMIN')")
    public ApiResponse<PageResult<PublishRecordVO>> page(@RequestParam(required = false) Long resourceSetId,
                                                         @RequestParam(required = false) Long snapshotId,
                                                         @RequestParam(defaultValue = "1") long page,
                                                         @RequestParam(defaultValue = "20") long pageSize) {
        Page<PublishRecordDO> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<PublishRecordDO> qw = new LambdaQueryWrapper<>();
        if (resourceSetId != null) {
            qw.eq(PublishRecordDO::getResourceSetId, resourceSetId);
        }
        if (snapshotId != null) {
            qw.eq(PublishRecordDO::getSnapshotId, snapshotId);
        }
        qw.orderByDesc(PublishRecordDO::getId);
        Page<PublishRecordDO> out = publishRecordMapper.selectPage(p, qw);

        List<PublishRecordDO> records = out.getRecords();

        // 批量查询关联的资源集名称
        List<Long> rsIds = records.stream()
                .map(PublishRecordDO::getResourceSetId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, ResourceSetDO> rsMap = rsIds.isEmpty() ? Collections.emptyMap()
                : resourceSetMapper.selectBatchIds(rsIds).stream()
                        .collect(Collectors.toMap(ResourceSetDO::getId, Function.identity()));

        // 批量查询关联的快照编号
        List<Long> snapIds = records.stream()
                .map(PublishRecordDO::getSnapshotId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, SnapshotDO> snapMap = snapIds.isEmpty() ? Collections.emptyMap()
                : snapshotMapper.selectBatchIds(snapIds).stream()
                        .collect(Collectors.toMap(SnapshotDO::getId, Function.identity()));

        // 组装 VO
        List<PublishRecordVO> voList = records.stream().map(r -> {
            PublishRecordVO vo = PublishRecordVO.from(r);
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

        return ApiResponse.ok(PageResult.of(out.getCurrent(), out.getSize(), out.getTotal(), voList));
    }
}

