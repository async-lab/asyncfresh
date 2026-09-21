import type { PeriodType, Role } from '@/types/api';

declare module 'vue-router' {
  interface RouteMeta {
    title?: string;
    description?: string;
    requiresAuth?: boolean;
    roles?: Role[];
    layout?: 'auth' | 'user' | 'admin' | 'blank';
    periodHint?: PeriodType[];
  }
}

export {};
