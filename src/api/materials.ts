import { deleteData, getData, postData, putData } from './http';
import type { Material, PageQuery } from '@/types/api';

export interface MaterialQuery extends PageQuery {
  groupId?: number;
  directionLevel1Id?: number;
  directionLevel2Id?: number;
  keyword?: string;
  hasAttachment?: boolean;
}

export interface MaterialPayload {
  title: string;
  contentMarkdown?: string;
  attachmentFileId?: number | null;
  removeAttachment?: boolean;
}

export function getMaterials(params?: MaterialQuery) {
  return getData<Material[]>('/materials', params);
}

export function getMaterial(id: number | string) {
  return getData<Material>(`/materials/${id}`);
}

export function createAdminMaterial(groupId: number | string, payload: MaterialPayload) {
  return postData<Material, MaterialPayload>(`/admin/groups/${groupId}/materials`, payload);
}

export function updateAdminMaterial(groupId: number | string, id: number | string, payload: MaterialPayload) {
  return putData<Material, MaterialPayload>(`/admin/groups/${groupId}/materials/${id}`, payload);
}

export function deleteAdminMaterial(groupId: number | string, id: number | string) {
  return deleteData<null>(`/admin/groups/${groupId}/materials/${id}`);
}

export function createLeaderMaterial(groupId: number | string, payload: MaterialPayload) {
  return postData<Material, MaterialPayload>(`/leader/groups/${groupId}/materials`, payload);
}

export function updateLeaderMaterial(groupId: number | string, id: number | string, payload: MaterialPayload) {
  return putData<Material, MaterialPayload>(`/leader/groups/${groupId}/materials/${id}`, payload);
}

export function deleteLeaderMaterial(groupId: number | string, id: number | string) {
  return deleteData<null>(`/leader/groups/${groupId}/materials/${id}`);
}
