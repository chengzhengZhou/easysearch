package com.ppwx.easysearch.qp.data;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ppwx.easysearch.qp.util.StreamUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 元信息获取
 */
public final class MetaGetter {

    /**
     * 品牌
     */
    public static Set<String> getBrands() throws IOException {
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

    /**
     * 品类
     */
    public static Set<String> getCategory() throws IOException {
        Set<String> categorys = new HashSet<>();
        ArffSourceConvertor convertor = new ArffSourceConvertor("meta/category_meta.arff");
        convertor.readData();
        convertor.getInstances().forEach(instance -> {
            String categoryName = (String) instance.getValueByAttrName("category_name");
            categorys.add(categoryName);
        });
        return categorys;
    }

    /**
     * 阿里机型打标
     */
    public static Map<String, JSONObject> alibabaModelTags() throws IOException {
        Map<String, JSONObject> map = new HashMap<>();
        try(InputStream vocabStream = StreamUtil.getResourceStream("alibaba-tags.txt")) {
            StreamUtil.readUtf8Lines(vocabStream, line -> {
                String json = line.substring(line.lastIndexOf(";") + 1);
                JSONObject parse = (JSONObject) JSONObject.parse(json);
                map.put(line.substring(0, line.lastIndexOf(";")), parse);
            });
        }
        return map;
    }

    /**
     * 属性标签
     */
    public static Map<String, JSONObject> alibabaPropTags() throws IOException {
        Map<String, JSONObject> map = new HashMap<>();
        try(InputStream vocabStream = StreamUtil.getResourceStream("property-tags.txt")) {
            StreamUtil.readUtf8Lines(vocabStream, line -> {
                String json = line.substring(line.lastIndexOf(";") + 1);
                JSONObject parse = (JSONObject) JSONObject.parse(json);
                map.put(line.substring(0, line.lastIndexOf(";")).toLowerCase(), parse);
            });
        }
        return map;
    }

    /**
     * 核心属性
     * 属性关联分类
     */
    public static Map<String, String> mapCoreProperties() {
        String dictionary = "D:\\projects\\yanxuan\\文档\\搜推\\词库采集\\品类属性";
        File[] files = FileUtil.ls(dictionary);
        Map<String, String> coreProperties = new HashMap<>();
        for (File file : files) {
            //String fileName = file.getName().substring(3, file.getName().length() - 4);
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
                        String key = attribute.getString("attrName");
                        /*if (!includeKey(key)) {
                            continue;
                        }*/
                        JSONArray values = attribute.getJSONArray("values");
                        for (int k = 0; k < values.size(); k++) {
                            JSONObject value = values.getJSONObject(k);
                            String valueName = value.getString("valueName");
                            coreProperties.put(valueName.toLowerCase(), key);
                        }
                    }
                }
            }
        }
        return coreProperties;
    }

    /**
     * 获取元信息
     */
    public static Map<String, ArffInstance> getMeta(String type) throws IOException {
        String path = "meta/";
        if ("product".equals(type)) {
            path = path + "product_meta.arff";
        } else if ("category".equals(type)) {
            path = path + "category_meta.arff";
        } else if ("brand".equals(type)) {
            path = path + "brand_meta.arff";
        }
        ArffSourceConvertor productArff = new ArffSourceConvertor(path);
        productArff.readData();
        Map<String, ArffInstance> map = new HashMap<>();
        productArff.getInstances().forEach(instance ->
                map.put(instance.getValueByAttrName("id").toString(), instance));
        return map;
    }

    private static boolean includeKey(String key) {
        return key.contains("颜色") || key.contains("购买渠道") || key.contains("内存");
    }
}
