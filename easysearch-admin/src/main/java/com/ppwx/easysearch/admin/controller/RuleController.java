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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.ppwx.easysearch.admin.domain.api.ApiResponse;
import com.ppwx.easysearch.admin.domain.enums.InterventionMode;
import com.ppwx.easysearch.admin.domain.enums.RuleModule;
import com.ppwx.easysearch.admin.domain.model.InterventionSentenceRuleDO;
import com.ppwx.easysearch.admin.domain.model.InterventionTermRuleDO;
import com.ppwx.easysearch.admin.service.InterventionRuleService;
import com.ppwx.easysearch.admin.service.PublishService;
import com.ppwx.easysearch.admin.service.DiffService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 规则管理 Controller
 * 简化版：直接操作当前规则表，无需 staging/versionId
 */
@RestController
@RequestMapping("/api/resource-sets/{id}")
public class RuleController {
    private final InterventionRuleService interventionRuleService;
    private final PublishService publishService;
    private final DiffService diffService;
    private final ObjectMapper objectMapper;

    public RuleController(InterventionRuleService interventionRuleService,
                          PublishService publishService,
                          DiffService diffService,
                          ObjectMapper objectMapper) {
        this.interventionRuleService = interventionRuleService;
        this.publishService = publishService;
        this.diffService = diffService;
        this.objectMapper = objectMapper;
    }

    // ========== 规则 CRUD（直接操作当前规则表） ==========

    @GetMapping("/rules")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','PUBLISHER','ADMIN')")
    public ApiResponse<?> listRules(@PathVariable("id") @NotNull Long resourceSetId,
                                    @RequestParam("module") RuleModule module,
                                    @RequestParam(value = "mode", required = false) InterventionMode mode,
                                    @RequestParam(value = "q", required = false) String q,
                                    @RequestParam(defaultValue = "1") long page,
                                    @RequestParam(defaultValue = "20") long pageSize) {
        if (module == RuleModule.intervention) {
            InterventionMode m = mode == null ? InterventionMode.sentence : mode;
            if (m == InterventionMode.sentence) {
                return ApiResponse.ok(interventionRuleService.pageSentence(resourceSetId, q, page, pageSize));
            }
            return ApiResponse.ok(interventionRuleService.pageTerm(resourceSetId, q, page, pageSize));
        }
        // 其他模块可扩展...
        throw new IllegalArgumentException("module not implemented: " + module);
    }

    @PostMapping("/rules")
    @PreAuthorize("hasAnyRole('EDITOR','PUBLISHER','ADMIN')")
    public ApiResponse<?> createRule(@PathVariable("id") @NotNull Long resourceSetId,
                                     @RequestParam("module") RuleModule module,
                                     @RequestParam(value = "mode", required = false) InterventionMode mode,
                                     @RequestBody JsonNode body) {
        if (module == RuleModule.intervention) {
            InterventionMode m = mode == null ? InterventionMode.sentence : mode;
            if (m == InterventionMode.sentence) {
                InterventionSentenceRuleDO in = objectMapper.convertValue(body, InterventionSentenceRuleDO.class);
                return ApiResponse.ok(interventionRuleService.createSentence(resourceSetId, in));
            }
            InterventionTermRuleDO in = objectMapper.convertValue(body, InterventionTermRuleDO.class);
            return ApiResponse.ok(interventionRuleService.createTerm(resourceSetId, in));
        }
        throw new IllegalArgumentException("module not implemented: " + module);
    }

    @PutMapping("/rules/{ruleId}")
    @PreAuthorize("hasAnyRole('EDITOR','PUBLISHER','ADMIN')")
    public ApiResponse<?> updateRule(@PathVariable("id") @NotNull Long resourceSetId,
                                     @PathVariable("ruleId") @NotNull Long ruleId,
                                     @RequestParam("module") RuleModule module,
                                     @RequestParam(value = "mode", required = false) InterventionMode mode,
                                     @RequestBody JsonNode body) {
        if (module == RuleModule.intervention) {
            InterventionMode m = mode == null ? InterventionMode.sentence : mode;
            if (m == InterventionMode.sentence) {
                InterventionSentenceRuleDO in = objectMapper.convertValue(body, InterventionSentenceRuleDO.class);
                return ApiResponse.ok(interventionRuleService.updateSentence(resourceSetId, ruleId, in));
            }
            InterventionTermRuleDO in = objectMapper.convertValue(body, InterventionTermRuleDO.class);
            return ApiResponse.ok(interventionRuleService.updateTerm(resourceSetId, ruleId, in));
        }
        throw new IllegalArgumentException("module not implemented: " + module);
    }

