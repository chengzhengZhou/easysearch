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
import com.ppwx.easysearch.admin.domain.model.ResourceVersionDO;
import com.ppwx.easysearch.admin.service.VersionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@RestController
public class VersionController {
    private final VersionService versionService;

    public VersionController(VersionService versionService) {
        this.versionService = versionService;
    }

    public static class CreateDraftReq {
        public String changeLog;
        public Long basedOnVersionId;
    }

    public static class ResetStagingReq {
        public String changeLog;
        public Long basedOnVersionId;
    }

    @PostMapping("/api/resource-sets/{id}/versions")
    @PreAuthorize("hasAnyRole('EDITOR','PUBLISHER','ADMIN')")
    public ApiResponse<ResourceVersionDO> createDraft(@PathVariable("id") @NotNull Long resourceSetId,
                                                      @Valid @RequestBody CreateDraftReq req) {
        return ApiResponse.ok(versionService.createDraft(resourceSetId, req.changeLog, req.basedOnVersionId));
    }

    /**
     * Reset staging(draft): archive existing draft (if any), then create a new draft copied from base version.
     * Base defaults to current_version_id.
     */
    @PostMapping("/api/resource-sets/{id}/staging/reset")
    @PreAuthorize("hasAnyRole('EDITOR','PUBLISHER','ADMIN')")
    public ApiResponse<ResourceVersionDO> resetStaging(@PathVariable("id") @NotNull Long resourceSetId,
                                                       @Valid @RequestBody ResetStagingReq req) {
        return ApiResponse.ok(versionService.resetStaging(resourceSetId, req.changeLog, req.basedOnVersionId));
    }

    @GetMapping("/api/resource-sets/{id}/versions")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','PUBLISHER','ADMIN')")
    public ApiResponse<PageResult<ResourceVersionDO>> listVersions(@PathVariable("id") @NotNull Long resourceSetId,
                                                                   @RequestParam(defaultValue = "1") long page,
                                                                   @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.ok(versionService.listByResourceSet(resourceSetId, page, pageSize));
    }

    @PostMapping("/api/versions/{versionId}/validate")
    @PreAuthorize("hasAnyRole('EDITOR','PUBLISHER','ADMIN')")
    public ApiResponse<VersionService.ValidateReport> validate(@PathVariable("versionId") @NotNull Long versionId) {
        return ApiResponse.ok(versionService.validate(versionId));
    }

    @PostMapping("/api/versions/{versionId}/publish")
    @PreAuthorize("hasAnyRole('PUBLISHER','ADMIN')")
    public ApiResponse<VersionService.ApiPublishResult> publish(@PathVariable("versionId") @NotNull Long versionId) {
        return ApiResponse.ok(versionService.publish(versionId));
    }

    @PostMapping("/api/resource-sets/{id}/rollback")
    @PreAuthorize("hasAnyRole('PUBLISHER','ADMIN')")
    public ApiResponse<Void> rollback(@PathVariable("id") @NotNull Long resourceSetId,
                                      @RequestParam("toVersion") @NotNull Long toVersionId) {
        versionService.rollback(resourceSetId, toVersionId);
        return ApiResponse.ok(null);
    }
}

