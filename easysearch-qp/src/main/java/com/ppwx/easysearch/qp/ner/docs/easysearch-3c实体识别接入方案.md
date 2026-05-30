# easysearch 3C 实体识别接入方案

> 视角：`easysearch` 作为在线 Query Processing 模块，只负责加载训练项目产出的模型与资源并执行推理；当前 HanLP/训练项目负责数据处理、特征构造、模型训练、评估与模型包产出。

## 1. 背景与目标

### 1.1 背景

当前训练项目已经具备二手 3C 场景的字符级 CRF NER 能力，核心包括：

- `Product3CCRFRecognizer`：支持 5 列字符特征输入的 CRF 推理适配器。
- `FeatureColumnBuilder` / `GazetteerMatcher`：根据原始 query 构造字符、字类、品牌词典、品类词典、参数模式等 5 列特征。
- `ProductNerPipeline`：完成文本归一化、拼写归一、特征构造、CRF 推理、BIO 解析、offset 映射与后处理。
- `data/template/3c_ner.tpl`、`data/train/*.txt`、`data/dict/3c/*`：训练模板、训练数据与词典资产。

`easysearch-qp` 当前已有实体识别接口：

- `EntityRecognizer`：统一实体识别入口。
- `Entity` / `EntityType`：在线实体表达。
- `DictEntityRecognizer`：词典实体识别。
- `CRFEntityRecognizer`：当前 HanLP 原生 token 级 CRF NER 适配器。
- `PriorityEntityRecognizer` / `EntityMerger`：词典与模型结果合并。

本方案目标是将训练项目产出的 3C NER 能力以最小侵入方式接入 `easysearch`。

### 1.2 目标

| 目标 | 说明 |
|---|---|
| 训练与在线解耦 | 训练项目产出模型包，`easysearch` 只消费模型包。 |
| 保持 `easysearch` 接口稳定 | 继续使用 `EntityRecognizer#extractEntities(String, List<Token>)`。 |
| 保留词典能力 | `DictEntityRecognizer` 继续作为高精度、带 ID/归一值的识别来源。 |
| 支持灰度切换 | 可在原 `CRFEntityRecognizer` 与新 `Product3CEntityRecognizer` 之间配置切换。 |
| 降低运行时依赖 | `easysearch` 不包含训练逻辑，只包含推理逻辑与资源加载。 |

## 2. 总体架构

### 2.1 训练项目职责

训练项目负责：

1. 清洗 query / 商品标题数据。
2. 构造 5 列字符级训练样本。
3. 维护 3C 领域词典与拼写归一表。
4. 训练 CRF 模型。
5. 评估模型效果。
6. 产出可被 `easysearch` 加载的模型包。

建议模型包目录：

```text
3c-ner-model/
├── model/
│   └── 3c_ner.crf.txt
├── dict/
│   └── 3c/
│       ├── VERSION
│       ├── brand.txt
│       ├── category.txt
│       ├── accessory.txt
│       ├── version.txt
│       └── ...
├── spelling_normalize.tsv
└── manifest.json
```

`manifest.json` 建议包含：

```json
{
  "modelName": "3c_ner",
  "modelVersion": "2026-05-30-001",
  "labelScheme": "BIO",
  "featureColumns": 5,
  "template": "3c_ner.tpl",
  "dictVersion": "2026-05-30",
  "trainedAt": "2026-05-30T00:00:00+08:00",
  "metrics": {
    "entityF1": 0.90,
    "brandF1": 0.95,
    "categoryF1": 0.93,
    "modelF1": 0.88
  }
}
```

### 2.2 easysearch 职责

`easysearch` 只负责：

1. 加载模型包。
2. 对输入 query 执行文本归一化与 5 列特征构造。
3. 调用 `Product3CCRFRecognizer` 推理。
4. 将 BIO 标签转成 `easysearch` 的 `Entity`。
5. 将 3C 标签映射为 `EntityType`。
6. 与词典实体做合并。
7. 将最终实体写入 `QueryContext`。

在线链路建议：

```text
QueryProcessor
  -> FormatStage
  -> InterventionStage
  -> SpellCorrectionStage
  -> TokenizerStage
  -> SynonymStage
  -> NerStage
       -> PriorityEntityRecognizer
            -> DictEntityRecognizer
            -> Product3CEntityRecognizer
       -> EntityMerger(DICT_FIRST)
  -> NormalizerStage
```

## 3. 新增模块设计

### 3.1 新增 `Product3CEntityRecognizer`

建议在 `easysearch-qp` 中新增：

```text
com.ppwx.easysearch.qp.ner.recognizer.Product3CEntityRecognizer
```

