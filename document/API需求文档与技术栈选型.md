# 实验室招新平台 API 需求文档与技术栈选型

> 版本：v2.0
> 日期：2026-06-24
> 依据文档：`requirements.md`

---

## 1. 文档目标

本文档定义：

- 前后端接口边界
- 请求与响应格式
- 鉴权方式
- 权限与时期校验要求
- 错误码约定
- 推荐技术栈

目标是让前端、后端和测试可以直接按本文档开展开发。

---

## 2. 总体架构

### 2.1 架构约定

- 前端：Vue 3 单页应用
- 后端：Spring Boot 提供 REST API
- API 前缀：`/api/v1`
- 前端构建产物由 Spring Boot 托管
- 登录态通过 `JWT + HttpOnly Cookie`

### 2.2 部署约定

推荐单体部署：

1. `frontend` 构建出静态资源
2. 资源产物拷贝到后端 `static/`
3. 后端统一对外提供页面与 API

### 2.3 接口设计原则

- 只返回前端需要的数据，不返回敏感字段
- 列表接口默认分页
- 写操作必须明确权限和业务时期
- 需要跨资源判断的接口必须在后端二次校验

---

## 3. 统一约定

### 3.1 认证方式

- 登录成功后签发 JWT
- JWT 存储于 `HttpOnly Cookie`
- Cookie 名称建议：`lab_recruit_token`
- 服务端通过 Redis 维护：
  - 验证码
  - JWT 黑名单
  - 登录失败计数
  - 限流状态

### 3.2 统一响应体

成功与失败均返回统一结构：

```json
{
  "code": 0,
  "message": "ok",
  "data": {},
  "timestamp": 1761273600000,
  "requestId": "4e9f2a5ef7b146f7"
}
```

字段说明：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | Integer | 业务状态码，`0` 为成功 |
| `message` | String | 人类可读消息 |
| `data` | Object / Array / null | 返回数据 |
| `timestamp` | Long | 服务端响应时间戳 |
| `requestId` | String | 请求追踪 ID |

### 3.3 分页结构

分页请求参数：

- `page`：页码，从 `1` 开始
- `size`：每页条数，默认 `10`，最大 `50`

分页返回结构：

```json
{
  "list": [],
  "page": 1,
  "size": 10,
  "total": 87,
  "totalPages": 9
}
```

### 3.4 时间与时区

- 所有时间字段统一使用 ISO 8601 字符串
- 示例：`2026-06-24T15:30:00+08:00`
- 项目统一使用 `Asia/Shanghai`

### 3.5 文件上传约定

- 使用 `multipart/form-data`
- 单文件最大 `50MB`
- 允许类型：
  - `pdf`
  - `doc`
  - `docx`
  - `zip`
  - `rar`
  - `png`
  - `jpg`
  - `jpeg`
- 后端必须校验 MIME 与扩展名

### 3.6 排序约定

未额外说明时，列表默认按 `createdAt desc` 返回。

---

## 4. 枚举与固定字典

### 4.1 角色枚举

| 值 | 说明 |
| --- | --- |
| `FRESHMAN` | 新生 |
| `LEADER` | 负责人 |
| `ADMIN` | 管理员 |

### 4.2 用户状态

| 值 | 说明 |
| --- | --- |
| `ACTIVE` | 正常 |
| `DISABLED` | 禁用 |

### 4.3 报名申请状态

| 值 | 说明 |
| --- | --- |
| `SUBMITTED` | 已提交，待分组 |
| `GROUPED` | 已分组 |
| `REJECTED` | 已驳回或系统关闭 |
| `WITHDRAWN` | 已撤回 |

### 4.4 时期枚举

| 值 | 说明 |
| --- | --- |
| `REGISTRATION` | 报名期 |
| `SELECTION` | 选拔期 |
| `INTERVIEW` | 面试期 |
| `FINISHED` | 已结束，运行时推导 |

### 4.5 公告/任务范围

| 值 | 说明 |
| --- | --- |
| `GLOBAL` | 全局 |
| `GROUP` | 分组 |

### 4.6 年级枚举

| 值 | 说明 |
| --- | --- |
| `YEAR_1` | 大一 |
| `YEAR_2` | 大二 |
| `YEAR_3` | 大三 |
| `YEAR_4` | 大四 |
| `GRADUATED` | 已毕业 |

