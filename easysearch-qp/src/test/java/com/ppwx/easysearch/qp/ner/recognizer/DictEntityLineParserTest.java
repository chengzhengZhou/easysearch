package com.ppwx.easysearch.qp.ner.recognizer;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson.JSON;
import com.ppwx.easysearch.qp.data.ArffInstance;
import com.ppwx.easysearch.qp.data.ArffSourceConvertor;
import com.ppwx.easysearch.qp.ner.EntityType;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class DictEntityLineParserTest {

    @Test
    public void testParseLine() {
        String line = "{\"entity\":\"苹果\",\"type\":\"MODEL\",\"normalizedValue\":\"apple\",\"attributes\":{\"brand\":\"apple\"},\"aliases\":[\"苹果\",\"苹果手机\"],\"relations\":{\"brand\":\"apple\"},\"id\":[\"apple\"]}";
        DictEntityLineParser.parseLine(line).ifPresent(record -> System.out.println(record.getClass()));
    }

    /**
     * 转化品类实体
     */
    @Test
    public void testCategoryTransform() throws IOException {
        ArffSourceConvertor productArff = new ArffSourceConvertor("meta/category_meta.arff");
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
     * 转化品牌实体
     */
    @Test
    public void testBrandTransform() throws IOException {
        ArffSourceConvertor productArff = new ArffSourceConvertor("meta/brand_meta.arff");
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
    public void testModelTransform() throws IOException {
        ArffSourceConvertor productArff = new ArffSourceConvertor("meta/product_meta.arff");
        productArff.readData();
        Map<String, ArffInstance> map = new HashMap<>();
        productArff.getInstances().forEach(instance -> map.put(instance.getValueByAttrName("id").toString(), instance));

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
            productArff.getInstances().forEach(instance -> entityLines.add(toModelEntity(instance, map)));

            File outFile = FileUtil.file(outputs[i]);
            FileUtil.writeUtf8Lines(entityLines.stream().map(JSON::toJSONString).collect(Collectors.toList()), outFile);
        }
    }

    private BaseDictEntityLine toModelEntity(ArffInstance instance, Map<String, ArffInstance> map) {
        ModelDictEntityLine entity = new ModelDictEntityLine();
        String id = instance.getValueByAttrName("id").toString();
        String word = instance.getValueByAttrName("word").toString();
        String[] names = word.split(",");
        entity.setEntity(names[0]);
        entity.setType(EntityType.MODEL);
        entity.setNormalizedValue(names[0]);
        entity.setId(Collections.singletonList(id));
        ArffInstance product = map.get(id);
        if (product == null) {
            System.out.println("未找到产品：" + id);
        }
        if (product != null) {
            entity.setAttributes(new HashMap<String, String>() {{
                put(ModelDictEntityLine.ATTR_BRAND_ID, product.getValueByAttrName("brand_id").toString());
                put(ModelDictEntityLine.ATTR_CATEGORY_ID, product.getValueByAttrName("category_id").toString());
            }});
        }
        if (names.length > 1) {
            entity.setAliases(Arrays.asList(Arrays.copyOfRange(names, 1, names.length)));
        }
        return entity;
    }
}
