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
