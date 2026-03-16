package com.ppwx.easysearch.qp.data;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.csv.CsvData;
import cn.hutool.core.text.csv.CsvRow;
import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.alinlp.model.v20200629.GetPosChEcomRequest;
import com.aliyuncs.alinlp.model.v20200629.GetPosChEcomResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import com.ppwx.easysearch.qp.util.StreamUtil;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class AlibabaNlpTest {

    /**
     * 阿里NLP词性标注
     * @throws IOException
     */
    @Test
    public void annotateWords() throws IOException {
        File outFile = FileUtil.file("export/CPU-ali.txt");
        try(InputStream vocabStream = StreamUtil.getResourceStream("products/CPU-product.txt")) {
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

    static String request(String text) throws ClientException {
        String accessKeyId = System.getenv("NLP_AK_ENV");
        String accessKeySecret = System.getenv("NLP_SK_ENV");
        DefaultProfile defaultProfile = DefaultProfile.getProfile(
                "cn-hangzhou",
                accessKeyId,
                accessKeySecret);
        IAcsClient client = new DefaultAcsClient(defaultProfile);
        //构造请求参数，其中GetPosChEcom是算法的actionName, 请查找对应的《API基础信息参考》文档并替换为您需要的算法的ActionName，示例详见下方
        GetPosChEcomRequest request = new GetPosChEcomRequest();
        //固定值，无需更改
        request.setSysEndpoint("alinlp.cn-hangzhou.aliyuncs.com");
        //固定值，无需更改
        request.setServiceCode("alinlp");
        //请求参数, 具体请参考《API基础信息文档》进行替换与填写
        request.setText(text);
        request.setTokenizerId("MAINSE");
        //获取请求结果，注意这里的GetPosChEcom也需要替换
        GetPosChEcomResponse response = client.getAcsResponse(request);
        return response.getData();
    }

/*
    @Test
    public void annotateProperties() throws IOException {
        List<String> lines = new LinkedList<>();
        MetaGetter.mapCoreProperties().keySet().forEach(pro -> {
            try {
                String result = request(pro);
                lines.add(pro + ";" + result);
            } catch (ClientException e) {
                System.out.println("error:" + pro);
            }
        });

        File outFile = FileUtil.file("export/property-tags.txt");
        FileUtil.writeLines(lines, outFile, StandardCharsets.UTF_8, true);
    }
*/


    /**
     * 统计词性标注的信息
     * @throws IOException
     */
    @Test
    public void testCount() throws IOException {
        Map<String, Integer> wordCount = new HashMap<>();
        Map<String, Integer> posCount = new HashMap<>();
        try(InputStream vocabStream = StreamUtil.getResourceStream("huawei-tags.txt")) {
            StreamUtil.readUtf8Lines(vocabStream, line -> {
                String json = line.substring(line.indexOf(";") + 1);
                JSONObject parse = (JSONObject) JSONObject.parse(json);
                JSONArray segments = parse.getJSONArray("result");
                for (int i = 0; i < segments.size(); i++) {
                    JSONObject segment = segments.getJSONObject(i);
                    String word = segment.getString("word");
                    String pos = segment.getString("pos");
                    wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
                    posCount.put(pos, posCount.getOrDefault(pos, 0) + 1);
                }
            });
        }
        System.out.println("词性统计：" + posCount.size());
        for (Map.Entry<String, Integer> entry : posCount.entrySet()) {
            System.out.println(entry.getKey() + ":" + entry.getValue());
        }
        System.out.println("词统计：" + wordCount.size());
        for (Map.Entry<String, Integer> entry : wordCount.entrySet()) {
            System.out.println(entry.getKey() + ":" + entry.getValue());
        }
    }

    @Test
    public void toVoc() throws IOException {
        List<String> lines = new LinkedList<>();
        try(InputStream vocabStream = StreamUtil.getResourceStream("huawei-tags.txt")) {
            StreamUtil.readUtf8Lines(vocabStream, line -> {
                System.out.println(line);
                String json = line.substring(line.indexOf(";") + 1);
                JSONObject parse = (JSONObject) JSONObject.parse(json);
                JSONArray segments = parse.getJSONArray("result");

                StringBuilder bd = new StringBuilder();
                for (int i = 0; i < segments.size(); i++) {
                    JSONObject segment = segments.getJSONObject(i);
                    bd.append(segment.getString("word")).append("/").append(segment.getString("pos"));
                    if (i != segments.size() - 1) {
                        bd.append("\t");
                    }
                }
                lines.add(bd.toString());
            });
        }
        File outFile = FileUtil.file("export/huawei-voc.txt");
        FileUtil.writeLines(lines, outFile, StandardCharsets.UTF_8, false);
    }

    public Set<String> getBrands() throws IOException {
        Set<String> brands = new HashSet<>();
        ArffSourceConvertor convertor = new ArffSourceConvertor("meta/brand_meta.arff");
        convertor.readData();
        convertor.getInstances().forEach(instance -> {
            String brand = (String) instance.getValueByAttrName("brand_name_ch");
            String brandEn = (String) instance.getValueByAttrName("brand_name_en");
            brands.add(brand);
            brands.add(brandEn);
        });
        return brands;
    }

    public Set<String> getCategory() throws IOException {
        Set<String> categorys = new HashSet<>();
        ArffSourceConvertor convertor = new ArffSourceConvertor("meta/category_meta.arff");
        convertor.readData();
        convertor.getInstances().forEach(instance -> {
            String categoryName = (String) instance.getValueByAttrName("category_name");
            categorys.add(categoryName);
        });
        return categorys;
    }

    @Test
    public void toNer() throws IOException {
        Set<String> brands = getBrands();
        Set<String> categorys = getCategory();
        List<String> lines = new LinkedList<>();
        AtomicInteger count = new AtomicInteger(1000);
        try(InputStream vocabStream = StreamUtil.getResourceStream("alibaba-tags.txt")) {
            StreamUtil.readUtf8Lines(vocabStream, line -> {
                if (count.get() <= 0 || RandomUtil.randomBoolean()) {
                    return;
                }
                System.out.println(line);
                String json = line.substring(line.indexOf(";") + 1);
                JSONObject parse = (JSONObject) JSONObject.parse(json);
                JSONArray segments = parse.getJSONArray("result");

                StringBuilder bd = new StringBuilder();
                for (int i = 0; i < segments.size(); i++) {
                    JSONObject segment = segments.getJSONObject(i);
                    if (StringUtils.equals("[", segment.getString("word")) || StringUtils.equals("]", segment.getString("word"))) {
                        continue;
                    }
                    if (brands.contains(segment.getString("word"))) {
                        bd.append(segment.getString("word")).append("/BRAND");
                    } else if (categorys.contains(segment.getString("word"))) {
                        bd.append(segment.getString("word")).append("/CATEGORY");
                    } else {
                        bd.append(segment.getString("word")).append("/").append(segment.getString("pos"));
                    }
                    if (i != segments.size() - 1) {
                        bd.append("\t");
                    }
                }
                lines.add(bd.toString());
                count.addAndGet(-1);
            });
        }
        File outFile = FileUtil.file("export/yanxuan-ner.txt");
        FileUtil.writeLines(lines, outFile, StandardCharsets.UTF_8, false);
    }

    @Test
    public void toNer2() throws IOException {
        Set<String> brands = getBrands();
        Set<String> categorys = getCategory();
        List<String> lines = new LinkedList<>();
        try(InputStream vocabStream = StreamUtil.getResourceStream("alibaba-tags.txt")) {
            StreamUtil.readUtf8Lines(vocabStream, line -> {
                System.out.println(line);
                String json = line.substring(line.indexOf(";") + 1);
                JSONObject parse = (JSONObject) JSONObject.parse(json);
                JSONArray segments = parse.getJSONArray("result");
                // 原始
                StringBuilder bd = new StringBuilder();
                for (int i = 0; i < segments.size(); i++) {
                    JSONObject segment = segments.getJSONObject(i);
                    if (StringUtils.equals("[", segment.getString("word")) || StringUtils.equals("]", segment.getString("word"))) {
                        continue;
                    }
                    if (brands.contains(segment.getString("word"))) {
                        bd.append(segment.getString("word")).append("/BRAND");
                    } else if (categorys.contains(segment.getString("word"))) {
                        bd.append(segment.getString("word")).append("/CATEGORY");
                    } else {
                        bd.append(segment.getString("word")).append("/").append(segment.getString("pos"));
                    }
                    if (i != segments.size() - 1) {
                        bd.append("\t");
                    }
                }
                lines.add(bd.toString());
                // 去空格
                bd = new StringBuilder();
                for (int i = 0; i < segments.size(); i++) {
                    JSONObject segment = segments.getJSONObject(i);
                    if (StringUtils.equals("[", segment.getString("word")) ||
                            StringUtils.equals("]", segment.getString("word")) ||
                            StringUtils.isBlank(segment.getString("word"))) {
                        continue;
                    }
                    if (brands.contains(segment.getString("word"))) {
                        bd.append(segment.getString("word")).append("/BRAND");
                    } else if (categorys.contains(segment.getString("word"))) {
                        bd.append(segment.getString("word")).append("/CATEGORY");
                    } else {
                        bd.append(segment.getString("word")).append("/").append(segment.getString("pos"));
                    }
                    if (i != segments.size() - 1) {
                        bd.append("\t");
                    }
                }
                lines.add(bd.toString());
                // 随机少空格
                bd = new StringBuilder();
                for (int i = 0; i < segments.size(); i++) {
                    JSONObject segment = segments.getJSONObject(i);
                    if (StringUtils.equals("[", segment.getString("word")) ||
                            StringUtils.equals("]", segment.getString("word"))) {
                        continue;
                    }
                    if (StringUtils.isBlank(segment.getString("word")) && RandomUtil.randomBoolean()) {
                        continue;
                    }

                    if (brands.contains(segment.getString("word"))) {
                        bd.append(segment.getString("word")).append("/BRAND");
                    } else if (categorys.contains(segment.getString("word"))) {
                        bd.append(segment.getString("word")).append("/CATEGORY");
                    } else {
                        bd.append(segment.getString("word")).append("/").append(segment.getString("pos"));
                    }
                    if (i != segments.size() - 1) {
                        bd.append("\t");
                    }
                }
                lines.add(bd.toString());
            });
        }
        File outFile = FileUtil.file("export/yanxuan-ner.txt");
        FileUtil.writeLines(lines, outFile, StandardCharsets.UTF_8, true);
    }

    /**
     * 输出核心属性
     */
    @Test
    public void listCoreProperties() {
        String dictionary = "D:\\projects\\yanxuan\\文档\\搜推\\词库采集\\品类属性";
        File[] files = FileUtil.ls(dictionary);
        Map<String, List<String>> coreProperties = new HashMap<>();
        for (File file : files) {
            String fileName = file.getName().substring(3, file.getName().length() - 4);
            coreProperties.put(fileName, new ArrayList<>());
            String content = FileUtil.readUtf8String(file);
            JSONObject root = (JSONObject) JSONObject.parse(content);
            JSONArray data = root.getJSONArray("data");
            for (int i = 0; i < data.size(); i++) {
                JSONObject item = data.getJSONObject(i);
                Integer classify = item.getInteger("classify");
                if (classify == 2) {
                    // 核心属性
                    JSONArray attributes = item.getJSONArray("attributes");
                    for (int j = 0; j < attributes.size(); j++) {
                        JSONObject attribute = attributes.getJSONObject(j);
                        String name = attribute.getString("attrName");
                        coreProperties.get(fileName).add(name);
                    }
                }
            }
        }
        // 全部输出
        /*for (Map.Entry<String, List<String>> entry : coreProperties.entrySet()) {
            System.out.println(entry.getKey() + "=" + entry.getValue());
        }*/
        // 反向匹配
        Map<String, Set<String>> valueMap = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : coreProperties.entrySet()) {
            for (String coreProperty : entry.getValue()) {
                if (valueMap.containsKey(coreProperty)) {
                    valueMap.get(coreProperty).add(entry.getKey());
                } else {
                    valueMap.put(coreProperty, new HashSet<>(Collections.singletonList(entry.getKey())));
                }
            }
        }
        for (Map.Entry<String, Set<String>> entry : valueMap.entrySet()) {
            System.out.println(entry.getKey() + "=" + entry.getValue());
        }
        // 全部属性
        /*Set<String> corePropertySet = new HashSet<>();
        coreProperties.values().forEach(corePropertySet::addAll);
        corePropertySet.forEach(System.out::println);*/
    }

    @Test
    public void testHuaweiMobile() throws IOException {
        // 对华为手机进行切割 category_id=655，brand=8557
        List<String> lines = new ArrayList<>();
        CsvData read = CsvUtil.getReader().read(FileUtil.file("2026-01-23-10-59-40_EXPORT_CSV_23705421_090_0.csv"));
        int rowCount = read.getRowCount();
        for (int i = 1; i < rowCount; i++) {
            CsvRow row = read.getRow(i);
            String categoryId = row.get(4);
            String brandId = row.get(6);
            if (StringUtils.equals(categoryId, "655") && StringUtils.equals(brandId, "8557")) {
                int i1 = RandomUtil.randomInt(100);
                if (i1 > 0 && i1 < 10) {
                    lines.add(row.get(9) + " " + trim(row.get(3)) + " " + row.get(11));
                }
            }
        }
        FileUtil.writeLines(lines, FileUtil.file("export/huawei-mobile.txt"), StandardCharsets.UTF_8, false);
    }

    private Map<String, JSONObject> mapTags() throws IOException {
        Map<String, JSONObject> map = new HashMap<>();
        try(InputStream vocabStream = StreamUtil.getResourceStream("alibaba-tags.txt")) {
            StreamUtil.readUtf8Lines(vocabStream, line -> {
                System.out.println(line);
                String json = line.substring(line.lastIndexOf(";") + 1);
                JSONObject parse = (JSONObject) JSONObject.parse(json);
                map.put(line.substring(0, line.lastIndexOf(";")), parse);
            });
        }
        return map;
    }

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
            lines.add((row.get(9).trim() + "||" + trim(row.get(3))).toLowerCase());
        }
        FileUtil.writeLines(lines, FileUtil.file("export/multi-propertis.txt"), StandardCharsets.UTF_8, false);
    }

    private String trim(String txt) {
        return txt.substring(1, txt.length() - 1).replaceAll("\"", "").replaceAll(",", "||");
    }
}
