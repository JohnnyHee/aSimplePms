# aSimplePms —— 小型人员管理系统（前后端分离）

把六年前半途而废的 Eclipse + Shiro + JSONP 老工程，整体重写为 Spring Boot 3 + Redis + MongoDB +
Spring Security 的现代前后端分离系统。**前后台之间只传输 JSON 字符串**（统一响应信封），
旧版「很多页没实现」的后台页面已全部补齐。

- 技术栈：Spring Boot 3.5.16 / Java 21 / Spring Security 6 / MongoDB / Redis / JJWT / springdoc-openapi
- 前端：Vue 3 + TypeScript + Vite + Element Plus + Pinia
- 认证：JWT 无状态（access + refresh 双令牌，refresh 轮换、access 黑名单），Redis 存刷新会话与黑名单
- 零依赖启动：默认 `dev` profile 全程内存实现（内存存储 + 内存令牌库），**不装 MongoDB/Redis 也能跑通全流程**

---

## 一、目录结构

```
aSimplePms-master/
├── pom.xml                     # 聚合 POM（parent = spring-boot-starter-parent 3.5.16）
├── docker-compose.yml          # MongoDB 7 + Redis 7.4（prod profile 用）
├── backend/                    # Spring Boot 后端（唯一 Maven 模块）
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/jyh/pms/
│       │   ├── PmsApplication.java
│       │   ├── core/           # 通用基础设施
│       │   │   ├── audit/      #   @Audited + 审计拦截器
│       │   │   ├── config/     #   SecurityConfig / WebConfig / PmsProperties / DataInitializer
│       │   │   ├── error/      #   ErrorCode / BusinessException / 全局异常 / JSON 安全处理器
│       │   │   └── web/        #   ApiResponse / PageResult / PageQuery / SecurityUtils
│       │   ├── domain/         # User / Role / Position / Salary / AuditLog + 枚举
│       │   ├── security/       # JWT 提供者、过滤器、令牌库（内存 / Redis）、UserDetails
│       │   ├── service/        # 业务服务（含 UserAccountPort 适配器）
│       │   ├── storage/        # 存储抽象：接口 + memory 实现 + mongodb 实现
│       │   ├── util/           # IpUtils
│       │   └── web/            # Controller + DTO + 查询条件 + 视图映射
│       └── resources/application*.yml
└── frontend/                   # Vue 3 前端
    ├── vite.config.ts          # @ 别名、5173 端口、/api 代理到 127.0.0.1:8080
    └── src/
        ├── api/                # axios 封装（信封解包、401 自动续期）+ 各模块接口
        ├── layouts/            # 侧边栏 / 顶栏 / 面包屑
        ├── router/             # 路由 + 登录与权限守卫
        ├── stores/             # Pinia：auth / app
        ├── utils/              # 格式化、权限判断、v-permission 指令
        └── views/              # login / dashboard / user / role / position / salary / audit / profile / error
```

## 二、环境要求

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| JDK | **21** | Spring Boot 3.5.x 不支持 Java 25，务必用 21 |
| Maven | 3.9+ | 或仓库内自带的 `.tools/apache-maven-3.9.9` |
| Node.js | 20+ | 前端构建 |
| pnpm | 9+ | 前端包管理 |
| MongoDB / Redis | 7.x | **可选**，只有 `mongodb` / `prod` profile 需要 |

## 三、启动后端

### 最省事：双击脚本（Windows）

| 脚本 | 作用 |
| --- | --- |
| `start-backend.cmd` | 启动后端（自动选 JDK 21、自动找 Maven） |
| `start-frontend.cmd` | 启动前端 dev server（直接用 node 调 vite） |

两者内部只做一件事：把中文提示与路径逻辑交给 `launch-backend.mjs` / `launch-frontend.mjs`。
**.cmd 文件保持纯 ASCII 是刻意的** —— `cmd.exe` 按系统代码页（简中 Windows 是 GBK）解析批处理，
UTF-8 编码的中文会把命令拆碎，所以中文提示一律放在 `.mjs` 里。

路径不用改脚本，`launch-backend.mjs` 会自己找：

- **JDK**：先看 `PMS_JDK21`，再看 `C:\Program Files\Java\jdk-21*` 等常见位置，并**读取 java 主版本号校验**
  —— 不校验的话会误用 `JAVA_HOME` 里的 JDK 25（Boot 3.5.x 不支持）。
- **Maven**：优先用 PATH 里的 `mvn`；找不到才回退到仓库内自带的 `.tools/apache-maven-3.9.9`。

也可以用环境变量显式指定：

```bat
set PMS_JDK21=C:\Program Files\Java\jdk-21.0.12
start-backend.cmd
```

### 或者手动敲命令

