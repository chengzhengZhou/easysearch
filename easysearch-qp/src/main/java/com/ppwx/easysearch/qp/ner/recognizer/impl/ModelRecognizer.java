package com.ppwx.easysearch.qp.ner.recognizer.impl;

import com.ppwx.easysearch.qp.ner.Entity;
import com.ppwx.easysearch.qp.ner.EntityIdentityMapper;
import com.ppwx.easysearch.qp.ner.EntityType;
import com.ppwx.easysearch.qp.ner.normalizer.EntityTypeNormalizer;
import com.ppwx.easysearch.qp.ner.recognizer.AbstractDictionaryBasedRecognizer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 型号实体识别器
 * 
 * @author ext.ahs.zhouchzh1@jd.com
 * @date 2024/10/9
 */
public class ModelRecognizer extends AbstractDictionaryBasedRecognizer {
    
    private static final List<Pattern> MODEL_PATTERNS = new ArrayList<>();
    
    static {
        initModelPatterns();
    }
    
    public ModelRecognizer(EntityTypeNormalizer normalizer,
                          EntityIdentityMapper identityMapper) {
        super(EntityType.MODEL, normalizer, identityMapper);
    }
    
    @Override
    protected Entity recognizeByRules(String word, String nature) {
        // 检查型号模式
        for (Pattern pattern : MODEL_PATTERNS) {
            Matcher matcher = pattern.matcher(word);
            if (matcher.matches()) {
                String group = matcher.group();
                String normalizedValue = normalizer.normalize(group);
                List<String> ids = identityMapper.map(entityType, word, normalizedValue);
                return new Entity(word, EntityType.MODEL, normalizedValue, ids, matcher.start(), matcher.end());
            }
        }
        return null;
    }

    protected void addPattern(String pattern) {
        MODEL_PATTERNS.add(Pattern.compile(pattern));
    }
    
    /**
     * 初始化型号模式
     */
    private static void initModelPatterns() {
        // 三个主流品牌
        initAppleModelPatterns();
        initHuaweiModelPatterns();
        initXiaomiModelPatterns();

        // 手机
        initOppoModelPatterns();
        initVivoModelPatterns();
        initSamsungModelPatterns();
        initOnePlusModelPatterns();
        initHonorModelPatterns();
        initIqooModelPatterns();
        initRealmeModelPatterns();
        initNubiaModelPatterns();
        initOtherBrandsModelPatterns();

        // 笔记本
        initNotepadModelPatterns();
    }

    private static void initAppleModelPatterns() {
        // iPhone 系列: iphone + 数字 + 可选后缀 (pro max, pro, plus, mini, se)
        MODEL_PATTERNS.add(Pattern.compile("(?i)(iphone|苹果)\\s*(\\d{1,2}\\s*([se]|se)?\\s*\\d*)\\s*(pro\\s*max|pro|plus|mini|max|ultra|air)?"));

        // iPad 系列: ipad + 可选系列名 + 可选代数 + 可选尺寸
        // 匹配: ipad mini, ipad air2, ipad 5代, ipad pro 1代 12.9英寸, ipad (m4) 11英寸
        MODEL_PATTERNS.add(Pattern.compile("(?i)ipad\\s*(pro|air|mini)?\\s*(\\d+代|\\d+|\\(m\\d+\\))?\\s*(\\d+(\\.\\d+)?英寸)?"));

        // MacBook 系列: macbook + 可选系列名 + 可选芯片 + 可选年份 + 可选尺寸
        // 匹配: macbook air, macbook pro m1 20年 13寸, macbook 15年 12寸
        MODEL_PATTERNS.add(Pattern.compile("(?i)(apple-)?macbook\\s*(pro|air)?\\s*(m\\d+)?\\s*(\\d{2}年|\\d{4}款)?\\s*(\\d+寸)?"));

        // Apple Watch 系列: apple watch + 可选系列/型号
        // 匹配: apple watch series 6, apple watch se, apple watch ultra 2, apple watch nike+ (series 4)
        MODEL_PATTERNS.add(Pattern.compile("(?i)apple\\s*watch\\s*(series\\s*\\d+|se\\s*\\d*|ultra\\s*\\d*|nike\\+?\\s*(\\(series\\s*\\d+\\))?)?"));

        // AirPods 系列: airpods + 可选型号 + 可选配件
        // 匹配: airpods pro 2, airpods max, airpods 3, airpods pro 2 单耳, airpods 2 充电仓
        MODEL_PATTERNS.add(Pattern.compile("(?i)airpods\\s*(pro\\s*\\d*|max|\\d+)?\\s*(\\(支持主动降噪\\)|\\(不支持主动降噪\\))?\\s*(单耳|充电仓)?"));

        // Apple Pencil 系列
        // 匹配: apple pencil (第一代), apple pencil pro, apple pencil (usb-c)
        MODEL_PATTERNS.add(Pattern.compile("(?i)apple\\s*pencil\\s*(pro|\\(usb-c\\)|（第[一二]代）|\\(第[一二]代\\))?"));

        // HomePod 系列
        // 匹配: homepod, homepod mini, homepod （第二代）
        MODEL_PATTERNS.add(Pattern.compile("(?i)homepod\\s*(mini|（第二代）)?"));

        // Mac Mini / Mac Studio 系列
        // 匹配: mac mini m1 2020年, mac studio 2023年
        MODEL_PATTERNS.add(Pattern.compile("(?i)mac\\s*(mini|studio)\\s*(m\\d+)?\\s*(\\d{4}年)?"));
    }

    /**
     * 初始化华为型号模式
     */
    private static void initHuaweiModelPatterns() {
        // Mate 系列: mate + 数字 + 可选后缀 (pro, pro+, rs, e)
        // 匹配: mate 10, mate 20 pro, mate 30 rs 保时捷设计, mate 40 pro+ (5g版), mate x, mate xs, mate x2
        MODEL_PATTERNS.add(Pattern.compile("(?i)华为\\s*mate\\s*(x[s\\d]*|\\d{1,3}[e]?)?\\s*(pro[+]?|rs|e)?\\s*(保时捷设计|典藏版)?\\s*(\\([45]g版\\))?"));
        
        // P 系列: p + 数字 + 可选后缀 (pro, pro+, art, pocket)
        // 匹配: p10 plus, p20, p30 pro, p40 pro+, p50, p60 art, p50 pocket
        MODEL_PATTERNS.add(Pattern.compile("(?i)华为\\s*p\\s*(\\d{2})\\s*[e]?\\s*(pro[+]?|plus|art|pocket)?\\s*(\\(骁龙版\\)|\\(麒麟版\\))?"));
        
        // Pura 系列: pura + 数字 + 可选后缀
        // 匹配: pura 70, pura 70 pro, pura 70 pro+, pura 70 ultra, pura 80, pura x
        MODEL_PATTERNS.add(Pattern.compile("(?i)华为\\s*pura\\s*(x|\\d{2})?\\s*(pro[+]?|ultra)?\\s*(北斗卫星消息版)?"));
        
        // Nova 系列: nova + 数字 + 可选字母 + 可选后缀
        // 匹配: nova 2s, nova 3, nova 3i, nova 4e, nova 5 pro, nova 6 se, nova 7 se (5g版), nova flip
        MODEL_PATTERNS.add(Pattern.compile("(?i)华为\\s*nova\\s*(\\d{1,2}[ei]?|flip)?\\s*[se]?\\s*(pro|plus|se|ultra)?\\s*(\\([45]g版\\)|\\(乐活版\\)|\\(活力版\\)|\\(星耀版\\))?"));
        
        // 畅享系列: 畅享 + 数字 + 可选字母 + 可选后缀
        // 匹配: 畅享9 plus, 畅享10, 畅享10s, 畅享 max, 畅享z, 畅享20 pro, 畅享50, 畅享60x, 畅享70, 畅享80
        MODEL_PATTERNS.add(Pattern.compile("(?i)华为\\s*畅享\\s*(\\d{1,2}[ez]?[sx]?)?\\s*(pro|plus|se|max)?\\s*(\\([45]g版\\)|\\(活力版\\))?"));
        
        // 麦芒系列: 麦芒 + 数字
        // 匹配: 麦芒6
        MODEL_PATTERNS.add(Pattern.compile("(?i)华为\\s*麦芒\\s*(\\d+)"));
        
        // Pocket 系列: pocket + 可选后缀
        // 匹配: pocket s, pocket 2, pocket 2 优享版
        MODEL_PATTERNS.add(Pattern.compile("(?i)华为\\s*pocket\\s*([s\\d]+)?\\s*(优享版)?"));
        
        // MatePad 系列: matepad + 可选型号 + 可选尺寸 + 可选款式
        // 匹配: matepad, matepad pro, matepad air, matepad se, matepad pro 10.8英寸, matepad 11英寸 2023款, matepad paper
        MODEL_PATTERNS.add(Pattern.compile("(?i)华为\\s*matepad\\s*(pro|air|se|paper)?\\s*(\\d+(\\.\\d+)?英寸)?\\s*(\\d{4}款)?\\s*(\\(柔光版\\)|\\(性能版\\)|\\(悦动版\\)|\\(灵动版\\)|\\(标准版\\)|\\(活力版\\))?"));
        
        // 华为平板 M 系列: 华为平板 m + 数字 + 可选尺寸
        // 匹配: 华为平板 m3 8.4英寸, 华为平板 m5 10.1英寸（青春版）, 华为平板 m6 8.4英寸 （高能版）
        MODEL_PATTERNS.add(Pattern.compile("(?i)华为\\s*平板\\s*m\\s*[\\d]+\\s*(\\d+(\\.\\d+)?英寸)?\\s*(（青春版）|（高能版）)?"));
        
        // 华为揽阅 M 系列: 华为揽阅 m + 数字 + 尺寸
        // 匹配: 华为揽阅 m2 7英寸 （青春版）, 华为揽阅 m2 10.1英寸
        MODEL_PATTERNS.add(Pattern.compile("(?i)华为揽阅\\s*m\\s*[\\d]+\\s*(\\d+(\\.\\d+)?英寸)?\\s*(（青春版）)?"));
        
        // 畅享平板系列
        // 匹配: 畅享平板 10.1英寸, 畅享平板2
        MODEL_PATTERNS.add(Pattern.compile("(?i)华为\\s*畅享平板\\s*[\\d]*\\s*(\\d+(\\.\\d+)?英寸)?"));
        
        // MateBook 系列: matebook + 系列名 + 可选尺寸/型号 + 可选年份/版本
        // 匹配: matebook d 14, matebook x pro, matebook 13s, matebook e go 2022, matebook d 16 se 2024, matebook d 14 linux版
        MODEL_PATTERNS.add(Pattern.compile("(?i)华为\\s*matebook\\s*([dxeb])?\\s*(go)?\\s*([\\d]+[s]?)?\\s*(pro)?\\s*(\\d{4})?\\s*(se)?\\s*(\\d{4})?\\s*(linux版)?\\s*(\\(微绒典藏版\\))?"));
        
        // 擎云系列: 擎云 + 型号
        // 匹配: 擎云 g540, 擎云 s520, 擎云 s520 gen2, 擎云 h7546, 擎云 g740, 擎云 h5546
        MODEL_PATTERNS.add(Pattern.compile("(?i)华为\\s*擎云\\s*([ghsb][\\d]+)\\s*(gen[\\d]+)?"));
        
        // Watch 系列: watch + 可选型号 + 可选款式
        // 匹配: watch 2, watch 3, watch 4, watch 5, watch d, watch d2, watch gt, watch gt 2, watch fit, watch buds, watch ultimate
        MODEL_PATTERNS.add(Pattern.compile("(?i)华为\\s*watch\\s*(gt|fit|buds|ultimate|d[\\d]?)?\\s*([\\d]+[e]?)?\\s*(pro|mini|cyber|runner|new)?\\s*(运动款|时尚款|活力款|雅致款|尊享款|典藏版|ecg款|保时捷设计款|新年款)?"));
        
        // 儿童手表系列: 儿童手表 + 数字 + 可选后缀
        // 匹配: 儿童手表 3 pro, 儿童手表 3s, 儿童手表 4x, 儿童手表 5, 儿童手表 5x pro
        MODEL_PATTERNS.add(Pattern.compile("(?i)华为\\s*儿童手表\\s*([\\d]+[sx]?)?\\s*(pro)?\\s*(超能版|新耀款|活力版)?"));
        
        // FreeBuds 系列: freebuds + 可选型号 + 可选版本
        // 匹配: freebuds 3, freebuds 4, freebuds 4i, freebuds 4e, freebuds 5, freebuds 5i, freebuds 6, freebuds 6i
        //       freebuds pro, freebuds pro 2, freebuds pro 3, freebuds pro 4, freebuds se, freebuds se2
        //       freebuds studio, freebuds lipstick, freebuds lipstick 2
        MODEL_PATTERNS.add(Pattern.compile("(?i)华为\\s*freebuds\\s*(pro|se|studio|lipstick)?\\s*([\\d]+[ei]?)?\\s*([+])?\\s*(\\(有线充版\\)|至臻版|标准版|悦享版)?"));
        
        // FreeLace 系列: freelace + 可选后缀
        // 匹配: freelace, freelace pro, freelace pro 2, freelace 活力版
        MODEL_PATTERNS.add(Pattern.compile("(?i)华为\\s*freelace\\s*(pro)?\\s*([\\d]+)?\\s*(活力版)?"));
        
        // FreeClip 系列
        // 匹配: freeclip
        MODEL_PATTERNS.add(Pattern.compile("(?i)华为\\s*freeclip"));
        
        // AI 音箱系列: ai 音箱 + 可选数字
        // 匹配: ai 音箱, ai 音箱2
        MODEL_PATTERNS.add(Pattern.compile("(?i)华为\\s*ai\\s*音箱\\s*([\\d]+)?"));
        
        // Sound 系列: sound + 可选型号
        // 匹配: sound, sound x, sound x4, sound joy, sound joy 2 智能版, sound x 鎏金剧院版
        MODEL_PATTERNS.add(Pattern.compile("(?i)华为\\s*sound\\s*(x[\\d]*|joy)?\\s*([\\d]+)?\\s*(智能版|蓝牙版|鎏金剧院版)?"));
        
        // M-Pencil / M-Pen 系列: m-pencil / m-pen + 可选版本
        // 匹配: m-pencil, m-pencil 第二代触控笔, m-pencil (第三代), m-pen lite, m-pen 2
        MODEL_PATTERNS.add(Pattern.compile("(?i)华为\\s*m-(pencil|pen)\\s*(lite)?\\s*([\\d]+)?\\s*(第[一二三]代触控笔|\\(第[一二三]代\\))?"));
    }
    
