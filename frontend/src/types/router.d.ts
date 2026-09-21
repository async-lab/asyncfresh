import type { PeriodType, Role } from './api';

declare module 'vue-router' {
  interface RouteMeta {
    title?: string;
    description?: string;
    requiresAuth?: boolean;
    roles?: Role[];
    periodHint?: PeriodType[];
    layout?: 'auth' | 'user' | 'admin' | 'blank';
  }
}

export {};
