import type {
  AdminDashboardSummary,
  Announcement,
  Application,
  Direction,
  Group,
  GroupMember,
  Material,
  PageResult,
  Task,
  TaskScore,
  TaskSubmission,
  User
} from '@/types/api';

export const mockDirections: Direction[] = [
  {
    id: 1,
    name: '后端',
    level: 1,
    enabled: true,
    children: [
      { id: 2, name: 'Java', level: 2, parentId: 1, enabled: true },
      { id: 3, name: 'Go', level: 2, parentId: 1, enabled: true }
    ]
  },
  {
    id: 4,
    name: '前端',
    level: 1,
    enabled: true,
    children: [
      { id: 5, name: 'Vue', level: 2, parentId: 4, enabled: true },
      { id: 6, name: 'React', level: 2, parentId: 4, enabled: true }
    ]
  },
  {
    id: 7,
    name: '算法',
    level: 1,
    enabled: true,
    children: [{ id: 8, name: '竞赛算法', level: 2, parentId: 7, enabled: true }]
  }
];

export const mockUsers: User[] = [
  {
    id: 1,
    username: 'student',
    email: 'student@example.com',
    role: 'FRESHMAN',
    status: 'ACTIVE',
    emailVerified: true,
    groups: [{ id: 10, name: '后端-Java-1组' }]
  },
  {
    id: 2,
    username: 'leader',
    email: 'leader@example.com',
    role: 'LEADER',
    status: 'ACTIVE',
    emailVerified: true,
    leaderGroupId: 10,
    groups: [{ id: 10, name: '后端-Java-1组' }]
  },
  {
    id: 3,
    username: 'admin',
    email: 'admin@example.com',
    role: 'ADMIN',
    status: 'ACTIVE',
    emailVerified: true,
    groups: []
  },
  {
    id: 4,
    username: 'vue-user',
    email: 'vue@example.com',
    role: 'FRESHMAN',
    status: 'ACTIVE',
    emailVerified: true,
    groups: [{ id: 11, name: '前端-Vue-1组' }]
  }
];

export const mockApplications: Application[] = [
  {
    id: 100,
    userId: 1,
    realName: '张三',
    phone: '13800000000',
    college: '计算机学院',
    major: '软件工程',
    className: '软工 1 班',
    grade: 'YEAR_1',
    admissionYear: 2026,
    directionLevel1Id: 1,
    directionLevel2Id: 2,
    introduction: '希望系统学习 Java 后端开发。',
    status: 'GROUPED',
    statusRemark: null,
    groupId: 10,
    createdAt: '2026-06-24T15:30:00+08:00',
    updatedAt: '2026-06-24T15:40:00+08:00'
  },
  {
    id: 101,
    userId: 1,
    realName: '张三',
    phone: '13800000000',
    college: '计算机学院',
    major: '软件工程',
    className: '软工 1 班',
    grade: 'YEAR_1',
    admissionYear: 2026,
    directionLevel1Id: 4,
    directionLevel2Id: 5,
    introduction: '也想了解 Vue 工程化。',
    status: 'SUBMITTED',
    statusRemark: null,
    groupId: null,
    createdAt: '2026-06-25T09:20:00+08:00',
    updatedAt: '2026-06-25T09:20:00+08:00'
  },
  {
    id: 102,
    userId: 4,
    realName: '李四',
    phone: '13900000000',
    college: '信息学院',
    major: '计算机科学与技术',
    className: '计科 2 班',
    grade: 'YEAR_1',
    admissionYear: 2026,
    directionLevel1Id: 4,
    directionLevel2Id: 5,
    introduction: '有基础页面开发经验。',
    status: 'GROUPED',
    statusRemark: null,
    groupId: 11,
    createdAt: '2026-06-24T16:00:00+08:00',
    updatedAt: '2026-06-26T10:00:00+08:00'
  }
];

export const mockGroups: Group[] = [
  {
    id: 10,
    name: '后端-Java-1组',
    directionLevel1Id: 1,
    directionLevel2Id: 2,
    grade: 'YEAR_1',
    admissionYear: 2026,
    maxSize: 20,
    leaderUserId: 2
  },
  {
    id: 11,
    name: '前端-Vue-1组',
    directionLevel1Id: 4,
    directionLevel2Id: 5,
    grade: 'YEAR_1',
    admissionYear: 2026,
    maxSize: 18,
    leaderUserId: null
  }
];

export const mockGroupMembers: Record<number, GroupMember[]> = {
  10: [
    {
      userId: 1,
      username: 'student',
      realName: '张三',
      applicationId: 100,
      grade: 'YEAR_1',
      admissionYear: 2026,
      directionLevel1Name: '后端',
      directionLevel2Name: 'Java',
      applicationStatus: 'GROUPED'
    },
    {
      userId: 2,
      username: 'leader',
      realName: '负责人',
      applicationId: 1000,
      grade: 'YEAR_1',
      admissionYear: 2026,
      directionLevel1Name: '后端',
      directionLevel2Name: 'Java',
      applicationStatus: 'GROUPED'
    }
  ],
  11: [
    {
      userId: 4,
      username: 'vue-user',
      realName: '李四',
      applicationId: 102,
      grade: 'YEAR_1',
      admissionYear: 2026,
      directionLevel1Name: '前端',
      directionLevel2Name: 'Vue',
      applicationStatus: 'GROUPED'
    }
  ]
};