    /**
     * 初始化小米型号模式
     */
    private static void initXiaomiModelPatterns() {
        // 小米数字系列: 小米 + 数字 + 可选后缀 (pro, se, ultra, s, 青春版, 至尊纪念版等)
        // 匹配: 小米 6, 小米 8 se, 小米 9 pro, 小米 10 青春版 (5g版), 小米 11 ultra, 小米 12s ultra, 小米 13 ultra, 小米 14 pro, 小米 15 pro
        MODEL_PATTERNS.add(Pattern.compile("(?i)小米\\s*(\\d{1,2}[s]?)\\s*(pro|se|ultra|lite|zoom)?\\s*(\\(5g版\\)|\\(后指纹版\\)|\\(屏幕指纹版\\)|\\(青春版\\)|\\(至尊纪念版\\)|\\(青春活力版\\))?"));
        
        // 小米 Mix 系列: 小米 mix + 可选型号 + 可选版本
        // 匹配: 小米 mix 2s, 小米 mix 4, 小米 mix fold, 小米 mix fold 2, 小米 mix fold 3, 小米 mix fold 4, 小米 mix flip, 小米 mix flip 2
        MODEL_PATTERNS.add(Pattern.compile("(?i)小米\\s*mix\\s*(fold|flip)?\\s*([\\d]+[s]?)?\\s*(\\(5g版\\))?"));
        
        // 小米 CC 系列: 小米 cc + 数字 + 可选后缀
        // 匹配: 小米 cc 9, 小米 cc 9e
        MODEL_PATTERNS.add(Pattern.compile("(?i)小米\\s*cc\\s*(\\d+[e]?)"));
        
        // 小米 Civi 系列: 小米 civi + 可选数字 + 可选后缀
        // 匹配: 小米 civi, 小米 civi 1s, 小米 civi 2, 小米 civi 3, 小米 civi 4 pro, 小米 civi 5 pro
        MODEL_PATTERNS.add(Pattern.compile("(?i)小米\\s*civi\\s*(\\d+[s]?)?\\s*(pro)?"));
        
        // 小米 Max/Play/Note 系列
        // 匹配: 小米 max, 小米 max 3, 小米 play, 小米 note 3, 小米 6x
        MODEL_PATTERNS.add(Pattern.compile("(?i)小米\\s*(max|play|note)\\s*(\\d+)?"));
        MODEL_PATTERNS.add(Pattern.compile("(?i)小米\\s*(\\d+)\\s*x"));
        
        // 红米系列: 红米 + 数字 + 可选字母
        // 匹配: 红米 6, 红米 6a, 红米 note 5
        MODEL_PATTERNS.add(Pattern.compile("(?i)红米\\s*(\\d+[a]?)"));
        MODEL_PATTERNS.add(Pattern.compile("(?i)红米\\s*note\\s*(\\d+)"));
        
        // Redmi 数字系列: redmi + 数字 + 可选字母 + 可选版本
        // 匹配: redmi 8a, redmi 9, redmi 9a, redmi 10a, redmi 10x, redmi 12, redmi 12c, redmi 13c 5g, redmi 13r, redmi 14r 5g, redmi 14c
        MODEL_PATTERNS.add(Pattern.compile("(?i)redmi\\s*(\\d{1,2}[acr]?)\\s*(\\d{1,2})?\\s*(5g|\\(5g版\\)|\\(4g版\\))?"));
        
        // Redmi K 系列: redmi k + 数字 + 可选后缀
        // 匹配: redmi k20 pro, redmi k20 pro (尊享版), redmi k30, redmi k30 (5g版), redmi k30 pro, redmi k30s, 
        //       redmi k40, redmi k40 pro, redmi k40 pro+, redmi k40 (游戏增强版), redmi k40s
        //       redmi k50, redmi k50 pro, redmi k50 (电竞版), redmi k50 (5g 至尊版)
        //       redmi k60, redmi k60 pro, redmi k60e, redmi k60 (至尊版)
        //       redmi k70, redmi k70 pro, redmi k70e, redmi k70 (至尊版)
        //       redmi k80, redmi k80 pro, redmi k80 (至尊版)
        MODEL_PATTERNS.add(Pattern.compile("(?i)redmi\\s*k\\s*(\\d{2}[is]?[e]?)\\s*(pro[+]?)?\\s*(\\(5g版\\)|\\(5g\\s*极速版\\)|\\(5g\\s*变焦版\\)|\\(尊享版\\)|\\(至尊纪念版\\)|\\(至尊版\\)|\\(游戏增强版\\)|\\(电竞版\\)|\\(5g\\s*至尊版\\))?"));
        
        // Redmi Note 系列: redmi note + 数字 + 可选后缀
        // 匹配: redmi note 7, redmi note 7 pro, redmi note 8, redmi note 8 pro, redmi note 9, redmi note 9 pro
        //       redmi note 10, redmi note 10 pro, redmi note 11, redmi note 11 pro, redmi note 11 pro+, redmi note 11t pro, redmi note 11t pro+, redmi note 11r, redmi note 11se
        //       redmi note 12, redmi note 12 pro, redmi note 12 pro+, redmi note 12 turbo, redmi note 12 pro 极速版, redmi note 12 探索版, redmi note 12r, redmi note 12t pro
        //       redmi note 13, redmi note 13 pro, redmi note 13 pro+, redmi note 13r
        //       redmi note 14, redmi note 14 pro, redmi note 14 pro+, redmi note 15 pro, redmi note 15 pro+
        MODEL_PATTERNS.add(Pattern.compile("(?i)redmi\\s*note\\s*(\\d{1,2})\\s*([rt])?\\s*(pro[+]?|se)?\\s*(\\(5g版\\)|\\(4g版\\)|极速版|探索版)?"));
        
        // Redmi Turbo 系列: redmi turbo + 数字 + 可选后缀
        // 匹配: redmi turbo 3, redmi turbo 4, redmi turbo 4 pro
        MODEL_PATTERNS.add(Pattern.compile("(?i)redmi\\s*turbo\\s*(\\d+)\\s*(pro)?"));
        
        // 小米平板系列: 小米平板 + 可选数字 + 可选后缀 + 可选尺寸
        // 匹配: 小米平板, 小米平板 4, 小米平板 4 plus, 小米平板 5, 小米平板 5 pro, 小米平板 5 pro 12.4英寸, 小米平板 6, 小米平板 6 pro
        //       小米平板 7, 小米平板 7 pro, 小米平板 7 (柔光版), 小米平板 7 pro (柔光版)
        //       小米 pad 6 max 14, 小米 pad 6s pro 12.4, 小米 pad 7, 小米 pad 7 pro
        MODEL_PATTERNS.add(Pattern.compile("(?i)小米\\s*(平板|pad)\\s*(\\d+)?\\s*(pro|plus|max)?\\s*(\\d+(\\.\\d+)?)?\\s*(\\(柔光版\\))?"));
        
        // Redmi Pad 系列: redmi pad + 可选后缀
        // 匹配: redmi pad, redmi pad se, redmi pad pro
        MODEL_PATTERNS.add(Pattern.compile("(?i)redmi\\s*pad\\s*(se|pro)?"));
        
        // 小米笔记本系列: 小米 + 笔记本/book + 系列名 + 可选尺寸 + 可选版本
        // 匹配: 小米笔记本 15.6寸, 小米笔记本air 12.5寸, 小米笔记本air 13.3寸, 小米笔记本 pro 14, 小米笔记本 pro 15, 小米笔记本 pro x 14, 小米笔记本 pro x 15, 小米笔记本 pro 15 oled
        //       小米 book air 13, 小米 book pro 14, 小米 book pro 16, 小米 book 12.4 二合一
        //       小米游戏本 15.6寸
        MODEL_PATTERNS.add(Pattern.compile("(?i)小米\\s*(笔记本|book|游戏本)\\s*(air|pro)?\\s*(x)?\\s*(\\d+(\\.\\d+)?|\\d+(\\.\\d+)?寸|\\d+(\\.\\d+)?英寸)?\\s*(oled|二合一)?"));
        
        // RedmiBook 系列: redmibook + 可选系列名 + 尺寸 + 可选版本 + 可选年份
        // 匹配: redmibook 13, redmibook 14, redmibook 14 ii, redmibook 15e, redmibook 16
        //       redmibook air 13, redmibook pro 14, redmibook pro 15, redmibook pro 14 2024, redmibook pro 15 2023, redmibook pro 16 2024
        //       redmi g, redmi g pro, redmi g 游戏本, redmi g pro 2024
        //       redmi book 14 2024, redmi book 16 2024, redmi book 14 2025, redmi book 16 2025
        //       redmi book pro 14 2024, redmi book pro 16 2024, redmi book pro 14 2025, redmi book pro 16 2025
        MODEL_PATTERNS.add(Pattern.compile("(?i)redmi\\s*(book|g)\\s*(air|pro)?\\s*(\\d{2})?\\s*([\\d]{1,2})?\\s*(ii|游戏本|[e])?\\s*(\\d{4})?"));
        
        // 小米 Watch 系列: 小米 watch + 型号 + 可选版本
        // 匹配: 小米 watch s1, 小米 watch s2, 小米 watch s3, 小米 watch s4, 小米 watch s4 sport
        //       小米 watch h1, 小米 watch h1 e, 小米 watch color, 小米 watch color 2
        //       小米手表 color, 小米手表 color 运动版, 小米手表 xmwt01
        MODEL_PATTERNS.add(Pattern.compile("(?i)小米\\s*(watch|手表)\\s*([sh]\\d+|color|xmwt\\d+)?\\s*([e]|sport|运动版)?"));
        
        // Redmi Watch 系列: redmi watch + 可选数字
        // 匹配: redmi 手表, redmi watch, redmi watch 2, redmi watch 3, redmi watch 5
        MODEL_PATTERNS.add(Pattern.compile("(?i)redmi\\s*(watch|手表)\\s*(\\d+)?"));
        
        // 小米米兔儿童手表系列: 小米 米兔儿童电话手表/学习手表 + 型号
        // 匹配: 小米 米兔儿童电话手表2s, 小米 米兔儿童电话手表3, 小米 米兔儿童电话手表3c, 小米 米兔儿童电话手表5c, 小米 米兔儿童电话手表5 pro
        //       小米 米兔儿童学习手表6, 小米 米兔儿童学习手表6c, 小米 米兔儿童学习手表6x, 小米 米兔儿童电话手表u1, 小米 米兔儿童电话手表c7a
        //       小米 小寻儿童电话手表a5
        MODEL_PATTERNS.add(Pattern.compile("(?i)小米\\s*(米兔|小寻)儿童(电话手表|学习手表)\\s*([\\d]+[scx]?|[a-z]\\d+[a]?|u\\d+)\\s*(pro)?"));
        
        // 小米 Buds 系列: 小米 buds + 数字 + 可选后缀
        // 匹配: 小米 buds 3t pro, 小米 buds 4, 小米 buds 4 pro, 小米 buds 5, 小米 buds 5 pro
        //       小米 flipbuds pro, 小米 air 3 se
        MODEL_PATTERNS.add(Pattern.compile("(?i)小米\\s*(buds|flipbuds|air)\\s*(\\d+[t]?)\\s*(se|pro)?"));
        
        // Redmi Buds 系列: redmi buds + 数字 + 可选版本
        // 匹配: redmi buds 3, redmi buds 3 青春版, redmi buds 4, redmi buds 4 pro, redmi buds 4 青春版, redmi buds 4 活力版
        //       redmi buds 5, redmi buds 5 pro, redmi buds 6 pro
        MODEL_PATTERNS.add(Pattern.compile("(?i)redmi\\s*buds\\s*(\\d+)\\s*(pro|青春版|活力版)?"));
        
        // Redmi AirDots 系列: redmi airdots + 数字 + 可选版本
        // 匹配: redmi airdots 2, redmi airdots 3, redmi airdots 3 pro, redmi airdots 3 pro 原神版
        MODEL_PATTERNS.add(Pattern.compile("(?i)redmi\\s*airdots\\s*(\\d+)\\s*(pro)?\\s*(原神版)?"));
        
        // 小米真无线耳机系列: 小米 真无线 + 类型 + 耳机 + 型号
        // 匹配: 小米 真无线蓝牙耳机air, 小米 真无线蓝牙耳机air2, 小米 真无线蓝牙耳机air 2s, 小米 真无线蓝牙耳机air2 se, 小米 真无线蓝牙耳机air2 pro
        //       小米 真无线降噪耳机 3, 小米 真无线降噪耳机 3 pro
        MODEL_PATTERNS.add(Pattern.compile("(?i)小米\\s*真无线(蓝牙|降噪)耳机\\s*(air)?\\s*([\\d]+[s]?)\\s*(se|pro)?"));
        
        // 小米其他耳机: 骨传导耳机, 开放式耳机
        // 匹配: 小米 骨传导耳机, 小米 开放式耳机
        MODEL_PATTERNS.add(Pattern.compile("(?i)小米\\s*(骨传导|开放式)耳机"));
        
        // 小米 Sound 系列: 小米 sound + 可选型号
        // 匹配: 小米 sound, 小米 sound move, 小米 sound pro
        MODEL_PATTERNS.add(Pattern.compile("(?i)小米\\s*sound\\s*(move|pro)?"));
        
        // 小米小爱音箱系列: 小米 小爱音箱 + 可选型号
        // 匹配: 小米 小爱音箱 pro, 小米 小爱触屏音箱pro 8
        MODEL_PATTERNS.add(Pattern.compile("(?i)小米\\s*小爱(音箱|触屏音箱)\\s*(pro)?\\s*(\\d+)?"));
        
        // 小米蓝牙音箱: 小米 蓝牙音箱 + 型号
        // 匹配: 小米 蓝牙音箱 (asm02a), 小米 户外蓝牙音箱 camp
        MODEL_PATTERNS.add(Pattern.compile("(?i)小米\\s*(蓝牙音箱|户外蓝牙音箱)\\s*(\\([a-z0-9]+\\)|camp)?"));
        
        // 小米智能家庭屏: 小米 智能家庭屏 + 数字
        // 匹配: 小米 智能家庭屏 6, 小米 智能家庭屏 10
        MODEL_PATTERNS.add(Pattern.compile("(?i)小米\\s*智能家庭屏\\s*(\\d+)"));
        
        // 小米米家投影仪系列: 小米 米家投影仪/激光投影 + 可选版本
        // 匹配: 小米 米家投影仪, 小米 米家投影仪 (青春版), 小米 米家投影仪 (青春版2), 小米 米家投影仪 (青春版2s)
        //       小米 米家激光投影电视 150英寸 主机, 小米 米家激光投影电视 4k, 小米 激光投影仪 1s
        MODEL_PATTERNS.add(Pattern.compile("(?i)小米\\s*(米家)?(激光)?投影(仪|电视)\\s*(\\d+[s]?)?\\s*(\\(青春版[\\d]*[s]?\\)|\\d+英寸\\s*主机|4k)?"));
        
        // Redmi 投影仪系列: redmi 投影仪 + 可选型号
        // 匹配: redmi 投影仪, redmi 投影仪 (lite版), redmi 投影仪 pro, redmi 投影仪 2, redmi 投影仪 2 pro, redmi 投影仪 3 pro
        MODEL_PATTERNS.add(Pattern.compile("(?i)redmi\\s*投影仪\\s*(\\d+)?\\s*(pro|\\(lite版\\))?"));
        
        // 小米触控笔系列: 小米 + 触控笔名称 + 可选代数
        // 匹配: 小米 灵感触控笔 (第一代), 小米 灵感触控笔 (第二代), 小米 焦点触控笔
        MODEL_PATTERNS.add(Pattern.compile("(?i)小米\\s*(灵感|焦点)触控笔\\s*(\\(第[一二]代\\))?"));
        
        // 小米无人机: 小米无人机 + 版本
        // 匹配: 小米无人机 4k版
        MODEL_PATTERNS.add(Pattern.compile("(?i)小米无人机\\s*(4k版)?"));
    }