```bash
# 1) 零依赖模式（默认 dev：内存存储 + 内存令牌库，无需 MongoDB / Redis）
mvn -pl backend spring-boot:run

# 2) 真实 MongoDB（连接 PMS_MONGO_URI，默认 mongodb://127.0.0.1:27017/pms）
mvn -pl backend spring-boot:run -Dspring-boot.run.profiles=mongodb

# 3) 生产组合：MongoDB + Redis（本机已有实例可直接用；没有就先 docker compose up -d）
docker compose up -d
mvn -pl backend spring-boot:run -Dspring-boot.run.profiles=prod
```

> 手动敲命令时注意两点：① 必须先让 `JAVA_HOME` 指向 **JDK 21**（本机默认是 JDK 25，Boot 3.5 不支持）；
> ② 若系统 PATH 里没有 `mvn`，用 `H:\aSimplePms-master\.tools\apache-maven-3.9.9\bin\mvn.cmd`，
> 并加上 `-Dmaven.repo.local=H:\aSimplePms-master\.tools\m2repo`。

连接真实 MongoDB 只需把 `PMS_MONGO_URI` 指向实例即可；MongoDB 不可达时启动会失败并明确提示，
不会静默降级到内存存储（避免"以为存进库了其实没有"）。`mongodb` / `prod` 都不使用内嵌 Mongo。

启动后：

- 服务地址 <http://127.0.0.1:8080>
- 接口文档 <http://127.0.0.1:8080/swagger-ui.html>（OpenAPI JSON：`/v3/api-docs`）
- 健康检查 <http://127.0.0.1:8080/actuator/health>

初始化数据由 `DataInitializer` 幂等写入（可用 `pms.init.enabled=false` 关闭）：

| 项目 | 值 |
| --- | --- |
| 管理员账号 | `admin` / `Admin@123` |
| 内置角色 | `ADMIN` 系统管理员、`MANAGER` 部门主管、`USER` 普通员工 |
| 示例职位 | `P5` 初级工程师、`P6` 高级工程师、`M2` 部门经理 |

> 内置角色的权限集合在启动时校验，`ADMIN` 拥有全部 19 项权限；管理员的密码可用
> `PMS_ADMIN_PASSWORD` 覆盖（首次初始化生效）。

## 四、启动前端

```bash
cd frontend
pnpm install
pnpm dev        # http://127.0.0.1:5173 ，/api 已代理到 127.0.0.1:8080
pnpm build      # 产出 dist/（vue-tsc 类型检查 + vite 打包）
```

也可以直接双击 `start-frontend.cmd`，它用 `node node_modules/vite/bin/vite.js` 启动，
不经过 pnpm。当 `pnpm dev` 报 `Command "dev" not found`（pnpm 在执行脚本前做依赖校验失败时会误报）
或报 `ERR_PNPM_STORE_DIR_OPEN_OPERATION_LOCK` 时，用这个方式绕过；此时可尝试
`pnpm run dev` 或 `pnpm --config.verify-deps-before-run=false dev`。

用 `admin / Admin@123` 登录，可见全部菜单：首页看板、人员管理、角色权限、职位管理、薪资管理、
审计日志、个人中心。

## 五、配置项（`backend/src/main/resources/application.yml`）

| 配置 | 默认值 | 说明 |
| --- | --- | --- |
| `spring.profiles.active` | `dev` | `dev` 全内存；`mongodb` 用 Mongo；`prod` 用 Mongo + Redis |
| `pms.storage` | `memory` | `memory` \| `mongodb` |
| `pms.cache` | `memory` | `memory` \| `redis` |
| `pms.token-store` | `memory` | `memory` \| `redis`（刷新令牌 / access 黑名单 / 登录失败计数） |
| `pms.security.jwt-secret` | dev 占位值 | **生产必须覆盖**，≥32 字节，建议 `PMS_JWT_SECRET` |
| `pms.security.access-token-ttl` | `30m` | 访问令牌有效期 |
| `pms.security.refresh-token-ttl` | `7d` | 刷新令牌有效期 |
| `pms.security.max-login-failures` | `5` | 连续失败次数上限 |
| `pms.security.lock-duration` | `10m` | 达到上限后的锁定时长 |
| `pms.init.enabled` | `true` | 是否写入初始化数据 |
| `pms.cors.allowed-origins` | `http://localhost:5173,http://127.0.0.1:5173` | 允许的前端来源 |
| `PMS_MONGO_URI` | `mongodb://127.0.0.1:27017/pms` | MongoDB 连接串 |
| `PMS_REDIS_HOST/PORT/PASSWORD/DATABASE` | `127.0.0.1` / `6379` / 空 / `0` | Redis 连接 |

## 六、接口约定

统一前缀 `/api`，**成功与失败都是同一信封**，前端只需读 `code`：

