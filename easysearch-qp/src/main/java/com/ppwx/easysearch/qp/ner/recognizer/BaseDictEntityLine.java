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
