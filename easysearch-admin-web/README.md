# easysearch-admin-web

Vue3 管理前端（独立目录，不加入 `easysearch/pom.xml` 的 `<modules>`）。

## 技术栈

- 前端框架：Vue 3（Composition API）
- 构建工具：Vite
- 语言：TypeScript
- 路由：Vue Router 4
- HTTP：Axios
- 类型检查：`vue-tsc`
- Node 运行时：Node.js 18+（本仓库已验证 `v18.17.1` 可用）

## 目录结构（约定）

```text
easysearch-admin-web/
  src/
    pages/        路由页面（概览/干预/同义词/实体/分词/元信息…）
    router/       路由定义
    services/     API client 与通用请求封装
```

## 开发

前端默认通过 Vite 代理把 `/api` 转发到 `http://localhost:8080`（见 `vite.config.ts`）。

```bash
cd easysearch/easysearch-admin-web
npm i
npm run dev
```

## 构建（CI）

```bash
cd easysearch/easysearch-admin-web
npm ci
npm run typecheck
npm run build
```

## 说明

- 后端管理 API 由 `easysearch-admin` 提供（Spring Boot）。
- 原型页面参考：`easysearch-admin/src/main/resources/static/prototypes/pages/`

