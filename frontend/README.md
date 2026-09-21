# 实验室招新平台前端开发文档

本工程是实验室招新平台的前端单页应用，技术栈为 Vue 3、Vite、TypeScript、Vue Router 4、Pinia、Element Plus、Axios、markdown-it、DOMPurify、ECharts。

页面、接口和验收口径以 [docs/前端页面规划.md](docs/前端页面规划.md) 为准。本 README 作为开发入口，汇总运行方式、当前实现边界、关键契约和后续开发规范。

---

## 1. 项目定位

- 前端按公开认证页、新生用户端、负责人工作台、管理端拆分页面与导航。
- 后端接口统一以 `/api/v1` 为基础路径，Vite 开发环境通过代理转发 `/api` 和 `/uploads`。
- 登录态由后端 `lab_recruit_token` HttpOnly Cookie 维护，前端不保存 JWT。
- 写操作请求需要携带 `X-CSRF-TOKEN`，值来自 `XSRF-TOKEN` Cookie。
- Markdown 内容必须通过安全渲染组件展示，不直接在页面中裸用 `v-html`。
- 文件上传保存后端返回的文件 ID，业务表单使用 `attachmentFileId`，不保存临时 URL 或静态路径。

当前规划不包含这些旧假设：自动分组、拖拽分组、全局任务、公开资料、按方向公开资料、任务提交历史版本。

---

## 2. 快速启动

```bash
npm install
npm run dev
```

开发地址：

```text
http://127.0.0.1:5173/
```

构建检查：

```bash
npm run build
```

预览构建产物：

```bash
npm run preview
```

Windows PowerShell 如果拦截 `npm`，使用：

```bash
npm.cmd install
npm.cmd run dev
npm.cmd run build
```

---

## 3. 环境变量

前端当前不再提供本地接口模拟开关。本地开发默认通过 Vite 代理访问真实后端。

---

## 4. 后端代理与联调前提

Vite 开发服务器代理配置在 `vite.config.ts`：

```text
/api     -> http://localhost:8081
/uploads -> http://localhost:8081
```

真实后端至少需要满足：

- 后端运行在 `localhost:8081`。
- 登录接口能设置 HttpOnly Cookie。
- 后端允许前端开发域携带 Cookie。
- `/api/v1/auth/me` 能根据 Cookie 返回当前用户。
- 所有接口返回统一 `ApiResponse<T>`。
- 分页接口返回 `PageResult<T>`。
- 附件下载和静态文件访问由后端鉴权或代理控制。

---

## 5. 角色与页面入口

| 角色 | 标识 | 默认入口 | 前端能力范围 |
| --- | --- | --- | --- |
| 新生 | `FRESHMAN` | `/app` | 报名、查看本人分组、查看公告资料、查看任务、提交任务、查看评分结果、修改密码 |
| 负责人 | `LEADER` | `/leader` | 管理负责分组、处理未分组申请、管理组内公告/资料/任务、批阅任务、查看组员、导出组数据 |
| 管理员 | `ADMIN` | `/admin` | 管理时期、方向、用户、报名、分组、负责人、公告、资料、任务、看板、导出、审计日志 |

登录跳转规则：

- `FRESHMAN` 进入 `/app`。
- `LEADER` 进入 `/leader`，当前实现重定向到 `/leader/announcements`。
- `ADMIN` 进入 `/admin`。
- 未登录访问受保护页面会跳转 `/login?redirect=原路径`。
- 已登录用户访问错误角色页面时，返回当前角色默认入口。
- `/admin/login` 复用登录页，但登录成功后仅允许管理员留在管理端。

---

## 6. 当前路由

全量页面规划见 [docs/前端页面规划.md](docs/前端页面规划.md)。当前代码已接入的主要路由如下：