    /**
     * 初始化OPPO型号模式
     */
    private static void initOppoModelPatterns() {
        // OPPO Reno 系列: reno + 可选数字 + 可选后缀
        // 匹配: reno, reno z, reno2, reno2 z, reno ace, reno3, reno4, reno4 se, reno5, reno5 pro, reno5 pro+, reno5 k
        //       reno6, reno6 pro, reno6 pro+, reno7, reno7 pro, reno7 se, reno8, reno8 pro, reno8 pro+
        //       reno9, reno9 pro, reno9 pro+, reno10, reno10 pro, reno10 pro+, reno11, reno11 pro, reno12, reno12 pro
        //       reno13, reno13 pro, reno14, reno14 pro
        MODEL_PATTERNS.add(Pattern.compile("(?i)oppo\\s*reno\\s*(\\d{1,2})?\\s*([z])?\\s*(pro[+]?|se|ace|k)?\\s*(\\([45]g版\\)|\\([45]g\\s*元气版\\))?"));
        
        // OPPO Find X 系列: find x + 数字 + 可选后缀
        // 匹配: find x2, find x3, find x3 pro, find x5, find x5 pro, find x6, find x6 pro
        //       find x7, find x7 ultra, find x8, find x8 pro, find x8 ultra, find x8s, find x8s+
        MODEL_PATTERNS.add(Pattern.compile("(?i)oppo\\s*find\\s*x\\s*(\\d+)\\s*([s][+]?)?\\s*(pro|ultra)?\\s*(\\(摄影师版\\))?"));
        
        // OPPO Find N 系列（折叠屏）: find n + 可选数字 + 可选后缀
        // 匹配: find n, find n2, find n2 flip, find n3, find n3 flip, find n5
        MODEL_PATTERNS.add(Pattern.compile("(?i)oppo\\s*find\\s*n\\s*(\\d+)?\\s*(flip)?"));
        
        // OPPO A 系列: a + 数字 + 可选字母
        // 匹配: a7, a8, a9, a9x, a11, a11x, a32, a35, a36, a51, a52, a53, a55, a56, a57, a58, a58x, a72, a79, a91, a92s, a93, a93s, a95, a96, a97
        //       a1, a1 pro, a1 活力版, a1x, a2, a2 pro, a2x, a2m, a3, a3 pro, a3x, a3i, a5, a5 pro, a5x, a5 (活力版)
        MODEL_PATTERNS.add(Pattern.compile("(?i)oppo\\s*a\\s*(\\d{1,2}[sxim]?)\\s*(pro[+]?)?\\s*(\\([45]g版\\)|\\(活力版\\)|活力版)?"));
        
        // OPPO K 系列: k + 数字 + 可选后缀
        // 匹配: k1, k3, k5, k7, k9, k9 pro, k9s, k9x, k10, k10 pro, k10x, k10 (活力版), k11, k11x, k12, k12x, k12 plus, k12s
        //       k13 turbo, k13 turbo pro
        MODEL_PATTERNS.add(Pattern.compile("(?i)oppo\\s*k\\s*(\\d{1,2}[sx]?)\\s*(pro|plus|turbo)?\\s*(pro)?\\s*(\\(活力版\\))?"));
        
        // OPPO R 系列: r + 数字 + 可选后缀
        // 匹配: r9s, r11, r15x, r17
        MODEL_PATTERNS.add(Pattern.compile("(?i)oppo\\s*r\\s*(\\d{1,2}[sx]?)"));
        
        // OPPO Ace 系列: ace + 数字
        // 匹配: ace2
        MODEL_PATTERNS.add(Pattern.compile("(?i)oppo\\s*ace\\s*(\\d+)\\s*(\\([45]g版\\))?"));
    }

