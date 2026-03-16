package com.ppwx.easysearch.qp.ner;

import com.ppwx.easysearch.qp.ner.recognizer.CRFEntityRecognizer;
import com.ppwx.easysearch.qp.ner.recognizer.DictEntityRecognizer;
import com.ppwx.easysearch.qp.tokenizer.Token;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 模型+词典的实体识别器
 * <p>
 * 组合 {@link CRFEntityRecognizer} 与 {@link DictEntityRecognizer}，
 * 模型训练调整周期较长，而词典能快速调整，两者结合可更好支持特殊词或临时实体词条。
 * <p>
 * 支持两种合并策略（{@link MergeStrategy}）：
 * <ul>
 *   <li>DICT_FIRST（默认）：词典优先，冲突时保留词典结果</li>
 *   <li>CRF_FIRST：CRF 优先，冲突时保留模型结果</li>
 * </ul>
 */
public class PriorityEntityRecognizer implements EntityRecognizer {

    private final CRFEntityRecognizer crfRecognizer;
    private final DictEntityRecognizer dictRecognizer;
    private final EntityMerger merger;

    /**
     * 使用默认策略 DICT_FIRST 与空词典 Dict 识别器构造。
     * 需要词典时请使用 {@link DictEntityRecognizer#fromPath(String)} 等工厂方法创建实例后传入带参构造。
     */
    public PriorityEntityRecognizer() {
        this(MergeStrategy.DICT_FIRST);
    }

    /**
     * 指定合并策略构造（词典识别器使用空词典）。
     *
     * @param strategy 合并策略，DICT_FIRST 或 CRF_FIRST
     */
    public PriorityEntityRecognizer(MergeStrategy strategy) {
        this(strategy, new DictEntityRecognizer());
    }

    /**
     * 指定合并策略与已加载词典的 Dict 识别器。
     *
     * @param strategy      合并策略
     * @param dictRecognizer 已通过工厂方法加载词典的 DictEntityRecognizer，可为 null（内部使用空词典）
     */
    public PriorityEntityRecognizer(MergeStrategy strategy, DictEntityRecognizer dictRecognizer) {
        this(strategy, new CRFEntityRecognizer(), dictRecognizer);
    }

    /**
     * 指定合并策略与已加载词典的 Dict 识别器。
     *
     * @param strategy      合并策略
     * @param dictRecognizer 已通过工厂方法加载词典的 DictEntityRecognizer，可为 null（内部使用空词典）
     */
    public PriorityEntityRecognizer(MergeStrategy strategy, CRFEntityRecognizer crfRecognizer, DictEntityRecognizer dictRecognizer) {
        this.crfRecognizer = crfRecognizer;
        this.dictRecognizer = dictRecognizer;
        this.merger = new EntityMerger(strategy);
    }

    /**
     * 使用默认策略 DICT_FIRST 与已加载词典的 Dict 识别器。
     *
     * @param dictRecognizer 已通过工厂方法加载词典的 DictEntityRecognizer
     */
    public PriorityEntityRecognizer(DictEntityRecognizer dictRecognizer) {
        this(MergeStrategy.DICT_FIRST, dictRecognizer);
    }

    @Override
    public Collection<Entity> extractEntities(String originText, List<Token> tokens) {
        if (originText == null || tokens == null || tokens.isEmpty()) {
            return Collections.emptyList();
        }

        Collection<Entity> dictEntities = dictRecognizer.extractEntities(originText, tokens);
        Collection<Entity> crfEntities = crfRecognizer.extractEntities(originText, tokens);

        return merger.merge(dictEntities, crfEntities);
    }
}
