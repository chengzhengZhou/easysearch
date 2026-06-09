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

package com.hankcs.hanlp.model.crf;

import com.hankcs.hanlp.corpus.document.sentence.Sentence;
import com.hankcs.hanlp.corpus.document.sentence.word.Word;
import com.hankcs.hanlp.model.crf.crfpp.FeatureIndex;
import com.ppwx.easysearch.qp.tokenizer.product3c.FeatureColumnBuilder;
import com.hankcs.hanlp.model.perceptron.PerceptronSegmenter;
import com.hankcs.hanlp.model.perceptron.common.TaskType;
import com.hankcs.hanlp.model.perceptron.feature.FeatureMap;
import com.hankcs.hanlp.model.perceptron.instance.CWSInstance;
import com.hankcs.hanlp.model.perceptron.tagset.CWSTagSet;
import com.hankcs.hanlp.tokenizer.lexical.Segmenter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * 面向二手 3C 搜索的 CRF 分词器。
 *
 * 特征列与 data/template/3c_cws.tpl 对齐：
 * char, char_type, lex_hit, param_hit。
 */
public class Product3CCRFSegmenter extends CRFTagger implements Segmenter
{
    public static final String DEFAULT_DICT_PATH = "data/dict/3c";
    public static final String DEFAULT_SPELLING_PATH = "data/dict/spelling_normalize.tsv";
    public static final String CRF_MODEL = "data/model/3c_cws.crf.txt.bin";
    public static final int NUM_COLUMNS = FeatureColumnBuilder.NUM_COLUMNS;

    private final FeatureColumnBuilder featureColumnBuilder;
    private PerceptronSegmenter decoder;
    private CWSTagSet cwsTagSet;

    public Product3CCRFSegmenter() throws IOException {
        this(CRF_MODEL, DEFAULT_DICT_PATH, DEFAULT_SPELLING_PATH);
    }

    public Product3CCRFSegmenter(String modelPath) throws IOException
    {
        this(modelPath, DEFAULT_DICT_PATH, DEFAULT_SPELLING_PATH);
    }

    public Product3CCRFSegmenter(String modelPath, String dictPath) throws IOException
    {
        this(modelPath, dictPath, DEFAULT_SPELLING_PATH);
    }

    public Product3CCRFSegmenter(String modelPath, String dictPath, String spellingPath) throws IOException
    {
        super(modelPath);
        this.featureColumnBuilder = new FeatureColumnBuilder(dictPath, spellingPath);
        initDecoder();
    }

    @Override
    public List<String> segment(String text)
    {
        List<String> wordList = new LinkedList<String>();
        segment(text, null, wordList);
        return wordList;
    }

    @Override
    public void segment(String text, String normalized, List<String> wordList)
    {
        if (text == null || text.length() == 0)
        {
            return;
        }
        ensureDecoder();

        FeatureColumnBuilder.Result result = featureColumnBuilder.build(text);
        if (result.length() == 0)
        {
            return;
        }

        int[] tagArray = decode(result.columns);
        tagsToWords(result, tagArray, wordList);
    }

    /**
     * 分词并返回带原始 offset 的 Token 信息：[rawText, startIndex, endIndex]。
     * startIndex/endIndex 对应原始 rawText 中的字节位置，可直接用于构造分词 Token。
     */
    public List<int[]> segmentWithOffsets(String text)
    {
        if (text == null || text.length() == 0)
        {
            return new LinkedList<int[]>();
        }
        ensureDecoder();
        FeatureColumnBuilder.Result result = featureColumnBuilder.build(text);
        if (result.length() == 0)
        {
            return new LinkedList<int[]>();
        }
        int[] tagArray = decode(result.columns);
        List<int[]> spans = new LinkedList<int[]>();
        collectSpans(result, tagArray, spans);
        return spans;
    }

    private void collectSpans(FeatureColumnBuilder.Result result, int[] tagArray, List<int[]> spans)
    {
        int tokenRawStart = -1;
        int tokenRawEnd = -1;
        for (int i = 0; i <= tagArray.length; i++)
        {
            boolean flush = (i == tagArray.length);
            if (!flush)
            {
                char ch = result.normalized.charAt(i);
                if (ch == ' ')
                {
                    flush = true;
                }
                else
                {
                    if ((tagArray[i] == cwsTagSet.B || tagArray[i] == cwsTagSet.S) && tokenRawStart >= 0)
                    {
                        spans.add(new int[]{tokenRawStart, tokenRawEnd});
                        tokenRawStart = -1;
                        tokenRawEnd = -1;
                    }
                    if (tokenRawStart < 0)
                    {
                        tokenRawStart = result.rawStart[i];
                    }
                    tokenRawEnd = Math.max(tokenRawEnd, result.rawEnd[i]);
                    if (tagArray[i] == cwsTagSet.S)
                    {
                        flush = true;
                    }
                }
            }
            if (flush && tokenRawStart >= 0)
            {
                spans.add(new int[]{tokenRawStart, tokenRawEnd});
                tokenRawStart = -1;
                tokenRawEnd = -1;
            }
        }
    }

