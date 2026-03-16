package com.ppwx.easysearch.qp.format;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ArrayUtil;
import com.ppwx.easysearch.qp.support.pipeline.Pipeline;

import java.util.Collection;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className WordFormats
 * @description WordFormat操作类
 * @date 2024/11/1 23:58
 **/
public final class WordFormats {

    private WordFormats(){}

    /**
     * 链式
     * @param charFormats 列表
     * @return 结果
     */
    public static WordFormat chains(final WordFormat... charFormats) {
        if(ArrayUtil.isEmpty(charFormats)) {
            return none();
        }

        return new WordFormatCompositor() {
            @Override
            protected void init(Pipeline<WordFormat> pipeline) {
                for(WordFormat charFormat : charFormats) {
                    pipeline.addLast(charFormat);
                }
            }
        };
    }

    /**
     * 链式
     * @param charFormats 列表
     * @return 结果
     */
    public static WordFormat chains(final Collection<WordFormat> charFormats) {
        if(CollectionUtil.isEmpty(charFormats)) {
            return none();
        }

        return new WordFormatCompositor() {
            @Override
            protected void init(Pipeline<WordFormat> pipeline) {
                for(WordFormat charFormat : charFormats) {
                    pipeline.addLast(charFormat);
                }
            }
        };
    }

    public static WordFormat none() {
        return WordFormatNone.getInstance();
    }

    public static WordFormat ignoreCase() {
        return WordFormatIgnoreCase.getInstance();
    }

    public static WordFormat trim() {
        return WordFormatTrim.getInstance();
    }

    public static WordFormat truncate() {
        return WordFormatTruncate.getInstance();
    }
}