```text
/
├── /login
├── /admin/login
├── /register
├── /forgot-password
├── /reset-password
├── /app
│   ├── /app/applications
│   ├── /app/applications/:id
│   ├── /app/announcements
│   ├── /app/announcements/:id
│   ├── /app/materials
│   ├── /app/materials/:id
│   ├── /app/groups
│   ├── /app/groups/:id
│   ├── /app/tasks
│   ├── /app/tasks/:id
│   ├── /app/scores
│   └── /app/settings
├── /leader
│   ├── /leader/announcements
│   ├── /leader/tasks
│   ├── /leader/tasks/:id/reviews
│   └── /leader/groups/:id/members
├── /admin
│   ├── /admin/periods
│   ├── /admin/directions
│   ├── /admin/users
│   ├── /admin/users/:id
│   ├── /admin/groups
│   ├── /admin/groups/:id
│   ├── /admin/leaders
│   ├── /admin/announcements
│   ├── /admin/materials
│   ├── /admin/tasks
│   └── /admin/export
├── /403
└── /404
```

规划文档中的管理端导出路由为 `/admin/exports`，当前实现暂为 `/admin/export`；后续落地管理端导出页时应统一。

---

## 7. API 开发约定

页面不要直接使用 `axios`，统一从 `src/api/*` 引入接口函数。

```ts
import { getApplications } from '@/api/applications';

const applications = await getApplications();
```

统一响应结构：

```ts
interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
  requestId: string;
}
```

分页结构：

```ts
interface PageResult<T> {
  list: T[];
  page: number;
  size: number;
  total: number;
  totalPages: number;
}
```

`src/api/http.ts` 统一负责：

- `baseURL=/api/v1`
- `withCredentials=true`
- 从 `XSRF-TOKEN` Cookie 读取并注入 `X-CSRF-TOKEN`
- 解包统一响应 `ApiResponse<T>`
- 处理 `40100`、`40300`、`40310`、`40900`、`40910`、`42200`、`42900` 等核心错误码
- 保留并展示 `requestId`

当前已有 API 模块：

```text
src/api
├── http.ts
├── auth.ts
├── meta.ts
├── applications.ts
├── groups.ts
├── tasks.ts
├── announcements.ts
├── materials.ts
├── files.ts
├── admin.ts
└── leader.ts
```

关键接口口径：

| 模块 | 当前契约 |
| --- | --- |
| 认证 | `POST /auth/login`、`POST /auth/logout`、`GET /auth/me`、注册、找回、重置、修改密码 |
| 元信息 | `GET /meta/current-period`、`GET /directions` |
| 报名 | `GET /applications`、`GET /applications/summary`、`GET /applications/{id}`、`POST /applications`、`PUT /applications/{id}`、`DELETE /applications/{id}` |
| 分组查询 | `GET /groups/{id}`；新生本人分组优先来自 `/auth/me` 的 `groups` 后再查详情 |
| 任务查看与提交 | `GET /tasks`、`GET /tasks/{id}`、`GET /tasks/{id}/submission`、`POST /tasks/{id}/submission` |
| 任务管理 | 管理员与负责人均按组操作：`/admin/groups/{groupId}/tasks`、`/leader/groups/{groupId}/tasks` |
| 公告 | 用户查看 `GET /announcements`、`GET /announcements/{id}`；管理使用 `/admin/announcements...` 或 `/leader/announcements...` |
| 资料 | 用户查看 `GET /materials`、`GET /materials/{id}`；管理使用 `/admin/groups/{groupId}/materials...` 或 `/leader/groups/{groupId}/materials...` |
| 文件 | 直传 `POST /uploads?purpose=...`，业务保存返回的文件 ID |
| 看板与导出 | 看板使用 `/admin/dashboard/overview`；导出使用 `/admin/exports/...`、`/leader/exports/...` |

---

## 8. 文件上传约定

默认直传模式：

```text
POST /uploads?purpose=TASK_ATTACHMENT
Content-Type: multipart/form-data
form field: file
```

文件用途：

- `TASK_ATTACHMENT`
- `TASK_SUBMISSION_ATTACHMENT`
- `MATERIAL_ATTACHMENT`

业务表单只保存上传返回对象的 `id`，字段名为 `attachmentFileId`。

如果部署环境启用分片上传，前端按顺序执行：

1. `POST /uploads/sessions`
2. `POST /uploads/sessions/{sessionId}/chunks/{chunkIndex}`
3. `POST /uploads/sessions/{sessionId}/complete`

前端不规划乱序分片、秒传和断点恢复；上传过程中应禁止重复提交业务表单。

---

## 9. 目录结构

