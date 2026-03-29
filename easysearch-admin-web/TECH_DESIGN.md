# easysearch-admin-web 前端技术设计方案

本文档描述 `easysearch-admin-web`（Vue 3 管理后台前端）的技术设计，用于指导页面开发、后端 API 对接、权限接入与上线部署。

## 1. 背景与目标

- **目标**：为 QP 资源治理提供统一管理后台界面，覆盖干预/同义词/实体/分词词典/元信息等模块，支持草稿编辑、校验、发布、回滚、审计追溯与 reload。
- **约束**：
  - 配置真源为 DB（前端不直接访问 DB）
  - 后端由 `easysearch-admin` 提供 `/api/**`（Spring Boot + MyBatis，MySQL 5.7+）
  - 权限体系采用 OAuth2（前端按公司统一登录/网关方案获取 token 或 cookie）

## 2. 技术栈与选型

- **框架**：Vue 3（Composition API）
- **构建**：Vite
- **语言**：TypeScript
- **路由**：Vue Router 4
- **HTTP**：Axios（统一拦截器：鉴权、错误处理、traceId 输出）
- **类型检查**：`vue-tsc`
- **运行时**：Node.js 18+

> 当前仓库为轻量骨架版本，后续按需要引入 UI 组件库（Element Plus/Naive UI 等）与状态管理（Pinia）。

## 3. 交付形态与部署

- **本地开发**：Vite dev server（默认端口 5173），通过代理把 `/api` 转发到后端（见 `vite.config.ts`）。
- **生产构建**：`npm run build` 生成 `dist/` 静态资源。
- **部署方式建议**：
  - **推荐**：`dist/` 由 Nginx/CDN 托管；同域下由网关转发 `/api` 到 `easysearch-admin`（避免 CORS）。
  - **备选**：后端托管静态资源（把 `dist` 复制到后端 `static/`），同域免 CORS，但前端发布会与后端包耦合。

## 4. 信息架构（IA）与路由规划

页面遵循后端方案 `QP_ADMIN_DB_ONLY_SOLUTION.md` 的 IA：

- **资源总览**：资源集列表、当前生效版本、规则数、最近发布时间、发布人；入口：建草稿/进入模块/回滚
- **模块管理页（统一模板）**：
  - 上下文区：resourceSet（module/scene/env）、version（draft/published/archived）、mode（如干预 sentence/term）、变更说明
  - 规则区：列表/检索/分页/新增/编辑/批量启停/批量删除/批量导入
  - 侧栏：预览（可选）、校验结果、发布记录、审计摘要
- **版本与发布页**：草稿列表、发布历史、回滚选择、发布详情
- **审计页**：操作明细与 before/after diff

建议路由（可渐进落地）：

- `/`：Overview（资源总览）
- `/intervention`：干预（整句/词表）
- `/synonym`：同义词
- `/entity`：实体
- `/token`：分词词典
- `/meta`：元信息
- `/versions`：版本与发布（可选：按资源集 drill down）
- `/audit`：审计

## 5. 前端分层与目录约定

建议按“页面-组件-服务-类型”分层，降低模块间耦合：

- `src/pages/`：路由页面（聚合 UI 与调用 service）
- `src/components/`：可复用组件
  - `ResourceContextBar`（资源集/版本选择 + 动作按钮）
  - `RuleTable`（表格/分页/批量操作）
  - `ValidatePanel` / `PublishPanel` / `AuditPanel`
- `src/services/`：API client
  - `http.ts`：Axios 实例、拦截器
  - `resourceSet.ts` / `version.ts` / `rules/*.ts` / `publish.ts` / `audit.ts`
- `src/types/`：后端 DTO 类型（与 API 契约一致）
- `src/router/`：路由
- `src/utils/`：通用工具（diff 展示、下载导入模板等）

## 6. 认证与权限（OAuth2）

### 6.1 Token 获取与传递

根据公司 OAuth2 接入方式，前端通常有两种形态：

- **方式 A（推荐）**：网关同域注入会话（cookie）或反向代理完成鉴权，前端无需直接管理 token。
- **方式 B**：前端持有 access token（例如 OAuth2/OIDC 登录后拿到），请求时加 `Authorization: Bearer <token>`。

前端实现建议：

- 在 `http.ts` 里统一注入鉴权头（若使用方式 B）
- 对 401/403 做统一处理（跳转登录页 / 提示无权限）

### 6.2 RBAC 与页面控制

后端角色（viewer/editor/publisher/admin）决定按钮可用性：

- `viewer`：只读（禁用建草稿/保存/导入/发布/回滚）
- `editor`：允许草稿编辑与校验
- `publisher`：允许发布/回滚/reload
- `admin`：允许资源集管理与权限配置（如有）

前端建议：

