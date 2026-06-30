import { deleteData, getData, postData, putData } from './http';
import type { Announcement, PageQuery, Scope } from '@/types/api';

export interface AnnouncementQuery extends PageQuery {
  scope?: Scope;
  groupId?: number;
  keyword?: string;
}

export interface AnnouncementPayload {
  title: string;
  content: string;
  scope: Scope;
  groupId?: number | null;
}

export function getAnnouncements(params?: AnnouncementQuery) {
  return getData<Announcement[]>('/announcements', params);
}

export function getAnnouncement(id: number | string) {
  return getData<Announcement>(`/announcements/${id}`);
}

export function createAdminAnnouncement(payload: AnnouncementPayload) {
  return postData<Announcement, AnnouncementPayload>('/admin/announcements', payload);
}

export function createLeaderAnnouncement(payload: AnnouncementPayload) {
  return postData<Announcement, AnnouncementPayload>('/leader/announcements', payload);
}

export function updateAdminAnnouncement(id: number | string, payload: AnnouncementPayload) {
  return putData<Announcement, AnnouncementPayload>(`/admin/announcements/${id}`, payload);
}

export function updateLeaderAnnouncement(id: number | string, payload: AnnouncementPayload) {
  return putData<Announcement, AnnouncementPayload>(`/leader/announcements/${id}`, payload);
}

export function deleteAdminAnnouncement(id: number | string) {
  return deleteData<null>(`/admin/announcements/${id}`);
}

export function deleteLeaderAnnouncement(id: number | string) {
  return deleteData<null>(`/leader/announcements/${id}`);
}