```text
src
├── api                 # 后端接口封装
├── assets              # 全局样式和静态资源
├── components
│   ├── common          # PageHeader、PeriodBadge、StatusTag、AttachmentLink 等
│   ├── forms           # DirectionCascader 等表单组件
│   └── markdown        # MarkdownViewer
├── layouts             # AuthLayout、UserLayout、AdminLayout
├── router              # 路由表、权限守卫、路由 meta 类型
├── stores              # Pinia 登录态、时期/方向状态
├── types               # API 类型、业务枚举、分页结构
├── utils               # 路由跳转、标签等工具
└── views
    ├── auth            # 登录、注册、找回密码、重置密码
    ├── app             # 新生用户端页面
    ├── leader          # 负责人页面，当前多为占位路由
    ├── admin           # 管理端页面，当前多为占位路由
    └── error           # 403、404
```

---

## 10. 当前实现状态

已完成或已接入：

- Vue 3、Vite、TypeScript、Element Plus 工程骨架。
- `AuthLayout`、`UserLayout`、`AdminLayout` 三类布局。
- 路由守卫：当前时期加载、当前用户加载、角色校验、按角色默认入口跳转。
- 登录、管理员登录、注册、找回密码、重置密码、个人设置。
- API 契约基础适配：`/uploads`、组级任务/资料、当前任务提交、管理端看板/导出路径、负责人任命、错误码与状态枚举。
- 用户端页面文件：首页、报名、公告列表/详情、资料列表/详情、我的分组/详情、任务列表/详情、成绩、设置。
- 用户端公告、资料、分组、任务相关页面已按当前后端契约调整。
- 公共组件基础版：`FileUploader`、`MarkdownEditor`、`PageTable`、`SearchBar`、`ConfirmAction` 已接入。
- 管理端核心页面：仪表盘、时期、方向、用户、报名、分组、负责人任命已从占位页改为可操作页面。
- 前端运行时已切到真实后端接口路径。

仍需重点完善：

- 负责人工作台、负责分组、未分组申请处理、组内公告/资料/任务、任务批阅、导出。
- 管理端公告、资料、任务、批阅、导出、审计日志。
- 通知中心与顶部未读提示。
- 后台内容类业务组件：公告/资料/任务表单弹窗、批阅弹窗、导出下载细节。
- 真实后端联调后的字段差异和权限/时期边界验收。

---

## 11. 开发优先级

后续开发按规划文档优先级推进：

| 优先级 | 内容 |
| --- | --- |
| P0 | 接口契约修正与联调基础。当前代码已完成主要修正，后续新增页面必须持续遵守该契约。 |
| P1 | 用户端闭环：认证、报名、公告、资料、分组、任务、成绩、设置。 |
| P2 | 负责人工作台：负责组、成员、未分组申请、组内内容、任务批阅、导出。 |
| P3 | 管理端核心配置与分组：仪表盘、时期、方向、用户、报名、分组、负责人任命。 |
| P4 | 管理端内容、导出与审计：公告、资料、任务、Excel 导出、审计日志、通知中心、图表优化。 |

---

## 12. 两人协作分工

按“70% 主负责人 + 30% 业务页面负责人”拆分。70% 负责人包含公共底座、公共组件、接口规范和最终联调，30% 负责人主要承接负责人端与管理端内容类页面。

| 人员 | 负责范围 |
| --- | --- |
| A | 公共底座、公共组件、接口规范、用户端、管理端核心、最终联调和代码 Review |
| B | 负责人端、管理端公告/资料/任务/导出/审计/通知等业务页面 |

### 12.1 A 负责范围

A 作为前端主负责人，负责影响全局一致性的公共部分和用户主流程。

公共底座：

- `src/api/http.ts`
- `src/types/api.ts`
- `src/router/index.ts`
- `src/stores/*`
- `src/layouts/*`
- 全局错误处理、CSRF、登录态恢复、角色跳转、时期控制

公共组件：

- `PageHeader`
- `StatusTag`
- `PeriodBadge`
- `AttachmentLink`
- `FileUploader`
- `MarkdownViewer` / `MarkdownEditor`
- `PageTable`
- `SearchBar`
- `ConfirmAction`

用户端全部：

