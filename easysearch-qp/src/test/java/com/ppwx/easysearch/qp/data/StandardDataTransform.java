package com.ppwx.easysearch.qp.data;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.text.csv.CsvUtil;
import com.alibaba.fastjson.JSON;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ppwx.easysearch.qp.ner.EntityType;
import com.ppwx.easysearch.qp.ner.recognizer.BaseDictEntityLine;
import com.ppwx.easysearch.qp.ner.recognizer.ModelDictEntityLine;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 三级分类数据处理脚本
 */
public class StandardDataTransform {

    /**
     * 获取新增的product、brand、category
     */
    @Test
    public void exportDiffItems() throws IOException {
        Set<Integer> productIds = Sets.newHashSetWithExpectedSize(10000);
        ArffSourceConvertor convertor = new ArffSourceConvertor("meta/product_meta.arff");
        convertor.readData();
        convertor.getInstances().forEach(arffInstance -> {
            productIds.add((Integer) arffInstance.getValueByIndex(0));
        });

        Set<Integer> brandIds = Sets.newHashSetWithExpectedSize(10000);
        convertor = new ArffSourceConvertor("meta/brand_meta.arff");
        convertor.readData();
        convertor.getInstances().forEach(arffInstance -> {
            brandIds.add((Integer) arffInstance.getValueByIndex(0));
        });

        Set<Integer> categoryIds = Sets.newHashSetWithExpectedSize(10000);
        convertor = new ArffSourceConvertor("meta/category_meta.arff");
        convertor.readData();
        convertor.getInstances().forEach(arffInstance -> {
            categoryIds.add((Integer) arffInstance.getValueByIndex(0));
        });

        List<String> products = Lists.newArrayList();
        Set<String> brands = Sets.newHashSet();
        Set<String> categorys = Sets.newHashSet();
        String file = "2026-02-28-18-18-32_product.csv";
        CsvUtil.getReader().read(FileUtil.file(file)).forEach(row -> {
            if (row.getOriginalLineNumber() > 0) {
                if (!productIds.contains(Integer.valueOf(row.get(1)))) {
                    // 型号id，名称，品牌id，品牌名称，分类id，分类名称
                    products.add(row.get(1) + "," + row.get(2).toLowerCase() + "," + row.get(5) + "," + row.get(3));
                }
                if (!brandIds.contains(Integer.valueOf(row.get(5)))) {
                    brands.add(row.get(5) + "," + row.get(6).toLowerCase());
                }
                if (!categoryIds.contains(Integer.valueOf(row.get(3)))) {
                    categorys.add(row.get(3) + "," + row.get(4).toLowerCase());
                }
            }
        });

        // 汇总
        System.out.println("diff product:" + products.size());
        System.out.println("diff brand:" + brands.size());
        System.out.println("diff category:" + categorys.size());
        //brands.forEach(System.out::println);
        //brands.forEach(System.out::println);
        FileUtil.writeLines(products, "export/diff_product.txt", "utf-8");
        //FileUtil.writeLines(brands, "export/diff_brand.txt", "utf-8");
    }

    /**
     * 生成品牌元数据
     */
    @Test
    public void generateBrandMeta() {
        String file = "export/diff_brand.txt";
        Set<String> wordList = Sets.newLinkedHashSet();
        FileUtil.readUtf8Lines(FileUtil.file(file), (LineHandler) line -> {
            line = trimRef(line);
            if (line.contains("（")) {
                String[] split = line.split(",");
                String word = split[1].replaceAll("（", ",").replaceAll("）", "");
                wordList.add(split[0] + "," + word.toLowerCase());
            } else {
                wordList.add(line.toLowerCase() + ",");
            }
        });
        // empty line
        wordList.add("");
        File outFile = FileUtil.file("export/brand_meta.arff");
        // write header
        List<String> header = Lists.newArrayList();
        header.add("@RELATION brand meta");
        header.add("");
        header.add("@ATTRIBUTE id INTEGER");
        header.add("@ATTRIBUTE brand_name_ch STRING");
        header.add("@ATTRIBUTE brand_name_en STRING");
        header.add("");
        header.add("@DATA");

        FileUtil.writeLines(header, outFile, StandardCharsets.UTF_8, false);
        // write data
        FileUtil.writeLines(wordList, outFile, StandardCharsets.UTF_8, true);
    }

