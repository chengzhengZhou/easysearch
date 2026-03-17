## easysearch：轻量级搜索系统框架

`easysearch` 的主旨是构建一个**轻量、可配置、可插拔、可运营**的搜索系统框架：把“搜索能力提升”从一次性工程开发，变成可持续演进的**配置化能力**与**标准化处理链条**。

### 项目价值

- **让搜索更快上线**：通过可复用的 Query Processing（干预/分词/同义词/NER）与可组装的 Pipeline（去重/插排/打分/排序等），快速形成可用的检索链路。
- **让搜索更易运营**：将干预词、同义词、实体词典、策略开关等沉淀为可发布的配置，支持灰度、回滚与审计，降低线上试错成本。
- **让搜索更可解释**：输出 token、实体、命中规则、置信度与 handler 执行轨迹，支撑排障、评估与持续优化。
- **让搜索更易扩展**：模块边界清晰，既可对接 ES/MySQL/本地数据源，也可逐步引入个性化（如 `easysearch-cf`）实现更高阶优化。

### 模块组成

- `easysearch-qp`：Query Processing（搜索干预、分词、同义词、NER、归一化等）
- `easysearch-core`：搜索执行内核（数据模型、处理管道、函数打分、指标等）
- `easysearch-cf`：协同过滤能力（可作为个性化召回/重排外挂）

### 项目蓝图

完整项目蓝图见 `BLUEPRINT.md`（对外阐述：愿景/架构/配置后台/在线服务/MVP 路线）。

### 架构图（参考）

![图片alt](https://afl-linli.oss-cn-hangzhou.aliyuncs.com/uat/c3cf2c4091b46998.png "愿景架构图")

![图片alt](https://afl-linli.oss-cn-hangzhou.aliyuncs.com/uat/e32147eeeee348f3.png "二排流程图")

![图片alt](https://afl-linli.oss-cn-hangzhou.aliyuncs.com/uat/dd130df0f7084dbf.png "类关系图")