    /**
     * 初始化Vivo型号模式
     */
    private static void initVivoModelPatterns() {
        // Vivo X 数字系列: x + 数字 + 可选后缀
        // 匹配: x20, x20 plus, x21, x21s, x23, x27, x27 pro, x30, x50, x60, x60 pro
        //       x70, x70 pro, x70 pro+, x70t, x80, x80 pro, x90, x90 pro, x90 pro+, x90s
        //       x100, x100 pro, x100s, x100s pro, x100 ultra, x200, x200 pro, x200 pro mini, x200 ultra, x200s
        MODEL_PATTERNS.add(Pattern.compile("(?i)vivo\\s*x\\s*(\\d{2,3}[st]?)\\s*(pro[+]?|plus|ultra|mini)?\\s*(\\(骁龙版\\)|（天玑\\d+版）|\\(后指纹版\\)|\\(直屏版\\))?"));
        
        // Vivo X Fold 系列: x fold + 可选数字/后缀
        // 匹配: x fold, x fold+, x fold 2, x fold 3, x fold 3 pro, x fold 5
        MODEL_PATTERNS.add(Pattern.compile("(?i)vivo\\s*x\\s*fold\\s*([+\\d]+)?\\s*(pro)?"));
        
        // Vivo X Flip 系列: x flip
        MODEL_PATTERNS.add(Pattern.compile("(?i)vivo\\s*x\\s*flip"));
        
        // Vivo X Note 系列: x note
        MODEL_PATTERNS.add(Pattern.compile("(?i)vivo\\s*x\\s*note"));
        
        // Vivo S 系列: s + 数字 + 可选后缀
        // 匹配: s5, s6, s7, s7e, s9, s9e, s10, s10 pro, s12, s15, s15 pro, s15e, s16, s16 pro, s16e
        //       s17, s17 pro, s17e, s17t, s18, s18 pro, s18e, s19, s19 pro, s20, s20 pro, s30, s30 pro mini
        MODEL_PATTERNS.add(Pattern.compile("(?i)vivo\\s*s\\s*(\\d{1,2}[e]?)\\s*(pro)?\\s*(mini)?\\s*(\\([45]g版\\)|\\([45]g\\s*\\d+款\\)|\\([45]g\\s*活力版\\))?"));
        
        // Vivo Y 系列: y + 数字 + 可选后缀
        // 匹配: y3, y3s, y5s, y9s, y30, y31s, y33s, y35, y35m, y35+, y36, y36m, y36i, y36c, y37, y37m, y37 pro
        //       y50, y50m, y51s, y52s, y53s, y53t, y70s, y73s, y73t, y76s, y77, y77e, y78, y78+, y78m, y78t, y85
        //       y100, y100i, y200, y200i, y200 gt, y200t, y300, y300i, y300t, y300 pro, y300 pro+, y300 gt, y500
        MODEL_PATTERNS.add(Pattern.compile("(?i)vivo\\s*y\\s*(\\d{1,3}[simte+]?)\\s*(pro[+]?|gt)?\\s*(\\([45]g版\\)|标准版)?"));
        
        // Vivo Z 系列: z + 数字 + 可选后缀
        // 匹配: z1, z3, z3i, z5, z5i, z5x, z6
        MODEL_PATTERNS.add(Pattern.compile("(?i)vivo\\s*z\\s*(\\d+[ix]?)\\s*(\\(标准版\\))?"));
        
        // Vivo Nex 系列: nex + 数字 + 可选后缀
        // 匹配: nex 3, nex 3s
        MODEL_PATTERNS.add(Pattern.compile("(?i)vivo\\s*nex\\s*(\\d+[s]?)\\s*(\\([45]g版\\))?"));
        
        // Vivo T 系列: t + 数字 + 可选后缀
        // 匹配: t1, t2x
        MODEL_PATTERNS.add(Pattern.compile("(?i)vivo\\s*t\\s*(\\d+[x]?)"));
        
        // Vivo U 系列: u + 数字 + 可选后缀
        // 匹配: u3x
        MODEL_PATTERNS.add(Pattern.compile("(?i)vivo\\s*u\\s*(\\d+[x]?)"));
    }

    /**
     * 初始化三星型号模式
     */
    private static void initSamsungModelPatterns() {
        // 三星 Galaxy S 系列: galaxy s + 数字 + 可选后缀
        // 匹配: galaxy s8, galaxy s8+, galaxy s9, galaxy s9+, galaxy s10, galaxy s10+, galaxy s20 ultra
        //       galaxy s21, galaxy s21 ultra, galaxy s22, galaxy s22+, galaxy s22 ultra
        //       galaxy s23, galaxy s23+, galaxy s23 ultra, galaxy s23 fe
        //       galaxy s24, galaxy s24+, galaxy s24 ultra, galaxy s25, galaxy s25+, galaxy s25 ultra, galaxy s25 edge
        MODEL_PATTERNS.add(Pattern.compile("(?i)三星\\s*galaxy\\s*s\\s*(\\d{1,2})\\s*([+]|ultra|fe|edge)?\\s*(\\([45]g版\\))?"));
        
        // 三星 Galaxy Note 系列: galaxy note + 数字 + 可选后缀
        // 匹配: galaxy note 20, galaxy note 20 ultra
        MODEL_PATTERNS.add(Pattern.compile("(?i)三星\\s*galaxy\\s*note\\s*(\\d{1,2})\\s*(ultra)?\\s*(\\([45]g版\\))?"));
        
        // 三星 Galaxy Z Fold 系列: galaxy z fold + 数字
        // 匹配: galaxy z fold2, galaxy z fold3, galaxy z fold4, galaxy z fold5, galaxy z fold6, galaxy z fold7
        MODEL_PATTERNS.add(Pattern.compile("(?i)三星\\s*galaxy\\s*z\\s*fold\\s*(\\d+)\\s*(\\([45]g版\\))?"));
        
        // 三星 Galaxy Z Flip 系列: galaxy z flip + 数字 + 可选后缀
        // 匹配: galaxy z flip3, galaxy z flip4, galaxy z flip5, galaxy z flip6, galaxy z flip7, galaxy z flip7 fe
        MODEL_PATTERNS.add(Pattern.compile("(?i)三星\\s*galaxy\\s*z\\s*flip\\s*(\\d+)\\s*(fe)?\\s*(\\([45]g版\\))?"));
        
        // 三星 Galaxy A 系列: galaxy a + 数字
        // 匹配: galaxy a51, galaxy a52, galaxy a54, galaxy a55
        MODEL_PATTERNS.add(Pattern.compile("(?i)三星\\s*galaxy\\s*a\\s*(\\d{2})\\s*(\\([45]g版\\))?"));
        
        // 三星 Galaxy C 系列: galaxy c + 数字
        // 匹配: galaxy c55
        MODEL_PATTERNS.add(Pattern.compile("(?i)三星\\s*galaxy\\s*c\\s*(\\d{2})"));
        
        // 三星 W 系列（心系天下）: w + 数字 + 可选 flip
        // 匹配: w21, w22, w23, w23 flip, w24, w24 flip, w25, w25 flip
        MODEL_PATTERNS.add(Pattern.compile("(?i)三星\\s*w\\s*(\\d{2})\\s*(flip)?\\s*(\\([45]g版\\))?"));
    }

