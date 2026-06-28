import type { Role } from '@/types/api';

export function getHomePathByRole(role?: Role | string) {
  if (role === 'ADMIN') {
    return '/admin';
  }

  if (role === 'LEADER') {
    return '/leader/announcements';
  }

  return '/app';
}

export function resolvePostLoginPath(role: Role, redirect?: string) {
  if (!redirect || !redirect.startsWith('/') || redirect.startsWith('//')) {
    return getHomePathByRole(role);
  }

  if (role === 'ADMIN') {
    return redirect.startsWith('/admin') ? redirect : '/admin';
  }

  if (role === 'LEADER') {
    return redirect.startsWith('/leader') ? redirect : '/leader/announcements';
  }

  return redirect.startsWith('/app') ? redirect : '/app';
}
