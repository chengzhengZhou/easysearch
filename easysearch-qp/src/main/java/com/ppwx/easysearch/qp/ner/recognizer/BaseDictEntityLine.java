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

package com.ppwx.easysearch.qp.ner.recognizer;

import com.ppwx.easysearch.qp.ner.EntityType;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 通用实体行实现：entity、type、normalizedValue 必选，其余为可选。
 */
public class BaseDictEntityLine implements DictEntityLine, Serializable {

    private static final long serialVersionUID = 1L;

    private String entity;
    private EntityType type;
    private String normalizedValue;

    private Map<String, String> attributes;
    private List<String> aliases;
    private Map<String, Object> relations;
    private List<String> id;

    @Override
    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    @Override
    public EntityType getType() {
        return type;
    }

    public void setType(EntityType type) {
        this.type = type;
    }

    @Override
    public String getNormalizedValue() {
        return normalizedValue;
    }

    public void setNormalizedValue(String normalizedValue) {
        this.normalizedValue = normalizedValue;
    }

    @Override
    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    @Override
    public List<String> getAliases() {
        return aliases;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = aliases;
    }

    @Override
    public Map<String, Object> getRelations() {
        return relations;
    }

    public void setRelations(Map<String, Object> relations) {
        this.relations = relations;
    }

    @Override
    public List<String> getId() {
        return id;
    }

    public void setId(List<String> id) {
        this.id = id;
    }
}
