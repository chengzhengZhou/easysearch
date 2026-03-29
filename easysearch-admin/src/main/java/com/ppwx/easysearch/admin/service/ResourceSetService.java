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
import com.ppwx.easysearch.admin.domain.exception.BizException;
import com.ppwx.easysearch.admin.domain.model.ResourceSetDO;
import com.ppwx.easysearch.admin.mapper.ResourceSetMapper;
import org.springframework.stereotype.Service;

@Service
public class ResourceSetService {
    private final ResourceSetMapper resourceSetMapper;
    private final SecurityUserService securityUserService;

    public ResourceSetService(ResourceSetMapper resourceSetMapper, SecurityUserService securityUserService) {
        this.resourceSetMapper = resourceSetMapper;
        this.securityUserService = securityUserService;
    }

    public ResourceSetDO create(ResourceSetDO in) {
        if (in.getModuleType() == null || in.getModuleType().trim().isEmpty()) {
            throw new BizException(400, "moduleType required");
        }
        if (in.getScene() == null || in.getScene().trim().isEmpty()) {
            in.setScene("default");
        }
        if (in.getEnv() == null || in.getEnv().trim().isEmpty()) {
            in.setEnv("prod");
        }
        if (in.getName() == null || in.getName().trim().isEmpty()) {
            in.setName(in.getModuleType() + "-" + in.getScene() + "-" + in.getEnv());
        }
        if (in.getStatus() == null) {
            in.setStatus(1);
        }
        in.setCreatedBy(securityUserService.currentUserOrSystem());
        resourceSetMapper.insert(in);
        return in;
    }

    public PageResult<ResourceSetDO> page(String moduleType, String scene, String env, long page, long pageSize) {
        Page<ResourceSetDO> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<ResourceSetDO> qw = new LambdaQueryWrapper<>();
        if (moduleType != null && !moduleType.trim().isEmpty()) {
            qw.eq(ResourceSetDO::getModuleType, moduleType);
        }
        if (scene != null && !scene.trim().isEmpty()) {
            qw.eq(ResourceSetDO::getScene, scene);
        }
        if (env != null && !env.trim().isEmpty()) {
            qw.eq(ResourceSetDO::getEnv, env);
        }
        qw.orderByDesc(ResourceSetDO::getId);
        Page<ResourceSetDO> out = resourceSetMapper.selectPage(p, qw);
        return PageResult.of(out.getCurrent(), out.getSize(), out.getTotal(), out.getRecords());
    }

    public ResourceSetDO getById(Long id) {
        ResourceSetDO rs = resourceSetMapper.selectById(id);
        if (rs == null) {
            throw new BizException(404, "resource set not found");
        }
        return rs;
    }
}