    /**
     * 直接使用列式特征分词：columns[col][i] = 第 i 个字符的第 col 列特征。
     */
    public List<String> segment(String[][] columns)
    {
        List<String> wordList = new LinkedList<String>();
        segment(columns, wordList);
        return wordList;
    }

    /**
     * 直接使用列式特征分词：columns[col][i] = 第 i 个字符的第 col 列特征。
     */
    public void segment(String[][] columns, List<String> wordList)
    {
        ensureDecoder();
        validateColumns(columns);
        if (columns[0].length == 0)
        {
            return;
        }
        tagsToWords(toSentence(columns), decode(columns), wordList);
    }

    /**
     * 直接使用列式特征输出 BMES 标签。
     */
    public String[] tag(String[][] columns)
    {
        ensureDecoder();
        validateColumns(columns);
        if (columns[0].length == 0)
        {
            return new String[0];
        }
        return toTags(decode(columns));
    }

    /**
     * 返回推理时实际使用的 4 列特征，便于排查训练/推理是否一致。
     * table[i][col] = 第 i 个字符的第 col 列特征。
     */
    public String[][] extractFeatureTable(String text)
    {
        return featureColumnBuilder.extractFeatureTable(text);
    }

    @Override
    protected void convertCorpus(Sentence sentence, BufferedWriter bw) throws IOException
    {
        for (Word word : sentence.toSimpleWordList())
        {
            featureColumnBuilder.writeWordTags(word.value, bw);
        }
    }

    @Override
    protected String getDefaultFeatureTemplate()
    {
        return "# char window\n" +
            "U00:%x[-2,0]\n" +
            "U01:%x[-1,0]\n" +
            "U02:%x[0,0]\n" +
            "U03:%x[1,0]\n" +
            "U04:%x[2,0]\n" +
            "\n" +
            "# char bigram\n" +
            "U10:%x[-1,0]/%x[0,0]\n" +
            "U11:%x[0,0]/%x[1,0]\n" +
            "\n" +
            "# char type\n" +
            "U20:%x[-2,1]\n" +
            "U21:%x[-1,1]\n" +
            "U22:%x[0,1]\n" +
            "U23:%x[1,1]\n" +
            "U24:%x[2,1]\n" +
            "U25:%x[-1,1]/%x[0,1]\n" +
            "U26:%x[0,1]/%x[1,1]\n" +
            "\n" +
            "# lex hit\n" +
            "U30:%x[-1,2]\n" +
            "U31:%x[0,2]\n" +
            "U32:%x[1,2]\n" +
            "U33:%x[-1,2]/%x[0,2]\n" +
            "U34:%x[0,2]/%x[1,2]\n" +
            "\n" +
            "# param hit\n" +
            "U40:%x[-1,3]\n" +
            "U41:%x[0,3]\n" +
            "U42:%x[1,3]\n" +
            "U43:%x[-1,3]/%x[0,3]\n" +
            "U44:%x[0,3]/%x[1,3]\n" +
            "\n" +
            "# mixed\n" +
            "U50:%x[0,0]/%x[0,1]\n" +
            "U51:%x[0,1]/%x[0,2]\n" +
            "U52:%x[0,1]/%x[0,3]\n" +
            "U53:%x[0,2]/%x[0,3]\n" +
            "\n" +
            "# transition\n" +
            "B";
    }

    private void initDecoder()
    {
        if (model == null)
        {
            decoder = null;
            cwsTagSet = null;
            return;
        }
        if (model.featureMap.tagSet.type != TaskType.CWS)
        {
            throw new IllegalArgumentException("传入的不是 CWS 分词模型");
        }
        cwsTagSet = (CWSTagSet) model.featureMap.tagSet;
        decoder = new PerceptronSegmenter(model);
    }

    private void ensureDecoder()
    {
        if (decoder == null || cwsTagSet == null)
        {
            throw new IllegalStateException("Product3CCRFSegmenter 未加载模型，无法分词");
        }
    }

    private int[] decode(String[][] columns)
    {
        CWSInstance instance = createInstance(columns);
        int[] tagArray = instance.tagArray;
        model.viterbiDecode(instance, tagArray);
        return tagArray;
    }

    private CWSInstance createInstance(final String[][] columns)
    {
        final FeatureTemplate[] templates = model.getFeatureTemplateArray();
        final int n = columns[0].length;
        return new CWSInstance(toSentence(columns), model.featureMap)
        {
            @Override
            protected int[] extractFeature(String sentence, FeatureMap featureMap, int position)
            {
                StringBuilder sb = new StringBuilder();
                List<Integer> vec = new LinkedList<Integer>();
                for (int i = 0; i < templates.length; i++)
                {
                    Iterator<int[]> offIt = templates[i].offsetList.iterator();
                    Iterator<String> delIt = templates[i].delimiterList.iterator();
                    delIt.next();
                    while (offIt.hasNext())
                    {
                        int[] off = offIt.next();
                        int row = off[0] + position;
                        int col = off[1];
                        if (row < 0)
                        {
                            sb.append(FeatureIndex.BOS[-(row + 1)]);
                        }
                        else if (row >= n)
                        {
                            sb.append(FeatureIndex.EOS[row - n]);
                        }
                        else
                        {
                            sb.append(columns[col][row]);
                        }
                        if (delIt.hasNext())
                        {
                            sb.append(delIt.next());
                        }
                        else
                        {
                            sb.append(i);
                        }
                    }
                    addFeatureThenClear(sb, vec, featureMap);
                }
                return toFeatureArray(vec);
            }
        };
    }

