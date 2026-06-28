export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
  requestId: string;
}

export interface PageResult<T> {
  list: T[];
  page: number;
  size: number;
  total: number;
  totalPages: number;
}

export interface PageQuery {
  page?: number;
  size?: number;
}

export type Role = 'FRESHMAN' | 'LEADER' | 'ADMIN';
export type UserStatus = 'ACTIVE' | 'DISABLED';
export type PeriodType = 'REGISTRATION' | 'SELECTION' | 'INTERVIEW' | 'FINISHED';
export type ApplicationStatus = 'SUBMITTED' | 'GROUPED' | 'REJECTED' | 'WITHDRAWN';
export type Scope = 'GLOBAL' | 'GROUP';
export type Grade = 'YEAR_1' | 'YEAR_2' | 'YEAR_3' | 'YEAR_4' | 'GRADUATED';
export type SubmissionStatus = 'NOT_SUBMITTED' | 'SUBMITTED' | 'EXPIRED' | 'REVIEWED';

export interface User {
  id: number;
  username: string;
  email: string;
  role: Role;
  status?: UserStatus;
  emailVerified?: boolean;
  leaderGroupId?: number;
  groups?: SimpleGroup[];
}

export interface SimpleGroup {
  id: number;
  name: string;
}

export interface CurrentPeriod {
  currentPeriod: PeriodType;
  serverTime: string;
}

export interface Direction {
  id: number;
  name: string;
  level: 1 | 2;
  parentId?: number | null;
  sortOrder?: number;
  enabled?: boolean;
  children?: Direction[];
}

export interface Application {
  id: number;
  userId?: number;
  realName: string;
  phone: string;
  college: string;
  major: string;
  className: string;
  grade: Grade;
  admissionYear: number;
  directionLevel1Id: number;
  directionLevel2Id: number;
  introduction?: string;
  status: ApplicationStatus;
  statusRemark?: string | null;
  groupId?: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface ApplicationForm {
  realName: string;
  phone: string;
  college: string;
  major: string;
  className: string;
  grade: Grade;
  admissionYear: number;
  directionLevel1Id: number;
  directionLevel2Id: number;
  introduction?: string;
}

export interface ApplicationSummary {
  applicationCount: number;
  submittedCount: number;
  groupedCount: number;
  groupIds: number[];
}

export interface Announcement {
  id: number;
  title: string;
  content?: string;
  scope: Scope;
  groupId?: number | null;
  publisherName: string;
  createdAt: string;
}

export interface Material {
  id: number;
  title: string;
  summary?: string;
  content?: string;
  attachmentUrl?: string | null;
  directionLevel1Id?: number | null;
  directionLevel2Id?: number | null;
  hasAttachment?: boolean;
  createdAt: string;
}

export interface Task {
  id: number;
  title: string;
  content?: string;
  scope: Scope;
  groupId?: number | null;
  attachmentUrl?: string | null;
  maxScore: number;
  deadlineAt: string;
  submissionStatus?: SubmissionStatus;
  reviewStatus?: SubmissionStatus;
}

export interface TaskSubmission {
  id: number;
  taskId: number;
  userId?: number;
  submitVersion: number;
  isLatest: boolean;
  submittedAt: string;
  content?: string;
  attachmentUrl?: string | null;
}

export interface TaskScore {
  taskId: number;
  taskTitle: string;
  score: number;
  maxScore: number;
  comment?: string;
  reviewedAt: string;
}

export interface Group {
  id: number;
  name: string;
  directionLevel1Id: number;
  directionLevel2Id: number;
  grade: Grade;
  admissionYear: number;
  maxSize: number;
  leaderUserId?: number | null;
}

export interface GroupMember {
  userId: number;
  username: string;
  realName: string;
  applicationId: number;
  grade: Grade;
  admissionYear: number;
  directionLevel1Name: string;
  directionLevel2Name: string;
  applicationStatus: ApplicationStatus;
}

export interface UploadedFile {
  fileName: string;
  url: string;
  size: number;
}

export interface AdminDashboardSummary {
  userCount: number;
  applicationCount: number;
  groupedUserCount: number;
  groupedApplicationCount: number;
  unassignedApplicationCount: number;
  leaderCount: number;
  taskCompletionRate: number;
}
