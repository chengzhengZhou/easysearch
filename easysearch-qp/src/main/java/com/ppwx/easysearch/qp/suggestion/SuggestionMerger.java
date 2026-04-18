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

package com.ppwx.easysearch.qp.suggestion;

import java.util.List;
import java.util.Map;

/**
 * 融合策略接口：将多路召回结果融合为统一排序的联想词列表。
 * <p>
 * 不同的融合实现（RRF、加权求和、LTR 等）均实现此接口。
 */
public interface SuggestionMerger {

    /**
     * 将多路召回结果融合排序。
     *
     * @param channelResults key=channel name, value=该路的召回结果（已按路内排序）
     * @param limit          最终返回条数
     * @return 融合排序后的联想词列表，不为 null
     */
    List<Suggestion> merge(Map<String, List<RecallResult>> channelResults, int limit);
}
