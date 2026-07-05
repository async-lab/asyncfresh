export {
  createLeaderAnnouncement,
  deleteLeaderAnnouncement,
  getAnnouncements,
  updateLeaderAnnouncement
} from './announcements';
export { getGroup, getGroupMembers } from './groups';
export {
  createLeaderMaterial,
  deleteLeaderMaterial,
  getMaterials,
  updateLeaderMaterial
} from './materials';
export {
  createLeaderTask,
  deleteLeaderTask,
  getLeaderTaskSubmissions,
  getLeaderManagedTasks,
  getTasks,
  returnLeaderSubmission,
  reviewLeaderSubmission,
  updateLeaderTask
} from './tasks';

export function getLeaderTaskBatchDownloadUrl(groupId: number | string, taskId?: number | string) {
  const suffix = taskId ? `?taskId=${taskId}` : '';
  return `/api/v1/leader/groups/${groupId}/tasks/submissions/download${suffix}`;
}

export function getLeaderGroupExportUrl(groupId: number | string) {
  return `/api/v1/leader/exports/groups/${groupId}`;
}

export function getLeaderGroupTasksExportUrl(groupId: number | string) {
  return `/api/v1/leader/exports/groups/${groupId}/tasks`;
}

import { getData, postData } from './http';
import type { Application, Grade, Group, PageQuery, PageResult } from '@/types/api';

export function getGroups(params?: PageQuery) {
  return getData<Group[]>('/leader/groups').then((groups) => {
    const page = params?.page ?? 1;
    const size = params?.size ?? (groups.length || 1);

    return {
      list: groups,
      page,
      size,
      total: groups.length,
      totalPages: groups.length ? Math.ceil(groups.length / size) : 0
    } satisfies PageResult<Group>;
  });
}

export function getLeaderUngroupedApplications(params?: {
  directionLevel1Id?: number;
  directionLevel2Id?: number;
  grade?: Grade;
  admissionYear?: number;
  keyword?: string;
}) {
  return getData<Application[]>('/leader/groups/ungrouped-applications', params);
}

export function addLeaderApplicationToGroup(groupId: number | string, applicationId: number | string) {
  return postData<null>(`/leader/groups/${groupId}/applications/${applicationId}`);
}

export function rejectLeaderApplication(applicationId: number | string, remark?: string) {
  return postData<null, { remark?: string }>(`/leader/applications/${applicationId}/reject`, { remark });
}
