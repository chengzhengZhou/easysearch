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
import com.ppwx.easysearch.admin.domain.model.ResourceSetDO;
import com.ppwx.easysearch.admin.service.ResourceSetService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/resource-sets")
public class ResourceSetController {
    private final ResourceSetService resourceSetService;

    public ResourceSetController(ResourceSetService resourceSetService) {
        this.resourceSetService = resourceSetService;
    }

    public static class CreateReq {
        @NotBlank
        public String moduleType;
        public String scene;
        public String env;
        @NotBlank
        public String name;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ResourceSetDO> create(@Valid @RequestBody CreateReq req) {
        ResourceSetDO in = new ResourceSetDO();
        in.setModuleType(req.moduleType);
        in.setScene(req.scene);
        in.setEnv(req.env);
        in.setName(req.name);
        return ApiResponse.ok(resourceSetService.create(in));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','PUBLISHER','ADMIN')")
    public ApiResponse<PageResult<ResourceSetDO>> list(@RequestParam(required = false) String moduleType,
                                                       @RequestParam(required = false) String scene,
                                                       @RequestParam(required = false) String env,
                                                       @RequestParam(defaultValue = "1") long page,
                                                       @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.ok(resourceSetService.page(moduleType, scene, env, page, pageSize));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','PUBLISHER','ADMIN')")
    public ApiResponse<ResourceSetDO> get(@PathVariable("id") @NotNull Long id) {
        return ApiResponse.ok(resourceSetService.getById(id));
    }
}

