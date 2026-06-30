import type { AxiosAdapter, AxiosResponse, InternalAxiosRequestConfig } from 'axios';

import type { ApiResponse, Announcement, Application, Group, Material, Task, User } from '@/types/api';
import {
  clearMockUser,
  findMockUser,
  hasMockAccount,
  loadMockUser,
  mockAccounts,
  registerMockAccount,
  saveMockUser,
  updateMockPassword,
  verifyMockPassword
} from '@/utils/mockAuth';
import {
  mockAnnouncements,
  mockApplications,
  mockDashboardSummary,
  mockDirections,
  mockGroupMembers,
  mockGroups,
  mockMaterials,
  mockPeriods,
  mockScores,
  mockSubmissions,
  mockTasks,
  mockUsers,
  pageOf
} from './data';

export const mockApiAdapter: AxiosAdapter = async (config) => {
  await sleep(180);

  const method = (config.method || 'get').toLowerCase();
  const path = normalizePath(config.url || '');
  const params = config.params || {};
  const body = parseBody(config.data);

  try {
    const data = handleMockRequest(method, path, params, body);
    return createResponse(config, ok(data));
  } catch (error) {
    const message = error instanceof MockApiError ? error.message : 'Mock 接口未实现';
    const code = error instanceof MockApiError ? error.code : 40400;
    return createResponse(config, fail(code, message));
  }
};

class MockApiError extends Error {
  constructor(
    public code: number,
    message: string
  ) {
    super(message);
  }
}

function getCurrentMockUser() {
  return loadMockUser() || mockAccounts[0].user;
}

