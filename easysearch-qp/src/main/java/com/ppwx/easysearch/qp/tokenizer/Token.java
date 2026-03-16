package com.ppwx.easysearch.qp.tokenizer;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 分词结果
 * 
 * @author system
 * @date 2024/12/19
 */
public class Token {
    
    private String text;           // 分词文本
    private String type;           // 词性类型
    private int startIndex;        // 起始位置
    private int endIndex;          // 结束位置
    private double confidence;     // 置信度
    private Map<String, String> attributes; // 扩展属性
    
    public Token() {
        this.attributes = new HashMap<>();
    }
    
    public Token(String text, String type, int startIndex, int endIndex, double confidence) {
        this.text = text;
        this.type = type;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.confidence = confidence;
        this.attributes = new HashMap<>();
    }
    
    /**
     * 创建Token的Builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * 创建Token副本的Builder
     */
    public Builder toBuilder() {
        return new Builder()
            .text(this.text)
            .type(this.type)
            .startIndex(this.startIndex)
            .endIndex(this.endIndex)
            .confidence(this.confidence)
            .attributes(new HashMap<>(this.attributes));
    }
    
    // Getters and Setters
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public int getStartIndex() {
        return startIndex;
    }
    
    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }
    
    public int getEndIndex() {
        return endIndex;
    }
    
    public void setEndIndex(int endIndex) {
        this.endIndex = endIndex;
    }
    
    public double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }
    
    public Map<String, String> getAttributes() {
        return attributes;
    }
    
    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes != null ? attributes : new HashMap<>();
    }
    
    public void addAttribute(String key, String value) {
        this.attributes.put(key, value);
    }
    
    public String getAttribute(String key) {
        return this.attributes.get(key);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Token token = (Token) o;
        return startIndex == token.startIndex &&
               endIndex == token.endIndex &&
               Double.compare(token.confidence, confidence) == 0 &&
               Objects.equals(text, token.text) &&
               Objects.equals(type, token.type);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(text, type, startIndex, endIndex, confidence);
    }
    
    @Override
    public String toString() {
        return "Token{" +
                "text='" + text + '\'' +
                ", type='" + type + '\'' +
                ", startIndex=" + startIndex +
                ", endIndex=" + endIndex +
                ", confidence=" + confidence +
                ", attributes=" + attributes +
                '}';
    }
    
    /**
     * Token Builder类
     */
    public static class Builder {
        private String text;
        private String type;
        private int startIndex;
        private int endIndex;
        private double confidence = 1.0;
        private Map<String, String> attributes = new HashMap<>();
        
        public Builder text(String text) {
            this.text = text;
            return this;
        }
        
        public Builder type(String type) {
            this.type = type;
            return this;
        }
        
        public Builder startIndex(int startIndex) {
            this.startIndex = startIndex;
            return this;
        }
        
        public Builder endIndex(int endIndex) {
            this.endIndex = endIndex;
            return this;
        }
        
        public Builder confidence(double confidence) {
            this.confidence = confidence;
            return this;
        }
        
        public Builder attributes(Map<String, String> attributes) {
            this.attributes = attributes != null ? attributes : new HashMap<>();
            return this;
        }
        
        public Builder addAttribute(String key, String value) {
            this.attributes.put(key, value);
            return this;
        }
        
        public Token build() {
            Token token = new Token(text, type, startIndex, endIndex, confidence);
            token.setAttributes(attributes);
            return token;
        }
    }
}