---

## 5. 错误码约定

| code | HTTP | 说明 |
| --- | --- | --- |
| `0` | `200` | 成功 |
| `40000` | `400` | 参数错误 |
| `40100` | `401` | 未登录或登录过期 |
| `40300` | `403` | 无权限 |
| `40310` | `403` | 当前时期不允许该操作 |
| `40400` | `404` | 资源不存在 |
| `40900` | `409` | 数据冲突，例如用户名或邮箱重复 |
| `40910` | `409` | 业务状态冲突，例如重复报名 |
| `42200` | `422` | 数据校验失败 |
| `42900` | `429` | 请求过于频繁 |
| `50000` | `500` | 系统异常 |

说明：

- 后端返回 HTTP 状态码，同时返回 `code`
- 前端错误处理优先读 `code`

---

## 6. 权限与时期校验原则

### 6.1 权限原则

- `/api/v1/admin/**`：仅 `ADMIN`
- `/api/v1/leader/**`：`LEADER` 或 `ADMIN`
- `/api/v1/**` 下的普通业务接口按资源归属继续校验

### 6.2 时期原则

- 注册、报名：仅 `REGISTRATION`
- 任务发布、任务提交、分组：仅 `SELECTION`
- 登录、登出、修改密码、找回密码：不受时期限制
- 公告、资料和历史成绩查看：全时期允许

### 6.3 资源归属原则

- 用户只能访问自己的报名、自己的提交、自己的成绩
- 负责人只能访问自己负责组的成员、公告、任务和提交
- 管理员不受组归属限制

---

## 7. 接口清单

## 7.1 认证模块

### `POST /api/v1/auth/send-email-code`

用途：发送邮箱验证码。  
权限：匿名可访问。  
时期限制：注册场景仅报名期可发；找回密码场景全时期可发。

请求体：

```json
{
  "email": "user@example.com",
  "scene": "REGISTER"
}
```

字段：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `email` | 是 | 邮箱 |
| `scene` | 是 | `REGISTER` 或 `RESET_PASSWORD` |

成功响应：

```json
{
  "code": 0,
  "message": "验证码已发送",
  "data": null,
  "timestamp": 1761273600000,
  "requestId": "req_001"
}
```

失败场景：

- 邮箱格式不合法
- 发送过于频繁
- 注册场景下邮箱已被占用
- 找回密码场景下邮箱不存在

### `POST /api/v1/auth/register`

用途：注册账号。  
权限：匿名可访问。  
时期限制：仅报名期。

请求体：

```json
{
  "username": "zhangsan",
  "email": "user@example.com",
  "password": "Pass1234",
  "confirmPassword": "Pass1234",
  "code": "123456"
}
```

字段：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `username` | 是 | 3 到 32 位，字母数字下划线，唯一 |
| `email` | 是 | 唯一 |
| `password` | 是 | 至少 8 位，必须包含字母和数字 |
| `confirmPassword` | 是 | 与密码一致 |
| `code` | 是 | 邮箱验证码 |

成功响应：

```json
{
  "code": 0,
  "message": "注册成功",
  "data": {
    "id": 1,
    "username": "zhangsan",
    "email": "user@example.com",
    "role": "FRESHMAN"
  },
  "timestamp": 1761273600000,
  "requestId": "req_002"
}
```

### `POST /api/v1/auth/login`

用途：邮箱密码登录。  
权限：匿名可访问。  
时期限制：无。

请求体：

```json
{
  "email": "user@example.com",
  "password": "Pass1234",
  "rememberMe": true
}
```

成功响应：

```json
{
  "code": 0,
  "message": "登录成功",
  "data": {
    "id": 1,
    "username": "zhangsan",
    "email": "user@example.com",
    "role": "FRESHMAN",
    "status": "ACTIVE",
    "emailVerified": true
  },
  "timestamp": 1761273600000,
  "requestId": "req_003"
}
```

说明：

- JWT 不放在响应体，写入 `HttpOnly Cookie`
- `rememberMe=true` 时设置更长的 Cookie TTL

### `POST /api/v1/auth/logout`

用途：退出登录。  
权限：已登录。  
时期限制：无。

成功响应：

