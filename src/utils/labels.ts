import type {
  ApplicationStatus,
  Grade,
  NotificationType,
  PeriodType,
  Role,
  Scope,
  SubmissionStatus,
  UserStatus
} from '@/types/api';

export const roleLabels: Record<Role, string> = {
  FRESHMAN: '新生',
  LEADER: '负责人',
  ADMIN: '管理员'
};

export const userStatusLabels: Record<UserStatus, string> = {
  ACTIVE: '正常',
  DISABLED: '禁用'
};

export const periodLabels: Record<PeriodType, string> = {
  REGISTRATION: '报名期',
  SELECTION: '选拔期',
  INTERVIEW: '面试期',
  NOT_OPEN: '未开放',
  FINISHED: '已结束'
};

export const applicationStatusLabels: Record<ApplicationStatus, string> = {
  SUBMITTED: '待分组',
  GROUPED: '已分组',
  REJECTED: '已驳回',
  WITHDRAWN: '已撤回'
};

export const submissionStatusLabels: Record<SubmissionStatus, string> = {
  PENDING: '待提交',
  SUBMITTED: '已提交',
  REVIEWED: '已批阅'
};

export const displaySubmissionStatusLabels = {
  ...submissionStatusLabels,
  EXPIRED: '已截止'
} as const;

export const scopeLabels: Record<Scope, string> = {
  GLOBAL: '全局',
  GROUP: '组内'
};

export const gradeLabels: Record<Grade, string> = {
  YEAR_1: '大一',
  YEAR_2: '大二',
  YEAR_3: '大三',
  YEAR_4: '大四'
};

export const notificationTypeLabels: Record<NotificationType, string> = {
  APPLICATION_REJECTED: '报名驳回',
  APPLICATION_GROUPED: '分组结果',
  APPLICATION_UNASSIGNED: '分组变更',
  TASK_PUBLISHED: '新任务',
  TASK_RETURNED: '任务打回',
  TASK_REVIEWED: '任务评测',
  ANNOUNCEMENT_PUBLISHED: '公告',
  MATERIAL_PUBLISHED: '学习资料'
};
