package com.ppwx.easysearch.qp.ner;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.LineHandler;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className DicExport
 * @description 元数据转词典
 * @date 2024/10/9 19:00
 **/
public class DicExport {

    @Test
    public void testExportBrand() {
        String file = "2024-09-27-17-15-28_品牌.csv";
        Splitter splitter = Splitter.on(",");
        Set<String> wordList = Sets.newHashSet();
        AtomicInteger index = new AtomicInteger(0);
        FileUtil.readUtf8Lines(FileUtil.file(file), new LineHandler() {
            @Override
            public void handle(String line) {
                if (index.getAndAdd(1) > 0) {
                    String substring = line.substring(line.indexOf(",")).replace("\"", "").replace(" ", "");
                    Iterator<String> iterator = splitter.split(substring).iterator();
                    while (iterator.hasNext()) {
                        String next = iterator.next();
                        if (StringUtils.isNoneBlank(next)) {
                            wordList.add(next.toLowerCase());
                        }
                    }
                }
            }
        });
        FileUtil.writeUtf8Lines(wordList, "export/brand_1.dic");
    }

    @Test
    public void testExportProduct() {
        String file = "2024-09-27-17-15-28_机型.csv";
        Splitter splitter = Splitter.on(",");
        Set<String> wordList = Sets.newHashSet();
        AtomicInteger index = new AtomicInteger(0);
        FileUtil.readUtf8Lines(FileUtil.file(file), new LineHandler() {
            @Override
            public void handle(String line) {
                if (index.getAndAdd(1) > 0) {
                    String substring = line.substring(line.indexOf(",")).replace("\"", "")
                            .replace(" ", "")
                            .replace("(", "")
                            .replace(")", "");
                    Iterator<String> iterator = splitter.split(substring).iterator();
                    while (iterator.hasNext()) {
                        String next = iterator.next();
                        if (StringUtils.isNoneBlank(next)) {
                            wordList.add(next.toLowerCase());
                        }
                    }
                }
            }
        });
        FileUtil.writeUtf8Lines(wordList, "export/product_4.dic");
    }
}
