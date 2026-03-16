package com.ppwx.easysearch.qp.data;

import com.hankcs.hanlp.corpus.document.sentence.Sentence;
import com.hankcs.hanlp.corpus.document.sentence.word.CompoundWord;
import com.hankcs.hanlp.corpus.document.sentence.word.IWord;
import com.hankcs.hanlp.corpus.document.sentence.word.Word;
import com.hankcs.hanlp.utility.Predefine;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.hankcs.hanlp.utility.Predefine.logger;

public class NerDataMerge {

    private static Pattern pattern = Pattern.compile("(\\[(([^\\t\\]]+/[0-9a-zA-Z]+)\\t+)+?([^\\t\\]]+/[0-9a-zA-Z]+)]/?[0-9a-zA-Z]+)|([^\\t]+/[0-9a-zA-Z]+)");
    private static String delimiter = "\t";

    public static void setSpaceDelimiter () {
        delimiter = " ";
        pattern = Pattern.compile("(\\[(([^\\s\\]]+/[0-9a-zA-Z]+)\\s+)+?([^\\s\\]]+/[0-9a-zA-Z]+)]/?[0-9a-zA-Z]+)|([^\\s]+/[0-9a-zA-Z]+)");
    }

    /**
     * 合并单实体标签
     */
    public static String merge(String nerData, boolean lowercase) {
        List<String> list = new ArrayList<>();
        Sentence sentence = create(nerData);
        List<IWord> wordList = sentence.wordList;
        for (IWord iWord : wordList) {
            if (iWord instanceof CompoundWord) {
                CompoundWord compoundWord = (CompoundWord) iWord;
                String label = compoundWord.getLabel();
                if (compoundWord.innerList.size() == 1) {
                    list.add((lowercase ? compoundWord.getValue().toLowerCase() : compoundWord.getValue()) + "/" + label);
                } else {
                    String inner = compoundWord.innerList.stream()
                            .map(word -> (lowercase ? word.getValue().toLowerCase() : word.getValue()) + "/" + word.getLabel())
                            .collect(Collectors.joining(delimiter));
                    list.add("[" + inner + "]/" + label);
                }
            } else {
                list.add((lowercase ? iWord.getValue().toLowerCase() : iWord.getValue()) + '/' + iWord.getLabel());
            }
        }
        return String.join(delimiter, list);
    }

    public static List<String> words(String value) {
        List<String> list = new ArrayList<>();
        Sentence sentence = create(value);
        List<IWord> wordList = sentence.wordList;
        for (IWord iWord : wordList) {
            if (iWord instanceof CompoundWord) {
                CompoundWord compoundWord = (CompoundWord) iWord;
                compoundWord.innerList.forEach(word -> list.add(word.getValue()));
            } else {
                list.add(iWord.getValue());
            }
        }
        return list;
    }

    public static Sentence create(String param)
    {
        if (param == null)
        {
            return null;
        }
        param = param.trim();
        if (param.isEmpty())
        {
            return null;
        }
        Matcher matcher = pattern.matcher(param);
        List<IWord> wordList = new LinkedList<IWord>();
        while (matcher.find())
        {
            String single = matcher.group();
            IWord word = createNest(single);
            if (word == null)
            {
                logger.warning("在用 " + single + " 构造单词时失败，句子构造参数为 " + param);
                return null;
            }
            wordList.add(word);
        }
        if (wordList.isEmpty()) // 按照无词性来解析
        {
            for (String w : param.split(delimiter))
            {
                wordList.add(new Word(w, null));
            }
        }

        return new Sentence(wordList);
    }

    public static IWord createNest(String param) {
        if (param == null) {
            return null;
        } else {
            return (IWord)(param.startsWith("[") && !param.startsWith("[/") ? createCompoundWord(param) : Word.create(param));
        }
    }

    public static CompoundWord createCompoundWord(String param) {
        if (param == null) {
            return null;
        } else {
            int cutIndex = param.lastIndexOf(93);
            if (cutIndex > 2 && cutIndex != param.length() - 1) {
                String wordParam = param.substring(1, cutIndex);
                List<Word> wordList = new LinkedList<>();

                for(String single : wordParam.split(delimiter)) {
                    if (!single.isEmpty()) {
                        Word word = Word.create(single);
                        if (word == null) {
                            Predefine.logger.warning("使用参数" + single + "构造单词时发生错误");
                            return null;
                        }

                        wordList.add(word);
                    }
                }

                String labelParam = param.substring(cutIndex + 1);
                if (labelParam.startsWith("/")) {
                    labelParam = labelParam.substring(1);
                }

                return new CompoundWord(wordList, labelParam);
            } else {
                return null;
            }
        }
    }
}
