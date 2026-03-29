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
import com.ppwx.easysearch.admin.domain.enums.InterventionMode;
import com.ppwx.easysearch.admin.domain.enums.RuleModule;
import com.ppwx.easysearch.admin.service.BatchOperationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping("/api/versions/{versionId}/rules")
public class BatchOperationController {
    private final BatchOperationService batchOperationService;

    public BatchOperationController(BatchOperationService batchOperationService) {
        this.batchOperationService = batchOperationService;
    }

    public static class BatchReq {
        @NotEmpty
        public List<Long> ids;
    }

    @PostMapping("/batch-enable")
    @PreAuthorize("hasAnyRole('EDITOR','PUBLISHER','ADMIN')")
    public ApiResponse<BatchOperationService.BatchOpResult> enable(@PathVariable("versionId") @NotNull Long versionId,
                                                                   @RequestParam("module") RuleModule module,
                                                                   @RequestParam(value = "mode", required = false) InterventionMode mode,
                                                                   @Valid @RequestBody BatchReq req) {
        return ApiResponse.ok(batchOperationService.enable(versionId, module, mode, req.ids));
    }

    @PostMapping("/batch-disable")
    @PreAuthorize("hasAnyRole('EDITOR','PUBLISHER','ADMIN')")
    public ApiResponse<BatchOperationService.BatchOpResult> disable(@PathVariable("versionId") @NotNull Long versionId,
                                                                    @RequestParam("module") RuleModule module,
                                                                    @RequestParam(value = "mode", required = false) InterventionMode mode,
                                                                    @Valid @RequestBody BatchReq req) {
        return ApiResponse.ok(batchOperationService.disable(versionId, module, mode, req.ids));
    }

    @PostMapping("/batch-delete")
    @PreAuthorize("hasAnyRole('EDITOR','PUBLISHER','ADMIN')")
    public ApiResponse<BatchOperationService.BatchOpResult> delete(@PathVariable("versionId") @NotNull Long versionId,
                                                                   @RequestParam("module") RuleModule module,
                                                                   @RequestParam(value = "mode", required = false) InterventionMode mode,
                                                                   @Valid @RequestBody BatchReq req) {
        return ApiResponse.ok(batchOperationService.delete(versionId, module, mode, req.ids));
    }
}

