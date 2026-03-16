package com.ppwx.easysearch.qp.data;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.csv.CsvData;
import cn.hutool.core.text.csv.CsvRow;
import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Joiner;
import com.hankcs.hanlp.corpus.document.sentence.Sentence;
import com.hankcs.hanlp.corpus.document.sentence.word.CompoundWord;
import com.hankcs.hanlp.corpus.document.sentence.word.IWord;
import com.ppwx.easysearch.qp.util.StreamUtil;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 训练数据生成
 * 基于标注词按规则生成语料
 *   1、型号 + 属性标签
 *   2、去品牌 + 型号 + 属性标签
 *   3、型号
 *   4、去品牌 + 型号
 *   5、空格随机
 *   6、型号随机丢弃1-n个
 * AI生成一句话
 */
public class TrainingDataGeneratorTest {

    /**
     * 采用前面机型清洗的数据，搭配核心属性组成完整数据
     * @throws IOException
     */
    @Test
    public void testHoleProducts() {
        Set<String> lines = new HashSet<>();
        CsvData read = CsvUtil.getReader().read(FileUtil.file("2026-01-23-10-59-40_EXPORT_CSV_23705421_090_0.csv"));
        int rowCount = read.getRowCount();
        for (int i = 1; i < rowCount; i++) {
            CsvRow row = read.getRow(i);
            lines.add((row.get(9) + "||" + trim(row.get(3))).toLowerCase());
        }
        FileUtil.writeLines(lines, FileUtil.file("export/multi-propertis.txt"), StandardCharsets.UTF_8, false);
    }

    Map<String, JSONObject> modelMap;

    Map<String, JSONObject> propMap;

    Set<String> brands;

    Set<String> categorys;

    Map<String, String> mapCoreProperties;

    /**
     * 标注词，基于NLP后语料
     */
    @Test
    public void testStandardAnnotate() throws IOException {
        modelMap = MetaGetter.alibabaModelTags();
        propMap = MetaGetter.alibabaPropTags();
        brands = MetaGetter.getBrands();
        categorys = MetaGetter.getCategory();
        mapCoreProperties = MetaGetter.mapCoreProperties();
        File outFile = FileUtil.file("export/hole-voc.txt");
        try(InputStream vocabStream = StreamUtil.getResourceStream("multi-propertis.txt")) {
            StreamUtil.readUtf8Lines(vocabStream, line -> {
                String[] texts = line.split("\\|\\|");
                if (!modelMap.containsKey(texts[0])) {
                    System.out.println("输出据异常：" + texts[0]);
                }
                // 策略
                List<String> tags = new ArrayList<>();
                //tags.add(overall(texts));
                tags.add(overallNoSpace(texts));
                tags.add(overallNoBrand(texts));
                //tags.add(overallRandomSpace(texts));
                FileUtil.writeLines(tags, outFile, StandardCharsets.UTF_8, true);
            });
        }
    }

    /**
     * ALI词性标注转化
     */
    @Test
    public void testAIData() throws IOException {
        File outFile = FileUtil.file("export/dify-sample-product-ner.txt");
        try(InputStream vocabStream = StreamUtil.getResourceStream("dify-sample-product-ali.txt")) {
            StreamUtil.readUtf8Lines(vocabStream, line -> {
                List<String> list = extractTags(JSONObject.parseObject(line));
                FileUtil.writeLines(Collections.singletonList(join(list)), outFile, StandardCharsets.UTF_8, true);
            });
        }
    }

    @Test
    public void testScript() throws IOException, InterruptedException {
        List<String> files = new ArrayList<>();
        files.add("CPU-product.txt");
        files.add("台式机-product.txt");
        files.add("吸尘器-product.txt");
        files.add("平板电脑-product.txt");
        files.add("手持稳定器-product.txt");
        files.add("手机-product.txt");
        files.add("投影机-product.txt");
        files.add("拍立得-product.txt");
        files.add("摄像机-product.txt");
        files.add("数码相机-product.txt");
        files.add("无人机-product.txt");
        files.add("显卡-product.txt");
        files.add("智能手写笔-product.txt");
        files.add("智能手表-product.txt");
        files.add("游戏掌机-product.txt");
        files.add("相机套机-product.txt");
        files.add("相机机身-product.txt");
        files.add("笔记本-product.txt");
        files.add("耳机耳麦-product.txt");
        files.add("运动相机-product.txt");
        files.add("镜头-product.txt");
        files.add("音箱音响-product.txt");
        for (String i : files) {
            testMergeSingleNer("export/ner/" + i, "export/merged/ai.txt");
        }
    }