    /**
     * 初始化一加型号模式
     */
    private static void initOnePlusModelPatterns() {
        // 一加数字系列: 一加 + 数字 + 可选后缀
        // 匹配: 一加 7 pro, 一加 7t, 一加 8, 一加 8 pro, 一加 8t, 一加 9, 一加 9 pro, 一加 9r, 一加 9rt
        //       一加 10 pro, 一加 11, 一加 12, 一加 13, 一加 13t
        MODEL_PATTERNS.add(Pattern.compile("(?i)一加\\s*(\\d{1,2}[rt]?)\\s*(pro)?\\s*(\\([45]g版\\))?"));
        
        // 一加 Ace 系列: 一加 ace + 可选数字/后缀
        // 匹配: ace, ace (竞速版), ace 2, ace 2 pro, ace 2v, ace 3, ace 3 pro, ace 3v
        //       ace 5, ace 5 pro, ace 5 至尊版, ace 5 竞速版, ace pro
        MODEL_PATTERNS.add(Pattern.compile("(?i)一加\\s*ace\\s*(\\d+[v]?)?\\s*(pro)?\\s*(\\(竞速版\\)|\\(至尊版\\)|竞速版|至尊版)?\\s*(\\([45]g版\\))?"));
    }

    /**
     * 初始化荣耀型号模式
     */
    private static void initHonorModelPatterns() {
        // 荣耀数字系列: 荣耀 + 数字 + 可选后缀
        // 匹配: 荣耀 9, 荣耀 9i, 荣耀 10, 荣耀 10 (青春版), 荣耀 20, 荣耀 20i, 荣耀 20 pro, 荣耀 20s, 荣耀 20 (青春版)
        //       荣耀 30, 荣耀 30 pro, 荣耀 30 pro+, 荣耀 30s, 荣耀 30 青春版
        //       荣耀 50, 荣耀 50 se, 荣耀 50 pro, 荣耀 60, 荣耀 60 se, 荣耀 60 pro
        //       荣耀 70, 荣耀 70 pro, 荣耀 70 pro+, 荣耀 80, 荣耀 80 se, 荣耀 80 gt, 荣耀 80 pro, 荣耀 80 pro 直屏版
        //       荣耀 90, 荣耀 90 gt, 荣耀 90 pro, 荣耀 100, 荣耀 100 pro, 荣耀 200, 荣耀 200 pro
        //       荣耀 300, 荣耀 300 pro, 荣耀 300 ultra, 荣耀 400, 荣耀 400 pro
        MODEL_PATTERNS.add(Pattern.compile("(?i)荣耀\\s*(\\d{1,3}[is]?)\\s*(pro[+]?|se|gt|ultra)?\\s*(\\([45]g版\\)|\\(青春版\\)|青春版|直屏版|\\(至臻版\\))?"));
        
        // 荣耀 Magic 数字系列: magic + 数字 + 可选后缀
        // 匹配: magic3, magic3 (至臻版), magic3 pro, magic4, magic4 (至臻版), magic4 pro
        //       magic5, magic5 (至臻版), magic5 pro, magic6, magic6 (至臻版), magic6 pro, magic6 rsr 保时捷设计
        //       magic7, magic7 pro, magic7 rsr 保时捷设计
        MODEL_PATTERNS.add(Pattern.compile("(?i)荣耀\\s*magic\\s*(\\d+)\\s*(pro)?\\s*(rsr\\s*保时捷设计|\\(至臻版\\))?\\s*(\\([45]g款\\))?"));
        
        // 荣耀 Magic V 系列（折叠屏）: magic v + 可选数字/后缀
        // 匹配: magic v, magic v2, magic v2 (至臻版), magic v2 rsr 保时捷设计, magic vs, magic vs 至臻版, magic vs2
        //       magic v3, magic v5, magic v flip, magic v flip2
        MODEL_PATTERNS.add(Pattern.compile("(?i)荣耀\\s*magic\\s*v\\s*(s[\\d]?|flip[\\d]?|[\\d]+)?\\s*(rsr\\s*保时捷设计|\\(至臻版\\)|至臻版)?"));
        
        // 荣耀 X 系列: x + 数字 + 可选后缀
        // 匹配: x10, x10 max, x20, x30, x30i, x40, x40 i, x40 gt, x40 gt 竞速版, x50, x50i, x50i+, x50 gt, x50 pro
        //       x60, x60i, x60 pro, x60 gt, x70, x70i
        MODEL_PATTERNS.add(Pattern.compile("(?i)荣耀\\s*x\\s*(\\d{2}[i]?)\\s*([+])?\\s*(pro|max|gt)?\\s*(竞速版)?\\s*(\\([45]g版\\))?"));
        
        // 荣耀 V 系列: v + 数字 + 可选后缀
        // 匹配: v30, v30 pro, v40, v40 (轻奢版), v purse
        MODEL_PATTERNS.add(Pattern.compile("(?i)荣耀\\s*v\\s*(\\d{2}|purse)\\s*(pro)?\\s*(\\(轻奢版\\))?\\s*(\\([45]g版\\))?"));
        
        // 荣耀畅玩系列: 畅玩 + 数字 + 可选后缀
        // 匹配: 畅玩7c, 畅玩8c, 畅玩9a, 畅玩20, 畅玩20a, 畅玩20 pro, 畅玩30, 畅玩30 plus
        //       畅玩40, 畅玩40c, 畅玩40 plus, 畅玩50, 畅玩50 plus, 畅玩50m, 畅玩60, 畅玩60m, 畅玩60 plus, 畅玩70 plus
        MODEL_PATTERNS.add(Pattern.compile("(?i)荣耀\\s*畅玩\\s*(\\d{1,2}[acm]?)\\s*(pro|plus)?"));
        
        // 荣耀 Play 系列: play + 数字 + 可选后缀
        // 匹配: play3, play4, play4t, play4t pro, play5, play 5t, play 5t 活力版, play 7t, play 8t, play 9t, play 9c
        MODEL_PATTERNS.add(Pattern.compile("(?i)荣耀\\s*play\\s*(\\d+[tc]?)\\s*(pro)?\\s*(活力版)?\\s*(\\([45]g版\\))?"));
        
        // 荣耀 8X 系列: 8x + 可选后缀
        // 匹配: 8x, 8x max
        MODEL_PATTERNS.add(Pattern.compile("(?i)荣耀\\s*8x\\s*(max)?"));
        
        // 荣耀 GT 系列: gt + 可选后缀
        // 匹配: gt, gt pro
        MODEL_PATTERNS.add(Pattern.compile("(?i)荣耀\\s*gt\\s*(pro)?"));
        
        // 荣耀 Power 系列
        MODEL_PATTERNS.add(Pattern.compile("(?i)荣耀\\s*power"));
    }

    /**
     * 初始化IQOO型号模式
     */
    private static void initIqooModelPatterns() {
        // IQOO 数字系列: iqoo + 可选数字 + 可选后缀
        // 匹配: iqoo, iqoo 3, iqoo 5, iqoo 7, iqoo 8, iqoo 8 pro, iqoo 9, iqoo 9 pro
        //       iqoo 10, iqoo 10 pro, iqoo 11, iqoo 11 pro, iqoo 11s, iqoo 12, iqoo 12 pro, iqoo 13
        MODEL_PATTERNS.add(Pattern.compile("(?i)iqoo\\s*(\\d{1,2}[s]?)\\s*(pro)?\\s*(\\([45]g版\\))?"));
        
        // IQOO Neo 系列: neo + 可选数字/后缀
        // 匹配: neo, neo (855版), neo3, neo5, neo5 (活力版), neo5s, neo5 se, neo6, neo6 se
        //       neo7, neo7 (竞速版), neo7 se, neo8, neo8 pro, neo9, neo9 pro, neo9s pro, neo9s pro+
        //       neo10, neo10 pro, neo10 pro+
        MODEL_PATTERNS.add(Pattern.compile("(?i)iqoo\\s*neo\\s*(\\d+[s]?)?\\s*(pro[+]?|se)?\\s*(\\(855版\\)|\\(活力版\\)|\\(竞速版\\))?\\s*(\\([45]g版\\))?"));
        
        // IQOO Z 系列: z + 数字 + 可选后缀
        // 匹配: z1, z1x, z3, z5, z5x, z6, z6x, z7, z7i, z7x, z7x (m), z8, z8x
        //       z9, z9 turbo, z9 turbo+, z9 turbo (长续航版), z9x, z10 turbo, z10 turbo pro, z10x
        MODEL_PATTERNS.add(Pattern.compile("(?i)iqoo\\s*z\\s*(\\d+[ix]?)\\s*(turbo)?\\s*(pro|[+])?\\s*(\\([m]\\)|\\(长续航版\\))?\\s*(\\([45]g版\\))?"));
        
        // IQOO U 系列: u + 数字 + 可选后缀
        // 匹配: u1x
        MODEL_PATTERNS.add(Pattern.compile("(?i)iqoo\\s*u\\s*(\\d+[x]?)"));
        
        // IQOO Pro 系列: pro
        // 匹配: pro
        MODEL_PATTERNS.add(Pattern.compile("(?i)iqoo\\s*pro\\s*(\\([45]g版\\))?"));
    }