```json
{
  "code": 0,
  "message": "退出成功",
  "data": null,
  "timestamp": 1761273600000,
  "requestId": "req_004"
}
```

### `GET /api/v1/auth/me`

用途：获取当前登录用户信息。  
权限：已登录。  
时期限制：无。

成功响应：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": 1,
    "username": "zhangsan",
    "email": "user@example.com",
    "role": "LEADER",
    "status": "ACTIVE",
    "emailVerified": true,
    "leaderGroupId": 10,
    "groups": [
      {
        "id": 10,
        "name": "后端-Java-1组"
      },
      {
        "id": 21,
        "name": "前端-Vue-2组"
      }
    ]
  },
  "timestamp": 1761273600000,
  "requestId": "req_005"
}
```

### `POST /api/v1/auth/forgot-password`

用途：触发找回密码验证码发送。  
权限：匿名可访问。  
时期限制：无。

请求体：

```json
{
  "email": "user@example.com"
}
```

说明：

- 逻辑上等价于 `send-email-code(scene=RESET_PASSWORD)`，保留该接口是为了前端语义更清晰

### `POST /api/v1/auth/reset-password`

用途：验证码重置密码。  
权限：匿名可访问。  
时期限制：无。

请求体：

```json
{
  "email": "user@example.com",
  "code": "123456",
  "newPassword": "NewPass1234",
  "confirmPassword": "NewPass1234"
}
```

成功响应：`data = null`

### `POST /api/v1/auth/change-password`

用途：登录后修改密码。  
权限：已登录。  
时期限制：无。

请求体：

```json
{
  "oldPassword": "Pass1234",
  "newPassword": "NewPass1234",
  "confirmPassword": "NewPass1234"
}
```

成功响应：`data = null`

说明：

- 修改成功后当前登录态立即失效，前端需要重新登录

---

## 7.2 当前时期与公共字典

### `GET /api/v1/meta/current-period`

用途：获取当前时期。  
权限：匿名可访问。  
时期限制：无。

成功响应：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "currentPeriod": "REGISTRATION",
    "serverTime": "2026-06-24T15:30:00+08:00"
  },
  "timestamp": 1761273600000,
  "requestId": "req_006"
}
```

### `GET /api/v1/directions`

用途：获取方向树。  
权限：匿名可访问。  
时期限制：无。

查询参数：

- `enabled=true` 可选，默认只返回启用方向

成功响应：

```json
{
  "code": 0,
  "message": "ok",
  "data": [
    {
      "id": 1,
      "name": "后端",
      "level": 1,
      "children": [
        {
          "id": 2,
          "name": "Java",
          "level": 2
        }
      ]
    }
  ],
  "timestamp": 1761273600000,
  "requestId": "req_007"
}
```

---

## 7.3 个人资料与报名模块

### `GET /api/v1/profile`

用途：获取当前用户主页所需概要信息。  
权限：已登录。  
时期限制：无。