实现：

```text
Product3CEntityRecognizer implements EntityRecognizer
```

职责：

- 持有 `Product3CCRFRecognizer`。
- 持有 `FeatureColumnBuilder` 或等价的 easysearch 内部实现。
- 负责 BIO 标签解析。
- 负责 3C 标签到 `EntityType` 的映射。
- 负责 offset 映射与置信度设置。

接口行为：

```java
Collection<Entity> extractEntities(String originText, List<Token> tokens)
```

说明：

- `originText` 是主要输入。
- `tokens` 可不依赖，只为兼容 `EntityRecognizer` 接口。
- 识别失败时返回空集合，不影响主流程。
- 模型懒加载，避免应用启动强依赖模型文件。

### 3.2 调整 `PriorityEntityRecognizer`

当前 `PriorityEntityRecognizer` 强依赖 `CRFEntityRecognizer`。建议改为依赖通用 `EntityRecognizer`：

```text
private final EntityRecognizer modelRecognizer;
private final DictEntityRecognizer dictRecognizer;
```

构造方式：

```text
new PriorityEntityRecognizer(MergeStrategy.DICT_FIRST, product3CRecognizer, dictRecognizer)
```

保留兼容构造：

```text
new PriorityEntityRecognizer()
```

默认仍可使用旧 `CRFEntityRecognizer`，避免破坏已有使用方。

### 3.3 新增配置对象

建议新增：

```text
Product3CNerConfig
```

字段：

| 字段 | 示例 | 说明 |
|---|---|---|
| `enabled` | `true` | 是否启用 3C NER。 |
| `modelPath` | `/data/model/3c_ner.crf.txt` | CRF 模型路径。 |
| `dictDir` | `/data/dict/3c` | 3C 词典目录。 |
| `spellingPath` | `/data/dict/spelling_normalize.tsv` | 拼写归一表。 |
| `mergeStrategy` | `DICT_FIRST` | 合并策略。 |
| `failFast` | `false` | 模型加载失败时是否启动失败。 |
| `confidence` | `0.6` | 模型实体默认置信度。 |

## 4. 资源与依赖方案

### 4.1 HanLP 依赖处理

`Product3CCRFRecognizer` 不在官方 `hanlp portable-1.8.6` 中，推荐两种方案。

#### 方案 A：发布内部 HanLP 包，推荐

训练项目将扩展后的 HanLP 发布为内部 Maven 包：

```xml
<dependency>
  <groupId>com.ppwx.thirdparty</groupId>
  <artifactId>hanlp-product3c</artifactId>
  <version>1.8.6-ppwx-3c-001</version>
</dependency>
```

优点：

- `easysearch` 不复制 HanLP 内部类。
- `Product3CCRFRecognizer` 可保留在 `com.hankcs.hanlp.model.crf` 包下，避免访问权限问题。
- 训练与在线使用同一套推理代码，结果一致。

缺点：

- 需要维护内部 Maven 版本。

#### 方案 B：复制推理相关类到 easysearch，不优先推荐

将 `Product3CCRFRecognizer`、`FeatureColumnBuilder`、`GazetteerMatcher`、`TextNormalizer`、`NerPostProcessor` 迁入 `easysearch-qp`。

风险：

- `Product3CCRFRecognizer` 依赖 HanLP CRF 包内部字段，跨包访问可能失败。
- 训练项目与在线项目容易出现代码分叉。
- 后续模型效果排查复杂。

结论：优先使用方案 A。

### 4.2 模型包加载方式

支持两种加载方式：

1. 文件系统路径：适合生产部署、模型热更新。
2. classpath 资源：适合单测、demo、轻量部署。

建议优先文件系统路径，模型包不随 jar 固化，便于灰度和回滚。

## 5. 标签映射设计

训练项目使用 3C 领域标签，`easysearch` 使用 `EntityType`。需要统一映射。

| 3C 标签 | 含义 | easysearch `EntityType` | 说明 |
|---|---|---|---|
| `BRD` | 品牌 | `BRAND` | 直接映射。 |
| `CAT` | 品类 | `CATEGORY` | 直接映射。 |
| `SER` | 系列 | `MODEL` | 首期可并入型号；后续建议新增 `SERIES`。 |
| `MOD` | 型号 | `MODEL` | 直接映射。 |
| `VER` | 版本 | `FEATURE` 或 `TAG` | 首期建议 `FEATURE`；后续建议新增 `VERSION`。 |
| `STO` | 存储 | `STORAGE` | 直接映射。 |
| `CON` | 成色 | `CONDITION` | 直接映射。 |
| `CLR` | 颜色 | `COLOR` | 直接映射。 |
| `YR` | 年款 | `TAG` | 后续可新增 `YEAR`。 |
| `CPU` | 处理器 | `CPU` | 直接映射。 |
| `GPU` | 显卡型号 | `FEATURE` | 后续建议新增 `GPU`。 |
| `SCR` | 屏幕尺寸 | `SCREEN` | 直接映射。 |
| `PWR` | 功率 | `FEATURE` | 可作为参数特征。 |
| `LEN` | 长度/焦距 | `SIZE` | 视业务语义调整。 |

