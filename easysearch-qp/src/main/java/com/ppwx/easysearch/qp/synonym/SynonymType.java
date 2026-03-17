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

/**
 * 同义词方向类型。
 * <ul>
 *   <li>UNIDIRECTIONAL：单向，仅 source → targets 参与匹配与改写/拓展</li>
 *   <li>BIDIRECTIONAL：双向，任一侧命中都可替换/拓展为另一侧</li>
 * </ul>
 */
public enum SynonymType {
    
    UNIDIRECTIONAL,
    
    BIDIRECTIONAL
}
