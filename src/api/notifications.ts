import { getData, postData } from './http';
import type { NotificationItem, NotificationSummary, PageResult } from '@/types/api';

export interface NotificationQuery {
  page?: number;
  size?: number;
  unreadOnly?: boolean;
}

export function getNotifications(params?: NotificationQuery) {
  return getData<PageResult<NotificationItem>>('/notifications', params);
}

export function getNotificationSummary() {
  return getData<NotificationSummary>('/notifications/summary');
}

export function markNotificationRead(id: number | string) {
  return postData<null>(`/notifications/${id}/read`);
}

export function markAllNotificationsRead() {
  return postData<null>('/notifications/read-all');
}
