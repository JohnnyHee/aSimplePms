# 人员管理系统 · 前端（PMS Frontend）

Vue 3 + TypeScript + Vite + Element Plus + Pinia + vue-router + axios 的前端工程，
对接后端 Spring Boot 3（`http://127.0.0.1:8080`，接口前缀 `/api`，统一响应信封）。

## 快速开始

```bash
# 1. 安装依赖
pnpm install

# 2. 开发（默认 5173，/api 代理到 127.0.0.1:8080）
pnpm dev

# 3. 类型检查 + 生产构建（产物在 dist/）
pnpm build

# 4. 预览构建产物
pnpm preview
```

演示账号：`admin / Admin@123`

## 目录结构

```
src/
├── api/          # axios 实例与按模块划分的接口（统一返回 data 字段）
│   ├── http.ts   # 响应解包、401 处理、刷新令牌去重
│   └── types.ts  # 与后端契约一一对应的 TS 类型
├── components/   # PageContainer（页面外壳）、SearchBar（查询栏）
├── layouts/      # DefaultLayout + 侧边栏 / 头部 / 面包屑
├── router/       # 路由与登录、权限前置守卫
├── stores/       # auth（登录态、权限）、app（侧边栏、主题）
├── styles/       # 全局样式与 CSS 变量
├── utils/        # format（格式化）、permission（usePermission + v-permission）
└── views/        # 页面：login / dashboard / user / role / position / salary / audit / profile / error
```

## 约定

- 统一响应信封：成功 `{"code":0,"message":"成功","data":...,"timestamp":"..."}`；
  失败时 HTTP 状态码与 `code` 同时表达错误（如 401 + 2001）。
- `src/api/http.ts` 的响应拦截器在 `code === 0` 时 **resolve 出 `data` 本身**，
  因此业务代码拿到的是 `UserView` / `PageResult<UserView>` 而不是整个信封。
- 分页参数统一为 `{ page, size, sort }`（`sort` 形如 `createdAt,desc`），
  返回 `{ records, total, page, size }`。
- 权限控制使用权限码（如 `user:create`），菜单与按钮均按 `usePermission().has(code)` 过滤。
- 新增页面请在 `src/router/index.ts` 的 `/` 子路由中声明 `meta.title` 与 `meta.permission`。

## 受限沙箱下的注意事项（仅影响本机开发环境）

本仓库的开发会话运行在 DSH 受限沙箱中，存在两个已知限制，`.npmrc` 已针对第 1、2 条做了配置：

1. **无法创建符号链接**（未开启开发者模式、非管理员）：
   `pnpm install` 必须使用 `--node-linker hoisted`（已写入 `.npmrc`），否则报
   `ERR_PNPM_SYMLINK_FAILED [symlinkAllModules] Maximum call stack size exceeded`。
2. **无法写入 `%LOCALAPPDATA%\pnpm-store-operation-locks`**（pnpm 的 store 操作锁）：
   系统 pnpm 12.8.1 执行 `pnpm install` / `pnpm run` 时会在打开该锁文件时报
   `ERR_PNPM_STORE_DIR_OPEN_OPERATION_LOCK ... 拒绝访问。 (os error 5)`。
   - 安装依赖请使用工作区内的 JS 版 pnpm：
     `node H:\aSimplePms-master\.pnpm-tools\node_modules\pnpm\bin\pnpm.cjs install --store-dir H:\aSimplePms-master\.pnpm-store`
   - 运行脚本请显式关闭自动依赖校验：
     `pnpm --config.verify-deps-before-run=false build`
3. **Vite 构建在沙箱内无法运行**：沙箱禁止 Node 以管道 stdio 创建子进程
   （`spawn EPERM`），而 esbuild 的 JS API 必须 spawn 服务进程，因此 `vite build`
   与 `vite dev` 都会在 `failed to load config ... Error: spawn EPERM` 处失败。
   类型检查（`vue-tsc --noEmit`）与 SFC/SCSS 静态编译校验可以通过
   （`node H:\aSimplePms-master\.build-logs\verify-frontend.mjs`）。
   完整构建请在沙箱外执行：`pnpm build`。
