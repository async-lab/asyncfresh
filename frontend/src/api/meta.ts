import { getData } from './http';
import type { CurrentPeriod, Direction } from '@/types/api';

export function getCurrentPeriod() {
  return getData<CurrentPeriod>('/meta/current-period');
}

export function getDirections(enabled = true) {
  return getData<Direction[]>('/directions', { enabled });
}
