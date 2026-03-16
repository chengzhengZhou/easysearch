# NER实体识别置信度计算策略

## 为什么不应该所有置信度都设为1.0？

### 1. 误导性问题
将所有识别结果的置信度都设为1.0会导致：
- ❌ **无法区分高质量和低质量的识别结果**
- ❌ **评估指标失真**：置信度分布分析失效
- ❌ **业务决策困难**：无法根据置信度过滤或排序结果
- ❌ **问题定位困难**：无法找出低质量识别来源

### 2. 实际识别质量差异很大

| 场景 | 示例 | 应有置信度 | 原因 |
|------|------|-----------|------|
| 长词精确匹配 | "苹果iPhone 15 Pro Max" | 0.95-0.98 | 匹配确定，歧义小 |
| 短词匹配 | "苹果" | 0.75-0.85 | 可能是品牌或水果 |
| 单字符匹配 | "Pro" | 0.50-0.60 | 太短，容易误匹配 |
| 有ID映射 | 品牌ID=brand_001 | +0.03加成 | 已标准化映射 |
| 品牌+型号组合 | "华为 Mate X6" | +0.03-0.05加成 | 上下文验证 |

## 动态置信度计算策略

### 基础置信度（按匹配类型）

```
精确匹配 (EXACT_MATCH)    : 0.95  // 完全匹配词典
部分匹配 (PARTIAL_MATCH)  : 0.75  // 部分匹配
模糊匹配 (FUZZY_MATCH)    : 0.60  // 相似度匹配
基于规则 (RULE_BASED)     : 0.85  // 规则推断
```

### 调整因素

#### 1. 长度因素
```java
长度 = 1字符   → 置信度 × 0.5   // 太短，不可靠
长度 < 2字符   → 置信度 × 0.7
长度 < 4字符   → 置信度 × 0.85
长度 ≥ 4字符   → 置信度 × 1.02  // 更可靠
```

#### 2. 实体类型因素
```java
品牌 (BRAND)      → 置信度 × 1.00  // 通常确定
型号 (MODEL)      → 置信度 × 1.00  // 较为可靠
类别 (CATEGORY)   → 置信度 × 0.95  // 可能有歧义
成色 (CONDITION)  → 置信度 × 0.90  // 词较短
标签 (TAG)        → 置信度 × 0.85  // 歧义较多
规格类           → 置信度 × 0.90  // 易误匹配
```

#### 3. 上下文因素
```java
有品牌+型号组合   → +0.03
有品牌+类别组合   → +0.02
有ID映射         → +0.03
```

#### 4. 多实体验证
```
品牌 + 型号 → 相互验证，提升置信度
品牌 + 类别 → 合理组合，提升置信度
单独出现   → 无验证，保持原置信度
```

## 计算示例

### 示例1：完整查询 "苹果iPhone 15 Pro Max"

```
识别结果：
1. 品牌: "苹果"
   - 基础置信度: 0.95 (精确匹配)
   - 长度调整: ×1.0 (2字符)
   - 类型调整: ×1.0 (品牌)
   - 上下文: +0.03 (有型号)
   - 最终: 0.98

2. 型号: "iPhone 15 Pro Max"
   - 基础置信度: 0.95 (精确匹配)
   - 长度调整: ×1.02 (≥4字符)
   - 类型调整: ×1.0 (型号)
   - 上下文: +0.03 (有品牌)
   - ID映射: +0.03
   - 最终: 1.00 (上限)
```

### 示例2：模糊查询 "苹果手机"

```
识别结果：
1. 品牌: "苹果"
   - 基础置信度: 0.95 (精确匹配)
   - 长度调整: ×1.0 (2字符)
   - 类型调整: ×1.0
   - 上下文: +0.02 (有类别)
   - 最终: 0.97

2. 类别: "手机"
   - 基础置信度: 0.95 (精确匹配)
   - 长度调整: ×1.0 (2字符)
   - 类型调整: ×0.95 (类别可能歧义)
   - 上下文: +0.02 (有品牌)
   - 最终: 0.92
```

### 示例3：短词 "Pro"

```
识别结果：
1. 型号: "Pro"
   - 基础置信度: 0.75 (部分匹配)
   - 长度调整: ×0.7 (太短)
   - 类型调整: ×1.0
   - 上下文: 无
   - 最终: 0.52 (低置信度！)
```

## 置信度等级分类

| 等级 | 置信度范围 | 说明 | 建议 |
|-----|-----------|------|------|
| **非常高** | ≥ 0.90 | 识别非常可靠 | ✅ 可直接使用 |
| **高** | 0.80-0.89 | 识别基本可信 | ✅ 基本可用 |
| **中等** | 0.70-0.79 | 可能存在问题 | ⚠️ 需要验证 |
| **低** | 0.60-0.69 | 不太可靠 | ⚠️ 谨慎使用 |
| **很低** | < 0.60 | 很可能错误 | ❌ 不建议使用 |

