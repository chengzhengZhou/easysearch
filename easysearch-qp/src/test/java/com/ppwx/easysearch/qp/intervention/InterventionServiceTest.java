package com.ppwx.easysearch.qp.intervention;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * InterventionService 基础单元测试：同时覆盖整句和词表改写。
 */
public class InterventionServiceTest {

    @Test
    public void rewrite_nullQueryReturnsNull() {
        InterventionService service = new InterventionService(null, null);
        assertNull(service.rewrite(null));
    }

    @Test
    public void rewrite_noEnginesReturnsOriginal() {
        InterventionService service = new InterventionService(null, null);
        assertEquals("苹果手机", service.rewrite("苹果手机"));
    }

    @Test
    public void rewrite_sentenceThenTermApplied() {
        // 构造整句规则：将“苹果手机多少钱”改写为“iphone 价格”
        String sentenceRules = "苹果手机多少钱\tiphone 价格\tEXACT\t10\n";
        SentenceInterventionEngine sentenceEngine = new SentenceInterventionEngine();
        sentenceEngine.load(new ByteArrayInputStream(sentenceRules.getBytes(StandardCharsets.UTF_8)));

        // 构造词表规则：将“iphone”进一步改写为“iphone 15”
        String termRules = "iphone\tiphone 15\t5\n";
        TermInterventionEngine termEngine = new TermInterventionEngine();
        termEngine.load(new ByteArrayInputStream(termRules.getBytes(StandardCharsets.UTF_8)));

        InterventionService service = new InterventionService(termEngine, sentenceEngine);
        String rewritten = service.rewrite("苹果手机多少钱");

        assertEquals("iphone 15 价格", rewritten);
    }
}

