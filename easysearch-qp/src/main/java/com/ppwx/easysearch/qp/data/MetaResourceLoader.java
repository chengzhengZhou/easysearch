package com.ppwx.easysearch.qp.data;

import com.ppwx.easysearch.core.util.SearchLog;
import com.ppwx.easysearch.qp.util.StreamUtil;

import java.io.IOException;
import java.util.Observable;
import java.util.concurrent.atomic.AtomicInteger;

public class MetaResourceLoader extends Observable implements DataChangedListener<MetaTermOpt> {

    private final String[] path;

    public MetaResourceLoader(String... paths) {
        this.path = paths;
    }

    @Override
    public void addItem(MetaTermOpt termOpt) {
        setChanged();
        termOpt.setOpt(MetaTermOpt.ADD);
        super.notifyObservers(termOpt);
    }

    @Override
    public void updateItem(MetaTermOpt termOpt) {
        setChanged();
        termOpt.setOpt(MetaTermOpt.UPDATE);
        super.notifyObservers(termOpt);
    }

    @Override
    public void deleteItem(MetaTermOpt termOpt) {
        setChanged();
        termOpt.setOpt(MetaTermOpt.DELETE);
        super.notifyObservers(termOpt);
    }

    @Override
    public void loadResources() throws IOException {
        AtomicInteger count = new AtomicInteger();
        for (String dictionaryFile : path) {
            String resourceName = StreamUtil.getResourceName(dictionaryFile);
            boolean categoryFile = resourceName.startsWith("category");
            boolean brandFile = resourceName.startsWith("brand");
            boolean modelFile = resourceName.startsWith("product");
            if (!categoryFile && !brandFile && !modelFile) {
                throw new IllegalArgumentException("dictionary file must start with category or brand or product");
            }
            ArffSourceConvertor convertor = new ArffSourceConvertor(dictionaryFile);
            convertor.readData();

            convertor.getInstances().forEach(instance -> {
                MetaTermOpt.Builder builder = MetaTermOpt.builder();
                if (categoryFile) {
                    builder.termType(MetaTermOpt.CATEGORY)
                            .categoryId(safeGetStringValue(instance, "id"))
                            .categoryName(safeGetStringValue(instance, "category_name"));
                } else if (brandFile) {
                    builder.termType(MetaTermOpt.BRAND)
                            .brandId(safeGetStringValue(instance, "id"))
                            .brandName(safeGetStringValue(instance, "brand_name_ch"))
                            .brandNameEn(safeGetStringValue(instance, "brand_name_en"));
                } else {
                    builder.termType(MetaTermOpt.MODEL)
                            .modelId(safeGetStringValue(instance, "id"))
                            .modelName(safeGetStringValue(instance, "product_name"))
                            .brandId(safeGetStringValue(instance, "brand_id"))
                            .categoryId(safeGetStringValue(instance, "category_id"));
                }
                addItem(builder.build());
                count.getAndIncrement();
            });
        }
        SearchLog.getLogger().info("加载元信息资源完成，共加载 {} 条数据", count);
    }

    /**
     * 安全获取字符串属性值
     */
    private String safeGetStringValue(ArffInstance instance, String attrName) {
        Object value = safeGetValue(instance, attrName);
        return value != null ? value.toString() : null;
    }

    /**
     * 安全获取属性值
     */
    private Object safeGetValue(ArffInstance instance, String attrName) {
        if (instance == null || attrName == null) {
            return null;
        }
        try {
            return instance.getValueByAttrName(attrName);
        } catch (Exception e) {
            return null;
        }
    }
}