## 使用方式

### 自动计算（推荐）

在 `CompositeEntityRecognizer` 中已集成自动置信度计算：

```java
// 实体识别完成后，自动重新计算置信度
Collection<Entity> entities = recognizer.extractEntities(text, tokens);

// 每个实体的置信度已根据策略动态计算
for (Entity entity : entities) {
    double confidence = entity.getConfidence();  // 不再是固定的1.0
    ConfidenceLevel level = ConfidenceCalculator.getConfidenceLevel(confidence);
}
```

### 手动计算

```java
// 简化版本：根据值、类型、匹配类型计算
double confidence = ConfidenceCalculator.calculate(
    "苹果", 
    EntityType.BRAND, 
    MatchType.EXACT_MATCH
);

// 完整版本：考虑所有因素
double confidence = ConfidenceCalculator.calculate(
    entity,           // 实体对象
    matchType,        // 匹配类型
    hasContext,       // 是否有上下文
    allEntities       // 所有实体（用于上下文分析）
);
```

## 置信度在评估中的应用

### 1. 按置信度分层评估

```java
// 高置信度实体：应该准确率很高
List<Entity> highConfidence = entities.stream()
    .filter(e -> e.getConfidence() >= 0.9)
    .collect(Collectors.toList());

// 低置信度实体：需要重点改进
List<Entity> lowConfidence = entities.stream()
    .filter(e -> e.getConfidence() < 0.7)
    .collect(Collectors.toList());
```

### 2. 置信度分布分析

评估报告会显示不同置信度区间的准确率：

```
【置信度分布】
置信度区间         正确数        总数      准确率
0.9-1.0             780         800       0.9750  ← 高置信度应该准确率高
0.8-0.9             520         580       0.8966
0.7-0.8             250         320       0.7812
0.6-0.7             100         180       0.5556  ← 低置信度准确率低（正常）
```

**如果发现问题**：
- 高置信度准确率低 → 置信度计算策略有问题
- 低置信度准确率高 → 置信度打分太保守

### 3. 业务应用

```java
// 根据置信度过滤
List<Entity> reliableEntities = entities.stream()
    .filter(e -> e.getConfidence() >= 0.80)
    .collect(Collectors.toList());

// 根据置信度排序
entities.sort(Comparator.comparingDouble(Entity::getConfidence).reversed());

// 展示置信度等级
for (Entity entity : entities) {
    ConfidenceLevel level = ConfidenceCalculator.getConfidenceLevel(
        entity.getConfidence()
    );
    System.out.println(entity.getValue() + " - " + level.getDescription());
}
```

## 调优建议

### 1. 初期设置（保守）
- 基础置信度可以设置得保守一些
- 观察实际效果后再调整

### 2. 根据评估结果调优

| 问题 | 调整方向 |
|------|---------|
| 高置信度实体准确率低 | 降低基础置信度或加强调整因素 |
| 低置信度实体准确率高 | 提高基础置信度 |
| 长词识别效果差 | 调整长度因素权重 |
| 某类型实体效果差 | 调整该类型的置信度系数 |

### 3. A/B测试

```java
// 版本A：保守策略
BASE_CONFIDENCE_EXACT_MATCH = 0.90;

// 版本B：激进策略  
BASE_CONFIDENCE_EXACT_MATCH = 0.98;

// 对比评估结果，选择最优策略
```

## 参数配置（可调）

在 `ConfidenceCalculator` 中的关键参数：

```java
// 可根据实际效果调整这些参数

// 基础置信度
BASE_CONFIDENCE_EXACT_MATCH = 0.95;      // 精确匹配
BASE_CONFIDENCE_PARTIAL_MATCH = 0.75;    // 部分匹配
BASE_CONFIDENCE_FUZZY_MATCH = 0.60;      // 模糊匹配

// 长度阈值
MIN_RELIABLE_LENGTH = 2;   // 最小可靠长度
OPTIMAL_LENGTH = 4;        // 最优长度

// 加成参数
CONTEXT_BOOST = 0.05;      // 上下文加成
ID_BOOST = 0.03;           // ID映射加成
```

## 总结

✅ **不应该所有置信度都设为1.0**  
✅ **应该根据多种因素动态计算**  
✅ **置信度应该反映识别的真实可靠性**  
✅ **置信度是评估和优化的重要指标**  

---

**更新日期**: 2025-10-11  
**版本**: 1.0

