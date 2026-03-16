package com.ppwx.easysearch.qp.ner;

import com.hankcs.hanlp.collection.trie.bintrie.BaseNode;
import com.hankcs.hanlp.collection.trie.bintrie.BinTrie;
import com.hankcs.hanlp.corpus.document.sentence.Sentence;
import com.hankcs.hanlp.corpus.tag.Nature;
import com.hankcs.hanlp.dictionary.CoreDictionary;
import com.hankcs.hanlp.model.crf.CRFLexicalAnalyzer;
import com.hankcs.hanlp.model.crf.CRFNERecognizer;
import com.hankcs.hanlp.model.crf.CRFPOSTagger;
import com.hankcs.hanlp.model.crf.CRFSegmenter;
import org.junit.Test;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class CRFNERecognizerTest {

    private static final BinTrie<CoreDictionary.Attribute> customDictionary = loadDictionary();

    @Test
    public void testGetter() {
        Sentence sentence = Sentence.create("[[华为/NR]/BRAND p50/NR]/MODEL 曜金黑/COLOR 全网通/VV 国行/VERSION [8g/NN +/PU 128g/NN]/RAM");
        System.out.println(sentence.toStandoff());
    }

    @Test
    public void test() throws IOException {
        String crfModelPath = "data/model/";
        CRFSegmenter segmenter = new CRFSegmenter(crfModelPath + "vocab_cws_crf3.txt.bin");
        CRFPOSTagger tagger = new CRFPOSTagger(crfModelPath + "vocab_pos_crf3.txt.bin");
        CRFNERecognizer ner = new CRFNERecognizer(crfModelPath + "vocab_ner_crf3.txt.bin");

        CRFLexicalAnalyzer lexicalAnalyzer = new CRFLexicalAnalyzer(segmenter, tagger, ner);
        lexicalAnalyzer.enableCustomDictionary(false);

        String sent = "华为平板 M5 10.1英寸（青春版）";
        String normalized = sent.replaceAll("\\s+", "").toLowerCase();
        List<String> segment = segmenter.segment(normalized);
        System.out.println(segment);
        System.out.println("---------------------");

        String[] tag = tagger.tag(segment);
        for (int i = 0; i < segment.size(); ++i)
        {
            System.out.print(segment.get(i) + "/" + tag[i] + " ");
        }
        System.out.println();
        System.out.println("---------------------");

        String[] recognize = ner.recognize(segment.toArray(new String[0]), tag);
        for (int i = 0; i < segment.size(); ++i)
        {
            System.out.print(segment.get(i) + "/" + tag[i] + "/" + recognize[i] + " ");
        }
        System.out.println();
        System.out.println("---------------------");

        List<CoreDictionary.Attribute> attributes = segmentWithAttribute(sent, segment, false);
        System.out.println(segment);
    }

    private List<CoreDictionary.Attribute> segmentWithAttribute(String original, List<String> wordList, boolean forceCustomDictionary)
    {
        List<CoreDictionary.Attribute> attributeList;
        if (forceCustomDictionary)
        {
            attributeList = new LinkedList<>();
            segment(original, wordList, attributeList);
        }
        else
        {
            attributeList = combineWithCustomDictionary(wordList);
        }
        return attributeList;
    }

    /**
     * 分词
     *
     * @param sentence      文本
     * @param wordList      储存单词列表
     * @param attributeList 储存用户词典中的词性，设为null表示不查询用户词典
     */
    protected void segment(final String sentence, final List<String> wordList, final List<CoreDictionary.Attribute> attributeList)
    {
        if (attributeList != null)
        {
            customDictionary.parseLongestText(sentence, (begin, end, value) -> {
                while (attributeList.size() < wordList.size())
                    attributeList.add(null);
                wordList.add(sentence.substring(begin, end));
                attributeList.add(value);
                assert wordList.size() == attributeList.size() : "词语列表与属性列表不等长";
            });
        }
    }

    /**
     * 使用用户词典合并粗分结果
     *
     * @param vertexList 粗分结果
     * @return 合并后的结果
     */
    protected List<CoreDictionary.Attribute> combineWithCustomDictionary(List<String> vertexList)
    {
        String[] wordNet = new String[vertexList.size()];
        vertexList.toArray(wordNet);
        CoreDictionary.Attribute[] attributeArray = new CoreDictionary.Attribute[wordNet.length];
        // BinTrie合并
        int length = wordNet.length;
        for (int i = 0; i < length; ++i)
        {
            if (wordNet[i] == null) continue;
            BaseNode<CoreDictionary.Attribute> state = customDictionary.transition(wordNet[i], 0);
            if (state != null)
            {
                int to = i + 1;
                int end = to;
                CoreDictionary.Attribute value = state.getValue();
                for (; to < length; ++to)
                {
                    if (wordNet[to] == null) continue;
                    state = state.transition(wordNet[to], 0);
                    if (state == null) break;
                    if (state.getValue() != null)
                    {
                        value = state.getValue();
                        end = to + 1;
                    }
                }
                if (value != null)
                {
                    combineWords(wordNet, i, end, attributeArray, value);
                    i = end - 1;
                }
            }
        }

        vertexList.clear();
        List<CoreDictionary.Attribute> attributeList = new LinkedList<CoreDictionary.Attribute>();
        for (int i = 0; i < wordNet.length; i++)
        {
            if (wordNet[i] != null)
            {
                vertexList.add(wordNet[i]);
                attributeList.add(attributeArray[i]);
            }
        }
        return attributeList;
    }

    /**
     * 将连续的词语合并为一个
     *
     * @param wordNet 词图
     * @param start   起始下标（包含）
     * @param end     结束下标（不包含）
     * @param value   新的属性
     */
    private static void combineWords(String[] wordNet, int start, int end, CoreDictionary.Attribute[] attributeArray, CoreDictionary.Attribute value)
    {
        if (start + 1 != end)   // 小优化，如果只有一个词，那就不需要合并，直接应用新属性
        {
            StringBuilder sbTerm = new StringBuilder();
            for (int j = start; j < end; ++j)
            {
                if (wordNet[j] == null) continue;
                if (j != start) sbTerm.append(" ");
                sbTerm.append(wordNet[j]);
                wordNet[j] = null;
            }
            wordNet[start] = sbTerm.toString();
        }
        attributeArray[start] = value;
    }

    public static BinTrie<CoreDictionary.Attribute> loadDictionary() {
        BinTrie<CoreDictionary.Attribute> trie = new BinTrie<>();
        trie.put("影石", new CoreDictionary.Attribute(Nature.create("BRAND")));
        trie.put("摄像机", new CoreDictionary.Attribute(Nature.create("CATEGORY")));
        trie.put("影石摄像机", new CoreDictionary.Attribute(Nature.create("MODEL")));
        return trie;
    }

}
