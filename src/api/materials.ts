import { deleteData, getData, postData, putData } from './http';
import type { Material, PageQuery, PageResult } from '@/types/api';

export interface MaterialQuery extends PageQuery {
  directionLevel1Id?: number;
  directionLevel2Id?: number;
  keyword?: string;
  hasAttachment?: boolean;
}

export interface MaterialPayload {
  title: string;
  summary?: string;
  content?: string;
  attachmentUrl?: string | null;
  directionLevel1Id?: number | null;
  directionLevel2Id?: number | null;
}

export function getMaterials(params?: MaterialQuery) {
  return getData<PageResult<Material>>('/materials', params);
}

export function getMaterial(id: number | string) {
  return getData<Material>(`/materials/${id}`);
}

export function createMaterial(payload: MaterialPayload) {
  return postData<Material, MaterialPayload>('/admin/materials', payload);
}

export function updateMaterial(id: number | string, payload: MaterialPayload) {
  return putData<Material, MaterialPayload>(`/admin/materials/${id}`, payload);
}

export function deleteMaterial(id: number | string) {
  return deleteData<null>(`/admin/materials/${id}`);
}