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
