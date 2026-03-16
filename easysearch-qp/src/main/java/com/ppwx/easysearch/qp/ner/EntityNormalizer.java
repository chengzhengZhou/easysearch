package com.ppwx.easysearch.qp.ner;

/**
 * 实体归一化接口
 */
public interface EntityNormalizer {

    /**
     * 归一化实体
     *
     * @param entityType 实体类型
     * @param word 实体词
     * @return 归一化后的实体词
     */
    String normalize(EntityType entityType, String word);

}
