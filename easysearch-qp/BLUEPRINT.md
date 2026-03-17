# easysearch-qp 项目蓝图（Query Processing）

`easysearch-qp`（简称 **QP**）是 easysearch 的“查询处理”模块：将用户输入的原始 query 处理为更适合检索的结构化结果，覆盖 **搜索干预、分词、同义词、实体识别（NER）与归一化** 等能力，为上层轻量搜索系统提供可插拔的 query 解析能力。

---

## 1. 模块目标与非目标

- **目标**
  - **可插拔**：分词、同义词、NER、干预都以接口/策略形式可替换、可组合、可灰度。
  - **可运营**：支持整句/词表两类干预，快速纠偏线上效果。
  - **可解释**：产出 token、实体、改写结果，并保留命中来源/置信度/调试信息，方便排障与评估。
  - **轻依赖下游**：输出中立结构（QueryContext），下游召回/排序无需理解内部实现细节。

- **非目标**
  - 不负责索引、召回、排序、重排等“检索引擎”能力。
  - 不绑定特定存储或配置中心；词表/模型加载应通过统一抽象对接。

---

## 2. 端到端处理链路（推荐默认顺序）

QP 的“推荐最小链路”如下（可按业务裁剪）：

1. **输入预处理（format）**
   - 空白规范化、大小写/全半角等（按需启用）。
2. **干预（intervention）**
   - 先整句干预（`SentenceInterventionEngine`），再词表干预（`TermInterventionEngine`），门面为 `InterventionService`。
3. **分词（tokenizer）**
   - 产出 `List<Token>`，token 必须包含：`text/type/startIndex/endIndex/confidence/attributes`。
4. **同义词（synonym）**
   - 两种典型用法：
     - **rewrite**：生成单条“主查询”改写，提高精确性；
     - **expand**：生成多条候选 query，提高召回（需防组合爆炸）。
5. **实体识别（ner）**
   - 输入：原文 + tokens；输出：`Collection<Entity>`（带 span/normalizedValue/confidence/source）。
   - 推荐组合：词典识别 + 模型识别 + 合并策略（`EntityMerger` / `PriorityEntityRecognizer`）。
6. **实体归一化与映射（ner.normalizer）**
   - 对不同 `EntityType` 输出统一的 normalizedValue；可追加外部 ID 映射。
7. **输出（QueryContext）**
   - 输出结构化结果供检索侧使用，并包含 trace/debug。

---

## 3. 分层与包职责

### 3.1 编排与门面层（`com.ppwx.easysearch.qp.support` 等）
- `Pipeline<T>` / `DefaultPipeline<T>`
  - 作为 stage 编排的基础容器（addFirst/addLast/slice 等）。

### 3.2 干预（`com.ppwx.easysearch.qp.intervention`）
- `SentenceInterventionEngine`
  - 整句改写：支持 `EXACT|PREFIX|CONTAINS` + priority，命中优先级最高的规则。
  - 规则格式（TSV）：`源句 \t 目标句 \t 匹配类型 \t 优先级`
- `TermInterventionEngine`
  - 词表改写：基于 Trie 的最长匹配，不重叠替换。
  - 规则格式（TSV）：`源词 \t 目标词 \t 优先级`
- `InterventionService`
  - 对外统一 `rewrite`，并固化顺序：**先整句再词表**。

### 3.3 分词（`com.ppwx.easysearch.qp.tokenizer`）
- 接口：`Tokenizer#tokenize(String) -> List<Token>`
- 典型实现（按能力从“稳妥”到“强干预/强融合”）
  - `CRFCompositeTokenizer`：CRF 基座 + 在“连续单字区间”上用词典补充（干预较弱，偏稳）
  - `DictOverrideCompositeTokenizer`：词典覆盖优先 + CRF 填缝（强干预）
  - `DualPathTokenizer`：双路（去空格整体 vs 按空格分片）再合并（电商型号/空格意图友好）
  - `LatticeFusionTokenizer`：CRF/词典/单字边构 lattice，DP 求最优路径（融合通用）
- `Token`
  - 标准字段：文本、词性、span、confidence、attributes。

### 3.4 同义词（`com.ppwx.easysearch.qp.synonym`）
- `SynonymEngine`
  - 基于 Trie 的最长匹配；词典格式（TSV）：
    - `源词 \t 方向(=>|<=|SYM) \t 目标1,目标2,...`
    - `SYM` 表示双向（会反向补入）。
- `SynonymService`
  - 门面：`match/rewrite/expand/reload`。
  - 策略扩展点：
    - `RewriteStrategy`（如 `ReplaceFirstRewriteStrategy`、`ReplaceAllRewriteStrategy`）
    - `ExpandStrategy`（如 `ExpandOrStrategy`）

