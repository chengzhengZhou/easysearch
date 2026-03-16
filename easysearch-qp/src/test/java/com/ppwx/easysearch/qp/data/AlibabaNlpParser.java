package com.ppwx.easysearch.qp.data;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyuncs.exceptions.ClientException;
import com.hankcs.hanlp.corpus.document.sentence.Sentence;
import com.hankcs.hanlp.corpus.document.sentence.word.CompoundWord;
import com.hankcs.hanlp.corpus.document.sentence.word.IWord;
import com.hankcs.hanlp.corpus.document.sentence.word.Word;
import com.ppwx.easysearch.qp.util.StreamUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 阿里NLP
 */
public final class AlibabaNlpParser {

    /**
     * 词性标注结果解析
     * @param words 响应的result部分
     * @return 词性标注结果
     */
    public static Sentence parsePosResult(JSONArray words) {
        List<IWord> wordList = new ArrayList<>();
        for (int i = 0; i < words.size(); i++) {
            JSONObject word = words.getJSONObject(i);
            if (StringUtils.isBlank(word.getString("word"))) {
                continue;
            }
            IWord iWord = new Word(word.getString("word"), word.getString("pos"));
            wordList.add(iWord);
        }
        return new Sentence(wordList);
    }

    /**
     * 将阿里NLP数据转换为PKU格式
     * @param nlpPath 阿里NLP数据文件路径
     * @param targetPath 转换后文件路径
     */
    public static void convertToPKU(String nlpPath, String targetPath) throws IOException {
        File outFile = FileUtil.file("export/" + targetPath);
        try(InputStream vocabStream = StreamUtil.getResourceStream(nlpPath)) {
            StreamUtil.readUtf8Lines(vocabStream, line -> {
                Sentence sentence = parsePosResult(parseResult(line));
                List<IWord> wordList = sentence.wordList;
                List<String> words = new ArrayList<>();
                for (IWord iWord : wordList) {
                    if (iWord instanceof CompoundWord) {
                        CompoundWord compoundWord = (CompoundWord) iWord;
                        compoundWord.innerList.forEach(word -> words.add(iWord.getValue() + "/" + iWord.getLabel()));
                    } else {
                        words.add(iWord.getValue() + "/" + iWord.getLabel());
                    }
                }
                FileUtil.appendUtf8Lines(Collections.singletonList(Joiner.join(" ", words)), outFile);
            });
        }
    }

    /**
     * 解析阿里NLP结果
     * @param result 阿里NLP结果
     * @return 阿里NLP结果
     */
    static JSONArray parseResult(String result) {
        JSONObject root = (JSONObject) JSONObject.parse(result);
        return root.getJSONArray("result");
    }

    public static void main(String[] args) throws IOException, ClientException {
        convertToPKU("dify-sample-product-ali.txt", "dify-sample-product-ali.txt");
    }
}
