# easysearch

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-1.8+-orange.svg)](https://www.oracle.com/java/)
[![Elasticsearch](https://img.shields.io/badge/Elasticsearch-7.10-green.svg)](https://www.elastic.co/)

## 简介

`easysearch` 是一个**轻量、可配置、可插拔、可运营**的搜索系统框架。它的目标是将"搜索能力提升"从一次性工程开发，转变为可持续演进的**配置化能力**与**标准化处理链条**。

## ✨ 核心特性

### 🚀 让搜索更快上线
通过可复用的 Query Processing（干预/分词/同义词/NER）与可组装的 Pipeline（去重/插排/打分/排序等），快速形成可用的检索链路。

### 🔧 让搜索更易运营
将干预词、同义词、实体词典、策略开关等沉淀为可发布的配置，支持灰度、回滚与审计，降低线上试错成本。

### 📊 让搜索更可解释
输出 token、实体、命中规则、置信度与 handler 执行轨迹，支撑排障、评估与持续优化。

### 🔌 让搜索更易扩展
模块边界清晰，既可对接 ES/MySQL/本地数据源，也可逐步引入个性化（如协同过滤）实现更高阶优化。

## 📦 模块组成

```
easysearch
├── easysearch-qp          # Query Processing - 查询处理模块
├── easysearch-core        # 搜索执行内核
├── easysearch-cf          # 协同过滤推荐模块
├── easysearch-admin       # 后台管理服务
└── easysearch-admin-web   # 管理后台前端
```

### easysearch-qp（Query Processing）

查询处理模块，负责搜索请求的预处理和语义理解：

| 功能模块 | 说明 |
|---------|------|
| **分词器 (tokenizer)** | 支持多种分词策略：CRF分词、词典分词、混合分词、格栅融合等 |
| **同义词 (synonym)** | 同义词扩展与替换，提升召回率 |
| **NER (ner)** | 命名实体识别，支持多种实体类型识别与归一化 |
| **纠错 (correction)** | Query 纠错能力，提升搜索容错性 |
| **格式化 (format)** | Query 标准化与格式归一 |

### easysearch-core（搜索内核）

搜索执行的核心引擎，采用责任链模式构建数据处理管道：

| 功能模块 | 说明 |
|---------|------|
| **数据管道 (pipeline)** | 责任链模式的数据处理管道，支持 Handler 灵活编排 |
| **函数打分 (function)** | 自定义打分函数，支持业务逻辑融入排序 |
| **查询构建 (query)** | ES 查询构建器，简化复杂查询场景 |
| **指标统计 (metrics)** | 搜索指标采集与监控 |
| **数据模型 (data)** | 统一的数据模型抽象 |

**内置 Handler：**
- `DuplicateIdSkipDataHandler` - 去重处理
- `ScoreSortDataHandler` - 分数排序
- `FixSlotsRateDataHandler` - 固定坑位比例控制
- `MMRInterleaveDataHandler` - MMR 多样性混排
- `SlidingWindowInterleaveDataHandler` - 滑动窗口混排
- `RateLimitationInterleaveDataHandler` - 频控混排

### easysearch-cf（协同过滤）

基于物品的协同过滤推荐模块，支持实时推荐计算：

| 功能模块 | 说明 |
|---------|------|
| **ItemCF 模型** | 基于物品相似度的协同过滤，支持增量更新 |
| **相似度计算** | 多种相似度算法支持 |
| **数据存储** | 支持 ES、JDBC、内存、ChronicleMap 等多种存储方式 |
| **评估指标** | MAE、RMSE 等推荐效果评估 |

### easysearch-admin（管理后台）

基于 Spring Boot 的后台管理服务，提供配置管理的 API 接口。

### easysearch-admin-web（管理前端）

基于 Vue 3 + TypeScript 的管理后台前端，提供可视化的配置管理界面。

## 🏗️ 技术栈

- **Java 1.8+**
- **Spring Framework 4.x**
- **Elasticsearch 7.10** - 搜索引擎
- **IKAnalyzer** - 中文分词
- **MyBatis** - ORM 框架
- **Vue 3 + TypeScript** - 前端技术栈
- **Guava** - 工具库
- **Logback** - 日志框架

## 🚀 快速开始

### 环境要求

- JDK 1.8+
- Maven 3.x
- Elasticsearch 7.10（可选）

### 编译构建

```bash
# 克隆项目
git clone https://github.com/your-repo/easysearch.git
cd easysearch

# 编译打包
mvn clean install -DskipTests
```

### 模块引入

根据需要选择性引入模块：

```xml
<!-- Query Processing 模块 -->
<dependency>
    <groupId>com.ppwx</groupId>
    <artifactId>easysearch-qp</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>

<!-- 搜索内核模块 -->
<dependency>
    <groupId>com.ppwx</groupId>
    <artifactId>easysearch-core</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>

<!-- 协同过滤模块（可选） -->
<dependency>
    <groupId>com.ppwx</groupId>
    <artifactId>easysearch-cf</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## 📖 使用示例

### Pipeline 数据处理

```java
// 构建数据处理管道
DataPipeline pipeline = new DataPipelineBuilder()
    .addLast("dedup", new DuplicateIdSkipDataHandler())
    .addLast("sort", new ScoreSortDataHandler())
    .addLast("interleave", new MMRInterleaveDataHandler())
    .build();

// 执行处理
pipeline.fireRead(dataModel);
```

### 分词处理

```java
// 创建分词器
Tokenizer tokenizer = new DualPathTokenizer();

// 执行分词
List<Token> tokens = tokenizer.tokenize("搜索关键词");
```

### 实体识别

```java
// 创建实体识别器
EntityRecognizer recognizer = new CompositeEntityRecognizer();

// 执行识别
List<Entity> entities = recognizer.recognize("苹果iphone 15");
```

## 📁 项目结构

```
easysearch/
├── easysearch-qp/                 # Query Processing 模块
│   └── src/main/java/
│       └── com/ppwx/easysearch/qp/
│           ├── tokenizer/         # 分词器
│           ├── synonym/           # 同义词
│           ├── ner/               # 命名实体识别
│           ├── correction/        # 纠错
│           └── format/            # 格式化
├── easysearch-core/               # 搜索内核模块
│   └── src/main/java/
│       └── com/ppwx/easysearch/core/
│           ├── pipeline/          # 数据管道
│           ├── function/          # 函数打分
│           ├── query/             # 查询构建
│           ├── metrics/           # 指标统计
│           └── data/              # 数据模型
├── easysearch-cf/                 # 协同过滤模块
│   └── src/main/java/
│       └── com/ppwx/easysearch/cf/
│           ├── model/             # 推荐模型
│           ├── similarity/        # 相似度计算
│           ├── repository/        # 数据存储
│           └── rank/              # 排序服务
├── easysearch-admin/              # 管理后台服务
└── easysearch-admin-web/          # 管理后台前端
```

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

## 📄 License

本项目采用 [Apache License 2.0](LICENSE) 开源协议。

```
Copyright 2026 chengzhengZhou

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