    /**
     * 初始化真我/Realme型号模式
     */
    private static void initRealmeModelPatterns() {
        // 真我 GT 系列: gt + 可选数字/后缀
        // 匹配: gt, gt (大师探索版), gt neo, gt neo (5g 闪速版), gt neo2, gt neo2t, gt neo3, gt neo 3 150w
        //       gt neo5 se, gt neo5 150w, gt neo5 240w, gt neo6, gt neo6 se
        //       gt2, gt2 (大师探索版), gt2 pro, gt5 150w, gt5 240w, gt5 pro, gt6, gt7, gt7 pro, gt7 pro 竞速版
        MODEL_PATTERNS.add(Pattern.compile("(?i)真我\\s*gt\\s*(neo)?\\s*([\\d]+)?\\s*(pro|se|150w|240w)?\\s*(\\([45]g款\\)|\\([45]g\\s*闪速版\\)|\\(大师探索版\\)|竞速版)?"));
        
        // 真我 Neo 系列: neo + 数字 + 可选后缀
        // 匹配: neo7, neo7 se, neo7 turbo, neo7x
        MODEL_PATTERNS.add(Pattern.compile("(?i)真我\\s*neo\\s*(\\d+[x]?)\\s*(se|turbo)?"));
        
        // 真我数字系列: 真我 + 数字 + 可选后缀
        // 匹配: 10 pro, 10 pro+, 11, 11 pro, 11 pro+, 12, 12 pro, 12 pro+, 12x, 13, 13 pro, 13 pro+
        MODEL_PATTERNS.add(Pattern.compile("(?i)真我\\s*(\\d{2}[x]?)\\s*(pro[+]?)?"));
        
        // 真我 Q 系列: q + 可选数字 + 可选后缀
        // 匹配: q, q2, q3, q3 pro, q3s, q5 pro
        MODEL_PATTERNS.add(Pattern.compile("(?i)真我\\s*q\\s*(\\d+[s]?)?\\s*(pro)?\\s*(\\([45]g版\\))?"));
        
        // 真我 V 系列: v + 数字 + 可选后缀
        // 匹配: v3, v5, v11s, v13, v15, v20, v23, v30, v30t, v60, v60 pro, v60s, v70s
        MODEL_PATTERNS.add(Pattern.compile("(?i)真我\\s*v\\s*(\\d{1,2}[st]?)\\s*(pro)?\\s*(\\([45]g版\\))?"));
        
        // 真我 X 系列: x + 可选数字/后缀
        // 匹配: x (青春版), x7 pro, x50 pro, x50 pro (5g 玩家版)
        MODEL_PATTERNS.add(Pattern.compile("(?i)真我\\s*x\\s*(\\d{1,2})?\\s*(pro)?\\s*(\\(青春版\\)|\\([45]g\\s*玩家版\\)|\\([45]g版\\))?"));
    }

    /**
     * 初始化努比亚型号模式
     */
    private static void initNubiaModelPatterns() {
        // 努比亚红魔系列: 红魔 + 可选数字 + 可选后缀
        // 匹配: 红魔 5g, 红魔 7, 红魔 7 pro, 红魔 7s, 红魔 7s pro, 红魔 8 pro, 红魔 8 pro+, 红魔 8s pro, 红魔 8s pro+
        //       红魔 9 pro, 红魔 9 pro+, 红魔 9s pro, 红魔 9s pro+, 红魔 10 pro, 红魔 10 pro+, 红魔 10 air, 红魔 10s pro, 红魔 10s pro+
        MODEL_PATTERNS.add(Pattern.compile("(?i)努比亚\\s*红魔\\s*(\\d{1,2}[s]?)\\s*(pro[+]?|air)?\\s*(5g|\\(5g版\\)|\\（5g版）)?"));
        
        // 努比亚 Z 系列: z + 数字 + 可选后缀
        // 匹配: z40 pro, z40s pro 80w, z50, z50 ultra, z50s pro, z60 ultra, z60 ultra 领先版, z60s pro, z70 ultra, z70s ultra
        MODEL_PATTERNS.add(Pattern.compile("(?i)努比亚\\s*z\\s*(\\d{2}[s]?)\\s*(pro|ultra)?\\s*(80w|领先版)?\\s*(\\([45]g版\\))?"));
        
        // 努比亚 Flip 系列: flip + 可选数字
        // 匹配: flip, flip 2
        MODEL_PATTERNS.add(Pattern.compile("(?i)努比亚\\s*flip\\s*(\\d+)?\\s*(\\([45]g版\\))?"));
        
        // 努比亚小牛
        MODEL_PATTERNS.add(Pattern.compile("(?i)努比亚\\s*小牛"));
    }

    /**
     * 初始化其他品牌型号模式
     */
    private static void initOtherBrandsModelPatterns() {
        // 黑鲨游戏手机: 黑鲨游戏手机 + 数字 + 可选后缀
        // 匹配: 黑鲨游戏手机 3, 4, 5, 5 pro, 5 rs
        MODEL_PATTERNS.add(Pattern.compile("(?i)黑鲨游戏手机\\s*(\\d+)\\s*(pro|rs)?\\s*(\\([45]g版\\))?"));
        
        // 华硕 ROG 游戏手机: rog 游戏手机 + 数字 + 可选后缀
        // 匹配: rog 游戏手机 3, 6, 7, 8, 8 pro, 9, 9 pro
        MODEL_PATTERNS.add(Pattern.compile("(?i)华硕\\s*rog\\s*游戏手机\\s*(\\d+)\\s*(pro)?"));
        
        // 魅族数字系列: 魅族 + 数字 + 可选后缀
        // 匹配: 魅族 17, 17 pro, 18, 18x, 20, 20 pro, 20 classic, 20 infinity 无界版, 21, 21 pro, 21 note
        MODEL_PATTERNS.add(Pattern.compile("(?i)魅族\\s*(\\d{2}[x]?)\\s*(pro|classic|infinity\\s*无界版)?\\s*(\\([45]g版\\))?"));
        
        // 魅族 Note 系列: note + 数字 + 可选后缀
        // 匹配: note 16, note 16 pro
        MODEL_PATTERNS.add(Pattern.compile("(?i)魅族\\s*note\\s*(\\d{2})\\s*(pro)?"));
        
        // 魅族魅蓝系列: 魅蓝 + 数字
        // 匹配: 魅蓝 20
        MODEL_PATTERNS.add(Pattern.compile("(?i)魅族\\s*魅蓝\\s*(\\d{2})"));
        
        // 魅族 Lucky 系列
        MODEL_PATTERNS.add(Pattern.compile("(?i)魅族\\s*lucky\\s*(\\d{2})"));
        
        // 摩托罗拉 Edge 系列: edge + 可选字母/数字
        // 匹配: edge s30, edge s pro, edge x30
        MODEL_PATTERNS.add(Pattern.compile("(?i)摩托罗拉\\s*edge\\s*([sx])\\s*(\\d{2}|pro)"));
        
        // 摩托罗拉 Razr 系列: razr + 数字 + 可选后缀
        // 匹配: razr 40, razr 40 ultra, razr 50, razr 50 ultra
        MODEL_PATTERNS.add(Pattern.compile("(?i)摩托罗拉\\s*razr\\s*(\\d{2})\\s*(ultra)?"));
        
        // 摩托罗拉 Moto 系列: moto + 字母 + 数字
        // 匹配: moto g54, moto g55, moto g75, moto s50
        MODEL_PATTERNS.add(Pattern.compile("(?i)摩托罗拉\\s*moto\\s*([gs])\\s*(\\d{2})"));
        
        // 摩托罗拉 S/X 系列: s/x + 数字 + 可选后缀
        // 匹配: s30 pro, s50, s50 neo, x40
        MODEL_PATTERNS.add(Pattern.compile("(?i)摩托罗拉\\s*([sx])\\s*(\\d{2})\\s*(pro|neo)?"));
        
        // 索尼 Xperia 系列: xperia + 数字 + 罗马数字
        // 匹配: xperia 1 iii, xperia 1 iv
        MODEL_PATTERNS.add(Pattern.compile("(?i)索尼\\s*xperia\\s*(\\d+)\\s*(i{1,4}|iv|v|vi)\\s*(\\([45]g版\\))?"));
        
        // 联想拯救者系列: 拯救者 y + 数字
        // 匹配: 拯救者 y70, 拯救者 y90
        MODEL_PATTERNS.add(Pattern.compile("(?i)联想\\s*拯救者\\s*y\\s*(\\d{2})\\s*(\\([45]g版\\))?"));
        
        // 联想 Z 系列: z + 数字
        // 匹配: z5
        MODEL_PATTERNS.add(Pattern.compile("(?i)联想\\s*z\\s*(\\d+)"));
        
        // Hi Nova 系列: hi nova + 数字 + 可选后缀
        // 匹配: hi nova 9, hi nova 9 se, hi nova 9 pro, hi nova 10, hi nova 10 pro, hi nova 11, hi nova12 se
        MODEL_PATTERNS.add(Pattern.compile("(?i)hi\\s*nova\\s*(\\d{1,2})\\s*(se|pro)?\\s*(\\([45]g版\\))?"));
        
        // 中兴 Axon 系列: axon + 数字 + 可选后缀
        // 匹配: axon 40 ultra
        MODEL_PATTERNS.add(Pattern.compile("(?i)中兴\\s*axon\\s*(\\d{2})\\s*(ultra)?"));
        
        // 中兴远航系列: 远航 + 数字
        // 匹配: 远航60
        MODEL_PATTERNS.add(Pattern.compile("(?i)中兴\\s*远航\\s*(\\d{2})"));
    }