export const mockAnnouncements: Announcement[] = [
  {
    id: 1,
    title: '招新报名开放通知',
    content: '## 报名说明\n请在报名期内完善个人信息，并选择感兴趣的方向。',
    scope: 'GLOBAL',
    groupId: null,
    publisherName: '管理员',
    createdAt: '2026-06-24T15:30:00+08:00'
  },
  {
    id: 2,
    title: '后端 Java 组选拔安排',
    content: '## 组选拔安排\n本周完成 Java 基础练习并提交附件。',
    scope: 'GROUP',
    groupId: 10,
    publisherName: 'leader',
    createdAt: '2026-06-25T20:00:00+08:00'
  }
];

export const mockMaterials: Material[] = [
  {
    id: 1,
    title: 'Java 学习指南',
    summary: '后端方向入门资料',
    content: '## Java 入门\n建议先掌握集合、异常、IO 和基础 Web。',
    attachmentFileId: 3001,
    attachmentFileName: 'java-guide.pdf',
    directionLevel1Id: 1,
    directionLevel2Id: 2,
    hasAttachment: true,
    createdAt: '2026-06-24T17:00:00+08:00'
  },
  {
    id: 2,
    title: 'Vue 组件开发规范',
    summary: '前端方向组件化学习资料',
    content: '## Vue 组件\n关注 props、emit、状态管理和路由拆分。',
    attachmentFileId: null,
    directionLevel1Id: 4,
    directionLevel2Id: 5,
    hasAttachment: false,
    createdAt: '2026-06-25T09:00:00+08:00'
  }
];

export const mockTasks: Task[] = [
  {
    id: 1,
    title: '完成 Java 基础练习',
    content: '## 要求\n提交一个包含异常处理和文件读写的小练习。',
    scope: 'GROUP',
    groupId: 10,
    attachmentFileId: 2001,
    attachmentFileName: 'task-java.zip',
    maxScore: 100,
    deadlineAt: '2026-07-05T23:59:59+08:00',
    submissionStatus: 'REVIEWED',
    reviewStatus: 'REVIEWED'
  },
  {
    id: 2,
    title: '阅读实验室协作规范',
    content: '## 要求\n阅读协作规范，并提交 200 字学习记录。',
    scope: 'GROUP',
    groupId: 10,
    attachmentFileId: null,
    maxScore: 20,
    deadlineAt: '2026-07-03T23:59:59+08:00',
    submissionStatus: 'PENDING',
    reviewStatus: 'PENDING'
  }
];

export const mockSubmissions: TaskSubmission[] = [
  {
    id: 501,
    taskId: 1,
    userId: 1,
    status: 'SUBMITTED',
    submitVersion: 1,
    isLatest: false,
    submittedAt: '2026-06-26T12:00:00+08:00',
    content: '第一次提交。',
    attachmentFileId: 4001,
    attachmentFileName: 'homework-v1.zip'
  },
  {
    id: 502,
    taskId: 1,
    userId: 1,
    status: 'REVIEWED',
    submitVersion: 2,
    isLatest: true,
    submittedAt: '2026-06-27T10:30:00+08:00',
    content: '补充异常处理后重新提交。',
    attachmentFileId: 4002,
    attachmentFileName: 'homework-v2.zip',
    score: 92,
    reviewComment: '完成度较高，继续补充边界条件。',
    reviewedAt: '2026-06-27T20:00:00+08:00'
  }
];

export const mockScores: TaskScore[] = [
  {
    taskId: 1,
    taskTitle: '完成 Java 基础练习',
    score: 92,
    maxScore: 100,
    comment: '完成度较高，继续补充边界条件。',
    reviewedAt: '2026-06-27T20:00:00+08:00'
  }
];

export const mockPeriods = [
  {
    id: 1,
    periodType: 'REGISTRATION' as const,
    startTime: '2026-06-20T00:00:00+08:00',
    endTime: '2026-06-30T23:59:59+08:00',
    enabled: true
  },
  {
    id: 2,
    periodType: 'SELECTION' as const,
    startTime: '2026-07-01T00:00:00+08:00',
    endTime: '2026-07-15T23:59:59+08:00',
    enabled: true
  },
  {
    id: 3,
    periodType: 'INTERVIEW' as const,
    startTime: '2026-07-16T00:00:00+08:00',
    endTime: '2026-07-20T23:59:59+08:00',
    enabled: true
  }
];

export const mockDashboardSummary: AdminDashboardSummary = {
  userCount: 128,
  applicationCount: 96,
  groupedUserCount: 76,
  groupedApplicationCount: 81,
  unassignedApplicationCount: 15,
  leaderCount: 6,
  taskCompletionRate: 0.76
};

export function pageOf<T>(list: T[], page = 1, size = 10): PageResult<T> {
  const start = (page - 1) * size;
  const total = list.length;

  return {
    list: list.slice(start, start + size),
    page,
    size,
    total,
    totalPages: Math.max(1, Math.ceil(total / size))
  };
}
