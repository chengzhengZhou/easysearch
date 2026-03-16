package com.ppwx.easysearch.qp.data;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.csv.CsvData;
import cn.hutool.core.text.csv.CsvRow;
import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import com.aliyuncs.exceptions.ClientException;
import com.hankcs.hanlp.corpus.document.sentence.Sentence;
import com.hankcs.hanlp.corpus.document.sentence.word.CompoundWord;
import com.hankcs.hanlp.corpus.document.sentence.word.IWord;
import com.hankcs.hanlp.corpus.document.sentence.word.Word;
import com.hankcs.hanlp.utility.Predefine;
import com.ppwx.easysearch.qp.util.StreamUtil;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.hankcs.hanlp.utility.Predefine.logger;
import static com.ppwx.easysearch.qp.data.AlibabaNlpTest.request;

/**
 * AI生成语料
 */
public class DifyLLMTest {

    private static final String URL = "https://dev-ai.aihuishou.com/v1/";

    /**
     * 1、按品类品牌机型聚合采样
     */
    @Test
    public void testSampleProducts() {
        int max = 1;
        // key=品类+品牌+机型  value=机型+属性+成色
        Map<String, Map<String, Map<String, Set<String>>>> productMap = new HashMap<>(2048);
        CsvData read = CsvUtil.getReader().read(FileUtil.file("2026-01-23-10-59-40_EXPORT_CSV_23705421_090_0.csv"));
        int rowCount = read.getRowCount();
        for (int i = 1; i < rowCount; i++) {
            // 随机采样
            if (RandomUtil.randomInt(10) > 7) {
                CsvRow row = read.getRow(i);
                String categoryName = row.get(5);
                String brandName = row.get(7);
                String productName = row.get(9);
                String props = row.get(3).replaceAll("[\"\\[\\]]", "");
                String quality = row.get(11);
                productMap.computeIfAbsent(categoryName, k -> new HashMap<>());
                productMap.get(categoryName).computeIfAbsent(brandName, k -> new HashMap<>());
                productMap.get(categoryName).get(brandName).computeIfAbsent(productName, k -> new HashSet<>());
                Set<String> products = productMap.get(categoryName).get(brandName).get(productName);
                String multi = productName + "," + props;
                // 数量限制
                if (products.size() >= max || products.contains(multi)) {
                    continue;
                }
                products.add(multi + "," + quality);
            }
        }
        // 记录采样的数据
        AtomicInteger count = new AtomicInteger();
        productMap.forEach((categoryName, maps) -> {
            List<String> products = maps.values().stream()
                    .flatMap(map -> map.values().stream()).flatMap(Set::stream)
                    .collect(Collectors.toList());
            count.addAndGet(products.size());
            FileUtil.writeLines(products, FileUtil.file("export/" + categoryName.replace("/", "") + "-product.txt"), StandardCharsets.UTF_8, false);
        });
        System.out.println("count:" + count);
    }

    /**
     * 2、调用Dify生成数据
     */
    @Test
    public void testDifyGenerate() throws IOException {
        File outFile = FileUtil.file("export/CPU-dify.txt");
        try(InputStream vocabStream = StreamUtil.getResourceStream("products/CPU-product.txt")) {
            StreamUtil.readUtf8Lines(vocabStream, line -> {
                System.out.println(line);
                try {
                    String result = completeRequest(line).replaceAll("\"", "");
                    FileUtil.writeLines(Collections.singletonList(result), outFile, StandardCharsets.UTF_8, true);
                } catch (Exception e) {
                    System.out.println("error:" + line);
                }
            });
        }
    }

    /**
     * 3. 调用阿里NLP标注
     */
    @Test
    public void testAliAnno() throws IOException {
        File outFile = FileUtil.file("export/CPU-ali.txt");
        try(InputStream vocabStream = StreamUtil.getResourceStream("CPU-dify.txt")) {
            StreamUtil.readUtf8Lines(vocabStream, line -> {
                System.out.println(line);
                try {
                    String result = request(line);
                    FileUtil.writeLines(Collections.singletonList(result), outFile, StandardCharsets.UTF_8, true);
                } catch (ClientException e) {
                    System.out.println("error:" + line);
                }
            });
        }
    }

    /**
     * 4、将标注结果转为PKU格式
     */
    @Test
    public void testConvertPKU() throws IOException {
        AlibabaNlpParser.convertToPKU("中频查询 (10%-50%)-ali.txt", "middle-pku.txt");
    }

    /**
     * 5、Dify NER
     */
    @Test
    public void testDifyNer() throws IOException {
        File outFile = FileUtil.file("export/low-ner.txt");
        try(InputStream vocabStream = StreamUtil.getResourceStream("low-pku.txt")) {
            StreamUtil.readUtf8Lines(vocabStream, line -> {
                System.out.println(line);
                try {
                    String result = chatRequest(line);
                    FileUtil.writeLines(Collections.singletonList(result), outFile, StandardCharsets.UTF_8, true);
                } catch (Exception e) {
                    System.out.println("error:" + line);
                }
            });
        }
    }

