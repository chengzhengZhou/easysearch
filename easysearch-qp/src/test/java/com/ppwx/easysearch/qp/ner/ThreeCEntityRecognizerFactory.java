package com.ppwx.easysearch.qp.ner;

import com.ppwx.easysearch.qp.ner.normalizer.CompositeEntityNormalizer;
import com.ppwx.easysearch.qp.ner.normalizer.EntityTypeNormalizer;
import com.ppwx.easysearch.qp.ner.normalizer.impl.*;
import com.ppwx.easysearch.qp.ner.recognizer.EntityTypeRecognizer;
import com.ppwx.easysearch.qp.ner.recognizer.impl.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 3C实体识别器工厂
 * 统一管理识别器和归一化器的创建
 * 
 * @author ext.ahs.zhouchzh1@jd.com
 * @date 2024/10/9
 */
public class ThreeCEntityRecognizerFactory {
    
    /**
     * 创建完整的实体识别器（含归一化）
     * 
     * @param identityMapper 实体ID映射器
     * @return 实体识别器
     */
    public static EntityRecognizer createEntityRecognizer(EntityIdentityMapper identityMapper) {
        // 1. 创建归一化器
        Map<String, EntityTypeNormalizer> normalizerMap = createNormalizers();
        
        // 2. 创建识别器（注入对应的归一化器）
        List<EntityTypeRecognizer> recognizers = createRecognizers(normalizerMap, identityMapper);
        
        // 3. 返回组合识别器
        return new CompositeEntityRecognizer(recognizers);
    }
    
    /**
     * 创建归一化器并返回映射
     */
    private static Map<String, EntityTypeNormalizer> createNormalizers() {
        Map<String, EntityTypeNormalizer> normalizerMap = new HashMap<>();
        
        normalizerMap.put("BRAND", new BrandNormalizer());
        normalizerMap.put("CATEGORY", new CategoryNormalizer());
        normalizerMap.put("MODEL", new ModelNormalizer());
        normalizerMap.put("CONDITION", new ConditionNormalizer());
        normalizerMap.put("STORAGE", new StorageNormalizer());
        normalizerMap.put("CPU", new CpuNormalizer());
        normalizerMap.put("TAG", new TagNormalizer());
        
        return normalizerMap;
    }
    
    /**
     * 创建识别器列表
     */
    private static List<EntityTypeRecognizer> createRecognizers(
            Map<String, EntityTypeNormalizer> normalizerMap,
            EntityIdentityMapper identityMapper) {
        
        List<EntityTypeRecognizer> recognizers = new ArrayList<>();
        
        recognizers.add(new BrandRecognizer(
                normalizerMap.get("BRAND"), identityMapper));
        recognizers.add(new CategoryRecognizer(
                normalizerMap.get("CATEGORY"), identityMapper));
        recognizers.add(new ModelRecognizer(
                normalizerMap.get("MODEL"), identityMapper));
        recognizers.add(new ConditionRecognizer(
                normalizerMap.get("CONDITION"), identityMapper));
        recognizers.add(new StorageRecognizer(
                normalizerMap.get("STORAGE"), identityMapper));
        recognizers.add(new TagRecognizer(
                normalizerMap.get("TAG"), identityMapper));
        
        return recognizers;
    }
    
    /**
     * 创建独立的归一化器（用于向后兼容）
     * 
     * @return 实体归一化器
     */
    public static EntityNormalizer createEntityNormalizer() {
        List<EntityTypeNormalizer> normalizers = new ArrayList<>();
        normalizers.add(new BrandNormalizer());
        normalizers.add(new CategoryNormalizer());
        normalizers.add(new ModelNormalizer());
        normalizers.add(new ConditionNormalizer());
        normalizers.add(new StorageNormalizer());
        normalizers.add(new CpuNormalizer());
        normalizers.add(new TagNormalizer());
        
        return new CompositeEntityNormalizer(normalizers);
    }
}

