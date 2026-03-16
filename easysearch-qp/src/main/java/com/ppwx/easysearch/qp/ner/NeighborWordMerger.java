package com.ppwx.easysearch.qp.ner;

import cn.hutool.core.lang.DefaultSegment;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ppwx.easysearch.qp.support.CustomStopChar;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className NeighborWordMerger
 * @description 临近同型词合并
 * @date 2024/10/9 22:49
 **/
public class NeighborWordMerger implements WordMerger{
    @Override
    public List<FindEntity> merge(String val, List<FindEntity> origin) {
        if (CollectionUtils.isEmpty(origin)) {
            return origin;
        }
        List<FindEntity> newEntries = Lists.newLinkedList(origin);
        // cut lower
        List<FindEntity> removeList = Lists.newArrayList();
        for (FindEntity preEntry : newEntries) {
            for (FindEntity nextEntry : newEntries) {
                if (preEntry == nextEntry) {
                    continue;
                }
                if (nextEntry.getWeight() < preEntry.getWeight() && include(nextEntry, preEntry)) {
                    if (excludeWord(nextEntry)
                            && StringUtils.equals(preEntry.getTerm(), nextEntry.getTerm())
                            && origin.size() > 3
                    ) {
                        continue;
                    }
                    removeList.add(preEntry);
                }
            }
        }
        newEntries.removeAll(removeList);

        List<FindEntity> appendList = Lists.newArrayList();
        List<FindEntity> productList = Lists.newArrayList();
        // 剔除机型属性
        newEntries.forEach(entry -> {
            if (StringUtils.equals(entry.getType(), "product")) {
                productList.add(entry);
            } else {
                appendList.add(entry);
            }
        });
        // 对机型进行合并
        if (!productList.isEmpty()) {
            List<FindEntity> group = productList.stream()
                    .sorted(Comparator.comparing(DefaultSegment::getStartIndex))
                    .collect(Collectors.toList());
            doMerge(val, group, appendList);
        }

        return appendList;
    }

    public void doMerge(String val, List<FindEntity> mergeList, List<FindEntity> appendList) {
        // 合并
        Iterator<FindEntity> iterator = mergeList.iterator();
        FindEntity pre = iterator.next();
        FindEntity next;
        while (iterator.hasNext()) {
            // merge and find next
            next = iterator.next();
            if (StringUtils.equals(pre.getType(), next.getType()) && !StringUtils.equals(pre.getTerm(), next.getTerm())) {
                if (pre.getEndIndex() == next.getStartIndex() - 1) {
                    pre = new FindEntity(pre.getTerm() + next.getTerm(), pre.getType(),
                            pre.getWeight(), pre.getStartIndex(), next.getEndIndex());
                    continue;
                } else if (pre.getEndIndex() < next.getStartIndex() && isStop(val, pre.getEndIndex(), next.getStartIndex())) {
                    // 替换掉停顿词统一用一个空格，但是保留位置
                    pre = new FindEntity(pre.getTerm() + " " + next.getTerm(), pre.getType(),
                            pre.getWeight(), pre.getStartIndex(), next.getEndIndex());
                    continue;
                }
            }

            appendList.add(pre);
            pre = next;
        }

        appendList.add(pre);
    }

    private boolean include(FindEntity pre, FindEntity next) {
        return pre.getStartIndex() <= next.getStartIndex() && pre.getEndIndex() >= next.getEndIndex();
    }

    private boolean isStop(String val, Integer endIndex, Integer startIndex) {
        for (int i = endIndex + 1; i < startIndex; i++) {
            if (CustomStopChar.isNotStopChar(val.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static final Set<String> excludeWord = Sets.newHashSet("pad", "平板", "笔记本");

    private boolean excludeWord(FindEntity entity) {
        return StringUtils.equals("category", entity.getType()) && excludeWord.contains(entity.getTerm());
    }
}
