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

package com.ppwx.easysearch.qp.synonym;

import java.util.List;

/**
 * 拓展策略：根据匹配结果生成多条候选 query（用于 OR 扩展等）。
 */
@FunctionalInterface
public interface ExpandStrategy {

    /**
     * 根据 matches 拓展出多条 query。
     *
     * @param query   原始查询
     * @param matches 同义词匹配结果
     * @return 拓展后的 query 列表；无匹配时通常返回只含原 query 的列表
     */
    List<String> expand(String query, List<SynonymMatch> matches);
}
