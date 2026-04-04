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

import com.ppwx.easysearch.admin.domain.api.ApiResponse;
import com.ppwx.easysearch.admin.domain.api.PageResult;
import com.ppwx.easysearch.admin.domain.vo.OperationLogVO;
import com.ppwx.easysearch.admin.service.AuditService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditController {
    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','PUBLISHER','ADMIN')")
    public ApiResponse<PageResult<OperationLogVO>> page(@RequestParam(required = false) Long resourceSetId,
                                                        @RequestParam(required = false) Long snapshotId,
                                                        @RequestParam(required = false) String action,
                                                        @RequestParam(required = false) String entityType,
                                                        @RequestParam(defaultValue = "1") long page,
                                                        @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.ok(auditService.page(resourceSetId, snapshotId, action, entityType, page, pageSize));
    }
}