function handleMockRequest(method: string, path: string, params: Record<string, unknown>, body: any) {
  if (method === 'get' && path === '/meta/current-period') {
    return {
      currentPeriod: 'REGISTRATION',
      serverTime: '2026-06-27T16:00:00+08:00'
    };
  }

  if (method === 'get' && path === '/directions') {
    return mockDirections;
  }

  if (method === 'post' && path === '/auth/login') {
    const user = findMockUser(body);
    if (!user) {
      throw new MockApiError(40100, '邮箱或密码错误');
    }
    saveMockUser(user);
    return user;
  }

  if (method === 'get' && path === '/auth/me') {
    const user = loadMockUser();
    if (!user) {
      throw new MockApiError(40100, '未登录或登录过期');
    }
    return user;
  }

  if (method === 'post' && path === '/auth/logout') {
    clearMockUser();
    return null;
  }

  if (method === 'post' && path === '/auth/send-email-code') {
    return null;
  }

  if (method === 'post' && path === '/auth/register') {
    const email = String(body.email || '').trim().toLowerCase();

    if (hasMockAccount(email) || mockUsers.some((item) => item.email.toLowerCase() === email)) {
      throw new MockApiError(40900, '该邮箱已注册');
    }

    const user: User = {
      id: nextId(mockUsers),
      username: String(body.username || '').trim(),
      email,
      role: 'FRESHMAN',
      status: 'ACTIVE',
      emailVerified: true,
      groups: []
    };
    mockUsers.push(user);
    registerMockAccount(user, String(body.password || ''));
    return user;
  }

  if (method === 'post' && path === '/auth/forgot-password') {
    return null;
  }

  if (method === 'post' && path === '/auth/reset-password') {
    const email = String(body.email || '').trim().toLowerCase();

    if (!hasMockAccount(email) && !mockUsers.some((item) => item.email.toLowerCase() === email)) {
      throw new MockApiError(40400, '账号不存在');
    }

    updateMockPassword(email, String(body.newPassword || ''));
    return null;
  }

  if (method === 'post' && path === '/auth/change-password') {
    const user = loadMockUser();

    if (!user) {
      throw new MockApiError(40100, '未登录或登录过期');
    }

    if (!verifyMockPassword(user.email, String(body.oldPassword || ''))) {
      throw new MockApiError(40000, '旧密码不正确');
    }

    updateMockPassword(user.email, String(body.newPassword || ''));
    clearMockUser();
    return null;
  }

  if (method === 'get' && path === '/applications') {
    return filterApplications(getCurrentUserApplications(), params);
  }

  if (method === 'get' && path === '/applications/page') {
    return pageOf(
      filterApplications(getCurrentUserApplications(), params),
      numberParam(params.page, 1),
      numberParam(params.size, 10)
    );
  }

  if (method === 'post' && path === '/applications') {
    const user = loadMockUser() || mockAccounts[0].user;
    const application: Application = {
      ...body,
      id: nextId(mockApplications),
      userId: user.id,
      status: 'SUBMITTED',
      statusRemark: null,
      groupId: null,
      createdAt: now(),
      updatedAt: now()
    };
    mockApplications.unshift(application);
    return application;
  }

  const applicationId = matchId(path, /^\/applications\/(\d+)$/);
  if (applicationId) {
    const application = findCurrentUserApplication(applicationId);
    if (method === 'get') return application;
    if (method === 'put') {
      Object.assign(application, body, { updatedAt: now() });
      return application;
    }
    if (method === 'delete') {
      application.status = 'WITHDRAWN';
      application.updatedAt = now();
      return null;
    }
  }

  if (method === 'get' && path === '/applications/summary') {
    const applications = getCurrentUserApplications();
    return {
      applicationCount: applications.length,
      submittedCount: applications.filter((item) => item.status === 'SUBMITTED').length,
      groupedCount: applications.filter((item) => item.status === 'GROUPED').length,
      groupIds: applications.flatMap((item) => (item.groupId ? [item.groupId] : []))
    };
  }

  if (method === 'get' && path === '/announcements') {
    return filterAnnouncements(mockAnnouncements, params);
  }

  const announcementId =
    matchId(path, /^\/announcements\/(\d+)$/) ||
    matchId(path, /^\/admin\/announcements\/(\d+)$/) ||
    matchId(path, /^\/leader\/announcements\/(\d+)$/);
  if (announcementId) {
    const announcement = findById(mockAnnouncements, announcementId, '公告不存在');
    if (method === 'get') return announcement;
    if (method === 'put') {
      Object.assign(announcement, body);
      return announcement;
    }
    if (method === 'delete') {
      removeById(mockAnnouncements, announcementId);
      return null;
    }
  }

  if (method === 'post' && (path === '/admin/announcements' || path === '/leader/announcements')) {
    const announcement = {
      ...body,
      id: nextId(mockAnnouncements),
      publisherName: loadMockUser()?.username || 'mock',
      createdAt: now()
    };
    mockAnnouncements.unshift(announcement);
    return announcement;
  }

  if (method === 'get' && path === '/materials') {
    return filterMaterials(mockMaterials, params);
  }

  const materialId =
    matchId(path, /^\/materials\/(\d+)$/) ||
    matchId(path, /^\/(?:admin|leader)\/groups\/\d+\/materials\/(\d+)$/);
  if (materialId) {
    const material = findById(mockMaterials, materialId, '资料不存在');
    if (method === 'get') return material;
    if (method === 'put') {
      Object.assign(material, body);
      return material;
    }
    if (method === 'delete') {
      removeById(mockMaterials, materialId);
      return null;
    }
  }

  if (method === 'post' && /^\/(admin|leader)\/groups\/\d+\/materials$/.test(path)) {
    const material: Material = {
      ...body,
      id: nextId(mockMaterials),
      groupId: Number(path.match(/groups\/(\d+)/)?.[1] || 0),
      hasAttachment: Boolean(body.attachmentFileId),
      createdAt: now()
    };
    mockMaterials.unshift(material);
    return material;
  }

  if (method === 'post' && path === '/uploads') {
    const file = typeof FormData !== 'undefined' && body instanceof FormData ? (body.get('file') as File | null) : null;
    const fileName = file?.name || 'mock-file.pdf';
    return {
      id: Date.now(),
      fileName,
      url: `/uploads/mock/${fileName}`,
      size: file?.size || 123456
    };
  }

  if (method === 'get' && path === '/tasks') {
    return filterTasks(mockTasks, params);
  }

  const taskId =
    matchId(path, /^\/tasks\/(\d+)$/) ||
    matchId(path, /^\/(?:admin|leader)\/groups\/\d+\/tasks\/(\d+)$/);
  if (taskId) {
    const task = findById(mockTasks, taskId, '任务不存在');
    if (method === 'get') return task;
    if (method === 'put') {
      Object.assign(task, body);
      return task;
    }
    if (method === 'delete') {
      removeById(mockTasks, taskId);
      return null;
    }
  }

  if (method === 'post' && /^\/(admin|leader)\/groups\/\d+\/tasks$/.test(path)) {
    const task: Task = {
      ...body,
      id: nextId(mockTasks),
      groupId: Number(path.match(/groups\/(\d+)/)?.[1] || 0),
      submissionStatus: 'PENDING',
      reviewStatus: 'PENDING'
    };
    mockTasks.unshift(task);
    return task;
  }

  const submitTaskId = matchId(path, /^\/tasks\/(\d+)\/submission$/);
  if (method === 'post' && submitTaskId) {
    const user = getCurrentMockUser();
    const task = findById(mockTasks, submitTaskId, '任务不存在');
    const existing = mockSubmissions.find((item) => item.taskId === submitTaskId && (!item.userId || item.userId === user.id));
    const submission = Object.assign(existing || {}, {
      ...body,
      id: existing?.id || nextOptionalId(mockSubmissions),
      taskId: submitTaskId,
      userId: user.id,
      status: 'SUBMITTED',
      submitVersion: (existing?.submitVersion || 0) + 1,
      isLatest: true,
      submittedAt: now()
    });
    if (!existing) mockSubmissions.push(submission);
    task.submissionStatus = 'SUBMITTED';
    task.reviewStatus = 'SUBMITTED';
    return submission;
  }

  const mySubmissionTaskId = matchId(path, /^\/tasks\/(\d+)\/submission$/);
  if (method === 'get' && mySubmissionTaskId) {
    const user = getCurrentMockUser();
    return (
      mockSubmissions.find((item) => item.taskId === mySubmissionTaskId && (!item.userId || item.userId === user.id)) || {
        taskId: mySubmissionTaskId,
        userId: user.id,
        status: 'PENDING'
      }
    );
  }

  const groupSubmissionTaskId =
    matchId(path, /^\/(?:leader|admin)\/tasks\/(\d+)\/submissions$/);
  if (method === 'get' && groupSubmissionTaskId) {
    return [
      {
        userId: 1,
        username: 'student',
        realName: '张三',
        latestSubmissionId: 502,
        submittedAt: '2026-06-27T10:30:00+08:00',
        reviewed: true,
        score: 92
      }
    ];
  }

  if (method === 'post' && /^\/(admin|leader)\/tasks\/\d+\/submissions\/\d+\/(review|return)$/.test(path)) {
    return null;
  }

  if (method === 'get' && path === '/groups') {
    return pageOf(filterVisibleGroups(mockGroups, params), numberParam(params.page, 1), numberParam(params.size, 10));
  }

  const groupId = matchId(path, /^\/groups\/(\d+)$/);
  if (groupId && method === 'get') {
    return findById(mockGroups, groupId, '分组不存在');
  }

  const membersGroupId = matchId(path, /^\/groups\/(\d+)\/members$/);
  if (membersGroupId && method === 'get') {
    return mockGroupMembers[membersGroupId] || [];
  }

  if (method === 'get' && path === '/admin/dashboard/overview') {
    return mockDashboardSummary;
  }

  if (method === 'get' && path === '/admin/periods') {
    return mockPeriods;
  }

  if (method === 'post' && path === '/admin/periods') {
    return body.periods || [];
  }

  const periodId = matchId(path, /^\/admin\/periods\/(\d+)$/);
  if (method === 'put' && periodId) {
    const period = findById(mockPeriods, periodId, '时期不存在');
    Object.assign(period, body);
    return period;
  }

  if (method === 'get' && path === '/admin/directions') {
    return mockDirections;
  }

  if (method === 'post' && path === '/admin/directions') {
    const direction = { ...body, id: nextDirectionId(), level: body.parentId ? 2 : 1 };
    if (body.parentId) {
      const parent = findDirection(body.parentId);
      parent.children = parent.children || [];
      parent.children.push(direction);
    } else {
      mockDirections.push({ ...direction, children: [] });
    }
    return direction;
  }

  const adminDirectionId = matchId(path, /^\/admin\/directions\/(\d+)$/);
  if (adminDirectionId) {
    const direction = findDirection(adminDirectionId);
    if (method === 'put') {
      Object.assign(direction, body);
      return direction;
    }
    if (method === 'delete') {
      removeDirection(adminDirectionId);
      return null;
    }
  }

  if (method === 'get' && path === '/admin/users') {
    let list = mockUsers;
    if (params.role) list = list.filter((item) => item.role === params.role);
    if (params.status) list = list.filter((item) => item.status === params.status);
    if (params.keyword) {
      const keyword = String(params.keyword);
      list = list.filter((item) => item.username.includes(keyword) || item.email.includes(keyword));
    }
    return pageOf(list, numberParam(params.page, 1), numberParam(params.size, 10));
  }

  const adminUserId = matchId(path, /^\/admin\/users\/(\d+)$/);
  if (method === 'get' && adminUserId) {
    return findById(mockUsers, adminUserId, '用户不存在');
  }

  const adminUserStatusId = matchId(path, /^\/admin\/users\/(\d+)\/status$/);
  if (method === 'patch' && adminUserStatusId) {
    const user = findById(mockUsers, adminUserStatusId, '用户不存在');
    user.status = body.status;
    return user;
  }

  const adminUserRoleId = matchId(path, /^\/admin\/users\/(\d+)\/role$/);
  if (method === 'patch' && adminUserRoleId) {
    const user = findById(mockUsers, adminUserRoleId, '用户不存在');
    if (body.role === 'ADMIN') {
      throw new MockApiError(40300, '不能通过该接口设置管理员角色');
    }
    user.role = body.role;
    return user;
  }

  if (method === 'get' && path === '/admin/groups') {
    return filterVisibleGroups(mockGroups, params);
  }

  if (method === 'get' && path === '/admin/groups/ungrouped-applications') {
    return filterApplications(
      mockApplications.filter((item) => item.status === 'SUBMITTED' && !item.groupId),
      params
    );
  }

  if (method === 'post' && path === '/admin/groups') {
    const group: Group = { ...body, id: nextId(mockGroups), leaderUserId: null };
    mockGroups.unshift(group);
    return group;
  }

  const adminGroupId = matchId(path, /^\/admin\/groups\/(\d+)$/);
  if (adminGroupId) {
    const group = findById(mockGroups, adminGroupId, '分组不存在');
    if (method === 'put') {
      Object.assign(group, body);
      return group;
    }
    if (method === 'delete') {
      removeById(mockGroups, adminGroupId);
      return null;
    }
  }

  if (method === 'post' && /^\/admin\/groups\/\d+\/applications\/\d+$/.test(path)) {
    const [, groupIdText, applicationIdText] = path.match(/^\/admin\/groups\/(\d+)\/applications\/(\d+)$/) || [];
    const groupIdValue = Number(groupIdText);
    const applicationIdValue = Number(applicationIdText);
    const group = findById(mockGroups, groupIdValue, '分组不存在');
    const application = findById(mockApplications, applicationIdValue, '报名申请不存在');

    application.groupId = group.id;
    application.status = 'GROUPED';
    application.updatedAt = now();
    mockGroupMembers[group.id] = mockGroupMembers[group.id] || [];
    if (!mockGroupMembers[group.id].some((item) => item.applicationId === application.id)) {
      mockGroupMembers[group.id].push({
        userId: application.userId || application.id,
        username: mockUsers.find((item) => item.id === application.userId)?.username || application.realName,
        realName: application.realName,
        applicationId: application.id,
        grade: application.grade,
        admissionYear: application.admissionYear,
        directionLevel1Name: getDirectionName(application.directionLevel1Id),
        directionLevel2Name: getDirectionName(application.directionLevel2Id),
        applicationStatus: 'GROUPED'
      });
    }
    return null;
  }

  if (method === 'post' && /^\/admin\/groups\/\d+\/applications\/\d+\/unassign$/.test(path)) {
    const [, groupIdText, applicationIdText] =
      path.match(/^\/admin\/groups\/(\d+)\/applications\/(\d+)\/unassign$/) || [];
    const groupIdValue = Number(groupIdText);
    const applicationIdValue = Number(applicationIdText);
    const application = findById(mockApplications, applicationIdValue, '报名申请不存在');

    application.groupId = null;
    application.status = 'SUBMITTED';
    application.statusRemark = body.remark || null;
    application.updatedAt = now();
    mockGroupMembers[groupIdValue] = (mockGroupMembers[groupIdValue] || []).filter(
      (item) => item.applicationId !== applicationIdValue
    );
    return null;
  }

  const rejectApplicationId = matchId(path, /^\/admin\/applications\/(\d+)\/reject$/);
  if (method === 'post' && rejectApplicationId) {
    const application = findById(mockApplications, rejectApplicationId, '报名申请不存在');
    application.groupId = null;
    application.status = 'REJECTED';
    application.statusRemark = body.reason || '不符合当前分组要求';
    application.updatedAt = now();
    return null;
  }

  const leaderGroupId = matchId(path, /^\/admin\/groups\/(\d+)\/leader$/);
  if (leaderGroupId) {
    const group = findById(mockGroups, leaderGroupId, '分组不存在');
    if (method === 'put') {
      group.leaderUserId = body.leaderUserId;
      return null;
    }
    if (method === 'delete') {
      group.leaderUserId = null;
      return null;
    }
  }

  throw new MockApiError(40400, `Mock 接口未实现：${method.toUpperCase()} ${path}`);
}

