import type { NotificationItem, NotificationType, Role } from '@/types/api';

type RouteTarget = Pick<NotificationItem, 'relatedType' | 'relatedId'>;

export function getNotificationTagType(type?: NotificationType): 'success' | 'danger' | 'primary' | 'warning' {
  switch (type) {
    case 'TASK_PUBLISHED':
      return 'success';
    case 'TASK_RETURNED':
    case 'APPLICATION_REJECTED':
      return 'danger';
    case 'TASK_REVIEWED':
    case 'APPLICATION_GROUPED':
      return 'primary';
    default:
      return 'warning';
  }
}

/**
 * 根据通知关联业务与当前角色解析跳转路径，返回 null 表示无对应详情页。
 */
export function resolveNotificationPath(item: RouteTarget, role?: Role) {
  const relatedType = item.relatedType;
  const relatedId = item.relatedId;

  if (!relatedType || relatedId == null) {
    return null;
  }

  if (relatedType === 'TASK') {
    if (role === 'ADMIN') {
      return '/admin/tasks';
    }

    if (role === 'LEADER') {
      return `/leader/tasks/${relatedId}/reviews`;
    }

    return `/app/tasks/${relatedId}`;
  }

  if (relatedType === 'ANNOUNCEMENT') {
    if (role === 'ADMIN') {
      return '/admin/announcements';
    }

    if (role === 'LEADER') {
      return '/leader/announcements';
    }

    return `/app/announcements/${relatedId}`;
  }

  if (relatedType === 'MATERIAL') {
    if (role === 'ADMIN') {
      return '/admin/materials';
    }

    if (role === 'LEADER') {
      return '/leader/materials';
    }

    return `/app/materials/${relatedId}`;
  }

  if (relatedType === 'APPLICATION') {
    if (role === 'ADMIN') {
      return '/admin/applications';
    }

    return '/app/applications';
  }

  return null;
}
