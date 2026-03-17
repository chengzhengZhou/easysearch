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

import java.util.Collections;
import java.util.List;

/**
 * 同义词条属性：目标词列表 + 方向类型。
 * 存于 BinTrie 的 value，与源词（key）一起表示一条同义规则。
 */
public class SynonymAttribute {

    private final List<String> targets;
    private final SynonymType type;

    public SynonymAttribute(List<String> targets, SynonymType type) {
        this.targets = targets == null ? Collections.emptyList() : targets;
        this.type = type;
    }

    public List<String> getTargets() {
        return targets;
    }

    public SynonymType getType() {
        return type;
    }

    /** 改写时取第一个目标；无目标返回空串。 */
    public String getFirstTarget() {
        return targets.isEmpty() ? "" : targets.get(0);
    }
}