```json
{ "code": 0, "message": "成功", "data": { }, "timestamp": "2026-10-01T23:56:24.601+08:00" }
```

- `code = 0` 成功；非 0 为业务错误码，同时 HTTP 状态码与之对应
- 分页数据：`data = { records: [], total, page, size }`
- 分页请求：`?page=1&size=20&sort=createdAt,desc`

### 错误码

| code | HTTP | 含义 |
| --- | --- | --- |
| 0 | 200 | 成功 |
| 1000 / 1001 | 400 | 参数不合法 / 参数校验失败 |
| 2000 / 2001 / 2002 | 401 | 未登录或登录失效 / 用户名或密码错误 / 账号被禁用 |
| 2003 | 423 | 账号被锁定（失败次数超限） |
| 2004 / 2005 | 401 | 令牌过期 / 令牌无效 |
| 3000 | 403 | 无访问权限 |
| 4000 / 4001 / 4002 | 404 / 409 / 422 | 资源不存在 / 冲突 / 业务处理失败 |
| 9000 | 500 | 服务器内部错误 |

### 接口一览

| 模块 | 方法与路径 | 所需权限 |
| --- | --- | --- |
| 认证 | `POST /api/auth/login`、`POST /api/auth/refresh`、`POST /api/auth/logout` | 匿名 / 已登录 |
| 认证 | `GET /api/auth/me`、`GET /api/auth/permissions`、`PUT /api/auth/password`、`PUT /api/auth/profile` | 已登录 |
| 人员 | `GET /api/users`（keyword/roleId/positionId/status 分页） | `user:view` |
| 人员 | `GET /api/users/{uid}`、`GET /api/users/departments`、`GET /api/users/check-username` | `user:view` |
| 人员 | `POST /api/users` | `user:create` |
| 人员 | `PUT /api/users/{uid}`、`PATCH /api/users/{uid}/status` | `user:update` |
| 人员 | `DELETE /api/users/{uid}` | `user:delete` |
| 人员 | `PUT /api/users/{uid}/password` | `user:reset-password` |
| 角色 | `GET /api/roles`、`GET /api/roles/{id}`、`GET /api/roles/permissions` | `role:view` |
| 角色 | `GET /api/roles/options` | `role:view` 等 |
| 角色 | `POST /api/roles` / `PUT /api/roles/{id}` / `DELETE /api/roles/{id}` | `role:create` / `role:update` / `role:delete` |
| 职位 | `GET /api/positions`、`GET /api/positions/options`、`GET /api/positions/{id}` | `position:view` |
| 职位 | `POST` / `PUT /{id}` / `DELETE /{id}` | `position:create` / `position:update` / `position:delete` |
| 薪资 | `GET /api/salaries`、`GET /api/salaries/history/{uid}` | `salary:view` |
| 薪资 | `POST` / `PUT /{id}` / `DELETE /{id}` | `salary:create` / `salary:update` / `salary:delete` |
| 审计 | `GET /api/audit-logs`（username/action/outcome/from/to） | `audit:view` |
| 审计 | `DELETE /api/audit-logs`（清空） | `audit:clear` |
| 统计 | `GET /api/stats/overview` | 已登录 |
| 统计 | `GET /api/stats/users-by-department`、`GET /api/stats/recent-audit-logs` | `user:view` 或 `audit:view` |
| 统计 | `GET /api/stats/pay-type-distribution` | `salary:view` 或 `audit:view` |

权限共 19 项，按组划分：人员管理（`user:view/create/update/delete/reset-password`）、
角色权限（`role:view/create/update/delete`）、职位管理（`position:view/create/update/delete`）、
薪资管理（`salary:view/create/update/delete`）、系统审计（`audit:view/clear`）。

## 七、认证与安全设计

- 登录成功签发 **access token**（30 分钟，携带 uid/username/roles/permissions）与
  **refresh token**（7 天，仅携带 uid/username/jti）。
- 每次刷新都会**轮换** refresh token（旧的立即吊销），并把旧 access token 的 jti 拉黑到其自然过期。
- 前端 axios 拦截器遇到 401 会用 refresh token 静默续期并重放原请求，并发请求共享同一次刷新；
  续期失败才清理会话并跳转 `/login?redirect=<当前路径>`。
- 密码使用 BCrypt 存储；连续失败次数达到 `max-login-failures` 后锁定账号 `lock-duration`
  （返回 HTTP 423 与业务码 2003，避免被前端误判为登录态失效）。
- 登录不走 Spring Security 的 Provider/表单链路：`AuthService` 直接校验密码并签发 JWT，
  请求身份识别统一由 `JwtAuthenticationFilter` 完成，因此 `formLogin` / `httpBasic` 均关闭。