### 3.5 实体识别与归一化（`com.ppwx.easysearch.qp.ner`）
- 核心接口：`EntityRecognizer#extractEntities(originText, tokens)`
- 数据结构：`Entity`
  - value/type/normalizedValue/id/confidence/startOffset/endOffset/attachment
- 组合模式 A：模型 + 词典合并（推荐默认）
  - `CRFEntityRecognizer`：基于 HanLP CRFNERecognizer，支持 BMEOS 标注解析
  - `DictEntityRecognizer`：词典实体识别（支持 TSV/JSONL 形式），可加载 aliases，按 token 序列状态转移最长匹配
  - `PriorityEntityRecognizer`：组合上述两者，并通过 `EntityMerger` 按策略处理 span 冲突
  - `EntityMerger`：`DICT_FIRST` / `CRF_FIRST` 两种冲突优先策略
- 组合模式 B：规则识别器聚合（适合快速扩展单类实体）
  - `CompositeEntityRecognizer` + `EntityTypeRecognizer`：按 priority 组合多类规则识别，并做去重、上下文置信度重算
- 置信度策略
  - 文档：`com/ppwx/easysearch/qp/ner/CONFIDENCE_STRATEGY.md`
  - 原则：置信度用于区分可靠性与支持过滤/排序，不应全部固定为 1.0。

---

## 4. 扩展点蓝图

### 4.1 统一的 QueryContext（计划新增）

```java
class QueryContext {
  String originalQuery;
  String normalizedQuery;   // format 后
  String intervenedQuery;   // 干预后
  List<Token> tokens;
  String rewrittenQuery;    // 同义词 rewrite 后（可选）
  List<String> expandedQueries; // 同义词 expand（可选）
  List<Entity> entities;
  Map<String, Object> trace; // stage 命中、耗时、版本号、规则来源等
}
```

### 4.2 Stage 化编排（计划新增）
将“预处理/干预/分词/同义词/NER/归一化”抽为 `Stage`：
- 每个 stage 只读写 `QueryContext` 的一部分，并写入 trace。
- stage 可由 `Pipeline<Stage>` 管理，实现“按业务场景组装链路”。

### 4.3 词表与模型的加载抽象
统一通过 `TextLineSource` 抽象加载：
- classpath / filesystem / DB / 配置中心 → `TextLineSource` → engine.load

### 4.4 组合爆炸与可控扩展
同义词 expand/多策略召回建议：
- 限制最大扩展条数（topK）
- 组合时做去重、按权重排序截断
- 可配置：只对某些 token/entityType 生效

---

## 5. 对外集成方式（门面 API）

计划为 `easysearch-qp` 对外提供一个稳定门面：

```java
QueryProcessor processor = QueryProcessor.builder()
  .withInterventionService(interventionService)
  .withTokenizer(tokenizer)
  .withSynonymService(synonymService)   // 可选
  .withEntityRecognizer(entityRecognizer)
  .withEntityNormalizer(entityNormalizer) // 可选
  .build();

QueryContext ctx = processor.process(query, options);
```

其中 options 用于控制：
- 是否启用 rewrite/expand、使用哪种策略
- NER 合并策略（DICT_FIRST/CRF_FIRST）
- tokenizer 选择（CRFComposite / DictOverride / DualPath / LatticeFusion）

---

## 6. 词表/规则格式清单

- **整句干预（Sentence）**：`源句 \t 目标句 \t 匹配类型 \t 优先级`
- **词表干预（Term）**：`源词 \t 目标词 \t 优先级`
- **同义词**：`源词 \t 方向(=>|<=|SYM) \t 目标1,目标2,...`
- **实体词典（DictEntityRecognizer）**
  - TSV：`entity \t type \t normalizedValue?`
  - JSONL：每行 JSON，对象可包含 aliases 等（以解析器实现为准）

---

## 7. 质量与评估

- **可观测性**
  - 记录：命中干预规则/同义词匹配/实体来源（dict/crf/rule）、耗时与词表版本。
- **回归与指标**
  - 利用 `src/test` 与 `eval` 包做离线评估：
    - 分词一致性（CWS）
    - 实体识别准确率/召回率
    - 置信度分布（高置信度应更准）

---

## 8. 术语与输出约定

- **Token span**：`[startIndex, endIndex)`，相对于干预后的 query（或明确记录是哪个版本的 query）。
- **Entity span**：`[startOffset, endOffset)`，相对于 originText（与 tokens 保持同一基准，避免错位）。
- **source 标记**
  - Token：attributes["source"] = crf/dict/single/...
  - Entity：计划在 attachment 或新增字段记录来源（dict/crf/rule）。

