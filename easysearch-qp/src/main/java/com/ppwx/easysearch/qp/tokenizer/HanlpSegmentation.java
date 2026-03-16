package com.ppwx.easysearch.qp.tokenizer;

import com.hankcs.hanlp.dictionary.CustomDictionary;
import com.hankcs.hanlp.seg.NShort.NShortSegment;
import com.hankcs.hanlp.seg.Segment;
import com.hankcs.hanlp.seg.common.Term;
import com.ppwx.easysearch.qp.data.DictionaryTermOpt;
import com.ppwx.easysearch.qp.data.ResourceObserver;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * HanLP分词器
 */
public class HanlpSegmentation extends ResourceObserver<DictionaryTermOpt> implements Segmentation {

    private final Segment segment;

    public HanlpSegmentation() {
        this.segment = new NShortSegment()
                .enableCustomDictionary(true)
                .enableNameRecognize(false)
                .enableTranslatedNameRecognize(false)
                .enableAllNamedEntityRecognize(false);
    }

    public HanlpSegmentation(Segment segment) {
        this.segment = segment;
    }

    @Override
    public List<Token> segment(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<Term> terms = segment.seg(text);

        return terms.stream()
                .map(this::convertToToken)
                .collect(Collectors.toList());
    }

    /**
     * 将HanLP的Term转换为Token
     *
     * @param term HanLP分词结果
     * @return Token对象
     */
    private Token convertToToken(Term term) {
        return Token.builder()
                .text(term.word)
                .type(term.nature.toString())
                .startIndex(term.offset)
                .endIndex(term.offset + term.length())
                .confidence(1.0)
                .build();
    }

    @Override
    protected void doUpdate(DictionaryTermOpt termOpt) {
        if (DictionaryTermOpt.DELETE == termOpt.getOpt()) {
            deleteItem(termOpt);
        } else if (DictionaryTermOpt.UPDATE == termOpt.getOpt()) {
            updateItem(termOpt);
        } else {
            addItem(termOpt);
        }
    }

    private void updateItem(final DictionaryTermOpt termOpt) {
        String attribute = getNatureWithFrequency(termOpt.getNature(), termOpt.getFrequency());
        termOpt.getWord().forEach(word -> {
            CustomDictionary.insert(word, attribute);
            // add no space word
            CustomDictionary.insert(noSpace(word), attribute);
        });
    }

    protected void deleteItem(DictionaryTermOpt termOpt) {
        termOpt.getWord().forEach(CustomDictionary::remove);
    }

    protected void addItem(DictionaryTermOpt termOpt) {
        String attribute = getNatureWithFrequency(termOpt.getNature(), termOpt.getFrequency());
        termOpt.getWord().forEach(word -> {
            CustomDictionary.add(word, attribute);
            // add no space word
            CustomDictionary.add(noSpace(word), attribute);
        });
    }

    @Override
    protected boolean acceptable(DictionaryTermOpt termOpt) {
        return DictionaryTermOpt.TYPE_DIC.equals(termOpt.getTermType());
    }

    /**
     * 词性和其对应的频次，比如“nz 1 v 2”，null时表示“nz 1”
     * @return 词性和频次
     */
    private String getNatureWithFrequency(List<String> nature, List<String> frequency) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < nature.size(); i++) {
            builder.append(nature.get(i));
            if (frequency != null && i < frequency.size()) {
                builder.append(" ").append(frequency.get(i));
            } else {
                builder.append(" 1");
            }
            builder.append(" ");
        }
        return builder.toString().trim();
    }

    /**
     * 去空格
     */
    private String noSpace(String word) {
        return word.replaceAll("\\s+", "").trim();
    }
}
