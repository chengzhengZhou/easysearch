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
import com.ppwx.easysearch.admin.mapper.OperationLogMapper;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final OperationLogMapper operationLogMapper;

    public AuditService(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    public PageResult<OperationLogDO> page(Long resourceSetId, Long versionId, long page, long pageSize) {
        Page<OperationLogDO> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<OperationLogDO> qw = new LambdaQueryWrapper<>();
        if (resourceSetId != null) {
            qw.eq(OperationLogDO::getResourceSetId, resourceSetId);
        }
        if (versionId != null) {
            qw.eq(OperationLogDO::getVersionId, versionId);
        }
        qw.orderByDesc(OperationLogDO::getId);
        Page<OperationLogDO> out = operationLogMapper.selectPage(p, qw);
        return PageResult.of(out.getCurrent(), out.getSize(), out.getTotal(), out.getRecords());
    }
}