    @Test
    public void test_3816() {
        ExecutorService threadPool = Executors.newCachedThreadPool();
        for (int i = 3816; i < 8000; i += 1000) {
            int finalI = i;
            threadPool.submit(() -> {
                try {
                    requestAIGenData(finalI);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("error:" + finalI);
                }
            });
        }
        LockSupport.park();
    }

    @Test
    public void test_dify_annotate() throws IOException {
        requestAnnotateData(400, 100);
    }

    /**
     * 对模型标注的结果审核
     */
    @Test
    public void testValidateNer() throws IOException {
        try(InputStream vocabStream = StreamUtil.getResourceStream("middle (10%-50%)-ner.txt")) {
            AtomicInteger rowIdx = new AtomicInteger(1);
            StreamUtil.readUtf8Lines(vocabStream, line -> {
                try {
                    Sentence sentence = create(line);
                    if (sentence == null) {
                        System.out.println("error:" + rowIdx.get());
                    }
                    //System.out.println(sentence.toStandoff());
                    //System.out.println(line);
                } catch (Exception e) {
                    System.out.println("error:" + rowIdx.get());
                    e.printStackTrace();
                }
                rowIdx.getAndIncrement();
            });
        }
    }

    @Test
    public void manualCheck() {
        String value = "\n";
        String value2 = "\n";
        String value3 = "\n";
        String value4 = "\n";
        Sentence sentence = create(value4);
        System.out.println(sentence.toStandoff());
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
        Pattern pattern = Pattern.compile("(\\[(([^\\t\\]]+/[0-9a-zA-Z]+)\\t+)+?([^\\t\\]]+/[0-9a-zA-Z]+)]/?[0-9a-zA-Z]+)|([^\\t]+/[0-9a-zA-Z]+)");
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
            for (String w : param.split("\\t+"))
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
                List<Word> wordList = new LinkedList();

                for(String single : wordParam.split("\\t+")) {
                    if (single.length() != 0) {
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

    //@Test
    public void requestAIGenData(int beginRow) throws IOException {
        //final int beginRow = 3816; // 含 4816、5816、6816、7406
        final int endRow = beginRow + 1000;  // 不含
        AtomicInteger index = new AtomicInteger(0);
        File outFile = FileUtil.file("export/dify-sample-product-llm-" + beginRow + ".txt");
        try(InputStream vocabStream = StreamUtil.getResourceStream("dify-sample-product.txt")) {
            StreamUtil.readUtf8Lines(vocabStream, line -> {
                if (index.incrementAndGet() < beginRow || index.get() >= endRow) {
                    return;
                }
                // 消除空格，充分使用大模型对机型的理解补充多样性
                line = line.replaceAll(" ", ",");
                String data = completeRequest(line);
                FileUtil.writeLines(Collections.singletonList(data), outFile, StandardCharsets.UTF_8, true);
            });
        }
    }

    public void requestAnnotateData(int beginRow, int offset) throws IOException {
        final int endRow = beginRow + offset;  // 不含
        AtomicInteger index = new AtomicInteger(0);
        File outFile = FileUtil.file("export/dify-sample-product-ner-" + beginRow + ".txt");
        try(InputStream vocabStream = StreamUtil.getResourceStream("dify-sample-product-ner.txt")) {
            StreamUtil.readUtf8Lines(vocabStream, line -> {
                if (index.incrementAndGet() < beginRow || index.get() >= endRow) {
                    return;
                }
                String data = chatRequest(line);
                FileUtil.writeLines(Collections.singletonList(data), outFile, StandardCharsets.UTF_8, true);
            });
        }
    }

    private String completeRequest(String text) {
        String accessKeyId = System.getenv("DIFY_AK_ENV");
        String json = "{" +
                "  \"inputs\": {\"query\": \"" + text + "\"},\n" +
                "  \"response_mode\": \"blocking\",\n" +
                "  \"user\": \"mickey.zhou\"\n" +
                "}";
        HttpRequest post = HttpUtil.createPost(URL + "completion-messages");
        post.header("Authorization", "Bearer " + accessKeyId);
        post.header("Accept-Encoding", "gzip, deflate, br");
        HttpRequest response = post.body(JSONObject.parseObject(json).toJSONString(), "application/json");
        String body = response.execute().body();
        JSONObject parse = (JSONObject) JSONObject.parse(body);
        String answer = parse.getString("answer");
        return answer;
    }

    private String chatRequest(String text) {
        String accessKeyId = "app-sFdokdK2p2pVP9fl9t9hI4m5";
        String json = "{" +
                "  \"query\": \"" + text + "\"," +
                "  \"inputs\":{}," +
                "  \"response_mode\": \"blocking\"," +
                "  \"user\": \"mickey.zhou\"" +
                "}";
        HttpRequest post = HttpUtil.createPost(URL + "chat-messages");
        post.header("Authorization", "Bearer " + accessKeyId);
        post.header("Accept-Encoding", "gzip, deflate, br");
        HttpRequest response = post.body(JSONObject.parseObject(json).toJSONString(), "application/json");
        String body = response.execute().body();
        JSONObject parse = (JSONObject) JSONObject.parse(body);
        String answer = parse.getString("answer");
        return answer;
    }
}
