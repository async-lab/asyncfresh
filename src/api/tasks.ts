import { deleteData, getData, postData, putData } from './http';
import type { PageQuery, PageResult, Scope, SubmissionStatus, Task, TaskScore, TaskSubmission } from '@/types/api';

export interface TaskQuery extends PageQuery {
  scope?: Scope;
  groupId?: number;
  keyword?: string;
  submissionStatus?: SubmissionStatus;
}

export interface TaskPayload {
  title: string;
  content: string;
  scope: Scope;
  groupId?: number | null;
  attachmentUrl?: string | null;
  maxScore: number;
  deadlineAt: string;
}

export interface SubmissionPayload {
  content: string;
  attachmentUrl?: string | null;
}

export interface ReviewPayload {
  score: number;
  comment: string;
}

export interface GroupSubmissionSummary {
  userId: number;
  username: string;
  realName: string;
  latestSubmissionId?: number | null;
  submittedAt?: string | null;
  reviewed: boolean;
  score?: number | null;
}

export function getTasks(params?: TaskQuery) {
  return getData<PageResult<Task>>('/tasks', params);
}

export function getTask(id: number | string) {
  return getData<Task>(`/tasks/${id}`);
}

export function createLeaderTask(payload: TaskPayload) {
  return postData<Task, TaskPayload>('/leader/tasks', payload);
}

export function createAdminTask(payload: TaskPayload) {
  return postData<Task, TaskPayload>('/admin/tasks', payload);
}

export function updateTask(id: number | string, payload: TaskPayload) {
  return putData<Task, TaskPayload>(`/tasks/${id}`, payload);
}

export function deleteTask(id: number | string) {
  return deleteData<null>(`/tasks/${id}`);
}

export function submitTask(id: number | string, payload: SubmissionPayload) {
  return postData<TaskSubmission, SubmissionPayload>(`/tasks/${id}/submissions`, payload);
}

export function getMyTaskSubmissions(id: number | string) {
  return getData<TaskSubmission[]>(`/tasks/${id}/submissions/me`);
}

export function getGroupTaskSubmissions(id: number | string) {
  return getData<GroupSubmissionSummary[]>(`/tasks/${id}/submissions/group`);
}

export function reviewSubmission(id: number | string, payload: ReviewPayload) {
  return postData<null, ReviewPayload>(`/submissions/${id}/review`, payload);
}

export function getMyTaskScores() {
  return getData<TaskScore[]>('/submissions/mine');
}

export function getMyScores() {
  return getMyTaskScores();
}