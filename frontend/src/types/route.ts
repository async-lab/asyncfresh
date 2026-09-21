import type { Role, PeriodType } from './api';

export interface RouteMeta {
  title?: string;
  requiresAuth?: boolean;
  roles?: Role[];
  layout?: 'auth' | 'user' | 'admin' | 'blank';
  periodHint?: PeriodType[];
}
