package com.ppwx.easysearch.qp.ner;

import cn.hutool.core.io.IORuntimeException;
import com.google.common.collect.Lists;
import com.ppwx.easysearch.core.util.SearchLog;
import com.ppwx.easysearch.qp.support.*;
import com.ppwx.easysearch.qp.util.StreamUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.Ordered;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className NamedEntityRecognition
 * @description 实体识别
 * @date 2024/10/9 16:08
 **/
public class NamedEntityRecognition {

    private List<EntityMatcher> matcherList;

    private WordMerger wordMerger = new NeighborWordMerger();

    public NamedEntityRecognition(String path) {
        this.load(new String[]{path});
    }

    public NamedEntityRecognition(String... path) {
        this.load(path);
    }

    private void load(String[] paths) {
        // 加载命名实体数据
        List<EntityMatcher> matcherList = Lists.newArrayList();
        String tail = ".dic";
        String name;
        int order;
        for (String path : paths) {
            String resourceName = StreamUtil.getResourceName(path);
            if (!resourceName.endsWith(tail)) {
                SearchLog.getLogger().warn("named entity file name must end with dic");
                continue;
            }
            String fileName = resourceName.substring(0, resourceName.lastIndexOf(tail));
            int index = fileName.indexOf("_");
            name = fileName.substring(0, index);
            order = Integer.parseInt(fileName.substring(index + 1));
            WordMatchTree dicTree = new WordMatchTree();
            dicTree.setCharFilter(CustomStopChar::isNotStopChar);
            try {
                StreamUtil.readUtf8Lines(path, line -> {
                    if (StringUtils.isNoneBlank(line)) {
                        dicTree.addWord(line);
                    }
                });
            } catch (IOException e) {
                throw new IORuntimeException(e);
            }
            matcherList.add(new DFAMatcher(name, dicTree, order));
        }
        // sort
        matcherList.sort(Comparator.comparingInt(Ordered::getOrder));

        this.matcherList = matcherList;
    }

    /**
     * @description 实体识别，对输入的字符进行命名实体识别
     * @author ext.ahs.zhouchzh1@jd.com
     * @date 2024/10/9 17:37
     * @param val 需要识别的字符
     * @return List<FindEntity>
    */
    public List<FindEntity> recognize(String val) {
        if (StringUtils.isBlank(val)) {
            return Collections.emptyList();
        }
        // common filter
        //StringBuilder restVal = new StringBuilder(val);

        // find keyword
        List<FindEntity> entities = Lists.newLinkedList();
        for (EntityMatcher matcher : matcherList) {
            List<FoundWord> foundWords = matcher.matchAll(val);
            for (FoundWord foundWord : foundWords) {
                entities.add(new FindEntity(foundWord.getFoundWord(), matcher.getName(), matcher.getOrder(),
                        foundWord.getStartIndex(), foundWord.getEndIndex()));
            }
            // remove found
            /*for (int i = foundWords.size() - 1; i >= 0; i--) {
                FoundWord entity = foundWords.get(i);
                restVal.replace(entity.getStartIndex(), entity.getEndIndex() + 1, "");
            }*/

        }

        // merge neighbor word
        entities = wordMerger.merge(val, entities);

        return entities;
    }

    public void addMatcher(EntityMatcher matcher) {
        matcherList.add(matcher);
    }

    public void setWordMerger(WordMerger wordMerger) {
        this.wordMerger = wordMerger;
    }
}
