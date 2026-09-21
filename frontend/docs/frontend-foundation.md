# 前端公共底座规范

本文档记录 A 负责维护的公共底座约定。页面开发应优先复用这些能力，避免在业务页面里重复封装。

## 接口调用

- 页面不得直接使用 `axios`。
- 业务接口统一放在 `src/api/*.ts`。
- 请求函数优先复用 `src/api/http.ts` 中的 `getData`、`postData`、`putData`、`deleteData`。
- 接口返回结构统一为 `ApiResponse<T>`，分页返回结构统一为 `PageResult<T>`。

## 路由 Meta

路由 `meta` 已在 `src/types/router.d.ts` 中声明类型：

```ts
meta: {
  title: '我的报名',
  description: '查看、创建、编辑或撤回本人报名申请。',
  requiresAuth: true,
  roles: ['FRESHMAN'],
  periodHint: ['REGISTRATION'],
  layout: 'user'
}
```

约定：

- `requiresAuth` 控制是否需要登录。
- `roles` 控制前端角色访问体验，后端仍必须鉴权。
- `periodHint` 只用于页面按钮禁用、提示和展示，不代替后端时期校验。
- 登录态恢复由路由守卫调用 `authStore.fetchMe()`。
- 当前时期和方向数据由 `metaStore.bootstrap()` 初始化。

## Store 初始化

`src/stores/auth.ts`：

- `initialized` 表示是否已经尝试恢复登录态。
- `loading` 表示认证请求进行中。
- 页面需要当前用户时优先使用 `authStore.user`、`authStore.role`、`authStore.hasRole()`。

`src/stores/meta.ts`：

- `initialized` 表示当前时期和方向数据是否已经初始化。
- `bootstrap()` 会同时加载当前时期和方向数据，并复用进行中的初始化请求。
- `isPeriodActive(periods)` 用于判断当前时期是否允许某类操作。

## 公共组件

公共组件可从目录入口导入：

```ts
import { PageHeader, PeriodBadge, StatusTag } from '@/components/common';
import { DirectionCascader } from '@/components/forms';
import { MarkdownViewer } from '@/components/markdown';
```

### PageHeader

```vue
<PageHeader title="我的报名" description="查看、创建、编辑或撤回本人报名申请。">
  <template #actions>
    <el-button type="primary">新增报名</el-button>
  </template>
</PageHeader>
```

### StatusTag

用于报名状态和任务提交状态：

```vue
<StatusTag :value="application.status" />
<StatusTag :value="task.submissionStatus" />
```

### PeriodBadge

用于展示当前时期：

```vue
<PeriodBadge :period="metaStore.period" />
```

### DirectionCascader

用于两级方向选择：

```vue
<DirectionCascader
  v-model="directionPath"
  :disabled="submitting"
  @change="handleDirectionChange"
/>
```

### MarkdownViewer

用于展示后端 Markdown 内容，不要在页面里直接使用 `v-html`：

```vue
<MarkdownViewer :content="detail.content" />
```

## 页面实现要求

- 页面标题统一使用 `PageHeader`。
- 状态展示统一使用 `StatusTag`。
- 当前时期展示统一使用 `PeriodBadge`。
- 方向选择统一使用 `DirectionCascader`。
- Markdown 展示统一使用 `MarkdownViewer`。
- 写操作必须有 loading 和失败提示。
- 删除、撤回类操作必须二次确认。