成功响应：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "user": {
      "id": 1,
      "username": "zhangsan",
      "email": "user@example.com",
      "role": "FRESHMAN"
    },
    "applicationCount": 2,
    "groupCount": 2,
    "groups": [
      {
        "id": 10,
        "name": "后端-Java-1组"
      },
      {
        "id": 21,
        "name": "前端-Vue-2组"
      }
    ]
  },
  "timestamp": 1761273600000,
  "requestId": "req_008"
}
```

### `GET /api/v1/applications`

用途：获取当前用户的报名申请列表。  
权限：已登录。  
时期限制：无。

成功响应：

```json
{
  "code": 0,
  "message": "ok",
  "data": [
    {
      "id": 100,
      "realName": "张三",
      "phone": "13800000000",
      "college": "计算机学院",
      "major": "软件工程",
      "className": "软工1班",
      "grade": "YEAR_1",
      "admissionYear": 2026,
      "directionLevel1Id": 1,
      "directionLevel2Id": 2,
      "introduction": "对后端开发感兴趣",
      "status": "GROUPED",
      "statusRemark": null,
      "groupId": 10,
      "createdAt": "2026-06-24T15:30:00+08:00",
      "updatedAt": "2026-06-24T15:40:00+08:00"
    }
  ],
  "timestamp": 1761273600000,
  "requestId": "req_009"
}
```

### `GET /api/v1/applications/{id}`

用途：获取当前用户某一份报名申请详情。  
权限：已登录。  
时期限制：无。  
资源校验：只能查看自己的申请；管理员通过管理端接口查看全部。

### `POST /api/v1/applications`

用途：创建一份报名申请。  
权限：`FRESHMAN`。  
时期限制：仅报名期。

请求体：

```json
{
  "realName": "张三",
  "phone": "13800000000",
  "college": "计算机学院",
  "major": "软件工程",
  "className": "软工1班",
  "grade": "YEAR_1",
  "admissionYear": 2026,
  "directionLevel1Id": 1,
  "directionLevel2Id": 2,
  "introduction": "对后端开发感兴趣"
}
```

成功响应：返回完整申请对象。

失败场景：

- 非报名期
- 同一用户已存在相同 `directionLevel2Id` 的申请
- 二级方向不属于所选一级方向
- 当前招新若仅允许大一、大二，则提交其他年级会被拒绝

### `PUT /api/v1/applications/{id}`

用途：修改本人报名申请。  
权限：`FRESHMAN`。  
时期限制：仅报名期，且该申请尚未分组。

请求体与 `POST /applications` 相同。

失败场景：

- 申请不存在
- 当前不是报名期
- 申请已分组或已撤回，禁止修改
- 修改后与本人其他申请出现方向冲突

### `DELETE /api/v1/applications/{id}`

用途：撤回本人报名申请。  
权限：`FRESHMAN`。  
时期限制：仅报名期。

规则：

- 仅 `SUBMITTED` 状态申请允许撤回
- 撤回后状态改为 `WITHDRAWN`

### `GET /api/v1/applications/summary`

用途：获取当前用户报名申请汇总状态。  
权限：已登录。  
时期限制：无。

成功响应：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "applicationCount": 2,
    "submittedCount": 1,
    "groupedCount": 2,
    "groupIds": [10, 21]
  },
  "timestamp": 1761273600000,
  "requestId": "req_010"
}
```

---

## 7.4 公告模块

### `GET /api/v1/announcements`

用途：获取当前用户可见公告列表。  
权限：已登录。  
时期限制：无。

查询参数：

| 参数 | 说明 |
| --- | --- |
| `scope` | 可选，`GLOBAL` 或 `GROUP` |
| `page` | 可选 |
| `size` | 可选 |

成功响应：`data` 为分页结构，`list` 元素包含：

```json
{
  "id": 1,
  "title": "选拔通知",
  "scope": "GLOBAL",
  "groupId": null,
  "publisherName": "管理员",
  "createdAt": "2026-06-24T15:30:00+08:00"
}
```

### `GET /api/v1/announcements/{id}`

用途：获取公告详情。  
权限：已登录。  
时期限制：无。  
资源校验：组内公告仅组成员、组负责人、管理员可见。

### `POST /api/v1/admin/announcements`

用途：发布全局公告。  
权限：`ADMIN`。  
时期限制：无。

请求体：

```json
{
  "title": "全局公告",
  "content": "## 内容",
  "scope": "GLOBAL"
}
```

### `POST /api/v1/leader/announcements`

用途：发布组内公告。  
权限：`LEADER` 或 `ADMIN`。  
时期限制：无。  
资源校验：若为 `LEADER`，`groupId` 必须为其负责组。

请求体：

```json
{
  "title": "组内公告",
  "content": "## 内容",
  "scope": "GROUP",
  "groupId": 10
}
```

### `PUT /api/v1/announcements/{id}`

用途：编辑公告。  
权限：

- 管理员可编辑所有公告
- 负责人仅可编辑自己负责组且自己有权限的公告

### `DELETE /api/v1/announcements/{id}`

用途：删除公告。  
权限规则同编辑。

---

## 7.5 学习资料模块

### `GET /api/v1/materials`

用途：资料列表。  
权限：已登录。  
时期限制：无。

查询参数：

| 参数 | 说明 |
| --- | --- |
| `directionLevel1Id` | 可选 |
| `directionLevel2Id` | 可选 |
| `page` | 可选 |
| `size` | 可选 |

返回列表元素：