    private static void initNotepadModelPatterns() {
        // ThinkPad 系列: thinkpad + 系列名 + 型号
        // 匹配: thinkpad x230, thinkpad x1 carbon, thinkpad t14, thinkpad e470, thinkpad p15v 2021
        //       thinkpad thinkbook 14, thinkbook 15, thinkbook 14+, thinkbook 16+
        MODEL_PATTERNS.add(Pattern.compile("(?i)thinkpad\\s*(thinkbook)?\\s*([xtelprw])?\\s*(\\d{1,3}[a-z]*)?\\s*(carbon|nano|yoga|隐士|tablet\\s*evo)?\\s*(\\d{4})?\\s*(gen\\d+)?"));

        // 戴尔 Inspiron 灵越系列: inspiron 灵越 + 数字 + 尺寸 + 型号
        // 匹配: inspiron 灵越 14 5425 系列, inspiron 灵越 15 5515 系列, inspiron 灵越 16 7610 系列
        MODEL_PATTERNS.add(Pattern.compile("(?i)戴尔\\s*inspiron\\s*灵越\\s*(\\d{2})\\s*(\\d{4})?\\s*(plus|ii)?\\s*系列"));

        // 戴尔 XPS 系列: xps + 尺寸 + 型号
        // 匹配: xps 13 9305 系列, xps 15 9560 系列
        MODEL_PATTERNS.add(Pattern.compile("(?i)戴尔\\s*xps\\s*(\\d{2})\\s*(\\d{4})?\\s*系列"));

        // 戴尔 G 系列游戏本: g + 数字 + 型号
        // 匹配: g3 3500 系列, g5 5587 系列, g7 7588 系列, g15 5520 系列, g16 7620 系列
        MODEL_PATTERNS.add(Pattern.compile("(?i)戴尔\\s*g\\s*(\\d{1,2})\\s*(\\d{4})?\\s*系列"));

        // 戴尔 Vostro 成就系列: vostro成就 + 尺寸/型号
        // 匹配: vostro成就 3420 系列, vostro成就 5510 系列
        MODEL_PATTERNS.add(Pattern.compile("(?i)戴尔\\s*vostro\\s*成就\\s*(\\d{4})\\s*系列"));

        // 戴尔 Latitude 系列: latitude + 型号
        // 匹配: latitude 5580 系列, latitude 7420 系列, latitude e7450 系列
        MODEL_PATTERNS.add(Pattern.compile("(?i)戴尔\\s*latitude\\s*([e])?\\s*(\\d{4})\\s*系列"));

        // 戴尔 Precision 系列: precision + 型号
        // 匹配: precision 3561 系列, precision 3571 系列
        MODEL_PATTERNS.add(Pattern.compile("(?i)戴尔\\s*precision\\s*(\\d{4})\\s*系列"));

        // Alienware 外星人系列: alienware + 型号
        // 匹配: alienware m15 r5, alienware x17 r1, alienware area-51m r2, alienware 15, alienware m16, alienware x16
        MODEL_PATTERNS.add(Pattern.compile("(?i)alienware\\s*([mx])?\\s*(\\d{2})?\\s*([r]\\d+)?\\s*(area-51m|aurora)?\\s*([r]\\d+)?"));

        // 华硕 ROG 系列: rog + 系列名 + 型号
        // 匹配: rog 幻16, rog 魔霸6, rog 枪神5, rog 冰刃4, rog zephyrus g14
        MODEL_PATTERNS.add(Pattern.compile("(?i)华硕\\s*rog\\s*(幻|魔霸|枪神|冰刃|zephyrus)?\\s*([\\d]+[x]?)?\\s*(plus|pro|air|翻转版|g\\d+)?\\s*(\\d{4})?"));

        // 华硕天选系列: 天选 + 数字 + 可选后缀
        // 匹配: 天选, 天选2, 天选3, 天选4, 天选air, 天选5 pro
        MODEL_PATTERNS.add(Pattern.compile("(?i)华硕\\s*天选\\s*(\\d+[r]?)?\\s*(pro|plus|air)?\\s*(\\d{4})?"));

        // 华硕无畏系列: 无畏 + 尺寸 + 可选后缀
        // 匹配: 无畏 14, 无畏 15, 无畏 16, 无畏 pro14, 无畏 pro15
        MODEL_PATTERNS.add(Pattern.compile("(?i)华硕\\s*无畏\\s*(pro)?\\s*(\\d{2}[i]?)\\s*(\\d{4})?\\s*(二合一|ai\\s*\\d+)?"));

        // 华硕灵耀系列: 灵耀 + 型号
        // 匹配: 灵耀 14, 灵耀x 双屏, 灵耀s 2代, 灵耀x 逍遥
        MODEL_PATTERNS.add(Pattern.compile("(?i)华硕\\s*灵耀\\s*([xs])?\\s*(\\d{1,2})?\\s*(双屏|逍遥|2代)?\\s*(pro)?\\s*(\\d{4})?"));

        // 华硕 VivoBook 系列: vivobook + 尺寸/型号
        // 匹配: vivobook 14, vivobook 15, vivobook s15, vivobook flip 14
        MODEL_PATTERNS.add(Pattern.compile("(?i)华硕\\s*vivobook\\s*(\\d{2})?\\s*([sxk]\\d+)?\\s*(flip|go)?\\s*(\\d+)?\\s*(\\d{4})?"));

        // 华硕飞行堡垒系列: 飞行堡垒 + 数字/型号
        // 匹配: 飞行堡垒 8, 飞行堡垒 9, 飞行堡垒 fx503
        MODEL_PATTERNS.add(Pattern.compile("(?i)华硕\\s*飞行堡垒\\s*(\\d+|[fz]x\\d+[a-z]*)?"));

        // 华硕顽石系列: 顽石 + 型号
        // 匹配: 顽石 e410, 顽石 7代 15, 顽石 fl8700
        MODEL_PATTERNS.add(Pattern.compile("(?i)华硕\\s*顽石\\s*(\\d+代)?\\s*([a-z]*\\d+[a-z]*)"));

        // 华硕其他系列: a豆, 破晓, redolbook, expertbook
        MODEL_PATTERNS.add(Pattern.compile("(?i)华硕\\s*(a豆|破晓|redolbook|expertbook|adolbook|无双|mars|transformer)\\s*(\\d{1,2})?\\s*(pro|air)?\\s*(\\d{4})?\\s*(\\d+寸)?"));

        // 惠普 EliteBook 系列: elitebook + 型号
        // 匹配: elitebook 830 g9 系列, elitebook 840 g8 系列, elitebook x360 14 系列
        MODEL_PATTERNS.add(Pattern.compile("(?i)惠普\\s*elitebook\\s*(x360)?\\s*(\\d{3,4})\\s*([g]\\d+)?\\s*系列"));

        // 惠普 ProBook 系列: probook + 型号
        // 匹配: probook 430 g8 系列, probook 450 g10 系列
        MODEL_PATTERNS.add(Pattern.compile("(?i)惠普\\s*probook\\s*(\\d{3})\\s*([g]\\d+)?\\s*系列"));

        // 惠普暗影精灵系列: 暗影精灵 + 数字/代数 + 可选后缀
        // 匹配: 暗影精灵ⅱ代, 暗影精灵ⅲ代 pro, 暗影精灵8, 暗影精灵9, 暗影精灵10
        MODEL_PATTERNS.add(Pattern.compile("(?i)惠普\\s*暗影精灵\\s*([ⅰⅱⅲⅳⅴ]+代|\\d+)?\\s*(pro|plus|slim|air|max|乐享版)?\\s*系列"));

        // 惠普光影精灵系列: 光影精灵 + 数字/代数
        // 匹配: 光影精灵ⅲ代, 光影精灵5代, 光影精灵6, 光影精灵7, victus 光影精灵7
        MODEL_PATTERNS.add(Pattern.compile("(?i)惠普\\s*(victus\\s*)?光影精灵\\s*([ⅲⅳⅴ]+代|\\d+代|\\d+)\\s*(\\d+(\\.\\d+)?寸)?\\s*系列"));

        // 惠普战66系列: 战66 + 代数 + 尺寸
        // 匹配: 战66 4代 14寸, 战66 5代 15.6寸, 战66 pro 14 g2
        MODEL_PATTERNS.add(Pattern.compile("(?i)惠普\\s*战\\s*(66|99)\\s*(pro)?\\s*(\\d+代)?\\s*(\\d{2}(\\.\\d+)?寸)?\\s*([g]\\d+)?\\s*系列"));

        // 惠普星系列: 星 + 数字/名称 + 可选后缀
        // 匹配: 星 13, 星 14, 星 15, 星book pro 14, 星book pro 16, 星14 青春版
        MODEL_PATTERNS.add(Pattern.compile("(?i)惠普\\s*星\\s*(book)?\\s*(pro|plus)?\\s*(\\d{2})\\s*(pro|air)?\\s*(青春版)?\\s*(\\d+[s]?)?\\s*(\\d{4})?\\s*系列"));

        // 惠普 Pavilion/Envy/Spectre 系列
        MODEL_PATTERNS.add(Pattern.compile("(?i)惠普\\s*(pavilion|envy|spectre)\\s*(x360)?\\s*(\\d{2})\\s*(dv\\d+)?\\s*系列"));

        // 惠普 ZBook 系列: zbook + 系列名 + 型号
        // 匹配: zbook fury 15 g7, zbook firefly 14 g8, zbook studio g8, zbook power g9
        MODEL_PATTERNS.add(Pattern.compile("(?i)惠普\\s*zbook\\s*(fury|firefly|studio|power)?\\s*(\\d{2})?\\s*([gu]\\d+)?\\s*系列"));

        // 惠普其他系列: 小欧, 15q, laptop, folio
        MODEL_PATTERNS.add(Pattern.compile("(?i)惠普\\s*(小欧|laptop|folio)\\s*(\\d{2}[sq]?)?\\s*(\\d+[mp])?\\s*系列"));

        // 联想小新系列: 小新 + 系列名 + 尺寸 + 可选后缀
        // 匹配: 小新 air 14, 小新 pro 14, 小新 14, 小新 15, 小新 duet, 小新 510s
        MODEL_PATTERNS.add(Pattern.compile("(?i)联想\\s*小新\\s*(air|pro|锐)?\\s*(\\d{2,3}[s]?)?\\s*(plus|se)?\\s*(\\d{4})?\\s*(gt\\s*ai元启版)?"));

        // 联想拯救者系列: 拯救者 + 型号
        // 匹配: 拯救者 y7000, 拯救者 y9000p, 拯救者 r7000, 拯救者 r9000p, 拯救者 y9000k, 拯救者 y9000x
        MODEL_PATTERNS.add(Pattern.compile("(?i)联想\\s*拯救者\\s*([yr])\\s*(\\d{4}[pkx]?)\\s*(\\d{4})?"));

        // 联想 Yoga 系列: yoga + 型号
        // 匹配: yoga 14c, yoga pro 13s, yoga 13s, yoga c940, yoga 730, yoga slim 7 carbon, yoga book 9, yoga air 14
        MODEL_PATTERNS.add(Pattern.compile("(?i)联想\\s*yoga\\s*(pro|air|slim|book|duet)?\\s*(\\d{1,2}[sc]?)?\\s*(\\d{4})?\\s*(carbon)?\\s*(\\d+寸)?\\s*(ai元启版|aura\\s*ai元启)?"));

        // 联想 IdeaPad 系列: ideapad + 型号
        // 匹配: ideapad 14, ideapad 15s, ideapad 330, ideapad l340, ideapad y700
        MODEL_PATTERNS.add(Pattern.compile("(?i)联想\\s*ideapad\\s*([\\d]+[scy]?)\\s*(\\d{2,3}[sc]?)?\\s*(\\d+寸)?\\s*(\\d{4})?"));

        // 联想扬天/昭阳系列: 扬天/昭阳 + 型号
        // 匹配: 扬天 威6 15寸, 扬天 s14, 昭阳 e41-50, 昭阳 k4e, 昭阳 x5-14
        MODEL_PATTERNS.add(Pattern.compile("(?i)联想\\s*(扬天|昭阳)\\s*(威\\d+)?\\s*([a-z]\\d{1,2})?\\s*([\\-]\\d{2,3})?\\s*(\\d{2}寸)?\\s*(pro)?"));

        // 联想其他系列: 天逸, 异能者, geekpro, lecoo 来酷, 开天
        MODEL_PATTERNS.add(Pattern.compile("(?i)联想\\s*(天逸|异能者|geekpro|lecoo\\s*来酷|来酷|开天)\\s*([a-z]?\\d{2,4}[a-z]?)"));

        // Acer 暗影骑士系列: 暗影骑士 + 型号
        // 匹配: 暗影骑士 龙, 暗影骑士 擎, 暗影骑士3, 暗影骑士4
        MODEL_PATTERNS.add(Pattern.compile("(?i)acer\\s*暗影骑士\\s*(龙|擎|擎pro)?\\s*(\\d+)?\\s*(进阶版)?\\s*([a-z]{2}\\d{3}[\\-]\\d{2})?"));

        // Acer 掠夺者系列: 掠夺者 + 型号
        // 匹配: 掠夺者 helios 300, 掠夺者 擎neo, 掠夺者 刀锋 300se
        MODEL_PATTERNS.add(Pattern.compile("(?i)acer\\s*掠夺者\\s*(helios|擎neo|刀锋)?\\s*(\\d{3}[a-z]*)?\\s*(\\d{4})?"));

        // Acer 蜂鸟/非凡/传奇系列: 蜂鸟/非凡/传奇 + 型号
        // 匹配: 蜂鸟 fun s40, 蜂鸟 swift1, 非凡 go 14, 传奇 x
        MODEL_PATTERNS.add(Pattern.compile("(?i)acer\\s*(蜂鸟|非凡|传奇|墨舞|优跃|未来)\\s*(fun|swift|go|x)?\\s*([a-z]*\\d+)?\\s*(pro|青春版|公主本)?"));

        // Acer 其他系列: aspire, travelmate, v3, s7, f5, e5, a315, ex215
        MODEL_PATTERNS.add(Pattern.compile("(?i)acer\\s*(aspire|travelmate)\\s*([a-z]\\d+|[a-z]+\\d+|p\\d+|[a-z]{2}\\d{3})"));

        // MSI 微星游戏本系列: msi微星 + 系列名 + 型号
        // 匹配: msi微星 gp76, msi微星 gf65, msi微星 gs65, msi微星 ge66, 强袭 ge76, 武士 gf66, 绝影 14
        MODEL_PATTERNS.add(Pattern.compile("(?i)msi\\s*微星\\s*(强袭|武士|绝影|星影|魔影|神枪手|神影|雷影|泰坦)?\\s*([a-z]{2}\\d{2,3})\\s*(hx|ai[+]?)?\\s*(\\d{4})?"));

        // 机械革命系列: 机械革命 + 系列名 + 型号
        // 匹配: 机械革命 蛟龙 15, 机械革命 极光 z, 机械革命 深海泰坦 x10, 机械革命 无界 14, 机械革命 s2
        MODEL_PATTERNS.add(Pattern.compile("(?i)机械革命\\s*(蛟龙|极光|深海泰坦|深海幽灵|无界|旷世|钛钽|耀世|翼龙|苍龙|恒悦|[szux]\\d+)\\s*([a-z]*\\d+[a-z]*)?\\s*(pro|plus|air|super|潮玩版)?\\s*(\\d{4})?"));

        // 机械师系列: 机械师 + 型号
        // 匹配: 机械师 曙光 15, 机械师 t58-v, 机械师 f117-7, 机械师 创物者 16m
        MODEL_PATTERNS.add(Pattern.compile("(?i)机械师\\s*(曙光|创物者|[tf]\\d+[\\-][a-z\\d]+)\\s*(\\d{2}[lm]?)?\\s*(\\d{4})?"));

        // 雷神系列: 雷神 + 型号
        // 匹配: 雷神 911, 雷神 911mt, 雷神 911air, 雷神 zero, 雷神 st, 雷神 猎刃16, 雷神 t-book 16d
        MODEL_PATTERNS.add(Pattern.compile("(?i)雷神\\s*(911[mtx]?|zero|st|iger\\s*e\\d+|猎刃[s]?|aibook|t-book)\\s*(\\d{2}[d]?)?\\s*(黑武士\\d*|air|星战[一二三四五]代|plus)?\\s*(\\d{4})?"));

        // 雷蛇系列: 雷蛇/razer + blade + 型号
        // 匹配: 雷蛇 blade stealth 灵刃潜行版 13寸, 雷蛇 灵刃15 2019, 雷蛇 book 13
        MODEL_PATTERNS.add(Pattern.compile("(?i)雷蛇\\s*(blade)?\\s*(stealth)?\\s*(灵刃|book)?\\s*(\\d{2})?\\s*(潜行版)?\\s*(\\d+寸)?\\s*(\\d{4})?"));

        // 神舟系列: 神舟 + 战神/优雅 + 型号
        // 匹配: 神舟 战神 z7, 神舟 战神 t8, 神舟 战神 g8, 神舟 精盾 u65a, 神舟 优雅 x5
        MODEL_PATTERNS.add(Pattern.compile("(?i)神舟\\s*(战神|精盾|优雅)\\s*([a-z]\\d+[a-z]*\\d*)\\s*(\\d{4})?\\s*(畅玩版)?"));

        // 荣耀笔记本系列: 荣耀 + magicbook/笔记本 + 型号
        // 匹配: 荣耀 magicbook 14, 荣耀 magicbook pro, 荣耀 magicbook x 14, 荣耀 magicbook art 14, 荣耀笔记本 x16
        MODEL_PATTERNS.add(Pattern.compile("(?i)荣耀\\s*(笔记本|magicbook)\\s*([xvz]\\d+)?\\s*(pro|art|x)?\\s*(\\d{2})?\\s*(se|plus|pro|战斗版|hunter版|极客版)?\\s*(\\d{4})?"));

        // 微软 Surface 系列: surface + 型号
        // 匹配: surface pro 9, surface laptop 5, surface laptop go, surface go 3, surface book 2
        MODEL_PATTERNS.add(Pattern.compile("(?i)微软\\s*surface\\s*(pro|laptop|book|go)\\s*(studio|go)?\\s*(\\d+)?\\s*(\\d+(\\.\\d+)?英寸)?\\s*([+])?"));

        // 三星笔记本系列: 三星 + galaxy book/型号
        // 匹配: 三星 galaxy book s, 三星 300e5k, 三星 星曜 730xbe, 三星 玄龙骑士 800g5m
        MODEL_PATTERNS.add(Pattern.compile("(?i)三星\\s*(galaxy\\s*book|星曜|玄龙骑士)?\\s*([s])?\\s*(\\d{3,4}[a-z]*\\d*)"));

        // LG Gram 系列: lg gram + 尺寸
        // 匹配: lg gram 16 2022, lg gram 17z90q, lg gram pro 16 2024
        MODEL_PATTERNS.add(Pattern.compile("(?i)lg\\s*gram\\s*(pro)?\\s*(\\d{2})\\s*([a-z]\\d+[a-z])?\\s*(\\d{4})?"));

        // 七彩虹系列: 七彩虹 + 系列名 + 型号
        // 匹配: 七彩虹 将星 x15, 七彩虹 隐星 p15, 七彩虹 meow 橘宝r15, 七彩虹 源 n14
        MODEL_PATTERNS.add(Pattern.compile("(?i)七彩虹\\s*(将星|隐星|meow\\s*橘宝|源)\\s*([a-z]*\\d{2})\\s*(pro|at|ta\\s*\\d{2})?"));

        // 火影系列: 火影 + 型号
        // 匹配: 火影 t7, 火影 t5c, 火影 t5a, 火影 众颜 u6, 火影 炙影 h6
        MODEL_PATTERNS.add(Pattern.compile("(?i)火影\\s*(众颜|炙影)?\\s*([tu]\\d+[acek]?)"));

        // 其他小众笔记本品牌
        // GPD 掌上电脑: gpd + 型号
        MODEL_PATTERNS.add(Pattern.compile("(?i)gpd\\s*(win\\s*max|pocket|p2\\s*max)\\s*([\\d]+)?\\s*(\\d{4})?"));

        // 壹号本系列: 壹号本 + 型号
        MODEL_PATTERNS.add(Pattern.compile("(?i)壹号本\\s*(one-gx\\d+|onemix|onemi\\s*x\\d+[s]?|one-netbook|游侠x\\d+)\\s*(pro)?"));

        // 炫龙系列
        MODEL_PATTERNS.add(Pattern.compile("(?i)炫龙\\s*(m\\d+|kp\\d+|毁灭者dd\\d+)"));

        // 海尔/清华同方/攀升/索尼等
        MODEL_PATTERNS.add(Pattern.compile("(?i)(海尔|清华同方|攀升|索尼)\\s*(凌越|锋锐|maxbook|vaio)\\s*([a-z]*\\d+[a-z]*)\\s*(pro[+]?)?"));

        // 英特尔 NUC 系列
        MODEL_PATTERNS.add(Pattern.compile("(?i)英特尔\\s*nuc\\s*x\\d+"));

        // 其他品牌: 戴睿, 吾空, 玄派, 优和, aierxuan, 酷比魔方, 中柏
        MODEL_PATTERNS.add(Pattern.compile("(?i)(戴睿|吾空|玄派|优和|aierxuan|酷比魔方|中柏)\\s*([a-z]*\\d+[a-z]*)\\s*(pro[+]?|max)?"));
    }
    
    @Override
    public int getPriority() {
        return 8;
    }
}