    /**
     * 生成商品元数据
     */
    @Test
    public void generateProductMeta() {
        String file = "export/diff_product.txt";
        Set<String> wordList = Sets.newLinkedHashSet();
        Joiner joiner = Joiner.on(",");
        FileUtil.readUtf8Lines(FileUtil.file(file), (LineHandler) line -> {
            line = trimRef(line);
            String[] split = line.split(",");
            wordList.add(joiner.join(split[0], split[1].toLowerCase(), split[2], split[3]));
        });
        wordList.add("");

        File outFile = FileUtil.file("export/product_meta.arff");
        // write header
        List<String> header = Lists.newArrayList();
        header.add("@RELATION product meta");
        header.add("");
        header.add("@ATTRIBUTE id INTEGER");
        header.add("@ATTRIBUTE product_name STRING");
        header.add("@ATTRIBUTE brand_id INTEGER");
        header.add("@ATTRIBUTE category_id INTEGER");
        header.add("");
        header.add("@DATA");

        FileUtil.writeLines(header, outFile, StandardCharsets.UTF_8, false);
        // write data
        FileUtil.writeLines(wordList, outFile, StandardCharsets.UTF_8, true);
    }

    /**
     * 创建品牌词典
     */
    @Test
    public void generateBrandDictionary() throws IOException {
        List<String> wordList = Lists.newArrayList();
        ArffSourceConvertor convertor = new ArffSourceConvertor("export/brand_meta.arff");
        convertor.readData();
        convertor.getInstances().forEach(arffInstance -> {
            String nameCh = arffInstance.getValueByAttrName("brand_name_ch").toString();
            String nameEh = arffInstance.getValueByAttrName("brand_name_en").toString();
            String productName = "";
            if (StringUtils.isNotBlank(nameCh) && StringUtils.isNotBlank(nameEh)) {
                productName = nameCh + "," + nameEh;
            } else if (StringUtils.isNotBlank(nameCh)) {
                productName = nameCh;
            } else {
                productName = nameEh;
            }
            wordList.add(Joiner.on(",").join("[" + productName + "]","model", "1",
                    arffInstance.getValueByAttrName("id")));
        });
        wordList.add("");
        export("export/brand_dic.arff", wordList);
    }

    /**
     * 创建苹果型号词典
     */
    @Test
    public void generateAppleDictionary() throws IOException {
        List<String> wordList = Lists.newArrayList();
        ArffSourceConvertor convertor = new ArffSourceConvertor("export/product_meta.arff");
        convertor.readData();
        convertor.getInstances().forEach(arffInstance -> {
            // 苹果
            if (!"14026".equals(arffInstance.getValueByAttrName("brand_id").toString() )) {
                return;
            }
            Object productName = arffInstance.getValueByAttrName("product_name");
            // 去除苹果文字
            if (productName.toString().startsWith("苹果")) {
                productName = productName.toString().substring(2).trim();
            }

            wordList.add(Joiner.on(",").join("[" + productName + "]","model", "1",
                    arffInstance.getValueByAttrName("id")));
        });
        wordList.add("");
        export("export/model_apple_dic.arff", wordList);

    }

    /**
     * 型号转换为词典格式
     */
    @Test
    public void generateXiaomiDictionary() throws IOException {
        List<String> wordList = Lists.newArrayList();
        ArffSourceConvertor convertor = new ArffSourceConvertor("export/product_meta.arff");
        convertor.readData();
        convertor.getInstances().forEach(arffInstance -> {
            if (!"133787".equals(arffInstance.getValueByAttrName("brand_id").toString() )) {
                return;
            }
            Object productName = arffInstance.getValueByAttrName("product_name");
            wordList.add(Joiner.on(",").join("[" + productName + "]","model", "1",
                    arffInstance.getValueByAttrName("id")));
        });
        wordList.add("");
        export("export/model_xiaomi_dic.arff", wordList);

    }
    /**
     * 型号转换为词典格式
     */
    @Test
    public void generateHuaweiDictionary() throws IOException {
        List<String> wordList = Lists.newArrayList();
        ArffSourceConvertor convertor = new ArffSourceConvertor("export/product_meta.arff");
        convertor.readData();
        convertor.getInstances().forEach(arffInstance -> {
            if (!"8557".equals(arffInstance.getValueByAttrName("brand_id").toString() )) {
                return;
            }
            Object productName = arffInstance.getValueByAttrName("product_name");
            wordList.add(Joiner.on(",").join("[" + productName + "]","model", "1",
                    arffInstance.getValueByAttrName("id")));
        });
        wordList.add("");
        export("export/model_huawei_dic.arff", wordList);

    }