- 接口权限用 `@PreAuthorize("hasAuthority('user:view')")` 声明式校验；401/403 由
  `JsonSecurityHandlers` 输出信封 JSON（不重定向、不返回 HTML）。
- 前端路由守卫 + `v-permission` 指令按权限裁剪菜单与按钮。

## 八、开发提示

- **必须用 JDK 21 构建**：`Spring Boot 3.5.x` 不支持 Java 25。若系统默认 JDK 是 25，
  构建前设置 `JAVA_HOME` 指向 JDK 21。
- 六年前的老 Eclipse 工程源码（Shiro + JSONP 那一套）已在本次重写中删除，仅在本 README
  第九节的对照表中保留其设计取舍的记录。
- 构建与测试：

  ```bash
  mvn -pl backend test        # 5 个端到端冒烟测试（MockMvc + 内存存储）
  mvn clean package           # 产出 backend/target/pms-backend.jar
  cd frontend && pnpm build   # 前端类型检查 + 打包
  ```

- 冒烟测试覆盖：未带令牌访问受保护接口 → 401/2000；密码错误 → 401/2001；管理员登录后可查人员列表；
  新增人员后新账号可登录且普通员工访问人员列表 → 403/3000；审计日志可查询。
- 本次重写的验证结论：

  | 验证项 | 结果 |
  | --- | --- |
  | `mvn clean package`（含 5 个冒烟测试） | BUILD SUCCESS，`Tests run: 5, Failures: 0, Errors: 0` |
  | `dev` profile（内存存储）启动 + 34 项 HTTP 契约断言 | 34/34 通过 |
  | `prod` profile（真实 MongoDB 7 + Redis）启动 + 同样 34 项断言 | 34/34 通过 |
  | 重启持久化：MongoDB 数据 / Redis 令牌会话 / 幂等初始化 | 通过（重启后旧 refresh token 仍可换新令牌、审计日志继续累积、内置角色职位不重复写入） |
  | `vue-tsc --noEmit`（前端类型检查） | 通过，0 error |
  | 17 个 `.vue` + 15 个 SCSS 块编译校验 | 通过，0 error |
  | 34 个前端文件 import 可解析性 | 通过 |
  | `pnpm build` / `pnpm dev`（vite 打包与本地联调） | **未在本机执行**，需你手动跑一次确认（见下） |

- 尚未在本机验证的部分，请按需自查：

  ```bash
  cd frontend && pnpm install && pnpm dev    # 或 pnpm build
  docker compose up -d                       # 若用容器起 mongodb + redis
  mvn -pl backend spring-boot:run -Dspring-boot.run.profiles=prod
  ```

  后端的两种存储模式都已端到端验证：`dev` 用内存实现（零依赖），`prod` 用真实
  MongoDB + Redis。前端因为本机沙箱限制未能打包，需要你手动确认一次。


## 九、与旧工程的对照

| 维度 | 旧工程（6 年前） | 现在 |
| --- | --- | --- |
| 框架 | Spring Boot 2.3.0 + Apache Shiro 1.7.1 | Spring Boot 3.5.16 + Spring Security 6 |
| Java | 1.8 | 21 |
| 交互 | 全部 `@GetMapping` 返回 JSON 字符串，用 `callback` 拼 JSONP（判断逻辑还写错了） | REST + 统一 JSON 信封，前后端彻底分离 |
| 前端 | `WebContent/` 是空的，一行页面都没有 | Vue 3 + TS + Element Plus，9 个视图全部实现 |
| 存储 | 直接 MongoTemplate + Criteria 散落在 DAO | 仓储接口 + memory/mongodb 双实现，可零依赖启动 |
| 认证 | Shiro Session + 明文密码 | JWT 双令牌 + BCrypt + 登录失败锁定 + 令牌黑名单 |
| 缓存 | 固定 key `RedisUtil.set("user", json)`，异常时 `CloseUtil.close()` 关掉整个上下文 | Redis 按 `pms:auth:*` 前缀分域，异常降级不影响业务 |
| 校验 | 无 | jakarta.validation + 全局异常处理，参数错误可读提示 |
| 审计 | 无 | `@Audited` 注解 + 拦截器，记录操作人/动作/IP/耗时/结果，可在页面查询与清空 |
| 统计 | 无 | 首页看板：人员、角色、职位、薪资汇总与部门/薪资类型分布 |
| 分页 | 无 | 统一 `PageQuery`（page/size/sort 白名单校验） |
| 依赖 | fastjson 1.2.70 + gson 混用，activemq-core / druid-core 等无用依赖 | 只用 Spring Boot 管理与 JJWT，JSON 统一 Jackson |

## 十、许可

沿用原仓库的 MIT 协议（见 `LICENSE`）。
