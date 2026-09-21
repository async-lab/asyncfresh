import { getData, postData } from './http';
import type { User } from '@/types/api';

export interface LoginPayload {
  email: string;
  password: string;
  rememberMe?: boolean;
}

export interface RegisterPayload {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
  code: string;
}

export interface SendEmailCodePayload {
  email: string;
  scene: 'REGISTER' | 'RESET_PASSWORD';
}

export interface ResetPasswordPayload {
  email: string;
  code: string;
  newPassword: string;
  confirmPassword: string;
}

export interface ChangePasswordPayload {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export function login(payload: LoginPayload) {
  return postData<User, LoginPayload>('/auth/login', payload);
}

export function logout() {
  return postData<null>('/auth/logout');
}

export function getMe() {
  return getData<User>('/auth/me');
}

export function sendEmailCode(payload: SendEmailCodePayload) {
  return postData<null, SendEmailCodePayload>('/auth/send-email-code', payload);
}

export function register(payload: RegisterPayload) {
  return postData<User, RegisterPayload>('/auth/register', payload);
}

export function forgotPassword(email: string) {
  return postData<null, { email: string }>('/auth/forgot-password', { email });
}

export function resetPassword(payload: ResetPasswordPayload) {
  return postData<null, ResetPasswordPayload>('/auth/reset-password', payload);
}

export function changePassword(payload: ChangePasswordPayload) {
  return postData<null, ChangePasswordPayload>('/auth/change-password', payload);
}