    @DeleteMapping("/rules/{ruleId}")
    @PreAuthorize("hasAnyRole('EDITOR','PUBLISHER','ADMIN')")
    public ApiResponse<Void> deleteRule(@PathVariable("id") @NotNull Long resourceSetId,
                                        @PathVariable("ruleId") @NotNull Long ruleId,
                                        @RequestParam("module") RuleModule module,
                                        @RequestParam(value = "mode", required = false) InterventionMode mode) {
        if (module == RuleModule.intervention) {
            InterventionMode m = mode == null ? InterventionMode.sentence : mode;
            if (m == InterventionMode.sentence) {
                interventionRuleService.deleteSentence(resourceSetId, ruleId);
                return ApiResponse.ok(null);
            }
            interventionRuleService.deleteTerm(resourceSetId, ruleId);
            return ApiResponse.ok(null);
        }
        throw new IllegalArgumentException("module not implemented: " + module);
    }

    // ========== 批量操作 ==========

    @PostMapping("/rules/batch-import")
    @PreAuthorize("hasAnyRole('EDITOR','PUBLISHER','ADMIN')")
    public ApiResponse<?> batchImport(@PathVariable("id") @NotNull Long resourceSetId,
                                      @RequestParam("module") RuleModule module,
                                      @RequestParam(value = "mode", required = false) InterventionMode mode,
                                      @RequestBody ArrayNode body) {
        if (module == RuleModule.intervention) {
            InterventionMode m = mode == null ? InterventionMode.sentence : mode;
            if (m == InterventionMode.sentence) {
                List<InterventionSentenceRuleDO> items = new ArrayList<>();
                for (JsonNode n : body) {
                    items.add(objectMapper.convertValue(n, InterventionSentenceRuleDO.class));
                }
                return ApiResponse.ok(interventionRuleService.batchCreateSentence(resourceSetId, items));
            }
            List<InterventionTermRuleDO> items = new ArrayList<>();
            for (JsonNode n : body) {
                items.add(objectMapper.convertValue(n, InterventionTermRuleDO.class));
            }
            return ApiResponse.ok(interventionRuleService.batchCreateTerm(resourceSetId, items));
        }
        throw new IllegalArgumentException("module not implemented: " + module);
    }

