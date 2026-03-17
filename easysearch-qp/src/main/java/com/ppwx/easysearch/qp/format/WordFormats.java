/*
 * Copyright 2026 chengzhengZhou
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ppwx.easysearch.qp.format;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ArrayUtil;
import com.ppwx.easysearch.qp.support.Pipeline;

import java.util.Collection;

/**
 * @className WordFormats
 * @description WordFormat操作类
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

    public static WordFormat truncate() {
        return WordFormatTruncate.getInstance();
    }
}
