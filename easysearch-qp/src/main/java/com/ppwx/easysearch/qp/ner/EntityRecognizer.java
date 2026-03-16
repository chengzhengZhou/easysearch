package com.ppwx.easysearch.qp.ner;

import com.ppwx.easysearch.qp.tokenizer.Token;

import java.util.Collection;
import java.util.List;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className EntityRecognizer
 * @description 实体识别
 * @date 2024/10/9 17:05
 **/
public interface EntityRecognizer {

    /**
     * 识别实体
     *
     * @param originText 原始文本
     * @param tokens     分词结果
     * @return 实体列表
     */
    Collection<Entity> extractEntities(String originText, List<Token> tokens);

}
