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
import com.ppwx.easysearch.admin.domain.model.EntityRuleDO;
import com.ppwx.easysearch.admin.domain.model.InterventionSentenceRuleDO;
import com.ppwx.easysearch.admin.domain.model.InterventionTermRuleDO;
import com.ppwx.easysearch.admin.domain.model.MetaRuleDO;
import com.ppwx.easysearch.admin.domain.model.ResourceVersionDO;
import com.ppwx.easysearch.admin.domain.model.SynonymRuleDO;
import com.ppwx.easysearch.admin.domain.model.TokenDictRuleDO;
import com.ppwx.easysearch.admin.service.EntityRuleService;
import com.ppwx.easysearch.admin.service.InterventionRuleService;
import com.ppwx.easysearch.admin.service.MetaRuleService;
import com.ppwx.easysearch.admin.service.SynonymRuleService;
import com.ppwx.easysearch.admin.service.TokenDictRuleService;
import com.ppwx.easysearch.admin.service.VersionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/resource-sets/{id}/staging")
public class StagingController {
    private final VersionService versionService;
    private final InterventionRuleService interventionRuleService;
    private final SynonymRuleService synonymRuleService;
    private final EntityRuleService entityRuleService;
    private final TokenDictRuleService tokenDictRuleService;
    private final MetaRuleService metaRuleService;
    private final ObjectMapper objectMapper;

    public StagingController(VersionService versionService,
                             InterventionRuleService interventionRuleService,
                             SynonymRuleService synonymRuleService,
                             EntityRuleService entityRuleService,
                             TokenDictRuleService tokenDictRuleService,
                             MetaRuleService metaRuleService,
                             ObjectMapper objectMapper) {
        this.versionService = versionService;
        this.interventionRuleService = interventionRuleService;
        this.synonymRuleService = synonymRuleService;
        this.entityRuleService = entityRuleService;
        this.tokenDictRuleService = tokenDictRuleService;
        this.metaRuleService = metaRuleService;
        this.objectMapper = objectMapper;
    }

    private Long ensureStagingVersionId(Long resourceSetId) {
        ResourceVersionDO v = versionService.createDraft(resourceSetId, null, null);
        return v.getId();
    }

    // ---------------- governance (staging) ----------------

    @PostMapping("/validate")
    @PreAuthorize("hasAnyRole('EDITOR','PUBLISHER','ADMIN')")
    public ApiResponse<VersionService.ValidateReport> validate(@PathVariable("id") @NotNull Long resourceSetId) {
        Long versionId = ensureStagingVersionId(resourceSetId);
        return ApiResponse.ok(versionService.validate(versionId));
    }

    @PostMapping("/publish")
    @PreAuthorize("hasAnyRole('PUBLISHER','ADMIN')")
    public ApiResponse<VersionService.ApiPublishResult> publish(@PathVariable("id") @NotNull Long resourceSetId) {
        Long versionId = ensureStagingVersionId(resourceSetId);
        return ApiResponse.ok(versionService.publish(versionId));
    }

    public static class OverwriteReq {
        public String changeLog;
    }

    @PostMapping("/overwrite")
    @PreAuthorize("hasAnyRole('EDITOR','PUBLISHER','ADMIN')")
    public ApiResponse<ResourceVersionDO> overwrite(@PathVariable("id") @NotNull Long resourceSetId,
                                                   @RequestParam("fromVersion") @NotNull Long fromVersionId,
                                                   @Valid @RequestBody(required = false) OverwriteReq req) {
        String changeLog = req == null ? null : req.changeLog;
        return ApiResponse.ok(versionService.resetStaging(resourceSetId, changeLog, fromVersionId));
    }

    // ---------------- rules (staging) ----------------