```json
{
  "id": 1,
  "title": "Java 学习指南",
  "summary": "基础资料",
  "directionLevel1Id": 1,
  "directionLevel2Id": 2,
  "hasAttachment": true,
  "createdAt": "2026-06-24T15:30:00+08:00"
}
```

### `GET /api/v1/materials/{id}`

用途：资料详情。  
权限：已登录。  
时期限制：无。

### `POST /api/v1/admin/materials`

用途：发布资料。  
权限：`ADMIN`。  
时期限制：无。

请求体：

```json
{
  "title": "Java 学习指南",
  "summary": "基础资料",
  "content": "## 内容",
  "attachmentUrl": "/uploads/materials/guide.pdf",
  "directionLevel1Id": 1,
  "directionLevel2Id": 2
}
```

### `PUT /api/v1/admin/materials/{id}`

用途：编辑资料。  
权限：`ADMIN`。

### `DELETE /api/v1/admin/materials/{id}`

用途：删除资料。  
权限：`ADMIN`。

---

## 7.6 文件上传模块

### `POST /api/v1/files/upload`

用途：上传资料、任务、提交附件。  
权限：已登录。  
时期限制：无。

表单字段：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `file` | 是 | 文件本体 |
| `bizType` | 是 | `MATERIAL` / `TASK` / `SUBMISSION` |

成功响应：

```json
{
  "code": 0,
  "message": "上传成功",
  "data": {
    "fileName": "guide.pdf",
    "url": "/uploads/2026/06/guide.pdf",
    "size": 123456
  },
  "timestamp": 1761273600000,
  "requestId": "req_011"
}
```

---

## 7.7 任务模块

### `GET /api/v1/tasks`

用途：获取当前用户可见任务列表。  
权限：已登录。  
时期限制：无。

查询参数：

| 参数 | 说明 |
| --- | --- |
| `scope` | 可选 |
| `page` | 可选 |
| `size` | 可选 |

返回列表元素：

```json
{
  "id": 1,
  "title": "完成 Java 基础练习",
  "scope": "GROUP",
  "groupId": 10,
  "maxScore": 100,
  "deadlineAt": "2026-06-28T23:59:59+08:00",
  "submissionStatus": "SUBMITTED",
  "reviewStatus": "REVIEWED"
}
```

### `GET /api/v1/tasks/{id}`

用途：任务详情。  
权限：已登录。  
时期限制：无。  
资源校验：仅任务可见范围内的用户可访问。

### `POST /api/v1/leader/tasks`

用途：发布组任务。  
权限：`LEADER` 或 `ADMIN`。  
时期限制：仅选拔期。  
资源校验：`LEADER` 只能发到自己负责组。

请求体：

```json
{
  "title": "完成 Java 基础练习",
  "content": "## 要求",
  "scope": "GROUP",
  "groupId": 10,
  "attachmentUrl": "/uploads/tasks/task1.zip",
  "maxScore": 100,
  "deadlineAt": "2026-06-28T23:59:59+08:00"
}
```

### `POST /api/v1/admin/tasks`

用途：管理员发布任务。  
权限：`ADMIN`。  
时期限制：仅选拔期。  
说明：管理员可发布全局任务，也可发布指定组任务。

请求体：

```json
{
  "title": "全局任务",
  "content": "## 要求",
  "scope": "GLOBAL",
  "groupId": null,
  "attachmentUrl": null,
  "maxScore": 100,
  "deadlineAt": "2026-06-30T23:59:59+08:00"
}
```

额外规则：

- `scope=GLOBAL` 时，`groupId` 必须为空
- `scope=GROUP` 时，`groupId` 必须存在且对应合法分组

### `PUT /api/v1/tasks/{id}`

用途：编辑任务。  
权限：

- 管理员可编辑所有任务
- 负责人仅可编辑自己负责组的任务

时期限制：仅选拔期。

### `DELETE /api/v1/tasks/{id}`

用途：删除任务。  
权限规则同编辑。  
说明：删除任务时必须同步删除其提交与批阅，或转为逻辑删除。建议本项目采用逻辑删除。

### `POST /api/v1/tasks/{id}/submissions`

用途：提交任务。  
权限：`FRESHMAN`。  
时期限制：仅选拔期。  
资源校验：用户必须对该任务可见。

请求体：

```json
{
  "content": "本次提交说明",
  "attachmentUrl": "/uploads/submissions/homework.zip"
}
```

