package com.ppwx.easysearch.qp.format;

import org.junit.Test;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className WordFormatTrimTester
 * @description WordFormat测试类
 * @date 2024/11/2 0:04
 **/
public class WordFormatTester {

    @Test
    public void testWordFormatTrimWorks() {
        WordFormat format = WordFormats.trim();
        StringBuilder re = format.format(new StringBuilder("  我爱你中国  "));
        System.out.println(re);
    }

    @Test
    public void testWordFormatCompositorWorks() {
        // WordFormats.trim(), WordFormats.truncate(), WordFormats.ignoreCase()
        WordFormat chains = WordFormats.chains(WordFormats.truncate());
        StringBuilder format = chains.format(new StringBuilder("  我爱你中国"));
        System.out.println(format);
    }

}
