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

package com.ppwx.easysearch.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ppwx.easysearch.admin.domain.model.EntityRuleDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 实体规则 Mapper
 * 简化版：使用 resourceSetId 代替 versionId
 */
public interface EntityRuleMapper extends BaseMapper<EntityRuleDO> {
    @Delete("DELETE FROM qp_rule_entity WHERE resource_set_id = #{resourceSetId}")
    int deleteByResourceSetId(@Param("resourceSetId") Long resourceSetId);

    @Select("SELECT COUNT(*) FROM qp_rule_entity WHERE resource_set_id = #{resourceSetId}")
    int countByResourceSetId(@Param("resourceSetId") Long resourceSetId);
}