- 认证、报名、公告、资料、分组、任务、成绩、个人设置

管理端核心：

- 仪表盘
- 时期管理
- 方向管理
- 用户管理
- 报名与未分组申请管理
- 分组管理
- 负责人任命与撤销

质量与联调：

- 维护接口规范
- Review B 涉及公共文件的改动
- 负责真实后端联调前的整体构建检查

### 12.2 B 负责范围

B 主要负责业务页面落地，优先复用 A 提供的接口封装、类型和公共组件。

负责人端：

- 负责人工作台
- 负责分组
- 组员信息
- 未分组申请处理
- 组内公告管理
- 组内资料管理
- 组内任务管理
- 任务批阅
- 负责人导出

管理端内容类页面：

- 公告管理
- 资料管理
- 任务管理
- 任务批阅
- Excel 导出
- 审计日志
- 通知中心

协作边界：

- B 新增接口优先写在 `src/api/leader.ts`、`src/api/admin.ts` 或对应业务 API 文件中。
- B 如需修改 `http.ts`、`types/api.ts`、`router`、`stores`、`layouts`、公共组件，应先和 A 对齐字段、事件和使用方式。
- B 页面中不得绕过 `src/api/*` 直接使用 `axios`。
- A 负责统一合并公共能力，避免同类表格、筛选、弹窗和确认逻辑在多个页面重复实现。

---

## 13. 页面开发规范

- 页面统一放在 `src/views/{auth|app|leader|admin}`。
- 页面标题使用 `PageHeader`。
- 状态标签使用 `StatusTag`。
- 当前时期展示使用 `PeriodBadge`。
- Markdown 展示使用 `MarkdownViewer`，不要直接 `v-html`。
- 方向选择使用 `DirectionCascader`。
- 新增业务类型优先补到 `src/types/api.ts`。
- 新增接口优先补到 `src/api`，不要散落在页面里。
- 写操作必须有 loading、失败提示；删除、撤回、拒绝、打回、撤销负责人等关键操作必须二次确认。
- 公告和资料列表当前后端未提供分页参数，前端只能做本地筛选和空态，不假设服务端分页。
- 任务和资料均为组级资源；创建、编辑、删除必须带 `groupId`。
- 任务提交只维护当前提交记录，不展示提交历史版本。

---

## 14. 接口维护

新增页面接口时：

1. 先在 `src/api/*.ts` 中补真实接口函数和类型。
2. 页面只调用接口封装，不直接散落请求路径。
3. 返回结构保持统一响应结构，不在页面里写临时假数据。
4. 数据字段必须贴近后端当前契约，例如 `attachmentFileId`、`PENDING / SUBMITTED / REVIEWED`。

---

## 15. 接后端检查清单

确认：

- 未登录访问受保护页面会跳转登录页。
- 登录后能按角色进入正确首页。
- `/app/**` 仅 `FRESHMAN` 可访问，`/leader/**` 仅 `LEADER` 可访问，`/admin/**` 仅 `ADMIN` 可访问。
- 非报名期注册、发送注册验证码、报名新增、报名编辑、报名撤回不可提交。
- 非选拔期分组、拒绝申请、取消分组、任务发布、任务提交、任务批阅、任务打回不可提交。
- 负责人只能操作自己负责组的数据。
- 管理员可查看并管理全局数据。
- 报名申请状态按 `SUBMITTED / GROUPED / REJECTED / WITHDRAWN` 展示。
- 任务提交状态按 `PENDING / SUBMITTED / REVIEWED` 展示。
- 文件上传使用后端文件 ID，附件下载走鉴权接口。
- 401、403、40310、409、422、429 等核心错误码有明确用户提示。
- 导出接口能正确下载 `xlsx` 文件，并能在失败时展示错误信息和 `requestId`。

常见问题：

- 刷新受保护页面跳登录：检查 `/auth/me` 和 Cookie。
- 登录成功但后续接口 401：检查 Cookie domain、path、sameSite。
- 写操作 403 或 CSRF 失败：检查 `XSRF-TOKEN` Cookie 和 `X-CSRF-TOKEN` Header。
- 开发环境跨域：优先使用 Vite proxy，不要让页面直接请求完整后端域名。
