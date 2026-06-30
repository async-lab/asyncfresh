import http from './http';
import type { ApiResponse, UploadedFile } from '@/types/api';
import type { FilePurpose } from '@/types/api';

export type BizType = 'MATERIAL' | 'TASK' | 'SUBMISSION';

const legacyPurposeMap: Record<BizType, FilePurpose> = {
  MATERIAL: 'MATERIAL_ATTACHMENT',
  TASK: 'TASK_ATTACHMENT',
  SUBMISSION: 'TASK_SUBMISSION_ATTACHMENT'
};

export async function uploadFile(file: File, purpose: FilePurpose | BizType): Promise<UploadedFile> {
  const formData = new FormData();
  formData.append('file', file);

  const normalizedPurpose = legacyPurposeMap[purpose as BizType] || purpose;

  const response = await http.post<ApiResponse<UploadedFile>>('/uploads', formData, {
    params: {
      purpose: normalizedPurpose
    },
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });

  return response.data.data;
}