    @GetMapping("/rules")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','PUBLISHER','ADMIN')")
    public ApiResponse<?> listRules(@PathVariable("id") @NotNull Long resourceSetId,
                                    @RequestParam("module") RuleModule module,
                                    @RequestParam(value = "mode", required = false) InterventionMode mode,
                                    @RequestParam(value = "q", required = false) String q,
                                    @RequestParam(value = "entityType", required = false) String entityType,
                                    @RequestParam(value = "termType", required = false) String termType,
                                    @RequestParam(defaultValue = "1") long page,
                                    @RequestParam(defaultValue = "20") long pageSize) {
        Long versionId = ensureStagingVersionId(resourceSetId);
        if (module == RuleModule.intervention) {
            InterventionMode m = mode == null ? InterventionMode.sentence : mode;
            if (m == InterventionMode.sentence) {
                return ApiResponse.ok(interventionRuleService.pageSentence(versionId, q, page, pageSize));
            }
            return ApiResponse.ok(interventionRuleService.pageTerm(versionId, q, page, pageSize));
        }
        if (module == RuleModule.synonym) {
            return ApiResponse.ok(synonymRuleService.page(versionId, q, page, pageSize));
        }
        if (module == RuleModule.entity) {
            return ApiResponse.ok(entityRuleService.page(versionId, q, entityType, page, pageSize));
        }
        if (module == RuleModule.token) {
            return ApiResponse.ok(tokenDictRuleService.page(versionId, q, page, pageSize));
        }
        if (module == RuleModule.meta) {
            return ApiResponse.ok(metaRuleService.page(versionId, q, termType, page, pageSize));
        }
        throw new IllegalArgumentException("module not implemented: " + module);
    }

