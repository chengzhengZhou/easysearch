package com.ppwx.easysearch.qp.tokenizer;

import java.util.List;

/**
 * 分词器核心接口
 * 
 * @author system
 * @date 2024/12/19
 */
public interface Tokenizer {
    
    /**
     * 对输入文本进行分词
     * 
     * @param text 输入文本
     * @return 分词结果
     */
    List<Token> tokenize(String text);

}
