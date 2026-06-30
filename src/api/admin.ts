import { deleteData, getData, patchData, postData, putData } from './http';
import type {
  AdminDashboardSummary,
  Application,
  Direction,
  Grade,
  Group,
  PageResult,
  PeriodType,
  Role,
  User
} from '@/types/api';

export interface PeriodConfig {
  id?: number;
  periodType: PeriodType;
  startTime: string;
  endTime: string;
  enabled: boolean;
}

export interface DirectionPayload {
  parentId?: number | null;
  name: string;
  sortOrder: number;
  enabled: boolean;
}

export interface GroupPayload {
  name: string;
  directionLevel1Id: number;
  directionLevel2Id: number;
  grade: Grade;
  admissionYear: number;
  maxSize: number;
}

export function getDashboardSummary() {
  return getData<AdminDashboardSummary>('/admin/dashboard/overview');
}

export function getAdminPeriods() {
  return getData<PeriodConfig[]>('/admin/periods');
}

export function saveAdminPeriods(periods: PeriodConfig[]) {
  return postData<PeriodConfig[], { periods: PeriodConfig[] }>('/admin/periods', { periods });
}

export function updateAdminPeriod(id: number | string, payload: PeriodConfig) {
  return putData<PeriodConfig, PeriodConfig>(`/admin/periods/${id}`, payload);
}

export function getAdminDirections() {
  return getData<Direction[]>('/admin/directions');
}

export function createDirection(payload: DirectionPayload) {
  return postData<Direction, DirectionPayload>('/admin/directions', payload);
}

export function updateDirection(id: number | string, payload: DirectionPayload) {
  return putData<Direction, DirectionPayload>(`/admin/directions/${id}`, payload);
}

export function deleteDirection(id: number | string) {
  return deleteData<null>(`/admin/directions/${id}`);
}

export function getAdminUsers(params?: {
  role?: Role;
  status?: string;
  keyword?: string;
  page?: number;
  size?: number;
}) {
  return getData<PageResult<User>>('/admin/users', params);
}

export function getAdminUser(id: number | string) {
  return getData<User>(`/admin/users/${id}`);
}

export function updateUserStatus(id: number | string, status: 'ACTIVE' | 'DISABLED') {
  return patchData<User, { status: 'ACTIVE' | 'DISABLED' }>(`/admin/users/${id}/status`, { status });
}

export function updateUserRole(id: number | string, role: Exclude<Role, 'ADMIN'>) {
  return patchData<User, { role: Exclude<Role, 'ADMIN'> }>(`/admin/users/${id}/role`, { role });
}

export function getAdminGroups(params?: {
  directionLevel1Id?: number;
  directionLevel2Id?: number;
  grade?: Grade;
  admissionYear?: number;
}) {
  return getData<Group[]>('/admin/groups', params);
}

export function getAdminUngroupedApplications(params?: {
  directionLevel1Id?: number;
  directionLevel2Id?: number;
  grade?: Grade;
  admissionYear?: number;
  keyword?: string;
}) {
  return getData<Application[]>('/admin/groups/ungrouped-applications', params);
}

export function createGroup(payload: GroupPayload) {
  return postData<Group, GroupPayload>('/admin/groups', payload);
}

export function updateGroup(id: number | string, payload: GroupPayload) {
  return putData<Group, GroupPayload>(`/admin/groups/${id}`, payload);
}

export function deleteGroup(id: number | string) {
  return deleteData<null>(`/admin/groups/${id}`);
}

export function addApplicationToGroup(groupId: number | string, applicationId: number | string) {
  return postData<null>(`/admin/groups/${groupId}/applications/${applicationId}`);
}

export function unassignApplicationFromGroup(groupId: number | string, applicationId: number | string, remark?: string) {
  return postData<null, { remark?: string }>(`/admin/groups/${groupId}/applications/${applicationId}/unassign`, {
    remark
  });
}

export function rejectAdminApplication(applicationId: number | string, reason?: string) {
  return postData<null, { reason?: string }>(`/admin/applications/${applicationId}/reject`, { reason });
}

export function assignLeader(groupId: number | string, userId: number) {
  return putData<null, { leaderUserId: number }>(`/admin/groups/${groupId}/leader`, { leaderUserId: userId });
}

export function removeLeader(groupId: number | string) {
  return deleteData<null>(`/admin/groups/${groupId}/leader`);
}

export function getApplicationsExportUrl() {
  return '/api/v1/admin/exports/applications';
}

export function getGroupsExportUrl() {
  return '/api/v1/admin/exports/groups';
}

export function getGroupTasksExportUrl(groupId: number | string) {
  return `/api/v1/admin/exports/groups/${groupId}/tasks`;
}
