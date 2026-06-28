# A 完成顺序

本文档根据 `README.md` 中 “A 前端总负责人” 的职责拆分，整理 A 的推荐完成顺序。整体原则是：先保证公共底座稳定，再完成用户端主流程闭环，最后补齐展示类页面和联调质量检查。

## 1. 工程与公共底座

优先确认并维护以下公共文件和目录：

```text
src/api/http.ts
src/router/index.ts
src/types/api.ts
src/stores/auth.ts
src/stores/meta.ts
src/layouts/AuthLayout.vue
src/layouts/UserLayout.vue
src/components/common
src/components/forms
src/components/markdown
```

完成目标：

- 统一接口调用方式，页面不得直接使用 `axios`。
- 维护 `ApiResponse<T>`、分页结构和核心业务类型。
- 保证登录态恢复、权限跳转、时期提示等全局行为可用。
- 明确公共组件的 props、事件、插槽和复用规范。

验收点：

- 路由权限守卫能正确拦截未登录、无权限角色。
- 登录态刷新后能通过 `/auth/me` 恢复。
- 公共组件能被用户端页面直接复用。

## 2. Mock 和接口规范

在正式开发页面前，先补齐用户端主流程需要的接口封装和 Mock：

```text
src/api/auth.ts
src/api/applications.ts
src/api/tasks.ts
src/api/announcements.ts
src/api/materials.ts
src/api/groups.ts
src/api/files.ts
src/mocks/data.ts
src/mocks/adapter.ts
```

完成目标：

- 新增接口先写到 `src/api/*.ts`，页面只调用接口函数。
- Mock 返回保持统一 `ApiResponse<T>` 结构。
- 分页接口保持 `PageResult<T>` 结构。
- 页面不写临时假数据。

验收点：

- 开启 `VITE_USE_MOCK_API=true` 后，用户端主要页面能正常取数。
- Mock 数据覆盖认证、报名、任务、公告、资料、分组、成绩等主流程。

## 3. 认证与账号体系

完成认证和账号相关页面：

```text
src/views/auth/LoginView.vue
src/views/auth/RegisterView.vue
src/views/auth/ForgotPasswordView.vue
src/views/auth/ResetPasswordView.vue
src/views/app/SettingsView.vue
src/api/auth.ts
```

完成目标：

- 登录、注册、找回密码、重置密码流程可用。
- 支持登出、获取当前用户、修改密码。
- 登录后按角色跳转到用户端、负责人端或管理端。
- 为真实后端联调预留 Cookie 登录态、`XSRF-TOKEN`、`X-CSRF-TOKEN` 处理。

验收点：

- Mock 新生、负责人、管理员账号均可登录。
- 刷新受保护页面后登录态不丢失。
- 退出登录后本地登录态被清除。

## 4. 用户端报名闭环

完成用户端首页和报名主流程：

```text
src/views/app/DashboardView.vue
src/views/app/ApplicationsView.vue
src/api/applications.ts
```

完成目标：

- 用户首页展示当前时期、方向、报名状态和关键入口。
- 报名列表可查看当前用户的报名记录。
- 支持报名新增、编辑、详情、撤回。
- 根据当前时期禁用不可用操作，并给出明确提示。
- 报名状态使用统一状态标签展示。

验收点：

- 新生可以完成一次完整报名流程。
- 非报名时期不能新增或编辑报名。
- 撤回类操作有二次确认、loading 和失败提示。

## 5. 用户端任务链路

完成任务列表、任务详情和提交流程：

```text
src/views/app/TasksView.vue
src/views/app/TaskDetailView.vue
src/api/tasks.ts
src/api/files.ts
```

完成目标：

- 展示任务列表、任务状态、截止时间和提交情况。
- 任务详情展示 Markdown 内容、附件和提交要求。
- 支持任务提交、附件上传、提交历史查看。
- 提供提交状态和成绩展示入口。

验收点：

- 用户可以从任务列表进入详情并提交任务。
- 附件上传成功后能随提交一起保存。
- 已提交任务能查看历史记录和批阅结果。

## 6. 用户端展示类页面

补齐用户侧展示类页面：

```text
src/views/app/announcements
src/views/app/materials
src/views/app/groups
src/views/app/scores
```

完成目标：

- 公告列表、公告详情。
- 资料列表、资料详情。
- 我的分组、分组详情。
- 我的成绩。
- Markdown 内容统一使用 `MarkdownViewer` 展示。
- 附件使用统一下载入口。

验收点：

- 用户可以查看公告、资料、分组和成绩。
- Markdown 内容不直接使用 `v-html`。
- 附件链接可访问或下载。

## 7. 真实后端联调

关闭 Mock 后联调真实后端：

```env
VITE_USE_MOCK_API=false
```

完成目标：

- 确认后端运行在 `localhost:8081`。
- 登录接口能设置 HttpOnly Cookie。
- `/api/v1/auth/me` 能根据 Cookie 返回当前用户。
- 后端返回结构符合 `ApiResponse<T>` 和 `PageResult<T>`。
- 文件地址 `/uploads/**` 可访问。

验收点：

- 登录、刷新、退出流程正常。
- 报名新增、编辑、撤回、详情流程正常。
- 任务提交和附件上传流程正常。
- 公告、资料、分组、成绩接口能正常展示。

## 8. 构建检查与 Review

最后进行质量检查和协作把关：

```bash
npm run build
```

完成目标：

- 修复 TypeScript、构建和 lint 类问题。
- Review B 对公共文件的改动。
- 将公共组件、接口、交互规范沉淀回 `README.md` 或 `docs`。

验收点：

- `npm run build` 通过。
- 公共文件改动有明确原因，不破坏用户端主流程。
- 后续 B 开发负责人端、管理端页面时可以复用公共能力。

## 推荐执行顺序总览

```text
工程与公共底座
-> Mock 和接口规范
-> 认证与账号体系
-> 用户端报名闭环
-> 用户端任务链路
-> 用户端展示类页面
-> 真实后端联调
-> 构建检查与 Review
```
