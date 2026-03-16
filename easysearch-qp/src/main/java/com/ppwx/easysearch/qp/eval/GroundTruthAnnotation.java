package com.ppwx.easysearch.qp.eval;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.ppwx.easysearch.qp.ner.Entity;
import com.ppwx.easysearch.qp.ner.EntityType;
import com.ppwx.easysearch.qp.util.StreamUtil;

import java.io.*;
import java.util.*;

/**
 * 标注数据（Ground Truth）管理类
 * 用于加载、保存和管理人工标注的实体数据
 */
public class GroundTruthAnnotation {
    
    private Map<String, List<AnnotatedEntity>> annotations = new HashMap<>();
    
    /**
     * 标注实体
     */
    public static class AnnotatedEntity {
        private String value;           // 实体值
        private String type;            // 实体类型
        private String normalizedValue; // 标准化值
        private int startOffset;        // 起始位置
        private int endOffset;          // 结束位置
        private String id;              // 实体ID（可选）
        
        public AnnotatedEntity() {}
        
        public AnnotatedEntity(String value, String type, String normalizedValue, 
                             int startOffset, int endOffset) {
            this.value = value;
            this.type = type;
            this.normalizedValue = normalizedValue;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
        }
        
        /**
         * 转换为Entity对象
         */
        public Entity toEntity() {
            Entity entity = new Entity(value, EntityType.getByName(type), normalizedValue);
            entity.setStartOffset(startOffset);
            entity.setEndOffset(endOffset);
            if (id != null && !id.isEmpty()) {
                entity.setId(Collections.singletonList(id));
            }
            return entity;
        }
        
        /**
         * 从Entity创建
         */
        public static AnnotatedEntity fromEntity(Entity entity) {
            AnnotatedEntity ae = new AnnotatedEntity();
            ae.value = entity.getValue();
            ae.type = entity.getType().name();
            ae.normalizedValue = entity.getNormalizedValue();
            ae.startOffset = entity.getStartOffset();
            ae.endOffset = entity.getEndOffset();
            if (entity.getId() != null && !entity.getId().isEmpty()) {
                ae.id = entity.getId().get(0);
            }
            return ae;
        }
        
        // Getters and Setters
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getNormalizedValue() { return normalizedValue; }
        public void setNormalizedValue(String normalizedValue) { this.normalizedValue = normalizedValue; }
        public int getStartOffset() { return startOffset; }
        public void setStartOffset(int startOffset) { this.startOffset = startOffset; }
        public int getEndOffset() { return endOffset; }
        public void setEndOffset(int endOffset) { this.endOffset = endOffset; }
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
    }
    
    /**
     * 添加标注
     */
    public void addAnnotation(String query, List<AnnotatedEntity> entities) {
        annotations.put(query, entities);
    }
    
    /**
     * 获取标注
     */
    public List<Entity> getAnnotation(String query) {
        List<AnnotatedEntity> annotated = annotations.get(query);
        if (annotated == null) {
            return null;
        }
        
        List<Entity> entities = new ArrayList<>();
        for (AnnotatedEntity ae : annotated) {
            entities.add(ae.toEntity());
        }
        return entities;
    }
    
    /**
     * 是否有标注
     */
    public boolean hasAnnotation(String query) {
        return annotations.containsKey(query);
    }
    
    /**
     * 从JSON文件加载标注数据
     * 使用FastJSON解析
     */
    public static GroundTruthAnnotation loadFromJson(String filePath) throws IOException {
        GroundTruthAnnotation annotation = new GroundTruthAnnotation();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(StreamUtil.getResourceStream(filePath)))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            
            // 使用FastJSON解析
            Map<String, List<AnnotatedEntity>> data = JSON.parseObject(
                sb.toString(), 
                new TypeReference<Map<String, List<AnnotatedEntity>>>() {}
            );
            
            annotation.annotations = data != null ? data : new HashMap<>();
            return annotation;
        }
    }
    
    /**
     * 保存标注数据到JSON文件
     * 使用FastJSON生成格式化的JSON
     */
    public void saveToJson(String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            // 使用FastJSON生成格式化的JSON，启用格式化输出
            String json = JSON.toJSONString(
                annotations, 
                SerializerFeature.PrettyFormat,
                SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteDateUseDateFormat
            );
            writer.write(json);
        }
    }
    
    /**
     * 从CSV格式加载（简化版）
     * CSV格式：query, entity_value, entity_type, normalized_value, start, end
     */
    public static GroundTruthAnnotation loadFromCsv(String filePath) throws IOException {
        GroundTruthAnnotation annotation = new GroundTruthAnnotation();
        
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isHeader = true;
            
            while ((line = br.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                
                String[] parts = line.split(",", -1);
                if (parts.length < 6) continue;
                
                String query = parts[0].trim();
                String value = parts[1].trim();
                String type = parts[2].trim();
                String normalized = parts[3].trim();
                int start = Integer.parseInt(parts[4].trim());
                int end = Integer.parseInt(parts[5].trim());
                
                AnnotatedEntity entity = new AnnotatedEntity(value, type, normalized, start, end);
                
                List<AnnotatedEntity> entities = annotation.annotations.computeIfAbsent(
                    query, k -> new ArrayList<>());
                entities.add(entity);
            }
        }
        
        return annotation;
    }
    
    /**
     * 保存到CSV格式
     */
    public void saveToCsv(String filePath) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            // 写入表头
            bw.write("query,entity_value,entity_type,normalized_value,start_offset,end_offset,id\n");
            
            for (Map.Entry<String, List<AnnotatedEntity>> entry : annotations.entrySet()) {
                String query = entry.getKey();
                for (AnnotatedEntity entity : entry.getValue()) {
                    bw.write(String.format("%s,%s,%s,%s,%d,%d,%s\n",
                        escapeCsv(query),
                        escapeCsv(entity.getValue()),
                        entity.getType(),
                        escapeCsv(entity.getNormalizedValue()),
                        entity.getStartOffset(),
                        entity.getEndOffset(),
                        entity.getId() != null ? entity.getId() : ""));
                }
            }
        }
    }
    
    /**
     * CSV转义
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
    
    /**
     * 获取标注数量
     */
    public int getAnnotationCount() {
        return annotations.size();
    }
    
    /**
     * 获取所有标注的查询
     */
    public Set<String> getAnnotatedQueries() {
        return annotations.keySet();
    }
    
    /**
     * 生成标注模板
     * 可以基于预测结果生成待人工校验的模板
     */
    public void generateTemplate(String query, Collection<Entity> predictedEntities, String outputFile) 
            throws IOException {
        List<AnnotatedEntity> entities = new ArrayList<>();
        if (predictedEntities != null) {
            for (Entity entity : predictedEntities) {
                entities.add(AnnotatedEntity.fromEntity(entity));
            }
        }
        
        GroundTruthAnnotation template = new GroundTruthAnnotation();
        template.addAnnotation(query, entities);
        template.saveToJson(outputFile);
    }
    
    public Map<String, List<AnnotatedEntity>> getAnnotations() {
        return annotations;
    }
}