成功响应：

```json
{
  "code": 0,
  "message": "提交成功",
  "data": {
    "id": 501,
    "taskId": 1,
    "submitVersion": 3,
    "isLatest": true,
    "submittedAt": "2026-06-24T15:30:00+08:00"
  },
  "timestamp": 1761273600000,
  "requestId": "req_012"
}
```

### `GET /api/v1/tasks/{id}/submissions/me`

用途：查看本人在某任务下的提交历史。  
权限：已登录。  
时期限制：无。  
资源校验：只能查看本人。

### `GET /api/v1/tasks/{id}/submissions/group`

用途：查看组内某任务提交情况。  
权限：`LEADER` 或 `ADMIN`。  
时期限制：无。  
资源校验：`LEADER` 只能查看自己负责组。

返回列表元素：

```json
{
  "userId": 1,
  "username": "zhangsan",
  "realName": "张三",
  "latestSubmissionId": 501,
  "submittedAt": "2026-06-24T15:30:00+08:00",
  "reviewed": true,
  "score": 95
}
```

### `POST /api/v1/submissions/{id}/review`

用途：批阅提交。  
权限：`LEADER` 或 `ADMIN`。  
时期限制：无。  
资源校验：`LEADER` 只能批阅自己负责组。

请求体：

```json
{
  "score": 95,
  "comment": "完成度较高，继续补充异常处理。"
}
```

### `GET /api/v1/submissions/mine`

用途：查看当前用户全部成绩。  
权限：已登录。  
时期限制：无。

返回列表元素：

```json
{
  "taskId": 1,
  "taskTitle": "完成 Java 基础练习",
  "score": 95,
  "maxScore": 100,
  "comment": "完成度较高，继续补充异常处理。",
  "reviewedAt": "2026-06-25T20:00:00+08:00"
}
```

---

## 7.8 分组与组员模块

### `GET /api/v1/groups/{id}`

用途：查看分组详情。  
权限：已登录。  
资源校验：

- 组成员可查看自己所在组
- 负责人可查看自己负责组
- 管理员可查看全部

### `GET /api/v1/groups/{id}/members`

用途：查看组员列表。  
权限：`LEADER` 或 `ADMIN`。  
资源校验：`LEADER` 仅可查看自己负责组。

返回列表元素：

```json
{
  "userId": 1,
  "username": "zhangsan",
  "realName": "张三",
  "applicationId": 100,
  "grade": "YEAR_1",
  "admissionYear": 2026,
  "directionLevel1Name": "后端",
  "directionLevel2Name": "Java",
  "applicationStatus": "GROUPED"
}
```

### `GET /api/v1/admin/groups`

用途：管理员查看分组列表。  
权限：`ADMIN`。  
时期限制：无。

查询参数：

| 参数 | 说明 |
| --- | --- |
| `directionLevel1Id` | 可选 |
| `directionLevel2Id` | 可选 |
| `grade` | 可选 |
| `admissionYear` | 可选 |
| `page` | 可选 |
| `size` | 可选 |

### `POST /api/v1/admin/groups`

用途：创建分组。  
权限：`ADMIN`。  
时期限制：仅选拔期。

请求体：

```json
{
  "name": "后端-Java-1组",
  "directionLevel1Id": 1,
  "directionLevel2Id": 2,
  "grade": "YEAR_1",
  "admissionYear": 2026,
  "maxSize": 20
}
```

### `PUT /api/v1/admin/groups/{id}`

用途：修改分组。  
权限：`ADMIN`。  
时期限制：仅选拔期。

### `DELETE /api/v1/admin/groups/{id}`

用途：删除分组。  
权限：`ADMIN`。  
时期限制：仅选拔期。  
限制：组内存在成员时禁止删除。

### `POST /api/v1/admin/groups/auto-assign`

用途：触发自动分组。  
权限：`ADMIN`。  
时期限制：仅选拔期。

请求体：

```json
{
  "dryRun": false
}
```

返回值建议：

```json
{
  "assignedCount": 50,
  "unassignedCount": 7,
  "unassignedApplicationIds": [101, 102]
}
```

自动分组规则补充：

- 以申请为候选单元
- 同一用户可因不同申请同时进入多个分组
- 同一申请最多只能被分配到一个分组

