# Tokenizer模块使用指南

## 概述

Tokenizer模块是一个专为3C领域设计的智能分词系统，基于HanLP分词引擎，支持自定义专业词典，能够精准识别品牌、型号、规格等关键信息。

## 核心特性

- **基于HanLP**: 使用强大的中文分词引擎
- **3C领域专业词典**: 内置品牌、产品、型号、规格等专业词典
- **数字单位合并**: 自动识别并合并"256GB"、"14寸"等数字单位组合
- **智能后处理**: 停用词过滤、去重、排序等优化处理
- **灵活扩展**: 支持词典热更新和自定义扩展

## 快速开始

### 1. 基本使用

```java
@Autowired
private Tokenizer tokenizer;

public void example() {
    String query = "iPhone15 Pro Max 256GB";
    List<Token> tokens = tokenizer.tokenize(query);
    
    for (Token token : tokens) {
        System.out.println(token.getText() + " -> " + token.getType());
    }
}
```

### 2. 批量分词

```java
List<String> queries = Arrays.asList(
    "MacBook Pro 14寸",
    "OPPO Reno8 Pro 5G版",
    "二手 iPad Air 256GB"
);

List<List<Token>> results = tokenizer.tokenize(queries);
```

### 3. Token信息获取

```java
List<Token> tokens = tokenizer.tokenize("iPhone15 Pro Max 256GB 蓝色");

for (Token token : tokens) {
    String text = token.getText();           // 分词文本
    String type = token.getType();           // 词性类型
    int startIndex = token.getStartIndex();  // 起始位置
    int endIndex = token.getEndIndex();      // 结束位置
    double confidence = token.getConfidence(); // 置信度
    Map<String, String> attrs = token.getAttributes(); // 扩展属性
}
```

## 核心组件

### 1. Tokenizer接口

主要的分词器接口，提供分词功能。

```java
public interface Tokenizer {
    List<Token> tokenize(String text);
    List<List<Token>> tokenize(List<String> texts);
}
```

### 2. Token类

分词结果的数据模型。

```java
public class Token {
    private String text;           // 分词文本
    private String type;           // 词性类型
    private int startIndex;        // 起始位置
    private int endIndex;          // 结束位置
    private double confidence;     // 置信度
    private Map<String, String> attributes; // 扩展属性
}
```

### 3. SpaceSegmentTokenizer类（推荐作为查询入口）

按空格分片的分词包装器，适用于用户查询含空格的场景（如二手 3C 电商搜索）。

**解决的问题**：用户输入如「真我 GT5 Pro」含空格，而词典与 CRF 语料多为无空格；若直接分词会降低识别效果；同时空格可作为用户意图边界，避免新词（新机型）被错误合并导致召回不足。

**使用方式**：
```java
// 作为分词上层入口，包装 CRFCompositeTokenizer
Tokenizer tokenizer = new SpaceSegmentTokenizer(new CompositeTokenizer());
List<Token> tokens = tokenizer.tokenize("真我 GT5 Pro 16GB");
// 按空格分片：["真我","GT5","Pro","16GB"]，每段单独分词，最后合并并保持原始位置
```

**推荐用法**：在 NerCategoryPrediction 等上层业务中，将 Tokenizer 注入为 `SpaceSegmentTokenizer(delegate)`，由 SpaceSegmentTokenizer 作为统一入口。

### 4. DualPathTokenizer类（双路分词 + 合并）

Path A（去空格）+ Path B（按空格分片）双路分词，合并时优先采用 Path B 的边界，对 Path A 中跨边界的 token 舍弃，片段内采用更细粒度切分。

**适用场景**：兼顾词典召回（Path A 无空格匹配）与用户空格边界（Path B），适合对召回与边界均有较高要求的查询。

**合并策略**：
- 以 Path B 的片段边界为准；
- Path A 中跨越 Path B 片段边界的 token 予以舍弃；
- 片段内若 Path A 更细则采用 Path A，否则采用 Path B。

```java
Tokenizer tokenizer = new DualPathTokenizer(new CompositeTokenizer());
List<Token> tokens = tokenizer.tokenize("iPhone16 Pro");
// Path A 可能分出 [iPhone, 16]（更细），Path B 按空格得 [iPhone16, Pro]；合并后取更细者
```

### 5. CompositeTokenizer类

CRF 基座 + Dict 补充的优先级分词器。SpaceSegmentTokenizer 默认使用此类作为底层分词器。

### 6. ThreeCTokenizer类

3C领域专业分词器实现类。

主要功能：
- HanLP基础分词
- 自定义词典增强
- 后处理优化
- 特殊字符标准化

### 7. HanLPSegment类

HanLP分词引擎的封装类。

功能：
- 配置HanLP分词器
- 执行基础分词
- 词性映射转换

### 8. CustomDictionaryManager类

自定义词典管理器。

功能：
- 加载和管理多个词典
- 词典匹配和增强
- 支持词典热更新

### 9. TokenPostProcessor类

分词后处理器。

