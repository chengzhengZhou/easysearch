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
import com.ppwx.easysearch.admin.domain.model.SnapshotSynonymDO;
import org.apache.ibatis.annotations.Param;

/**
 * 同义词快照 Mapper
 */
public interface SnapshotSynonymMapper extends BaseMapper<SnapshotSynonymDO> {
    /**
     * 从当前规则表复制到快照表
     */
    int copyFromRuleTable(@Param("snapshotId") Long snapshotId, @Param("resourceSetId") Long resourceSetId);

    /**
     * 从快照表还原到当前规则表
     */
    int restoreToRuleTable(@Param("snapshotId") Long snapshotId, @Param("resourceSetId") Long resourceSetId);

    /**
     * 删除指定快照的所有规则
     */
    int deleteBySnapshotId(@Param("snapshotId") Long snapshotId);
}
