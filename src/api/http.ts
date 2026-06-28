import axios, { AxiosError } from 'axios';
import { ElMessage } from 'element-plus';

import { mockApiAdapter } from '@/mocks/adapter';
import type { ApiResponse } from '@/types/api';

const http = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
  withCredentials: true,
  xsrfCookieName: '',
  xsrfHeaderName: '',
  adapter: import.meta.env.VITE_USE_MOCK_API === 'true' ? mockApiAdapter : undefined
});

const csrfFreeAuthPaths = new Set([
  '/auth/login',
  '/auth/register',
  '/auth/send-email-code',
  '/auth/forgot-password',
  '/auth/reset-password'
]);

http.interceptors.request.use((config) => {
  if (shouldAttachCsrf(config.method, config.url)) {
    const token = readCookie('XSRF-TOKEN');
    if (token) {
      config.headers.set('X-CSRF-TOKEN', token);
    }
  }

  return config;
});

http.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse<unknown>;

    if (body && typeof body.code === 'number' && body.code !== 0) {
      handleBusinessError(body);
      return Promise.reject(body);
    }

    return response;
  },
  (error: AxiosError<ApiResponse<unknown>>) => {
    const body = error.response?.data;

    if (typeof body?.code === 'number') {
      handleBusinessError(body);
    } else {
      ElMessage.error('网络异常，请稍后重试');
    }

    return Promise.reject(error);
  }
);

function shouldAttachCsrf(method = 'get', url = '') {
  const normalizedMethod = method.toLowerCase();
  const isUnsafeMethod = !['get', 'head', 'options'].includes(normalizedMethod);

  if (!isUnsafeMethod) {
    return false;
  }

  return !csrfFreeAuthPaths.has(normalizePath(url));
}

function normalizePath(url: string) {
  const rawPath = url.replace(/^https?:\/\/[^/]+/, '').split('?')[0].replace(/^\/api\/v1/, '');
  return rawPath.startsWith('/') ? rawPath : `/${rawPath}`;
}

function readCookie(name: string) {
  if (typeof document === 'undefined') {
    return '';
  }

  const prefix = `${name}=`;
  const cookie = document.cookie
    .split(';')
    .map((item) => item.trim())
    .find((item) => item.startsWith(prefix));

  if (!cookie) {
    return '';
  }

  const value = cookie.slice(prefix.length);

  try {
    return decodeURIComponent(value);
  } catch {
    return value;
  }
}

function handleBusinessError(body: ApiResponse<unknown>) {
  if (body.code === 40100) {
    ElMessage.warning('登录已过期，请重新登录');
    window.location.assign(`/login?redirect=${encodeURIComponent(window.location.pathname)}`);
    return;
  }

  if (body.code === 40300) {
    ElMessage.error('无权访问该资源');
    return;
  }

  if (body.code === 40310) {
    ElMessage.warning(body.message || '当前时期不允许该操作');
    return;
  }

  ElMessage.error(body.message || `请求失败：${body.code}`);
}

export async function getData<T>(url: string, params?: object): Promise<T> {
  const response = await http.get<ApiResponse<T>>(url, { params });
  return response.data.data;
}

export async function postData<T, B = unknown>(url: string, body?: B): Promise<T> {
  const response = await http.post<ApiResponse<T>>(url, body);
  return response.data.data;
}

export async function putData<T, B = unknown>(url: string, body?: B): Promise<T> {
  const response = await http.put<ApiResponse<T>>(url, body);
  return response.data.data;
}

export async function deleteData<T>(url: string): Promise<T> {
  const response = await http.delete<ApiResponse<T>>(url);
  return response.data.data;
}

export default http;