function createResponse<T>(
  config: InternalAxiosRequestConfig,
  data: ApiResponse<T>
): AxiosResponse<ApiResponse<T>> {
  return {
    data,
    status: 200,
    statusText: 'OK',
    headers: {},
    config
  };
}

function ok<T>(data: T): ApiResponse<T> {
  return {
    code: 0,
    message: 'ok',
    data,
    timestamp: Date.now(),
    requestId: `mock_${Date.now()}`
  };
}

function fail<T = null>(code: number, message: string, data: T | null = null): ApiResponse<T | null> {
  return {
    code,
    message,
    data,
    timestamp: Date.now(),
    requestId: `mock_${Date.now()}`
  };
}

function sleep(ms: number) {
  return new Promise((resolve) => window.setTimeout(resolve, ms));
}

function parseBody(data: unknown) {
  if (!data) return {};
  if (typeof FormData !== 'undefined' && data instanceof FormData) return data;
  if (typeof data === 'string') {
    try {
      return JSON.parse(data);
    } catch {
      return {};
    }
  }
  return data;
}

function normalizePath(url: string) {
  const withoutOrigin = url.replace(/^https?:\/\/[^/]+/, '');
  const withoutQuery = withoutOrigin.split('?')[0];
  return withoutQuery.replace(/^\/api\/v1/, '') || '/';
}