### `POST /api/v1/admin/groups/{id}/applications/{applicationId}`

用途：将某份申请调整到指定分组。  
权限：`ADMIN`。  
时期限制：仅选拔期。

说明：

- 若该申请已有旧分组，系统自动先移除旧关系再加入新组
- 必须校验方向、年级、入学年份和容量
- 同一申请若已存在旧组关系，调整时必须先移除旧关系再加入新组

### `DELETE /api/v1/admin/groups/{id}/applications/{applicationId}`

用途：将某份申请移出分组。  
权限：`ADMIN`。  
时期限制：仅选拔期。

### `POST /api/v1/admin/groups/{id}/leader`

用途：任命负责人。  
权限：`ADMIN`。  
时期限制：仅选拔期。

请求体：

```json
{
  "userId": 1
}
```

### `DELETE /api/v1/admin/groups/{id}/leader`

用途：撤销负责人。  
权限：`ADMIN`。  
时期限制：仅选拔期。

---

## 7.9 学习方向管理模块

### `GET /api/v1/admin/directions`

用途：管理员查看完整方向树。  
权限：`ADMIN`。

### `POST /api/v1/admin/directions`

用途：新增方向。  
权限：`ADMIN`。

请求体：

```json
{
  "parentId": 1,
  "name": "Java",
  "sortOrder": 10,
  "enabled": true
}
```

说明：

- `parentId = null` 表示新增一级方向
- `parentId != null` 表示新增二级方向

### `PUT /api/v1/admin/directions/{id}`

用途：修改方向。  
权限：`ADMIN`。

### `DELETE /api/v1/admin/directions/{id}`

用途：删除方向。  
权限：`ADMIN`。  
限制：存在报名、分组、资料绑定时禁止删除。

---

## 7.10 招新时期管理模块

### `GET /api/v1/admin/periods`

用途：获取全部时期配置。  
权限：`ADMIN`。

成功响应：

```json
{
  "code": 0,
  "message": "ok",
  "data": [
    {
      "id": 1,
      "periodType": "REGISTRATION",
      "startTime": "2026-06-20T00:00:00+08:00",
      "endTime": "2026-06-30T23:59:59+08:00",
      "enabled": true
    }
  ],
  "timestamp": 1761273600000,
  "requestId": "req_013"
}
```

### `POST /api/v1/admin/periods`

用途：批量创建或覆盖时期配置。  
权限：`ADMIN`。

请求体：

```json
{
  "periods": [
    {
      "periodType": "REGISTRATION",
      "startTime": "2026-06-20T00:00:00+08:00",
      "endTime": "2026-06-30T23:59:59+08:00",
      "enabled": true
    },
    {
      "periodType": "SELECTION",
      "startTime": "2026-07-01T00:00:00+08:00",
      "endTime": "2026-07-15T23:59:59+08:00",
      "enabled": true
    }
  ]
}
```

校验规则：

- 同一时期只能有一条启用配置
- 三个时期时间不能重叠

### `PUT /api/v1/admin/periods/{id}`

用途：修改单个时期配置。  
权限：`ADMIN`。

---

## 7.11 用户与数据看板模块

### `GET /api/v1/admin/users`

用途：管理员查看用户与报名列表。  
权限：`ADMIN`。

查询参数：

| 参数 | 说明 |
| --- | --- |
| `role` | 可选 |
| `applicationStatus` | 可选 |
| `groupId` | 可选 |
| `keyword` | 可选，匹配用户名/邮箱/姓名 |
| `page` | 可选 |
| `size` | 可选 |

### `GET /api/v1/admin/users/{id}`

用途：管理员查看用户全量信息。  
权限：`ADMIN`。

### `GET /api/v1/admin/dashboard/summary`

用途：看板统计。  
权限：`ADMIN`。

