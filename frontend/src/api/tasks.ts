import { deleteData, getData, postData, putData } from './http';
import type { PageQuery, SubmissionStatus, Task, TaskAttachment, TaskScore, TaskSubmission } from '@/types/api';

export interface TaskQuery extends PageQuery {
  groupId?: number;
  keyword?: string;
  submissionStatus?: SubmissionStatus;
}

export interface TaskPayload {
  title: string;
  contentMarkdown?: string;
  attachmentFileId?: number | null;
  removeAttachment?: boolean;
  maxScore: number;
  deadlineAt: string;
}

export interface SubmissionPayload {
  contentMarkdown?: string;
  attachmentFileId?: number | null;
}

export interface ReviewPayload {
  score: number;
  comment: string;
}

export interface GroupSubmissionSummary {
  userId: number;
  username?: string | null;
  realName?: string | null;
  status: SubmissionStatus;
  contentMarkdown?: string | null;
  attachment?: TaskAttachment | null;
  submittedAt?: string | null;
  reviewerUserId?: number | null;
  reviewerUsername?: string | null;
  score?: number | null;
  reviewComment?: string | null;
  reviewedAt?: string | null;
}

export function getTasks(params?: TaskQuery) {
  return getData<Task[]>('/tasks', params);
}

export function getLeaderManagedTasks(groupId: number | string) {
  return getData<Task[]>(`/leader/groups/${groupId}/tasks`);
}

export function getAdminManagedTasks(groupId: number | string) {
  return getData<Task[]>(`/admin/groups/${groupId}/tasks`);
}

export function getTask(id: number | string) {
  return getData<Task>(`/tasks/${id}`);
}

export function createLeaderTask(groupId: number | string, payload: TaskPayload) {
  return postData<Task, TaskPayload>(`/leader/groups/${groupId}/tasks`, payload);
}

export function createAdminTask(groupId: number | string, payload: TaskPayload) {
  return postData<Task, TaskPayload>(`/admin/groups/${groupId}/tasks`, payload);
}

export function updateLeaderTask(groupId: number | string, taskId: number | string, payload: TaskPayload) {
  return putData<Task, TaskPayload>(`/leader/groups/${groupId}/tasks/${taskId}`, payload);
}

export function updateAdminTask(groupId: number | string, taskId: number | string, payload: TaskPayload) {
  return putData<Task, TaskPayload>(`/admin/groups/${groupId}/tasks/${taskId}`, payload);
}

export function deleteLeaderTask(groupId: number | string, taskId: number | string) {
  return deleteData<null>(`/leader/groups/${groupId}/tasks/${taskId}`);
}

export function deleteAdminTask(groupId: number | string, taskId: number | string) {
  return deleteData<null>(`/admin/groups/${groupId}/tasks/${taskId}`);
}

export function submitTask(id: number | string, payload: SubmissionPayload) {
  return postData<TaskSubmission, SubmissionPayload>(`/tasks/${id}/submission`, payload);
}

export function getMyTaskSubmission(id: number | string) {
  return getData<TaskSubmission>(`/tasks/${id}/submission`);
}

export function getLeaderTaskSubmissions(id: number | string) {
  return getData<GroupSubmissionSummary[]>(`/leader/tasks/${id}/submissions`);
}

export function getAdminTaskSubmissions(id: number | string) {
  return getData<GroupSubmissionSummary[]>(`/admin/tasks/${id}/submissions`);
}

export function reviewLeaderSubmission(taskId: number | string, userId: number | string, payload: ReviewPayload) {
  return postData<null, ReviewPayload>(`/leader/tasks/${taskId}/submissions/${userId}/review`, payload);
}

export function reviewAdminSubmission(taskId: number | string, userId: number | string, payload: ReviewPayload) {
  return postData<null, ReviewPayload>(`/admin/tasks/${taskId}/submissions/${userId}/review`, payload);
}

export function returnLeaderSubmission(taskId: number | string, userId: number | string) {
  return postData<null>(`/leader/tasks/${taskId}/submissions/${userId}/return`);
}

export function returnAdminSubmission(taskId: number | string, userId: number | string) {
  return postData<null>(`/admin/tasks/${taskId}/submissions/${userId}/return`);
}

export async function getMyTaskScores() {
  const tasks = await getTasks();
  const submissions = await Promise.all(
    tasks.map(async (task) => {
      try {
        return await getMyTaskSubmission(task.id);
      } catch {
        return null;
      }
    })
  );

  return tasks.flatMap((task, index): TaskScore[] => {
    const submission = submissions[index];
    if (!submission || submission.status !== 'REVIEWED' || submission.score == null) {
      return [];
    }

    return [
      {
        taskId: task.id,
        taskTitle: task.title,
        score: submission.score,
        maxScore: task.maxScore,
        comment: submission.reviewComment || undefined,
        reviewedAt: submission.reviewedAt || ''
      }
    ];
  });
}

export function getMyScores() {
  return getMyTaskScores();
}