建议实体 `attachment` 中保留原始 3C 标签：

```text
entity.attachment = {
  source: "product3c-crf",
  rawType: "SER",
  modelVersion: "2026-05-30-001"
}
```

## 6. 推理流程

### 6.1 在线识别步骤

`Product3CEntityRecognizer#extractEntities` 流程：

1. 参数校验：`originText` 为空时返回空。
2. 文本归一化：NFKC、去异常字符、大小写归一。
3. 拼写归一：例如 `iphnoe` -> `iphone`，按训练项目规则执行。
4. 5 列特征构造：字符、字类、品牌命中、品类命中、参数模式。
5. CRF 推理：`Product3CCRFRecognizer#recognize(String[][] columns)`。
6. BIO 解析：`B-XXX/I-XXX` -> span。
7. offset 映射：从归一化文本位置映射回原始 query 位置。
8. 后处理：品牌补全、词典兜底、非法 span 过滤。
9. 类型映射：3C 标签 -> `EntityType`。
10. 返回 `Collection<Entity>`。

### 6.2 合并策略

推荐默认：`DICT_FIRST`。

原因：

- 词典实体通常带 `id` 和标准化值，适合搜索过滤、召回和排序。
- CRF 模型适合补召回复杂组合、连写型号和参数。
- 冲突时应优先保留可映射到业务 ID 的词典结果。

合并优先级：

1. 有 ID 的词典实体。
2. 高置信度模型实体。
3. 更长 span 的实体。
4. 同类型重叠时保留优先级更高者。
5. 不同类型重叠时按业务规则处理，例如 `BRAND` 与 `MODEL` 可共存，`MODEL` 与 `CATEGORY` 重叠需择优。

## 7. 训练项目到 easysearch 的交付契约

训练项目每次发布模型时，应交付完整模型包与元信息。

### 7.1 必需文件

| 文件 | 是否必需 | 说明 |
|---|---|---|
| `model/3c_ner.crf.txt` | 是 | HanLP 可加载的 CRF 文本模型。 |
| `dict/3c/brand.txt` | 是 | 品牌词典。 |
| `dict/3c/category.txt` | 是 | 品类词典。 |
| `dict/3c/accessory.txt` | 否 | 配件词典。 |
| `dict/3c/version.txt` | 否 | 版本词典。 |
| `spelling_normalize.tsv` | 否 | 拼写归一表。 |
| `manifest.json` | 是 | 模型版本、指标、训练信息。 |

### 7.2 兼容性要求

训练项目必须保证：

1. 模型训练使用的特征模板与在线特征构造一致。
2. 模型标签集合与 `easysearch` 标签映射配置一致。
3. `featureColumns` 固定为 5；如增加列，必须升级在线适配器。
4. 文本归一化逻辑训练与在线一致。
5. `manifest.json` 中记录模型版本，便于问题追溯。

### 7.3 发布流程

建议流程：

```text
训练数据更新
  -> 训练项目生成样本
  -> 训练 CRF 模型
  -> 离线评估
  -> 生成模型包
  -> 上传模型仓库/制品库
  -> easysearch 配置新模型路径
  -> 灰度发布
  -> 指标观察
  -> 全量或回滚
```

## 8. easysearch 配置示例

建议配置：

```properties
qp.ner.engine=product3c
qp.ner.product3c.enabled=true
qp.ner.product3c.modelPath=/opt/easysearch/models/3c-ner/model/3c_ner.crf.txt
qp.ner.product3c.dictDir=/opt/easysearch/models/3c-ner/dict/3c
qp.ner.product3c.spellingPath=/opt/easysearch/models/3c-ner/spelling_normalize.tsv
qp.ner.product3c.mergeStrategy=DICT_FIRST
qp.ner.product3c.failFast=false
qp.ner.product3c.defaultConfidence=0.60
```

引擎切换：

