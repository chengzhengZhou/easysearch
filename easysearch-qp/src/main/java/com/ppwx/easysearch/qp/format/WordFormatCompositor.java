package com.ppwx.easysearch.qp.format;

import com.ppwx.easysearch.qp.support.DefaultPipeline;
import com.ppwx.easysearch.qp.support.Pipeline;

import java.util.List;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className WordFormatCompositor
 * @description WordFormat聚合
 * @date 2024/11/1 23:56
 **/
public abstract class WordFormatCompositor implements WordFormat {

    @Override
    public StringBuilder format(StringBuilder original) {
        Pipeline<WordFormat> pipeline = new DefaultPipeline<>();
        init(pipeline);

        StringBuilder result = original;

        List<WordFormat> charFormats = pipeline.list();
        for(WordFormat charFormat : charFormats) {
            result = charFormat.format(result);
        }

        return result;
    }

    /**
     * 初始化列表
     *
     * @param pipeline 当前列表泳道
     * @since 0.0.13
     */
    protected abstract void init(final Pipeline<WordFormat> pipeline);
}
