package com.ppwx.easysearch.qp.tokenizer;

import com.google.common.collect.Lists;
import com.hankcs.hanlp.dictionary.CustomDictionary;
import com.hankcs.hanlp.seg.NShort.NShortSegment;
import com.hankcs.hanlp.seg.Segment;
import com.ppwx.easysearch.qp.data.DictionaryTermOpt;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * ThreeCTokenizer测试类
 * 
 * @author system
 * @date 2024/12/19
 */
public class ThreeCTokenizerTest {
    
    private Tokenizer tokenizer;

    private HanlpSegmentation segment;
    
    @Before
    public void setUp() {
        // 手动创建用于测试
        segment = new HanlpSegmentation();
        tokenizer = new ThreeCTokenizer(segment);
    }
    
    @Test
    public void testBasicTokenization() {
        String input = "iPhone15 Pro Max";
        List<Token> result = tokenizer.tokenize(input);
        
        assertNotNull(result);
        assertTrue(result.size() > 0);
        System.out.println("Basic tokenization result: " + result);
    }
    
    @Test
    public void testPhoneTokenization() {
        String input = "iPhone15 Pro Max 256GB 蓝色";
        List<Token> result = tokenizer.tokenize(input);
        
        assertNotNull(result);
        assertTrue(result.size() > 0);
        
        System.out.println("Phone tokenization result: " + result);
    }
    
    @Test
    public void testLaptopTokenization() {
        String input = "MacBook Pro 14寸 M3芯片";
        List<Token> result = tokenizer.tokenize(input);
        
        assertNotNull(result);
        assertTrue(result.size() > 0);
        System.out.println("Laptop tokenization result: " + result);
    }
    
    @Test
    public void testNumberUnitMerge() {
        String input = "256GB存储 16GB内存";
        List<Token> result = tokenizer.tokenize(input);
        
        assertNotNull(result);
        assertTrue(result.size() > 0);
        
        System.out.println("Number unit merge result: " + result);
    }
    
    @Test
    public void testComplexQuery() {
        String input = "二手 OPPO Reno8 Pro 5G版 256GB 黑色 95成新";
        List<Token> result = tokenizer.tokenize(input);
        
        assertNotNull(result);
        assertTrue(result.size() > 0);
        System.out.println("Complex query result: " + result);
        
        // 打印每个token的详细信息
        for (Token token : result) {
            System.out.println(String.format("  Token: text='%s', type='%s', confidence=%.2f",
                token.getText(), token.getType(), token.getConfidence()));
        }
    }

    @Test
    public void testEmptyInput() {
        List<Token> result = tokenizer.tokenize("");
        assertNotNull(result);
        assertEquals(0, result.size());
        
        String spaces = "   ";
        result = tokenizer.tokenize(spaces);
        assertNotNull(result);
        assertEquals(0, result.size());
        
        String nullStr = null;
        result = tokenizer.tokenize(nullStr);
        assertNotNull(result);
        assertEquals(0, result.size());
    }
    
    @Test
    public void testSpecialCharacters() {
        String input = "iPhone15（全新）256GB！";
        List<Token> result = tokenizer.tokenize(input);
        
        assertNotNull(result);
        assertTrue(result.size() > 0);
        System.out.println("Special characters result: " + result);
    }
    
    @Test
    public void testChineseAndEnglishMixed() {
        String input = "苹果iPhone15手机256GB大容量";
        List<Token> result = tokenizer.tokenize(input);
        
        assertNotNull(result);
        assertTrue(result.size() > 0);
        System.out.println("Mixed language result: " + result);
    }

    @Test
    public void testCustomDictionary() {
        CustomDictionary.add("苹果iPhone13");
        String input = "苹果iPhone13手机256GB大容量";
        List<Token> tokenize = tokenizer.tokenize(input);
        System.out.println("Custom dictionary result: " + tokenize);
    }

    @Test
    public void testAddItem() {
        DictionaryTermOpt termOpt = new DictionaryTermOpt();
        termOpt.setOpt(DictionaryTermOpt.ADD);
        termOpt.setWord(Lists.newArrayList("苹果iPhone15", "256GB", "大容量"));
        termOpt.setNature(Lists.newArrayList("n"));
        termOpt.setFrequency(Lists.newArrayList("1"));
        segment.addItem(termOpt);
        System.out.println("Add item result: " + tokenizer.tokenize("苹果iPhone15手机256GB大容量"));
    }
}