    private void tagsToWords(FeatureColumnBuilder.Result result, int[] tagArray, List<String> output)
    {
        String rawText = result.raw;
        StringBuilder current = new StringBuilder();
        int tokenRawStart = -1;
        int tokenRawEnd = -1;
        for (int i = 0; i < tagArray.length; i++)
        {
            char ch = result.normalized.charAt(i);
            if (ch == ' ')
            {
                flushToken(rawText, current, tokenRawStart, tokenRawEnd, output);
                current.setLength(0);
                tokenRawStart = -1;
                tokenRawEnd = -1;
                continue;
            }
            if ((tagArray[i] == cwsTagSet.B || tagArray[i] == cwsTagSet.S) && current.length() > 0)
            {
                flushToken(rawText, current, tokenRawStart, tokenRawEnd, output);
                current.setLength(0);
                tokenRawStart = -1;
                tokenRawEnd = -1;
            }
            if (tokenRawStart < 0)
            {
                tokenRawStart = result.rawStart[i];
            }
            tokenRawEnd = Math.max(tokenRawEnd, result.rawEnd[i]);
            current.append(ch);
            if (tagArray[i] == cwsTagSet.S)
            {
                flushToken(rawText, current, tokenRawStart, tokenRawEnd, output);
                current.setLength(0);
                tokenRawStart = -1;
                tokenRawEnd = -1;
            }
        }
        flushToken(rawText, current, tokenRawStart, tokenRawEnd, output);
    }

    private void flushToken(String rawText, StringBuilder current, int rawStart, int rawEnd, List<String> output)
    {
        if (current.length() == 0)
        {
            return;
        }
        if (rawStart >= 0 && rawEnd > rawStart && rawEnd <= rawText.length())
        {
            String raw = rawText.substring(rawStart, rawEnd).trim();
            if (raw.length() > 0)
            {
                output.add(raw);
                return;
            }
        }
        output.add(current.toString());
    }

    private void tagsToWords(String sentence, int[] tagArray, List<String> output)
    {
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < tagArray.length; i++)
        {
            char ch = sentence.charAt(i);
            if (ch == ' ')
            {
                flushToken(current, output);
                continue;
            }
            if ((tagArray[i] == cwsTagSet.B || tagArray[i] == cwsTagSet.S) && current.length() > 0)
            {
                flushToken(current, output);
            }
            current.append(ch);
            if (tagArray[i] == cwsTagSet.S)
            {
                flushToken(current, output);
            }
        }
        flushToken(current, output);
    }

    private void flushToken(StringBuilder current, List<String> output)
    {
        if (current.length() == 0)
        {
            return;
        }
        output.add(current.toString());
        current.setLength(0);
    }

    private String[] toTags(int[] tagArray)
    {
        String[] tags = new String[tagArray.length];
        for (int i = 0; i < tagArray.length; i++)
        {
            if (tagArray[i] == cwsTagSet.B)
            {
                tags[i] = "B";
            }
            else if (tagArray[i] == cwsTagSet.M)
            {
                tags[i] = "M";
            }
            else if (tagArray[i] == cwsTagSet.E)
            {
                tags[i] = "E";
            }
            else
            {
                tags[i] = "S";
            }
        }
        return tags;
    }

    private void validateColumns(String[][] columns)
    {
        if (columns == null || columns.length != NUM_COLUMNS)
        {
            throw new IllegalArgumentException("3C CWS 特征必须是 " + NUM_COLUMNS + " 列");
        }
        if (columns[0] == null)
        {
            throw new IllegalArgumentException("特征列不能为空");
        }
        int n = columns[0].length;
        for (int col = 1; col < columns.length; col++)
        {
            if (columns[col] == null || columns[col].length != n)
            {
                throw new IllegalArgumentException("所有特征列长度必须一致");
            }
        }
    }

    private String toSentence(String[][] columns)
    {
        StringBuilder sentence = new StringBuilder(columns[0].length);
        for (int i = 0; i < columns[0].length; i++)
        {
            String value = columns[0][i];
            if (FeatureColumnBuilder.SPACE_FEATURE.equals(value))
            {
                sentence.append(' ');
            }
            else if (value != null && value.length() > 0)
            {
                sentence.append(value.charAt(0));
            }
            else
            {
                sentence.append(' ');
            }
        }
        return sentence.toString();
    }
}