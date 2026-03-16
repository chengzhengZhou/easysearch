package com.ppwx.easysearch.qp.ner;

import java.util.List;

/**
 * 实体类
 */
public class Entity {
    private String value;
    private EntityType type;
    private String normalizedValue;
    private List<String> id;
    private double confidence;
    private int startOffset;
    private int endOffset;

    private Object attachment;

    public Entity() {}
    
    public Entity(String value, EntityType type) {
        this.value = value;
        this.type = type;
        this.confidence = 1.0;
    }
    
    public Entity(String value, EntityType type, String normalizedValue) {
        this.value = value;
        this.type = type;
        this.normalizedValue = normalizedValue;
        this.confidence = 1.0;
    }

    public Entity(String value, EntityType type, String normalizedValue, List<String> id) {
        this.value = value;
        this.type = type;
        this.normalizedValue = normalizedValue;
        this.id = id;
        this.confidence = 1.0;
    }
    
    public Entity(String value, EntityType type, String normalizedValue, List<String> id, double confidence) {
        this.value = value;
        this.type = type;
        this.normalizedValue = normalizedValue;
        this.id = id;
        this.confidence = confidence;
    }

    public Entity(String value, EntityType type, String normalizedValue, double confidence, int startOffset, int endOffset) {
        this.value = value;
        this.type = type;
        this.normalizedValue = normalizedValue;
        this.confidence = confidence;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }

    public Entity(String value, EntityType type, String normalizedValue, List<String> id,
                  int startOffset, int endOffset) {
        this.value = value;
        this.type = type;
        this.normalizedValue = normalizedValue;
        this.id = id;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.confidence = 1.0;
    }
    
    // Getters and Setters
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
    
    public EntityType getType() {
        return type;
    }
    
    public void setType(EntityType type) {
        this.type = type;
    }
    
    public String getNormalizedValue() {
        return normalizedValue != null ? normalizedValue : value;
    }
    
    public void setNormalizedValue(String normalizedValue) {
        this.normalizedValue = normalizedValue;
    }
    
    public double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }
    
    public int getStartOffset() {
        return startOffset;
    }
    
    public void setStartOffset(int startOffset) {
        this.startOffset = startOffset;
    }
    
    public int getEndOffset() {
        return endOffset;
    }
    
    public void setEndOffset(int endOffset) {
        this.endOffset = endOffset;
    }

    public List<String> getId() {
        return id;
    }

    public void setId(List<String> id) {
        this.id = id;
    }

    public void setAttachment(Object attachment) {
        this.attachment = attachment;
    }

    public Object getAttachment() {
        return attachment;
    }

    @Override
    public String toString() {
        return "Entity{" +
                "value='" + value + '\'' +
                ", type=" + type +
                ", normalizedValue='" + normalizedValue + '\'' +
                ", id=" + id +
                ", confidence=" + confidence +
                ", startOffset=" + startOffset +
                ", endOffset=" + endOffset +
                '}';
    }
}