| 配置值 | 行为 |
|---|---|
| `legacy-crf` | 使用当前 `CRFEntityRecognizer`。 |
| `product3c` | 使用新增 `Product3CEntityRecognizer`。 |
| `dict-only` | 仅使用 `DictEntityRecognizer`。 |
| `hybrid` | 词典 + 3C CRF，推荐生产默认。 |

## 9. 监控与评估

### 9.1 离线指标

训练项目输出：

- entity-level precision / recall / F1。
- 分类型 precision / recall / F1。
- 严格 span 匹配与宽松 span 匹配。
- OOV query 子集指标。
- 高频 query 子集指标。

### 9.2 在线指标

`easysearch` 侧记录：

| 指标 | 说明 |
|---|---|
| `ner.entity.count` | 每 query 识别实体数。 |
| `ner.product3c.latency` | 3C NER 推理耗时。 |
| `ner.product3c.error.count` | 推理异常数。 |
| `ner.product3c.empty.rate` | 空结果比例。 |
| `ner.model.version` | 当前模型版本。 |
| `ner.entity.type.distribution` | 实体类型分布。 |

日志建议只采样记录，避免泄露和日志膨胀。

### 9.3 回归集

维护固定回归集：

- 高频 query。
- 新机型 query。
- 中英数字连写 query。
- 错拼 query。
- 短 query。
- 参数 query。
- 词典强依赖 query。

每次模型发布前必须跑回归集。

## 10. 灰度与回滚

### 10.1 灰度策略

1. 首先本地/测试环境对比旧 NER 与 3C NER 输出。
2. 线上 1% 流量 shadow 运行，只打日志不影响结果。
3. 线上 5% 流量启用结果，观察搜索转化、召回、点击。
4. 无异常后逐步扩大。

### 10.2 回滚策略

回滚优先级：

1. 配置切回 `legacy-crf` 或 `dict-only`。
2. 模型路径切回上一版本。
3. 禁用 `Product3CEntityRecognizer`。

要求：模型加载失败不得导致主搜索链路不可用，除非 `failFast=true`。

## 11. 实施步骤

### 阶段一：模型包标准化

训练项目完成：

- 生成 `3c_ner.crf.txt`。
- 整理 `dict/3c`。
- 生成 `manifest.json`。
- 固化模型包目录结构。

### 阶段二：easysearch 新增适配器

`easysearch-qp` 完成：

- 新增 `Product3CEntityRecognizer`。
- 新增 `Product3CNerConfig`。
- 调整 `PriorityEntityRecognizer` 支持通用模型识别器。
- 新增标签映射工具。
- 新增单元测试：BIO 解析、标签映射、offset 映射、异常兜底。

### 阶段三：集成与灰度

完成：

- 接入配置开关。
- 接入模型版本日志。
- 接入推理耗时与异常监控。
- 跑离线回归集。
- 开启 shadow / 灰度。

## 12. 风险与应对

| 风险 | 影响 | 应对 |
|---|---|---|
| 训练与在线特征不一致 | 模型效果大幅下降 | 特征构造代码由同一依赖包提供，manifest 记录模板版本。 |
| 标签映射不完整 | 实体类型丢失或误用 | 首期固定映射表，新增标签必须评审。 |
| 模型文件加载失败 | NER 空结果或启动失败 | 默认 `failFast=false`，异常返回空并报警。 |
| 词典与模型冲突 | 错误实体覆盖正确实体 | 默认 `DICT_FIRST`，保留有 ID 词典结果。 |
| offset 映射错误 | 下游高亮/召回异常 | 建立中英混排、错拼、空格、全半角回归用例。 |
| HanLP 内部类依赖 | 编译或运行不稳定 | 发布内部 HanLP 推理包，避免复制内部实现。 |

## 13. 推荐结论

推荐方案：

```text
训练项目：负责数据处理、训练、评估、模型包发布
        ↓
easysearch：新增 Product3CEntityRecognizer 作为在线推理适配器
        ↓
PriorityEntityRecognizer：DictEntityRecognizer + Product3CEntityRecognizer
        ↓
EntityMerger：默认 DICT_FIRST
```

不建议直接用 `Product3CCRFRecognizer` 替换当前 `CRFEntityRecognizer`，原因是：

1. 输入形态不同：前者是字符级 5 列特征，后者是 token + POS。
2. 标注体系不同：前者是 3C BIO，后者是当前 BMEOS/token 逻辑。
3. 资源依赖不同：前者依赖 3C 词典、拼写归一与专用模型。
4. 直接替换会破坏现有词典归一、ID 映射与合并逻辑。

最终建议以“新增适配器 + 配置切换 + 词典优先合并”的方式接入，既能复用训练项目的 3C 模型能力，也能保持 `easysearch` 在线链路稳定。