    @PostMapping("/rules")
    @PreAuthorize("hasAnyRole('EDITOR','PUBLISHER','ADMIN')")
    public ApiResponse<?> createRule(@PathVariable("id") @NotNull Long resourceSetId,
                                     @RequestParam("module") RuleModule module,
                                     @RequestParam(value = "mode", required = false) InterventionMode mode,
                                     @RequestBody JsonNode body) {
        Long versionId = ensureStagingVersionId(resourceSetId);
        if (module == RuleModule.intervention) {
            InterventionMode m = mode == null ? InterventionMode.sentence : mode;
            if (m == InterventionMode.sentence) {
                InterventionSentenceRuleDO in = objectMapper.convertValue(body, InterventionSentenceRuleDO.class);
                return ApiResponse.ok(interventionRuleService.createSentence(versionId, in));
            }
            InterventionTermRuleDO in = objectMapper.convertValue(body, InterventionTermRuleDO.class);
            return ApiResponse.ok(interventionRuleService.createTerm(versionId, in));
        }
        if (module == RuleModule.synonym) {
            SynonymRuleDO in = objectMapper.convertValue(body, SynonymRuleDO.class);
            return ApiResponse.ok(synonymRuleService.create(versionId, in));
        }
        if (module == RuleModule.entity) {
            EntityRuleDO in = objectMapper.convertValue(body, EntityRuleDO.class);
            return ApiResponse.ok(entityRuleService.create(versionId, in));
        }
        if (module == RuleModule.token) {
            TokenDictRuleDO in = objectMapper.convertValue(body, TokenDictRuleDO.class);
            return ApiResponse.ok(tokenDictRuleService.create(versionId, in));
        }
        if (module == RuleModule.meta) {
            MetaRuleDO in = objectMapper.convertValue(body, MetaRuleDO.class);
            return ApiResponse.ok(metaRuleService.create(versionId, in));
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
        Long versionId = ensureStagingVersionId(resourceSetId);
        if (module == RuleModule.intervention) {
            InterventionMode m = mode == null ? InterventionMode.sentence : mode;
            if (m == InterventionMode.sentence) {
                InterventionSentenceRuleDO in = objectMapper.convertValue(body, InterventionSentenceRuleDO.class);
                return ApiResponse.ok(interventionRuleService.updateSentence(versionId, ruleId, in));
            }
            InterventionTermRuleDO in = objectMapper.convertValue(body, InterventionTermRuleDO.class);
            return ApiResponse.ok(interventionRuleService.updateTerm(versionId, ruleId, in));
        }
        if (module == RuleModule.synonym) {
            SynonymRuleDO in = objectMapper.convertValue(body, SynonymRuleDO.class);
            return ApiResponse.ok(synonymRuleService.update(versionId, ruleId, in));
        }
        if (module == RuleModule.entity) {
            EntityRuleDO in = objectMapper.convertValue(body, EntityRuleDO.class);
            return ApiResponse.ok(entityRuleService.update(versionId, ruleId, in));
        }
        if (module == RuleModule.token) {
            TokenDictRuleDO in = objectMapper.convertValue(body, TokenDictRuleDO.class);
            return ApiResponse.ok(tokenDictRuleService.update(versionId, ruleId, in));
        }
        if (module == RuleModule.meta) {
            MetaRuleDO in = objectMapper.convertValue(body, MetaRuleDO.class);
            return ApiResponse.ok(metaRuleService.update(versionId, ruleId, in));
        }
        throw new IllegalArgumentException("module not implemented: " + module);
    }

    @DeleteMapping("/rules/{ruleId}")
    @PreAuthorize("hasAnyRole('EDITOR','PUBLISHER','ADMIN')")
    public ApiResponse<Void> deleteRule(@PathVariable("id") @NotNull Long resourceSetId,
                                        @PathVariable("ruleId") @NotNull Long ruleId,
                                        @RequestParam("module") RuleModule module,
                                        @RequestParam(value = "mode", required = false) InterventionMode mode) {
        Long versionId = ensureStagingVersionId(resourceSetId);
        if (module == RuleModule.intervention) {
            InterventionMode m = mode == null ? InterventionMode.sentence : mode;
            if (m == InterventionMode.sentence) {
                interventionRuleService.deleteSentence(versionId, ruleId);
                return ApiResponse.ok(null);
            }
            interventionRuleService.deleteTerm(versionId, ruleId);
            return ApiResponse.ok(null);
        }
        if (module == RuleModule.synonym) {
            synonymRuleService.delete(versionId, ruleId);
            return ApiResponse.ok(null);
        }
        if (module == RuleModule.entity) {
            entityRuleService.delete(versionId, ruleId);
            return ApiResponse.ok(null);
        }
        if (module == RuleModule.token) {
            tokenDictRuleService.delete(versionId, ruleId);
            return ApiResponse.ok(null);
        }
        if (module == RuleModule.meta) {
            metaRuleService.delete(versionId, ruleId);
            return ApiResponse.ok(null);
        }
        throw new IllegalArgumentException("module not implemented: " + module);
    }

    @PostMapping("/rules/batch-import")
    @PreAuthorize("hasAnyRole('EDITOR','PUBLISHER','ADMIN')")
    public ApiResponse<?> batchImport(@PathVariable("id") @NotNull Long resourceSetId,
                                      @RequestParam("module") RuleModule module,
                                      @RequestParam(value = "mode", required = false) InterventionMode mode,
                                      @RequestBody ArrayNode body) {
        Long versionId = ensureStagingVersionId(resourceSetId);
        if (module == RuleModule.intervention) {
            InterventionMode m = mode == null ? InterventionMode.sentence : mode;
            if (m == InterventionMode.sentence) {
                List<InterventionSentenceRuleDO> items = new ArrayList<>();
                for (JsonNode n : body) {
                    items.add(objectMapper.convertValue(n, InterventionSentenceRuleDO.class));
                }
                return ApiResponse.ok(interventionRuleService.batchCreateSentence(versionId, items));
            }
            List<InterventionTermRuleDO> items = new ArrayList<>();
            for (JsonNode n : body) {
                items.add(objectMapper.convertValue(n, InterventionTermRuleDO.class));
            }
            return ApiResponse.ok(interventionRuleService.batchCreateTerm(versionId, items));
        }
        if (module == RuleModule.synonym) {
            List<SynonymRuleDO> items = new ArrayList<>();
            for (JsonNode n : body) {
                items.add(objectMapper.convertValue(n, SynonymRuleDO.class));
            }
            return ApiResponse.ok(synonymRuleService.batchImport(versionId, items));
        }
        if (module == RuleModule.entity) {
            List<EntityRuleDO> items = new ArrayList<>();
            for (JsonNode n : body) {
                items.add(objectMapper.convertValue(n, EntityRuleDO.class));
            }
            return ApiResponse.ok(entityRuleService.batchImport(versionId, items));
        }
        if (module == RuleModule.token) {
            List<TokenDictRuleDO> items = new ArrayList<>();
            for (JsonNode n : body) {
                items.add(objectMapper.convertValue(n, TokenDictRuleDO.class));
            }
            return ApiResponse.ok(tokenDictRuleService.batchImport(versionId, items));
        }
        if (module == RuleModule.meta) {
            List<MetaRuleDO> items = new ArrayList<>();
            for (JsonNode n : body) {
                items.add(objectMapper.convertValue(n, MetaRuleDO.class));
            }
            return ApiResponse.ok(metaRuleService.batchImport(versionId, items));
        }
        throw new IllegalArgumentException("module not implemented: " + module);
    }
}