    /**
     * 合并单个实体
     */
    //@Test
    public void testMergeSingleNer(String origin, String target) throws IOException {
        File outFile = FileUtil.file(target);
        NerDataMerge.setSpaceDelimiter();
        try(InputStream vocabStream = StreamUtil.getResourceStream(origin)) {
            StreamUtil.readUtf8Lines(vocabStream, line -> {
                line = NerDataMerge.merge(line, true);
                FileUtil.writeLines(Collections.singletonList(line), outFile, StandardCharsets.UTF_8, true);
            });
        }
    }

    @Test
    public void merge() throws IOException {
        //testMergeSingleNer("export/ai-pku-train.txt", "export/ai-pku-train2.txt");
        //testMergeSingleNer("export/search-pku-train.txt", "export/search-pku-train2.txt");
        testMergeSingleNer("export/standard-pku.txt", "export/standard-pku2.txt");
    }


    /**
     * 生成词典
     */
    @Test
    public void testGenDic() throws IOException {
        String origin = "D:\\projects\\yanxuan\\文档\\搜推\\词库采集\\训练语料\\20260302\\test\\search-pku-test.txt";
        File outFile = FileUtil.file("export/search-pku-dic.txt");
        Set<String> words = new HashSet<>();
        NerDataMerge.setSpaceDelimiter();
        try(InputStream vocabStream = FileUtil.getInputStream(origin)) {
            StreamUtil.readUtf8Lines(vocabStream, line -> {
                words.addAll(NerDataMerge.words(line));
            });
        }
        FileUtil.writeLines(words, outFile, StandardCharsets.UTF_8, true);
    }

    @Test
    public void wordList() {
        String value = "这/DT 台/M [佳能/NN]/BRAND [IXUS/NR 115/NN HS/NN]/MODEL 虽然/CS 是/VC 二手/NN ，/PU 但/AD 保养/VV 得/DER 不错/VA ，/PU 有/VE [8/NN 成新/VV]/CONDITION 呢/SP ！/PU 小巧/JJ 便携/NN ，/PU 拍照/VV 效果/NN 清晰/VA ，/PU 日常/AD 记录/VV 生活/NN 完全/AD 够用/VV ~/PU\n";
        NerDataMerge.setSpaceDelimiter();
        System.out.println(NerDataMerge.words(value));
    }

    /**
     * 校验格式，避免出行空格
     * @throws IOException
     */
    @Test
    public void valid() throws IOException {
        try(InputStream vocabStream = StreamUtil.getResourceStream("export/hole-voc.txt")) {
            StreamUtil.readUtf8Lines(vocabStream, line -> {
                Sentence sentence = Sentence.create(line);
                List<IWord> wordList = sentence.wordList;
                for (IWord word : wordList) {
                    if (word instanceof CompoundWord) {
                        for (IWord innerWord : ((CompoundWord) word).innerList) {
                            if (StringUtils.isBlank(innerWord.getValue()) || innerWord.getValue().contains(" ")) {
                                System.out.println("输出据异常：" + line);
                            }
                            if (StringUtils.isBlank(innerWord.getLabel()) || innerWord.getLabel().contains(" ")) {
                                System.out.println("输出据异常：" + line);
                            }
                        }
                        if (StringUtils.isBlank(word.getLabel()) || word.getLabel().contains(" ")) {
                            System.out.println("输出据异常：" + line);
                        }
                    } else {
                        if (StringUtils.isBlank(word.getValue()) || word.getValue().contains(" ")) {
                            System.out.println("输出据异常：" + line);
                        }
                        if (StringUtils.isBlank(word.getLabel()) || word.getLabel().contains(" ")) {
                            System.out.println("输出据异常：" + line);
                        }
                    }
                }
            });
        }
    }

    /**
     * 按行数切割文档
     */
    @Test
    public void testSliceFile() throws IOException {
        int size = 100;
        AtomicInteger count = new AtomicInteger(0);
        try(InputStream vocabStream = StreamUtil.getResourceStream("高频查询 (Top 10%).txt")) {
            List<String> lines = new ArrayList<>();
            StreamUtil.readUtf8Lines(vocabStream, line -> {
                lines.add(line);
                count.incrementAndGet();
                if (lines.size() == size) {
                    FileUtil.writeLines(lines, FileUtil.file("export/slice/高频查询 (Top 10%)-" + count + ".txt"), StandardCharsets.UTF_8, false);
                    lines.clear();
                }
            });
        }
    }

