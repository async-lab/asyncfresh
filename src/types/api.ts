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
export type PeriodType = 'REGISTRATION' | 'SELECTION' | 'INTERVIEW' | 'NOT_OPEN' | 'FINISHED';
export type ApplicationStatus = 'SUBMITTED' | 'GROUPED' | 'REJECTED' | 'WITHDRAWN';
export type Scope = 'GLOBAL' | 'GROUP';
export type Grade = 'YEAR_1' | 'YEAR_2' | 'YEAR_3' | 'YEAR_4' | 'GRADUATED';
export type SubmissionStatus = 'PENDING' | 'SUBMITTED' | 'REVIEWED';
export type DisplaySubmissionStatus = SubmissionStatus | 'EXPIRED';
export type FilePurpose = 'TASK_ATTACHMENT' | 'TASK_SUBMISSION_ATTACHMENT' | 'MATERIAL_ATTACHMENT';

export interface User {
  id: number;
  username: string;
  email: string;
  role: Role;
  status?: UserStatus;
  emailVerified?: boolean;
  leaderGroupId?: number;
  leaderGroups?: SimpleGroup[];
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
  contentMarkdown?: string;
  scope: Scope;
  groupId?: number | null;
  publisherName: string;
  createdAt: string;
  updatedAt?: string;
}

export interface Material {
  id: number;
  groupId?: number;
  title: string;
  summary?: string;
  content?: string;
  contentMarkdown?: string;
  attachmentFileId?: number | null;
  attachmentFileName?: string | null;
  attachmentUrl?: string | null;
  directionLevel1Id?: number | null;
  directionLevel2Id?: number | null;
  hasAttachment?: boolean;
  publisherName?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface Task {
  id: number;
  groupId: number;
  title: string;
  content?: string;
  contentMarkdown?: string;
  scope?: Scope;
  attachmentFileId?: number | null;
  attachmentFileName?: string | null;
  attachmentUrl?: string | null;
  maxScore: number;
  deadlineAt: string;
  publisherName?: string;
  submissionStatus?: SubmissionStatus;
  reviewStatus?: SubmissionStatus;
  submission?: TaskSubmission;
  createdAt?: string;
  updatedAt?: string;
}

export interface TaskSubmission {
  id?: number;
  taskId: number;
  userId?: number;
  status: SubmissionStatus;
  submittedAt?: string | null;
  content?: string;
  contentMarkdown?: string | null;
  attachmentFileId?: number | null;
  attachmentFileName?: string | null;
  attachmentUrl?: string | null;
  reviewerUserId?: number | null;
  score?: number | null;
  reviewComment?: string | null;
  reviewedAt?: string | null;
  submitVersion?: number;
  isLatest?: boolean;
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
  id: number;
  fileName: string;
  originalFileName?: string;
  contentType?: string;
  url?: string;
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

export interface AuditLog {
  id: number;
  operatorName: string;
  module: string;
  action: string;
  target?: string;
  detail?: string;
  ip?: string;
  createdAt: string;
}

export interface NotificationItem {
  id: number;
  title: string;
  content: string;
  channel: 'SYSTEM' | 'EMAIL';
  targetRole?: Role;
  status: 'DRAFT' | 'SENT';
  createdAt: string;
  sentAt?: string | null;
}
