import { ElMessage } from 'element-plus';

import type { ApiResponse } from '@/types/api';

export async function downloadAuthenticatedFile(url: string, fallbackFileName: string) {
  if (!url) {
    ElMessage.warning('请先选择责任包');
    return;
  }

  const response = await fetch(url, {
    method: 'GET',
    credentials: 'include'
  });

  const contentType = response.headers.get('content-type') || '';
  if (!response.ok || contentType.includes('application/json')) {
    await handleDownloadError(response);
    return;
  }

  const blob = await response.blob();
  if (blob.type.includes('application/json')) {
    handleJsonErrorText(await blob.text());
    return;
  }

  const fileName = parseFileName(response.headers.get('content-disposition')) || fallbackFileName;
  const objectUrl = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = objectUrl;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(objectUrl);
}

async function handleDownloadError(response: Response) {
  const text = await response.text();
  handleJsonErrorText(text, response.status);
}

function handleJsonErrorText(text: string, status?: number) {
  try {
    const body = JSON.parse(text) as ApiResponse<unknown>;
    if (body.code === 40100 || status === 401) {
      ElMessage.warning(body.message || '登录已过期，请重新登录');
      window.location.assign(`/login?redirect=${encodeURIComponent(window.location.pathname)}`);
      return;
    }
    if (body.code === 40300 || status === 403) {
      ElMessage.error(body.message || '无权访问该资源');
      return;
    }
    ElMessage.error(body.message || '导出失败，请稍后重试');
  } catch {
    ElMessage.error(status === 404 ? '导出接口不存在' : '导出失败，请稍后重试');
  }
}

function parseFileName(contentDisposition: string | null) {
  if (!contentDisposition) {
    return '';
  }

  const utf8Match = contentDisposition.match(/filename\*=(?:UTF-8''|)([^;]+)/i);
  if (utf8Match?.[1]) {
    const rawName = utf8Match[1].replace(/"/g, '').trim();
    try {
      return decodeURIComponent(rawName);
    } catch {
      return rawName;
    }
  }

  const basicMatch = contentDisposition.match(/filename="?([^";]+)"?/i);
  return basicMatch?.[1]?.trim() || '';
}