    @Test
    public void test() throws IOException {
        String origin = "D:\\projects\\yanxuan\\文档\\搜推\\词库采集\\训练语料\\20260302\\test\\199801-pku-test.txt";
        File outFile = FileUtil.file("export/199801-pku-origin");
        NerDataMerge.setSpaceDelimiter();
        List<String> sentences = new ArrayList<>();
        try(InputStream vocabStream = FileUtil.getInputStream(origin)) {
            StreamUtil.readUtf8Lines(vocabStream, line -> sentences.add(String.join("", NerDataMerge.words(line))));
        }
        FileUtil.writeLines(sentences, outFile, StandardCharsets.UTF_8, true);
    }

    /**
     * 全量完整
     */
    private String overall(String[] texts) {
        // 型号
        JSONObject jsonObject = modelMap.get(texts[0]);
        JSONArray segments = jsonObject.getJSONArray("result");
        // 原始
        List<String> tags = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            JSONObject segment = segments.getJSONObject(i);
            String word = segment.getString("word");
            if (StringUtils.equals("[",word) || StringUtils.equals("]", word)) {
                continue;
            }
            // 首个品牌则提取
            if (brands.contains(word)) {
                tags.add(word + "/BRAND");
                if (i + 1 < segments.size()) {
                    segment = segments.getJSONObject(i + 1);
                    if (StringUtils.isBlank(segment.getString("word"))) {
                        tags.add(segment.getString("word") + "/" + segment.getString("pos"));
                        i++;
                    }
                }
                continue;
            }

            List<String> productTags = new ArrayList<>();
            while (i < segments.size()) {
                segment = segments.getJSONObject(i);
                productTags.add(segment.getString("word") + "/" + segment.getString("pos"));
                i++;
            }
            if (productTags.size() > 1) {
                tags.add("[" + Joiner.on("\t").join(productTags) + "]/MODEL");
            } else if (!productTags.isEmpty()){
                // 替换标签
                tags.add(productTags.get(0).substring(0, productTags.get(0).lastIndexOf("/")) + "/MODEL");
            }
        }
        // 合并
        List<String> lines = new ArrayList<>(tags);

        // 属性标签
        randomProp(lines, texts);

