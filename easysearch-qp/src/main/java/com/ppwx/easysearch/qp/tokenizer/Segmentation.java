package com.ppwx.easysearch.qp.tokenizer;

import java.util.List;

/**
 * 分词接口
 */
public interface Segmentation {

    /**
     * 对输入文本进行分词
     *
     * @param text 输入文本
     * @return 分词结果
     */
    List<Token> segment(String text);

}
