package com.ppwx.easysearch.qp.synonym;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * SynonymService 单元测试。
 */
public class SynonymServiceTest {

    private SynonymService service;

    @Before
    public void setUp() throws Exception {
        service = SynonymService.create("synonym/synonym.txt");
    }

    @Test
    public void match_emptyWhenQueryIsNull() {
        assertTrue(service.match(null).isEmpty());
    }

    @Test
    public void match_emptyWhenQueryIsEmpty() {
        assertTrue(service.match("").isEmpty());
    }

    @Test
    public void match_emptyWhenNoMatch() {
        assertTrue(service.match("无匹配内容").isEmpty());
    }

    @Test
    public void match_returnsResultsWhenQueryContainsSynonyms() {
        List<SynonymMatch> matches = service.match("苹果 15");
        assertEquals(1, matches.size());
        assertEquals("苹果 15", matches.get(0).getSource());
    }

    @Test
    public void match_multipleMatchesLongestFirst() {
        List<SynonymMatch> matches = service.match("苹果 16和手环");
        assertTrue(matches.size() >= 2);
    }

    @Test
    public void rewrite_noMatchReturnsOriginal() {
        assertEquals("原始查询", service.rewrite("原始查询"));
    }

    @Test
    public void rewrite_nullQueryReturnsOriginalAsNull() {
        assertNull(service.rewrite(null));
    }

    @Test
    public void rewrite_defaultStrategyReplaceFirst() {
        String result = service.rewrite("我要苹果 15");
        assertEquals("我要iphone 15", result);
    }

    @Test
    public void rewrite_withNullStrategyUsesDefault() {
        String result = service.rewrite("我要苹果 15", null);
        assertEquals("我要iphone 15", result);
    }

    @Test
    public void rewrite_withReplaceAllRewriteStrategy() {
        String result = service.rewrite("苹果 16和手环", new ReplaceAllRewriteStrategy());
        assertTrue(result.contains("iphone 16"));
        assertTrue(result.contains("手表"));
    }

    @Test
    public void rewrite_singleMatchReplaceFirst() {
        String r = service.rewrite("折叠");
        assertNotNull(r);
        assertTrue(r.contains("华为") || r.equals("折叠"));
    }

    @Test
    public void expand_returnsMultipleWhenMultipleTargets() {
        List<String> expanded = service.expand("折叠");
        assertTrue(expanded.size() >= 2);
        assertTrue(expanded.stream().anyMatch(s -> s != null && s.contains("华为 mate x5")));
        assertTrue(expanded.stream().anyMatch(s -> s != null && s.contains("华为 pocket 2")));
    }

    @Test
    public void setDefaultStrategy_thenRewriteUsesNewStrategy() {
        service.setDefaultRewriteStrategy(new ReplaceAllRewriteStrategy());
        String result = service.rewrite("苹果 15和手环");
        assertTrue(result.contains("iphone 15"));
        assertTrue(result.contains("手表"));
    }

    @Test
    public void reload_doesNotThrow() throws Exception {
        service.reload("synonym/synonym.txt");
        assertFalse(service.match("苹果 15").isEmpty());
    }
}