        return join(lines);
    }

    /**
     * 全量完整无空格
     */
    private String overallNoSpace(String[] texts) {
        // 型号
        JSONObject jsonObject = modelMap.get(texts[0]);
        JSONArray segments = jsonObject.getJSONArray("result");
        // 原始
        List<String> tags = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            JSONObject segment = segments.getJSONObject(i);
            String word = noSpace(segment.getString("word"));
            if (StringUtils.equals("[",word) || StringUtils.equals("]", word) || StringUtils.isBlank(word)) {
                continue;
            }
            // 首个品牌则提取
            if (i == 0 && brands.contains(word)) {
                tags.add(word + "/BRAND");
                continue;
            }

            List<String> productTags = new ArrayList<>();
            while (i < segments.size()) {
                segment = segments.getJSONObject(i);
                word = noSpace(segment.getString("word"));
                // 忽略空格
                if (StringUtils.isBlank(word)) {
                    i++;
                    continue;
                }
                productTags.add(word + "/" + segment.getString("pos"));
                i++;
            }
            if (productTags.size() > 1) {
                tags.add("[" + join(productTags) + "]/MODEL");
            } else if (!productTags.isEmpty()){
                // 替换标签
                tags.add(productTags.get(0).substring(0, productTags.get(0).lastIndexOf("/")) + "/MODEL");
            }
        }
        // 合并
        List<String> lines = new ArrayList<>(tags);

        // 属性标签
        randomProp(lines, texts);

        return join(lines);
    }

    /**
     * 全量无品牌
     */
    private String overallNoBrand(String[] texts) {
        // 型号
        JSONObject jsonObject = modelMap.get(texts[0]);
        JSONArray segments = jsonObject.getJSONArray("result");
        // 原始
        List<String> tags = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            JSONObject segment = segments.getJSONObject(i);
            String word = noSpace(segment.getString("word"));
            if (StringUtils.equals("[",word) || StringUtils.equals("]", word) || StringUtils.isBlank(word)) {
                continue;
            }
            // 首个品牌则提取
            if (i == 0 && brands.contains(word)) {
                //tags.add(word + "/BRAND");
                continue;
            }

            List<String> productTags = new ArrayList<>();
            while (i < segments.size()) {
                segment = segments.getJSONObject(i);
                word = noSpace(segment.getString("word"));
                // 忽略空格
                if (StringUtils.isBlank(word)) {
                    i++;
                    continue;
                }
                productTags.add(word + "/" + segment.getString("pos"));
                i++;
            }
            if (productTags.size() > 1) {
                tags.add("[" + join(productTags) + "]/MODEL");
            } else if (!productTags.isEmpty()){
                // 替换标签
                tags.add(productTags.get(0).substring(0, productTags.get(0).lastIndexOf("/")) + "/MODEL");
            }
        }
        // 合并
        List<String> lines = new ArrayList<>(tags);

        // 属性标签
        randomProp(lines, texts);

        return join(lines);
    }

    /**
     * 随机裁剪空格和头部品牌
     */
    private String overallRandomSpace(String[] texts) {
        // 型号
        JSONObject jsonObject = modelMap.get(texts[0]);
        JSONArray segments = jsonObject.getJSONArray("result");
        // 原始
        List<String> tags = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            JSONObject segment = segments.getJSONObject(i);
            String word = segment.getString("word");
            if (StringUtils.equals("[",word) || StringUtils.equals("]", word)) {
                continue;
            }

            // 随机品牌则提取
            if (brands.contains(word)) {
                if (RandomUtil.randomBoolean()) {
                    tags.add(word + "/BRAND");
                    if (i + 1 < segments.size()) {
                        segment = segments.getJSONObject(i + 1);
                        if (StringUtils.isBlank(segment.getString("word"))) {
                            tags.add(segment.getString("word") + "/" + segment.getString("pos"));
                            i++;
                        }
                    }
                } else {
                    // 跳过，并带走后面空格
                    if (i + 1 < segments.size()) {
                        segment = segments.getJSONObject(i + 1);
                        if (StringUtils.isBlank(segment.getString("word"))) {
                            i++;
                        }
                    }
                }
                continue;
            }

            List<String> productTags = new ArrayList<>();
            while (i < segments.size()) {
                segment = segments.getJSONObject(i);
                // 随机空格
                if (StringUtils.isBlank(segment.getString("word")) && RandomUtil.randomBoolean()) {
                    i++;
                    continue;
                }
                productTags.add(segment.getString("word") + "/" + segment.getString("pos"));
                i++;
            }
            if (productTags.size() > 1) {
                tags.add("[" + join(productTags) + "]/MODEL");
            } else if (!productTags.isEmpty()){
                // 替换标签
                tags.add(productTags.get(0).substring(0, productTags.get(0).lastIndexOf("/")) + "/MODEL");
            }
        }
        // 合并
        List<String> lines = new ArrayList<>(tags);

        // 属性标签
        randomProp(lines, texts);

        return join(lines);
    }

    private void randomProp(List<String> lines, String[] texts) {
        if (texts.length <= 1) {
            return;
        }
        if (StringUtils.isBlank(texts[1])) {
            System.out.println("空属性数据：" + texts[0]);
            return;
        }
        List<String> props = new ArrayList<>();
        for (int i = 1; i < texts.length; i++) {
            String name = mapCoreProperties.get(texts[i]);
            if (name == null) {
                JSONObject jsonObject = propMap.get(texts[i]);
                if (jsonObject == null) {
                    //System.out.println("属性数据异常：" + texts[i]);
                    // 丢弃掉
                    continue;
                }
                props.add(join(extractTags(jsonObject)));
                continue;
            }

            if (name.equals("颜色")) {
                props.add(noSpace(texts[i]) + "/COLOR");
            } else if (name.equals("购买渠道")) {
                props.add(noSpace(texts[i]) + "/VERSION");
            } else if (name.equals("内存")) {
                List<String> list = extractTags(propMap.get(noSpace(texts[i])));
                if (!list.isEmpty()) {
                    props.add("[" + join(list) + "]/RAM");
                } else {
                    props.add(texts[i] + "/RAM");
                }
            } else {
                props.add(join(extractTags(propMap.get(texts[i]))));
            }
        }

        Collections.shuffle(props);
        lines.addAll(props);
    }

    private List<String> extractTags(JSONObject jsonObject) {
        List<String> list = new ArrayList<>();
        JSONArray segments = jsonObject.getJSONArray("result");
        for (int i = 0; i < segments.size(); i++) {
            JSONObject segment = segments.getJSONObject(i);
            list.add(noSpace(segment.getString("word")).toLowerCase() + "/" + segment.getString("pos"));
        }
        return list;
    }

    private String join(Collection lines) {
        return Joiner.on(" ").join(lines);
    }

    private String join(Collection lines, String separator) {
        return Joiner.on(separator).join(lines);
    }

    private String trim(String txt) {
        return txt.substring(1, txt.length() - 1).replaceAll("\"", "").replaceAll(",", "||");
    }

    private String noSpace(String text) {
        return text.replaceAll("\\s+", "");
    }
}
