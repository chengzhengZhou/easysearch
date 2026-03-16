package com.ppwx.easysearch.qp.ner;


import java.util.List;

/**
 * 实体识别结果的id映射
 */
public interface EntityIdentityMapper {

    /**
     * 映射实体id
     * @param type 实体类型
     * @param word 实体值
     * @param normalizedValue 映射后的实体值
     */
    List<String> map(EntityType type, String word, String normalizedValue);

}