    @PostMapping("/rules/batch-enable")
    @PreAuthorize("hasAnyRole('EDITOR','PUBLISHER','ADMIN')")
    public ApiResponse<Void> batchEnable(@PathVariable("id") @NotNull Long resourceSetId,
                                         @RequestParam("module") RuleModule module,
                                         @RequestParam(value = "mode", required = false) InterventionMode mode,
                                         @RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return ApiResponse.ok(null);
        }
        if (module == RuleModule.intervention) {
            InterventionMode m = mode == null ? InterventionMode.sentence : mode;
            if (m == InterventionMode.sentence) {
                interventionRuleService.batchEnableSentence(resourceSetId, ids, true);
            } else {
                interventionRuleService.batchEnableTerm(resourceSetId, ids, true);
            }
            return ApiResponse.ok(null);
        }
        throw new IllegalArgumentException("module not implemented: " + module);
    }

    @PostMapping("/rules/batch-disable")
    @PreAuthorize("hasAnyRole('EDITOR','PUBLISHER','ADMIN')")
    public ApiResponse<Void> batchDisable(@PathVariable("id") @NotNull Long resourceSetId,
                                          @RequestParam("module") RuleModule module,
                                          @RequestParam(value = "mode", required = false) InterventionMode mode,
                                          @RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return ApiResponse.ok(null);
        }
        if (module == RuleModule.intervention) {
            InterventionMode m = mode == null ? InterventionMode.sentence : mode;
            if (m == InterventionMode.sentence) {
                interventionRuleService.batchEnableSentence(resourceSetId, ids, false);
            } else {
                interventionRuleService.batchEnableTerm(resourceSetId, ids, false);
            }
            return ApiResponse.ok(null);
        }
        throw new IllegalArgumentException("module not implemented: " + module);
    }

    @PostMapping("/rules/batch-delete")
    @PreAuthorize("hasAnyRole('EDITOR','PUBLISHER','ADMIN')")
    public ApiResponse<Void> batchDelete(@PathVariable("id") @NotNull Long resourceSetId,
                                         @RequestParam("module") RuleModule module,
                                         @RequestParam(value = "mode", required = false) InterventionMode mode,
                                         @RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return ApiResponse.ok(null);
        }
        if (module == RuleModule.intervention) {
            InterventionMode m = mode == null ? InterventionMode.sentence : mode;
            if (m == InterventionMode.sentence) {
                interventionRuleService.batchDeleteSentence(resourceSetId, ids);
            } else {
                interventionRuleService.batchDeleteTerm(resourceSetId, ids);
            }
            return ApiResponse.ok(null);
        }
        throw new IllegalArgumentException("module not implemented: " + module);
    }

    // ========== 发布与回滚 ==========

    @PostMapping("/validate")
    @PreAuthorize("hasAnyRole('EDITOR','PUBLISHER','ADMIN')")
    public ApiResponse<PublishService.ValidateReport> validate(@PathVariable("id") @NotNull Long resourceSetId) {
        return ApiResponse.ok(publishService.validate(resourceSetId));
    }

    public static class PublishRequest {
        public String changeLog;
    }

    @PostMapping("/publish")
    @PreAuthorize("hasAnyRole('PUBLISHER','ADMIN')")
    public ApiResponse<PublishService.ApiPublishResult> publish(@PathVariable("id") @NotNull Long resourceSetId,
                                                                @Valid @RequestBody(required = false) PublishRequest req) {
        String changeLog = req == null ? null : req.changeLog;
        return ApiResponse.ok(publishService.publish(resourceSetId, changeLog));
    }

    @PostMapping("/rollback")
    @PreAuthorize("hasAnyRole('PUBLISHER','ADMIN')")
    public ApiResponse<Void> rollback(@PathVariable("id") @NotNull Long resourceSetId,
                                      @RequestParam("toSnapshot") @NotNull Long toSnapshotId) {
        publishService.rollback(resourceSetId, toSnapshotId);
        return ApiResponse.ok(null);
    }

    // ========== 快照查看 ==========

    @GetMapping("/snapshots")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','PUBLISHER','ADMIN')")
    public ApiResponse<?> listSnapshots(@PathVariable("id") @NotNull Long resourceSetId,
                                        @RequestParam(defaultValue = "1") long page,
                                        @RequestParam(defaultValue = "50") long pageSize) {
        return ApiResponse.ok(publishService.listSnapshots(resourceSetId, page, pageSize));
    }

    // ========== 版本对比 ==========

    @GetMapping("/snapshot-diff")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','PUBLISHER','ADMIN')")
    public ApiResponse<DiffService.DiffResult> snapshotDiff(
            @PathVariable("id") @NotNull Long resourceSetId,
            @RequestParam(value = "snapshotA", required = false) Long snapshotA,
            @RequestParam(value = "snapshotB", required = false) Long snapshotB,
            @RequestParam(value = "mode", required = false) InterventionMode mode) {
        return ApiResponse.ok(diffService.diffSnapshots(resourceSetId, snapshotA, snapshotB, mode));
    }

    // ========== 变更摘要 ==========

    @GetMapping("/diff-summary")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','PUBLISHER','ADMIN')")
    public ApiResponse<DiffService.DiffSummary> diffSummary(@PathVariable("id") @NotNull Long resourceSetId,
                                                             @RequestParam(value = "module", defaultValue = "intervention") RuleModule module) {
        return ApiResponse.ok(diffService.diffSummary(resourceSetId, module));
    }
}
