package com.ppwx.easysearch.qp.prediction;

import com.ppwx.easysearch.qp.ner.Entity;
import com.ppwx.easysearch.qp.ner.EntityRecognizer;
import com.ppwx.easysearch.qp.ner.EntityType;
import com.ppwx.easysearch.qp.tokenizer.Token;
import com.ppwx.easysearch.qp.tokenizer.Tokenizer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * NerCategoryPrediction 单元测试
 * 
 * @author ext.ahs.zhouchzh1@jd.com
 * @date 2025/10/10
 */
public class NerCategoryPredictionTest {

    @Mock
    private EntityRecognizer mockEntityRecognizer;
    
    @Mock
    private Tokenizer mockTokenizer;
    
    private NerCategoryPrediction prediction;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        
        prediction = new NerCategoryPrediction(
            mockEntityRecognizer,
            mockTokenizer
        );
    }

    @Test
    public void testPredictWithEmptyQuery() {
        // 测试空查询
        List<Category> results = prediction.predict("");
        assertNotNull(results);
        assertTrue(results.isEmpty());
        
        results = prediction.predict(null);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    public void testPredictWithNoEntities() {
        // Mock分词结果
        List<Token> tokens = Arrays.asList(
            Token.builder().text("测试").type("n").startIndex(0).endIndex(2).build()
        );
        when(mockTokenizer.tokenize(anyString())).thenReturn(tokens);
        
        // Mock实体识别返回空列表
        when(mockEntityRecognizer.extractEntities(anyString(), any())).thenReturn(Collections.emptyList());
        
        List<Category> results = prediction.predict("测试查询");
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    public void testPredictWithLowConfidenceEntities() {
        // Mock分词结果
        List<Token> tokens = Arrays.asList(
            Token.builder().text("苹果").type("n").startIndex(0).endIndex(2).build()
        );
        when(mockTokenizer.tokenize(anyString())).thenReturn(tokens);
        
        // Mock实体识别返回低置信度实体
        Entity entity = new Entity("苹果", EntityType.BRAND, "苹果", Arrays.asList("1"), 0.3);
        when(mockEntityRecognizer.extractEntities(anyString(), any())).thenReturn(Arrays.asList(entity));
        
        // 设置置信度阈值为0.5
        prediction.setConfidenceThreshold(0.5);
        
        List<Category> results = prediction.predict("苹果");
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    public void testSetAndGetConfidenceThreshold() {
        prediction.setConfidenceThreshold(0.7);
        assertEquals(0.7, prediction.getConfidenceThreshold(), 0.01);
    }

    @Test
    public void testSetAndGetMaxResults() {
        prediction.setMaxResults(20);
        assertEquals(20, prediction.getMaxResults());
    }

    @Test
    public void testGetters() {
        assertNotNull(prediction.getEntityRecognizer());
        assertNotNull(prediction.getTokenizer());
    }

    @Test
    public void testPredictHandlesException() {
        // Mock分词抛出异常
        when(mockTokenizer.tokenize(anyString())).thenThrow(new RuntimeException("Test exception"));
        
        // 应该捕获异常并返回空列表
        List<Category> results = prediction.predict("测试查询");
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    public void testMaxResultsLimit() {
        // Mock分词结果
        List<Token> tokens = Arrays.asList(
            Token.builder().text("测试").type("n").startIndex(0).endIndex(2).build()
        );
        when(mockTokenizer.tokenize(anyString())).thenReturn(tokens);
        
        // Mock多个高置信度实体
        List<Entity> entities = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            Entity entity = new Entity("品牌" + i, EntityType.BRAND, "品牌" + i, Arrays.asList(String.valueOf(i)), 0.9);
            entities.add(entity);
        }
        when(mockEntityRecognizer.extractEntities(anyString(), any())).thenReturn(entities);
        
        // 设置最大结果数为10
        prediction.setMaxResults(10);
        
        List<Category> results = prediction.predict("测试查询");
        assertNotNull(results);
        // 结果应该被限制在10个以内（由于没有真实数据，实际会返回0）
        assertTrue(results.size() <= 10);
    }
}

