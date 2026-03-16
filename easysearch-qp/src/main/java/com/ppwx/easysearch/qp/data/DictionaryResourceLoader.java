package com.ppwx.easysearch.qp.data;

import com.ppwx.easysearch.core.util.SearchLog;
import com.ppwx.easysearch.qp.util.StreamUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.Observable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 字典资源加载器
 */
public class DictionaryResourceLoader extends Observable implements DataChangedListener<DictionaryTermOpt> {

    private String[] path;

    public DictionaryResourceLoader(String... paths) {
        this.path = paths;
    }

    public void addPath(String path) {
        if (this.path == null) {
            this.path = new String[]{path};
        } else {
            this.path = Arrays.copyOf(this.path, this.path.length + 1);
            this.path[this.path.length - 1] = path;
        }
    }

    @Override
    public void loadResources() throws IOException {
        AtomicInteger count = new AtomicInteger();
        for (String dictionaryFile : path) {
            String resourceName = StreamUtil.getResourceName(dictionaryFile);
            boolean dicFile = resourceName.endsWith("_dic.arff");
            boolean idFile = resourceName.endsWith("_id.arff");
            if (!dicFile && !idFile) {
                throw new IllegalArgumentException("dictionary file must end with _dic.arff or _id.arff");
            }
            ArffSourceConvertor convertor = new ArffSourceConvertor(dictionaryFile);
            convertor.readData();

            convertor.getInstances().forEach(instance -> {
                String word = (String) instance.getValueByAttrName("word");
                String nature = (String) instance.getValueByAttrName("nature");
                String frequency = (String) instance.getValueByAttrName("frequency");
                String id = (String) instance.getValueByAttrName("id");

                DictionaryTermOpt termOpt = DictionaryTermOpt.builder()
                        .opt(DictionaryTermOpt.ADD)
                        .termType(dicFile ? DictionaryTermOpt.TYPE_DIC : DictionaryTermOpt.TYPE_ID)
                        .word(Arrays.stream(word.split(",")).collect(Collectors.toList()))
                        .nature(Arrays.stream(nature.split(",")).collect(Collectors.toList()))
                        .frequency(Arrays.stream(frequency.split(",")).collect(Collectors.toList()))
                        .id(Arrays.stream(id.split(",")).collect(Collectors.toList()))
                        .build();

                addItem(termOpt);
                count.getAndIncrement();
            });
        }
        SearchLog.getLogger().info("加载字典资源完成，共加载 {} 条数据", count);
    }

    @Override
    public void addItem(DictionaryTermOpt termOpt) {
        setChanged();
        termOpt.setOpt(DictionaryTermOpt.ADD);
        super.notifyObservers(termOpt);
    }

    @Override
    public void updateItem(DictionaryTermOpt termOpt) {
        setChanged();
        termOpt.setOpt(DictionaryTermOpt.UPDATE);
        super.notifyObservers(termOpt);
    }

    @Override
    public void deleteItem(DictionaryTermOpt termOpt) {
        setChanged();
        termOpt.setOpt(DictionaryTermOpt.DELETE);
        super.notifyObservers(termOpt);
    }
}
