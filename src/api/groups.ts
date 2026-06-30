import { getData } from './http';
import type { Grade, Group, GroupMember, PageQuery } from '@/types/api';

export interface GroupQuery extends PageQuery {
  directionLevel1Id?: number;
  directionLevel2Id?: number;
  grade?: Grade;
  admissionYear?: number;
  keyword?: string;
}

export function getGroup(id: number | string) {
  return getData<Group>(`/groups/${id}`);
}

export function getGroupMembers(id: number | string) {
  return getData<GroupMember[]>(`/groups/${id}/members`);
}