    /**
     * 手机型号转换为词典格式
     */
    @Test
    public void generateMobileDictionary() throws IOException {
        List<String> wordList = Lists.newArrayList();
        ArffSourceConvertor convertor = new ArffSourceConvertor("export/product_meta.arff");
        convertor.readData();
        Set<String> excludeBrands = Sets.newHashSet("8557", "133787", "14026");
        convertor.getInstances().forEach(arffInstance -> {
            if (!"655".equals(arffInstance.getValueByAttrName("category_id").toString())) {
                return;
            }
            if (excludeBrands.contains(arffInstance.getValueByAttrName("brand_id").toString() )) {
                return;
            }
            Object productName = arffInstance.getValueByAttrName("product_name");
            wordList.add(Joiner.on(",").join("[" + productName + "]","model", "1",
                    arffInstance.getValueByAttrName("id")));
        });
        wordList.add("");
        export("export/model_mobile_dic.arff", wordList);
    }

    /**
     * 笔记本型号转换为词典格式
     */
    @Test
    public void generateNotepadDictionary() throws IOException {
        List<String> wordList = Lists.newArrayList();
        ArffSourceConvertor convertor = new ArffSourceConvertor("export/product_meta.arff");
        convertor.readData();
        Set<String> excludeBrands = Sets.newHashSet("8557", "133787", "14026");
        convertor.getInstances().forEach(arffInstance -> {
            if (!"672".equals(arffInstance.getValueByAttrName("category_id").toString())) {
                return;
            }
            if (excludeBrands.contains(arffInstance.getValueByAttrName("brand_id").toString() )) {
                return;
            }
            Object productName = arffInstance.getValueByAttrName("product_name");
            wordList.add(Joiner.on(",").join("[" + productName + "]","model", "1",
                    arffInstance.getValueByAttrName("id")));
        });
        wordList.add("");
        export("export/model_notepad_dic.arff", wordList);
    }

    /**
     * 非手机型号转换为词典格式
     */
    @Test
    public void otherModelDictionary() throws IOException {
        List<String> wordList = Lists.newArrayList();
        ArffSourceConvertor convertor = new ArffSourceConvertor("export/product_meta.arff");
        convertor.readData();
        Set<String> excludeCats = Sets.newHashSet("655", "672");
        Set<String> excludeBrands = Sets.newHashSet("8557", "133787", "14026");
        convertor.getInstances().forEach(arffInstance -> {
            if (excludeCats.contains(arffInstance.getValueByAttrName("category_id").toString())) {
                return;
            }
            if (excludeBrands.contains(arffInstance.getValueByAttrName("brand_id").toString() )) {
                return;
            }

            Object productName = arffInstance.getValueByAttrName("product_name");
            wordList.add(Joiner.on(",").join("[" + productName + "]","model", "1",
                    arffInstance.getValueByAttrName("id")));
        });
        wordList.add("");
        export("export/model_other_dic.arff", wordList);
    }

    /**
     * 转化品类实体
     */
    @Test
    public void genCategoryNer() throws IOException {
        ArffSourceConvertor productArff = new ArffSourceConvertor("export/category_meta.arff");
        productArff.readData();
        List<BaseDictEntityLine> entityLines = new LinkedList<>();
        productArff.getInstances().forEach(instance -> {
            BaseDictEntityLine entity = new BaseDictEntityLine();
            entityLines.add(entity);

            String id = instance.getValueByAttrName("id").toString();
            String categoryName = (String) instance.getValueByAttrName("category_name");
            String[] names = categoryName.split(",");
            entity.setEntity(names[0]);
            entity.setType(EntityType.CATEGORY);
            entity.setNormalizedValue(names[0]);
            entity.setId(Collections.singletonList(id));
            if (names.length > 1) {
                entity.setAliases(Arrays.asList(Arrays.copyOfRange(names, 1, names.length)));
            }
        });
        File outFile = FileUtil.file("export/category_ner.txt");
        FileUtil.writeUtf8Lines(entityLines.stream().map(JSON::toJSONString).collect(Collectors.toList()), outFile);
    }

    /**
     * 品牌实体
     */
    @Test
    public void generateBrandNer() throws IOException {
        ArffSourceConvertor productArff = new ArffSourceConvertor("export/brand_meta.arff");
        productArff.readData();
        List<BaseDictEntityLine> entityLines = new LinkedList<>();
        productArff.getInstances().forEach(instance -> {
            BaseDictEntityLine entity = new BaseDictEntityLine();
            entityLines.add(entity);

            String id = instance.getValueByAttrName("id").toString();
            String nameCh = (String) instance.getValueByAttrName("brand_name_ch");
            String nameEn = (String) instance.getValueByAttrName("brand_name_en");
            entity.setEntity(nameCh);
            entity.setType(EntityType.BRAND);
            entity.setNormalizedValue(nameCh);
            entity.setId(Collections.singletonList(id));
            if (StringUtils.isNotBlank(nameEn)) {
                entity.setAliases(Collections.singletonList(nameEn));
            }
        });
        File outFile = FileUtil.file("export/brand_ner.txt");
        FileUtil.writeUtf8Lines(entityLines.stream().map(JSON::toJSONString).collect(Collectors.toList()), outFile);
    }