功能：
- 停用词过滤
- 数字单位合并
- Token去重
- 位置排序

## 词典配置

### 词典文件格式

每个词典是一个`.dic`文件，每行一个词条。

**示例：brand.dic**
```
iPhone
iPhone15
iPhone15Pro
Samsung
OPPO
Xiaomi
MacBook
```

### 词典类型

系统支持以下词典类型：

- **brand**: 品牌词典
- **product**: 产品类别词典
- **model**: 型号词典
- **spec**: 规格参数词典

### 添加自定义词典

1. 在`resources/dictionaries/`目录下创建词典文件
2. 在`CustomDictionaryManager`中添加加载配置
3. 重启应用或调用热更新接口

```java
// 热更新词典
dictionaryManager.reloadDictionary("brand", "dictionaries/brand.dic");

// 重新加载所有词典
dictionaryManager.reloadAllDictionaries();
```

## 分词流程

```
输入文本
  ↓
预处理(去空格、标准化特殊字符)
  ↓
HanLP基础分词
  ↓
自定义词典增强
  ↓
后处理优化(停用词过滤、数字单位合并、去重、排序)
  ↓
返回Token列表
```

## Token类型说明

### HanLP词性类型

- `person`: 人名
- `place`: 地名
- `organization`: 机构名
- `number`: 数词
- `quantifier`: 量词
- `noun`: 名词
- `verb`: 动词
- `adjective`: 形容词
- `adverb`: 副词
- `word`: 普通词汇

### 自定义类型

- `brand`: 品牌
- `product`: 产品类别
- `model`: 型号
- `spec`: 规格参数
- `number_unit`: 数字单位组合(如"256GB")

## 配置参数

### application-tokenizer.yml

```yaml
tokenizer:
  hanlp:
    enable-debug: false
    enable-ner: true
  
  dictionaries:
    brand:
      path: "classpath:dictionaries/brand.dic"
      enabled: true
  
  post-processing:
    enable-stop-word-filter: true
    enable-number-unit-merge: true
    enable-deduplication: true
```

## 性能优化建议

### 1. 使用缓存

```java
@Cacheable(value = "tokenizer", key = "#text.hashCode()")
public List<Token> tokenize(String text) {
    return tokenizer.tokenize(text);
}
```

### 2. 批量处理

对于大量文本，使用批量接口可以提高性能：

```java
List<List<Token>> results = tokenizer.tokenize(textList);
```

### 3. 异步处理

```java
@Async
public CompletableFuture<List<Token>> tokenizeAsync(String text) {
    return CompletableFuture.completedFuture(tokenizer.tokenize(text));
}
```

## 常见问题

### Q1: 如何添加新的词条？

A: 在对应的词典文件中添加新词条，然后调用热更新接口或重启应用。

### Q2: 分词结果为空怎么办？

A: 检查输入文本是否为空，或者是否全部为停用词。

### Q3: 如何提高分词准确度？

A: 完善自定义词典，添加更多领域专业词汇。

### Q4: Token的置信度如何计算？

A: HanLP基础分词的置信度为1.0，自定义词典匹配的置信度为0.9，合并token的置信度取最小值。

## 测试

运行单元测试：

```bash
# 按空格分片分词器（推荐查询入口）
mvn test -Dtest=SpaceSegmentTokenizerTest

# 双路分词器（Path A + Path B 合并）
mvn test -Dtest=DualPathTokenizerTest

# CRF+Dict 组合分词器
mvn test -Dtest=CompositeTokenizerTest

mvn test -Dtest=ThreeCTokenizerTest
```

## 示例

### 示例1: 手机查询分词

```java
String query = "二手 iPhone15 Pro Max 256GB 蓝色 95成新";
List<Token> tokens = tokenizer.tokenize(query);

// 输出示例:
// Token{text='二手', type='adjective', ...}
// Token{text='iPhone15', type='brand', ...}
// Token{text='Pro', type='model', ...}
// Token{text='Max', type='model', ...}
// Token{text='256GB', type='number_unit', ...}
// Token{text='蓝色', type='noun', ...}
// Token{text='95', type='number', ...}
// Token{text='成新', type='quantifier', ...}
```

### 示例2: 笔记本查询分词

```java
String query = "MacBook Pro 14寸 M3芯片 16GB内存";
List<Token> tokens = tokenizer.tokenize(query);

// 输出示例:
// Token{text='MacBook', type='brand', ...}
// Token{text='Pro', type='model', ...}
// Token{text='14寸', type='number_unit', ...}
// Token{text='M3', type='noun', ...}
// Token{text='芯片', type='noun', ...}
// Token{text='16GB', type='number_unit', ...}
// Token{text='内存', type='noun', ...}
```

## 版本历史

- **v1.0.0** (2024-12-19): 初始版本
  - 基于HanLP的基础分词
  - 支持3C领域专业词典
  - 数字单位自动合并
  - 智能后处理优化

## 联系方式

如有问题或建议，请联系开发团队。
