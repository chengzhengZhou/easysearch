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

package com.ppwx.easysearch.qp.support;

import com.ppwx.easysearch.qp.ner.Entity;
import com.ppwx.easysearch.qp.ner.EntityNormalizer;

import java.util.List;

/**
 * 实体归一化阶段：对已识别的实体执行归一化（标准化 value）。
 * <p>
 * 受 {@link ProcessOptions#isEnableNormalization()} 控制。
 * 遍历 {@link QueryContext#getEntities()}，调用 {@link EntityNormalizer#normalize(com.ppwx.easysearch.qp.ner.EntityType, String)}
 * 设置每个实体的 normalizedValue。
 */
public class NormalizerStage extends AbstractStage {

    public static final String STAGE_NAME = "normalizer";

    private final EntityNormalizer entityNormalizer;

    public NormalizerStage(EntityNormalizer entityNormalizer) {
        super(STAGE_NAME);
        this.entityNormalizer = entityNormalizer;
    }

    @Override
    protected void doProcess(QueryContext ctx) {
        ProcessOptions options = ctx.getOptions();
        if (options != null && !options.isEnableNormalization()) {
            ctx.putTrace(STAGE_NAME + ".skipped", true);
            return;
        }

        List<Entity> entities = ctx.getEntities();
        if (entities == null || entities.isEmpty()) {
            return;
        }

        int normalizedCount = 0;
        for (Entity entity : entities) {
            if (entity.getType() == null) {
                continue;
            }
            String normalized = entityNormalizer.normalize(entity.getType(), entity.getValue());
            if (normalized != null) {
                entity.setNormalizedValue(normalized);
                normalizedCount++;
            }
        }
        ctx.putTrace(STAGE_NAME + ".normalizedCount", normalizedCount);
    }
}
