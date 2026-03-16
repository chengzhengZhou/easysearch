package com.ppwx.easysearch.qp.ner.recognizer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ppwx.easysearch.qp.ner.EntityType;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 词典 JSON 行解析：将一行文本解析为 {@link DictEntityLine}。
 */
public final class DictEntityLineParser {

    private DictEntityLineParser() {
    }

    /**
     * 将一行 JSON 文本解析为 DictEntityLine。
     * 根据 type 字段返回具体子类（MODEL → ModelDictEntityLine，其余 → BaseDictEntityLine）。
     *
     * @param line 一行 JSON 字符串
     * @return 解析成功返回 Optional.of(record)，否则 Optional.empty()
     */
    public static Optional<DictEntityLine> parseLine(String line) {
        if (StringUtils.isBlank(line) || !line.trim().startsWith("{")) {
            return Optional.empty();
        }
        try {
            JSONObject obj = JSON.parseObject(line.trim());
            String entity = obj.getString("entity");
            if (StringUtils.isBlank(entity)) {
                return Optional.empty();
            }
            String typeStr = obj.getString("type");
            EntityType entityType = EntityType.getByName(typeStr);

            BaseDictEntityLine record = createByType(entityType);
            record.setEntity(entity);
            record.setType(entityType);
            record.setNormalizedValue(obj.getString("normalizedValue"));

            JSONObject attrs = obj.getJSONObject("attributes");
            if (attrs != null) {
                Map<String, String> map = new HashMap<>();
                for (String key : attrs.keySet()) {
                    map.put(key, attrs.getString(key));
                }
                record.setAttributes(map);
            }

            JSONArray aliases = obj.getJSONArray("aliases");
            if (aliases != null) {
                record.setAliases(aliases.toJavaList(String.class));
            }

            JSONObject relations = obj.getJSONObject("relations");
            if (relations != null) {
                record.setRelations(new HashMap<>(relations));
            }

            JSONArray ids = obj.getJSONArray("id");
            if (ids != null) {
                record.setId(ids.toJavaList(String.class));
            }

            return Optional.of(record);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * 根据类型字符串创建对应的实体行实例，便于不同实体类型提供独特方法。
     */
    public static BaseDictEntityLine createByType(EntityType type) {
        if (type == EntityType.MODEL) {
            return new ModelDictEntityLine();
        }
        return new BaseDictEntityLine();
    }
}
