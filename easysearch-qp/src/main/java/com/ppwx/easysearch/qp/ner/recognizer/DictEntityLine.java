package com.ppwx.easysearch.qp.ner.recognizer;

import com.ppwx.easysearch.qp.ner.EntityType;

import java.util.List;
import java.util.Map;

/**
 * 词典实体行抽象：通用维度为 entity、type、normalizedValue，其余为可选扩展。
 * 不同实体类型可基于此接口实现子类并提供类型独特方法。
 */
public interface DictEntityLine {

    /**
     * 实体词条（主键）
     */
    String getEntity();

    /**
     * 类型，如 MODEL、BRAND
     */
    EntityType getType();

    /**
     * 归一化值，缺省时与 entity 一致
     */
    String getNormalizedValue();

    /**
     * 扩展属性（可选）
     */
    default Map<String, String> getAttributes() {
        return null;
    }

    /**
     * 别名列表，每个别名也会写入词典（可选）
     */
    default List<String> getAliases() {
        return null;
    }

    /**
     * 关系（可选）
     */
    default Map<String, Object> getRelations() {
        return null;
    }

    /**
     * ID 列表（可选）
     */
    default List<String> getId() {
        return null;
    }
}