- 后端在 `GET /api/me` 或 token claim 返回 `roles`；前端缓存到内存（必要时持久化）
- UI 层做“按钮显隐/禁用”，但**最终以服务端鉴权为准**

## 7. API 对接规范（前端视角）

### 7.1 基础约定

- 所有请求走 `/api/**`（开发阶段由 Vite 代理转发）
- 统一响应结构建议：`{ code, message, data, traceId }`
- 列表统一：`{ page, pageSize, total, items }`
- 错误码策略：
  - `401`：未登录/登录过期
  - `403`：无权限
  - `409`：版本状态冲突（例如非 draft 写入）
  - `422`：校验失败（validate/publish 前置校验）

### 7.2 关键交互与 API 映射

- **建草稿**：`POST /api/resource-sets/{id}/versions`
  - UI：选择 resourceSet + 填 changeLog → 点击“建草稿”
  - 结果：版本下拉切到新 draft，规则列表加载 draft 数据

- **规则 CRUD**（仅 draft）：
  - `GET /api/versions/{versionId}/rules?module=...&mode=...`
  - `POST/PUT/DELETE /api/versions/{versionId}/rules...`

- **对比 diff**（建议新增接口）：
  - `GET /api/versions/{versionId}/diff?baseVersionId=...&module=...&mode=...`
  - UI：弹窗选择 baseVersion → 展示 added/deleted/modified

- **校验**：`POST /api/versions/{versionId}/validate`
  - UI：展示 errors/warnings 列表；errors 阻止发布

- **发布**：`POST /api/versions/{versionId}/publish`
  - UI：展示 publish_record running→success/failed；成功后资源总览刷新 currentVersion

- **回滚**：`POST /api/resource-sets/{id}/rollback?toVersion=...`

- **审计**：`GET /api/audit-logs?...`

## 8. 状态管理策略（轻量起步 → 逐步增强）

建议采用渐进式策略：

- **阶段 1（当前骨架）**：页面内部 `ref/reactive` 管理状态；通过 query 参数维持上下文（resourceSetId/versionId/mode）。
- **阶段 2（建议）**：引入 Pinia：
  - `useSessionStore`：用户信息/角色
  - `useContextStore`：当前 resourceSet/version/mode
  - `useCacheStore`：资源集列表、版本列表缓存（带过期）

缓存策略建议：

- resourceSet 列表：5~30s 缓存，避免频繁刷新
- 版本列表：切换资源集后拉取并缓存
- 规则列表：按 versionId + module(+mode) 缓存分页数据（可选）

## 9. 规则编辑与表单设计

各模块字段来自后端表结构（与 `QP_ADMIN_DB_ONLY_SOLUTION.md` 规则表一致）：

- 干预整句：source/target/matchType/priority/enabled/remark
- 干预词表：source/target/priority/enabled/remark
- 同义词：source/direction/targets_json/enabled/remark（targets 支持标签输入）
- 实体：entity/type/normalizedValue/aliases_json/attributes_json/relations_json/ids_json/enabled
- 分词：word/nature/frequency/bizId/dictType/enabled
- 元信息：termType + 各 id/name 字段组合

表单规则：

- 与后端 validate 保持一致（前端做基础校验，后端做最终校验）
- JSON 字段（如 attributes/relations）建议提供“结构化编辑器”或“JSON 编辑模式 + 校验提示”

## 10. 批量导入/导出

导入建议支持：

- CSV（对运营友好）
- JSON（对研发友好）

前端实现要点：

- 上传前做基本格式检查（文件大小/扩展名）
- 上传后展示解析结果：成功/失败条数、失败原因（行号+字段错误）
- 提供“下载模板”与“错误明细导出”

## 11. 错误处理与用户体验

- 统一提示：
  - 成功：toast
  - 失败：toast + 可展开详情（含 traceId）
- 发布/回滚等长耗时操作：
  - 展示 running 状态（轮询 publish_record 或后端长轮询/WS，后期可演进）
- 版本状态冲突（409）：
  - 引导用户刷新版本列表，提示“当前版本已发布/已归档，无法编辑”

## 12. 可观测性与埋点（前端）

- 每次关键操作记录：
  - 操作类型（建草稿/保存/导入/校验/发布/回滚）
  - resourceSetId/versionId/module/mode
  - traceId（从后端响应透传）
- 便于与后端审计日志（`qp_operation_log`）互相对照定位问题

## 13. 从 prototypes 迁移的落地计划（建议顺序）

参考 `easysearch-admin/src/main/resources/static/prototypes/pages/`：

1. 资源总览页（列表 + 建草稿入口）
2. 干预页（整句/词表）——优先实现“建草稿 → 编辑 → 校验 → 发布”
3. 发布记录与回滚入口打通
4. 同义词/实体/分词/元信息逐模块迁移
5. 审计页与 diff 体验完善（字段级 diff、高亮）

