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

import com.ppwx.easysearch.qp.support.DefaultPipeline;
import com.ppwx.easysearch.qp.support.Pipeline;

import java.util.List;

/**
 * @className WordFormatCompositor
 * @description WordFormat聚合
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