    /**
     * 转化型号实体
     */
    @Test
    public void generateModelNer() throws IOException {
        ArffSourceConvertor productArff = new ArffSourceConvertor("meta/product_meta.arff");
        productArff.readData();
        Map<String, ArffInstance> modelMap = MetaGetter.getMeta("product");
        Map<String, ArffInstance> brandMap = MetaGetter.getMeta("brand");
        Map<String, ArffInstance> categoryMap = MetaGetter.getMeta("category");

        String[] inputs = new String[]{
                "dictionary/model_apple_dic.arff",
                "dictionary/model_huawei_dic.arff",
                "dictionary/model_xiaomi_dic.arff",
                "dictionary/model_mobile_dic.arff",
                "dictionary/model_notepad_dic.arff",
                "dictionary/model_other_dic.arff",
        };
        String[] outputs = new String[]{
                "export/model_apple_ner.txt",
                "export/model_huawei_ner.txt",
                "export/model_xiaomi_ner.txt",
                "export/model_mobile_ner.txt",
                "export/model_notepad_ner.txt",
                "export/model_other_ner.txt",
        };
        for (int i = 0; i < inputs.length; i++) {
            List<BaseDictEntityLine> entityLines = new LinkedList<>();
            productArff = new ArffSourceConvertor(inputs[i]);
            productArff.readData();
            productArff.getInstances().forEach(instance ->
                    entityLines.add(toModelEntity(instance, modelMap, brandMap, categoryMap)));
            // empty line
            List<String> collect = entityLines.stream().map(JSON::toJSONString).collect(Collectors.toList());
            collect.add("");
            File outFile = FileUtil.file(outputs[i]);
            FileUtil.writeUtf8Lines(collect, outFile);
        }
    }

    private BaseDictEntityLine toModelEntity(ArffInstance instance,
                                             Map<String, ArffInstance> modelMap,
                                             Map<String, ArffInstance> brandMap,
                                             Map<String, ArffInstance> categoryMap) {
        ModelDictEntityLine entity = new ModelDictEntityLine();
        String id = instance.getValueByAttrName("id").toString();
        String word = instance.getValueByAttrName("word").toString();
        String[] names = word.split(",");
        entity.setEntity(names[0]);
        entity.setType(EntityType.MODEL);
        entity.setNormalizedValue(names[0]);
        entity.setId(Collections.singletonList(id));
        ArffInstance product = modelMap.get(id);
        if (product == null) {
            System.out.println("未找到产品：" + id);
        }
        if (product != null) {
            String brandId = product.getValueByAttrName("brand_id").toString();
            String categoryId = product.getValueByAttrName("category_id").toString();
            ArffInstance brand = brandMap.get(brandId);
            if (brand == null) {
                System.out.println("未找到品牌：" + brandId);
            }
            ArffInstance category = categoryMap.get(categoryId);
            if (category == null) {
                System.out.println("未找到品类：" + categoryId);
            }
            entity.setAttributes(new HashMap<String, String>() {{
                put(ModelDictEntityLine.ATTR_BRAND_ID, brandId);
                put(ModelDictEntityLine.ATTR_CATEGORY_ID, categoryId);
                if (brand != null) {
                    put(ModelDictEntityLine.ATTR_BRAND_NAME, brand.getValueByAttrName("brand_name_ch").toString());
                }
                if (category != null) {
                    String name = category.getValueByAttrName("category_name").toString();
                    if (name.contains(",")) {
                        name = name.substring(0, name.indexOf(","));
                    }
                    put(ModelDictEntityLine.ATTR_CATEGORY_NAME, name);
                }
            }});
        }
        if (names.length > 1) {
            entity.setAliases(Arrays.asList(Arrays.copyOfRange(names, 1, names.length)));
        }
        return entity;
    }

    private void export(String filePath, List<String> wordList) {
        File outFile = FileUtil.file(filePath);
        // write header
        List<String> header = Lists.newArrayList();
        header.add("@RELATION product meta");
        header.add("");
        header.add("@ATTRIBUTE word STRING");
        header.add("@ATTRIBUTE nature STRING");
        header.add("@ATTRIBUTE frequency STRING");
        header.add("@ATTRIBUTE id STRING");
        header.add("");
        header.add("@DATA");
        FileUtil.writeLines(header, outFile, StandardCharsets.UTF_8, false);
        // write data
        FileUtil.writeLines(wordList, outFile, StandardCharsets.UTF_8, true);
    }

    private String trimRef(String line) {
        return line.replaceAll("\"", "");
    }
}
