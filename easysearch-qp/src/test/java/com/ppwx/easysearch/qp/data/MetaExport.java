package com.ppwx.easysearch.qp.data;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.text.csv.CsvUtil;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className MetaExport
 * @description todo
 * @date 2024/10/12 11:20
 **/
public class MetaExport {

    @Test
    public void testExportBrand() {
        String file = "2025-10-09-11-27-24_brand.csv";
        Set<String> wordList = Sets.newLinkedHashSet();
        AtomicInteger index = new AtomicInteger(0);
        FileUtil.readUtf8Lines(FileUtil.file(file), (LineHandler) line -> {
            if (index.getAndAdd(1) > 0) {
                line = trimRef(line);
                if (line.contains("（")) {
                    String[] split = line.split(",");
                    String word = split[1].replaceAll("（", ",").replaceAll("）", "");
                    wordList.add(split[0] + "," + word.toLowerCase());
                } else {
                    wordList.add(line.toLowerCase() + ",");
                }
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

    @Test
    public void testExportProduct() {
        String file = "2025-10-09-11-27-23_product.csv";
        Set<String> wordList = Sets.newLinkedHashSet();
        AtomicInteger index = new AtomicInteger(0);
        Joiner joiner = Joiner.on(",");
        FileUtil.readUtf8Lines(FileUtil.file(file), (LineHandler) line -> {
            if (index.getAndAdd(1) > 0) {
                line = trimRef(line);
                String[] split = line.split(",");
                wordList.add(joiner.join(split[0], split[1].toLowerCase(), split[2].toLowerCase(), split[4].toLowerCase()));
            }
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

    @Test
    public void brandTransformer() throws IOException {
        List<String> wordList = Lists.newArrayList();
        ArffSourceConvertor convertor = new ArffSourceConvertor("meta/diff/brand_meta.arff");
        convertor.readData();
        convertor.getInstances().forEach(arffInstance -> {
            Object nameCh = arffInstance.getValueByAttrName("brand_name_ch");
            Object nameEh = arffInstance.getValueByAttrName("brand_name_en");
            String productName = "";
            if (nameCh != null && nameEh != null) {
                productName = nameCh + "," + nameEh;
            } else if (nameCh != null) {
                productName = nameCh.toString();
            } else {
                productName = nameEh.toString();
            }
            wordList.add(Joiner.on(",").join("[" + productName + "]","model", "1",
                    arffInstance.getValueByAttrName("id")));
        });
        wordList.add("");
        export("export/brand_dic.arff", wordList);
    }

    /**
     * 型号转换为词典格式
     */
    @Test
    public void appleModelTransformer() throws IOException {
        List<String> wordList = Lists.newArrayList();
        ArffSourceConvertor convertor = new ArffSourceConvertor("meta/diff/product_meta.arff");
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
    public void xiaomiModelTransformer() throws IOException {
        List<String> wordList = Lists.newArrayList();
        ArffSourceConvertor convertor = new ArffSourceConvertor("meta/diff/product_meta.arff");
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
    public void huaweiModelTransformer() throws IOException {
        List<String> wordList = Lists.newArrayList();
        ArffSourceConvertor convertor = new ArffSourceConvertor("meta/diff/product_meta.arff");
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
    public void mobileModelTransformer() throws IOException {
        List<String> wordList = Lists.newArrayList();
        ArffSourceConvertor convertor = new ArffSourceConvertor("meta/diff/product_meta.arff");
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
    public void notepadModelTransformer() throws IOException {
        List<String> wordList = Lists.newArrayList();
        ArffSourceConvertor convertor = new ArffSourceConvertor("meta/diff/product_meta.arff");
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
    public void otherModelTransformer() throws IOException {
        List<String> wordList = Lists.newArrayList();
        ArffSourceConvertor convertor = new ArffSourceConvertor("meta/diff/product_meta.arff");
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

    private String addQuote(String line) {
        return "[" + line + "]";
    }

    @Test
    public void diffProducts() throws IOException {
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
        String file = "2025-11-21-15-10-00_product.csv";
        CsvUtil.getReader().read(FileUtil.file(file)).forEach(row -> {
            if (row.getOriginalLineNumber() > 0) {
                if (!productIds.contains(Integer.valueOf(row.get(0)))) {
                    products.add(row.get(0) + "," + row.get(1).toLowerCase() + "," + row.get(2) + "," + row.get(4));
                }
                if (!brandIds.contains(Integer.valueOf(row.get(2)))) {
                    brands.add(row.get(2) + "," + row.get(3).toLowerCase());
                }
                if (!categoryIds.contains(Integer.valueOf(row.get(4)))) {
                    categorys.add(row.get(4) + "," + row.get(5).toLowerCase());
                }
            }
        });

        // 汇总
        System.out.println("diff product:" + products.size());
        System.out.println("diff brand:" + brands.size());
        System.out.println("diff category:" + categorys.size());
        products.forEach(System.out::println);
        //brands.forEach(System.out::println);
    }
}
