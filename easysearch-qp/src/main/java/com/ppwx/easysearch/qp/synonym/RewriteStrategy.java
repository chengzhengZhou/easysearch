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
 * 改写策略：根据匹配结果将 query 改写为一条新 query。
 */
@FunctionalInterface
public interface RewriteStrategy {

    /**
     * 将 query 按 matches 改写为一条字符串。
     *
     * @param query   原始查询
     * @param matches 同义词匹配结果（通常来自 SynonymEngine.match）
     * @return 改写后的单条 query；无匹配时建议返回原 query
     */
    String rewrite(String query, List<SynonymMatch> matches);
}
