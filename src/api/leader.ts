export {
  createLeaderAnnouncement,
  deleteLeaderAnnouncement,
  getAnnouncements,
  updateLeaderAnnouncement
} from './announcements';
export { getGroup, getGroupMembers, getGroups } from './groups';
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
import type { Application, Grade } from '@/types/api';

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

export function rejectLeaderApplication(applicationId: number | string, reason?: string) {
  return postData<null, { reason?: string }>(`/leader/applications/${applicationId}/reject`, { reason });
}