成功响应：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "userCount": 120,
    "applicationCount": 95,
    "groupedUserCount": 80,
    "groupedApplicationCount": 110,
    "unassignedApplicationCount": 15,
    "leaderCount": 6,
    "taskCompletionRate": 0.76
  },
  "timestamp": 1761273600000,
  "requestId": "req_014"
}
```

### `GET /api/v1/admin/dashboard/export`

用途：导出报名、分组或成绩数据。  
权限：`ADMIN`。

查询参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `type` | 是 | `APPLICATION` / `GROUP_RESULT` / `TASK_SCORE` |

响应：

- 文件流下载
- `Content-Type` 使用 Excel 对应 MIME

---

## 8. 推荐技术栈

### 8.1 前端

| 层级 | 选型 | 说明 |
| --- | --- | --- |
| 框架 | Vue 3 | 组合式 API，适合表单和后台页面 |
| 构建 | Vite | 构建快 |
| 语言 | TypeScript | 保证接口模型一致性 |
| 路由 | Vue Router 4 | 路由守卫做前端权限控制 |
| 状态管理 | Pinia | 管理用户态、字典、当前时期 |
| UI 组件 | Element Plus | 适合管理端、表单、表格 |
| 请求库 | Axios | 统一拦截器 |
| Markdown | `markdown-it` + 编辑器组件 | 渲染和编辑 |
| 图表 | ECharts | 管理看板 |

### 8.2 后端

| 层级 | 选型 | 说明 |
| --- | --- | --- |
| 框架 | Spring Boot | 主框架 |
| Web | Spring MVC | REST API |
| 安全 | Spring Security | 登录和权限 |
| 认证 | JWT + HttpOnly Cookie | 满足安全与单体部署 |
| ORM | Spring Data JPA | 本项目数据模型中等复杂，足够使用 |
| 校验 | Jakarta Validation | DTO 字段校验 |
| 缓存 | Spring Data Redis | 验证码、黑名单、限流 |
| 邮件 | Spring Mail | 注册和找回密码 |
| 文档 | springdoc-openapi | 自动生成接口文档 |
| 数据迁移 | Flyway | 管理数据库版本 |
| 对象映射 | MapStruct | DTO/VO 转换 |
| JWT 库 | `jjwt` 或 `java-jwt` | 签发与解析 Token |

### 8.3 数据层

| 组件 | 选型 | 说明 |
| --- | --- | --- |
| 主数据库 | MySQL 8 | 核心业务数据 |
| 缓存 | Redis 7 | 临时状态和限流 |
| 文件存储 | 本地目录，预留 MinIO 抽象 | 便于先做单机部署 |

### 8.4 运行环境

| 组件 | 说明 |
| --- | --- |
| JDK | 与后端工程保持一致，建议在团队内固定具体版本 |
| Maven | 后端构建 |
| Node.js | 前端构建 |

---

## 9. 推荐项目结构

### 9.1 后端

```text
backend
├── src/main/java/club/muimi/backend
│   ├── common
│   ├── config
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── exception
│   ├── repository
│   ├── security
│   ├── service
│   ├── support
│   └── vo
└── src/main/resources
    ├── application.properties
    ├── db/migration
    ├── static
    └── templates
```

### 9.2 前端

```text
frontend
├── src
│   ├── api
│   ├── components
│   ├── router
│   ├── stores
│   ├── types
│   ├── utils
│   └── views
└── public
```

---

## 10. 开发实施建议

### 10.1 后端优先顺序

1. 统一响应体、异常码、全局异常处理
2. 用户、报名、方向、时期、分组表结构与迁移脚本
3. 认证模块
4. 权限与时期校验组件
5. 报名模块
6. 分组模块
7. 公告、资料、任务模块
8. 看板与导出

### 10.2 前端优先顺序

1. 登录/注册/找回密码
2. 用户态与路由守卫
3. 报名页
4. 公告与资料展示
5. 管理端基础布局
6. 分组与任务管理

### 10.3 必须实现的后端校验

- 用户名、邮箱唯一
- 报名期校验
- 报名年级和入学年份合法性校验
- 同一用户申请方向唯一校验
- 方向父子关系校验
- 分组容量校验
- 分组年级和入学年份匹配校验
- 同一申请只能进入一个分组
- 负责人组归属校验
- 任务截止时间校验
- 分数区间校验
- 文件类型和大小校验

---

## 11. 文档结论

本文件已经将 API 接口粒度下沉到可开发层面。  
开发时如需新增接口，应满足以下要求：

- 命名风格与本文件一致
- 补充权限说明
- 补充时期限制
- 补充请求字段和响应示例
- 不得绕过统一错误码和统一响应结构
