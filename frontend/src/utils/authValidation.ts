export const passwordRuleMessage = '密码至少 8 位，且必须同时包含字母和数字';

export function normalizeEmail(value: string) {
  return value.trim().toLowerCase();
}

export function isValidEmail(value: string) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalizeEmail(value));
}

export function isValidUsername(value: string) {
  return /^[A-Za-z0-9_]{3,32}$/.test(value.trim());
}

export function isValidPassword(value: string) {
  return value.length >= 8 && /[A-Za-z]/.test(value) && /\d/.test(value);
}

export function isValidEmailCode(value: string) {
  return /^\d{6}$/.test(value.trim());
}