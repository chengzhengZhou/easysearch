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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ppwx.easysearch.admin.domain.model.OperationLogDO;
import com.ppwx.easysearch.admin.mapper.OperationLogMapper;
import org.springframework.stereotype.Service;

@Service
public class OperationLogService {
    private final OperationLogMapper operationLogMapper;
    private final ObjectMapper objectMapper;
    private final SecurityUserService securityUserService;

    public OperationLogService(OperationLogMapper operationLogMapper, ObjectMapper objectMapper, SecurityUserService securityUserService) {
        this.operationLogMapper = operationLogMapper;
        this.objectMapper = objectMapper;
        this.securityUserService = securityUserService;
    }

    public void log(String action, Long resourceSetId, Long versionId, String entityType, Long entityId, Object before, Object after) {
        OperationLogDO l = new OperationLogDO();
        l.setUserName(securityUserService.currentUserOrSystem());
        l.setAction(action);
        l.setResourceSetId(resourceSetId);
        l.setVersionId(versionId);
        l.setEntityType(entityType);
        l.setEntityId(entityId);
        l.setBeforeJson(toJsonQuiet(before));
        l.setAfterJson(toJsonQuiet(after));
        operationLogMapper.insert(l);
    }

    private String toJsonQuiet(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}