function numberParam(value: unknown, fallback: number) {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

function matchId(path: string, pattern: RegExp) {
  const match = path.match(pattern);
  return match ? Number(match[1]) : null;
}

function findById<T extends { id: number }>(list: T[], id: number, message: string) {
  const item = list.find((entry) => entry.id === id);
  if (!item) throw new MockApiError(40400, message);
  return item;
}

function getCurrentUserApplications() {
  const user = loadMockUser() || mockAccounts[0].user;
  return mockApplications.filter((application) => !application.userId || application.userId === user.id);
}

function findCurrentUserApplication(id: number) {
  const application = findById(mockApplications, id, '报名申请不存在');
  const user = loadMockUser() || mockAccounts[0].user;

  if (application.userId && application.userId !== user.id) {
    throw new MockApiError(40300, '无权访问该报名申请');
  }

  return application;
}

function removeById<T extends { id: number }>(list: T[], id: number) {
  const index = list.findIndex((entry) => entry.id === id);
  if (index >= 0) list.splice(index, 1);
}

function nextId(list: Array<{ id: number }>) {
  return Math.max(0, ...list.map((item) => item.id)) + 1;
}

function nextOptionalId(list: Array<{ id?: number }>) {
  return Math.max(0, ...list.map((item) => item.id || 0)) + 1;
}

function nextDirectionId() {
  const all = mockDirections.flatMap((item) => [item, ...(item.children || [])]);
  return nextId(all);
}

function findDirection(id: number) {
  const direction = mockDirections.flatMap((item) => [item, ...(item.children || [])]).find((item) => item.id === id);
  if (!direction) throw new MockApiError(40400, '方向不存在');
  return direction;
}

function removeDirection(id: number) {
  const rootIndex = mockDirections.findIndex((item) => item.id === id);
  if (rootIndex >= 0) {
    mockDirections.splice(rootIndex, 1);
    return;
  }

  mockDirections.forEach((parent) => {
    parent.children = (parent.children || []).filter((child) => child.id !== id);
  });
}

function getDirectionName(id: number) {
  try {
    return findDirection(id).name;
  } catch {
    return `方向 #${id}`;
  }
}

function filterApplications(list: Application[], params: Record<string, unknown>) {
  let result = [...list];

  if (params.status) {
    result = result.filter((item) => item.status === params.status);
  }
  if (params.groupId) {
    result = result.filter((item) => item.groupId === Number(params.groupId));
  }
  if (params.directionLevel1Id) {
    result = result.filter((item) => item.directionLevel1Id === Number(params.directionLevel1Id));
  }
  if (params.directionLevel2Id) {
    result = result.filter((item) => item.directionLevel2Id === Number(params.directionLevel2Id));
  }
  if (params.grade) {
    result = result.filter((item) => item.grade === params.grade);
  }
  if (params.admissionYear) {
    result = result.filter((item) => item.admissionYear === Number(params.admissionYear));
  }

  const keyword = stringParam(params.keyword);
  if (keyword) {
    result = result.filter((item) =>
      [item.realName, item.phone, item.college, item.major, item.className, item.introduction]
        .filter(Boolean)
        .some((value) => String(value).includes(keyword))
    );
  }

  return result;
}

function filterAnnouncements(list: Announcement[], params: Record<string, unknown>) {
  let result = filterByScope(list, stringParam(params.scope));

  if (params.groupId) {
    result = result.filter((item) => item.groupId === Number(params.groupId));
  }

  const keyword = stringParam(params.keyword);
  if (keyword) {
    result = result.filter((item) =>
      [item.title, item.contentMarkdown, item.content].filter(Boolean).some((value) => String(value).includes(keyword))
    );
  }

  return result;
}

function filterMaterials(list: Material[], params: Record<string, unknown>) {
  let result = [...list];

  if (params.directionLevel1Id) {
    result = result.filter((item) => item.directionLevel1Id === Number(params.directionLevel1Id));
  }
  if (params.directionLevel2Id) {
    result = result.filter((item) => item.directionLevel2Id === Number(params.directionLevel2Id));
  }

  const hasAttachment = booleanParam(params.hasAttachment);
  if (hasAttachment !== undefined) {
    result = result.filter((item) => Boolean(item.hasAttachment || item.attachmentFileId || item.attachmentUrl) === hasAttachment);
  }

  const keyword = stringParam(params.keyword);
  if (keyword) {
    result = result.filter((item) =>
      [item.title, item.summary, item.contentMarkdown, item.content].filter(Boolean).some((value) => String(value).includes(keyword))
    );
  }

  return result;
}

function filterTasks(list: Task[], params: Record<string, unknown>) {
  let result = [...list];
  const scope = stringParam(params.scope);
  if (scope) {
    result = result.filter((item) => item.scope === scope);
  }

  if (params.groupId) {
    result = result.filter((item) => item.groupId === Number(params.groupId));
  }
  if (params.submissionStatus) {
    result = result.filter((item) => item.submissionStatus === params.submissionStatus);
  }

  const keyword = stringParam(params.keyword);
  if (keyword) {
    result = result.filter((item) =>
      [item.title, item.contentMarkdown, item.content].filter(Boolean).some((value) => String(value).includes(keyword))
    );
  }

  return result;
}

function filterVisibleGroups(list: Group[], params: Record<string, unknown>) {
  const user = getCurrentMockUser();
  const groupIds = new Set((user.groups || []).map((item) => item.id));
  let result = user.role === 'ADMIN' ? [...list] : list.filter((item) => groupIds.has(item.id));

  if (params.directionLevel1Id) {
    result = result.filter((item) => item.directionLevel1Id === Number(params.directionLevel1Id));
  }
  if (params.directionLevel2Id) {
    result = result.filter((item) => item.directionLevel2Id === Number(params.directionLevel2Id));
  }
  if (params.grade) {
    result = result.filter((item) => item.grade === params.grade);
  }
  if (params.admissionYear) {
    result = result.filter((item) => item.admissionYear === Number(params.admissionYear));
  }

  const keyword = stringParam(params.keyword);
  if (keyword) {
    result = result.filter((item) => item.name.includes(keyword));
  }

  return result;
}

function filterByScope<T extends { scope: string }>(list: T[], scope: string) {
  return scope ? list.filter((item) => item.scope === scope) : list;
}

function stringParam(value: unknown) {
  return typeof value === 'string' ? value.trim() : '';
}

function booleanParam(value: unknown) {
  if (typeof value === 'boolean') return value;
  if (value === 'true') return true;
  if (value === 'false') return false;
  return undefined;
}

function now() {
  return new Date().toISOString();
}
