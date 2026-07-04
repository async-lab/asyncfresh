import { ElMessage } from 'element-plus';
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';

import AuthLayout from '@/layouts/AuthLayout.vue';
import UserLayout from '@/layouts/UserLayout.vue';
import AdminLayout from '@/layouts/AdminLayout.vue';
import { useAuthStore } from '@/stores/auth';
import { useMetaStore } from '@/stores/meta';
import { getHomePathByRole } from '@/utils/authRouting';

const LoginView = () => import('@/views/auth/LoginView.vue');
const RegisterView = () => import('@/views/auth/RegisterView.vue');
const ForgotPasswordView = () => import('@/views/auth/ForgotPasswordView.vue');
const ResetPasswordView = () => import('@/views/auth/ResetPasswordView.vue');
const AppDashboardView = () => import('@/views/app/DashboardView.vue');
const SettingsView = () => import('@/views/app/SettingsView.vue');
const ApplicationsView = () => import('@/views/app/ApplicationsView.vue');
const TasksView = () => import('@/views/app/TasksView.vue');
const TaskDetailView = () => import('@/views/app/TaskDetailView.vue');
const AnnouncementListView = () => import('@/views/app/announcements/AnnouncementListView.vue');
const AnnouncementDetailView = () => import('@/views/app/announcements/AnnouncementDetailView.vue');
const MaterialListView = () => import('@/views/app/materials/MaterialListView.vue');
const MaterialDetailView = () => import('@/views/app/materials/MaterialDetailView.vue');
const MyGroupsView = () => import('@/views/app/groups/MyGroupsView.vue');
const GroupDetailView = () => import('@/views/app/groups/GroupDetailView.vue');
const ScoresView = () => import('@/views/app/scores/ScoresView.vue');
const AdminDashboardView = () => import('@/views/admin/AdminDashboardView.vue');
const AdminPeriodsView = () => import('@/views/admin/AdminPeriodsView.vue');
const AdminDirectionsView = () => import('@/views/admin/AdminDirectionsView.vue');
const AdminUsersView = () => import('@/views/admin/AdminUsersView.vue');
const AdminApplicationsView = () => import('@/views/admin/AdminApplicationsView.vue');
const AdminGroupsView = () => import('@/views/admin/AdminGroupsView.vue');
const AdminLeadersView = () => import('@/views/admin/AdminLeadersView.vue');
const PlaceholderView = () => import('@/views/PlaceholderView.vue');
const LeaderDashboardView = () => import('@/views/leader/dashboard/index.vue');
const LeaderGroupsView = () => import('@/views/leader/groups/index.vue');
const LeaderGroupMembersView = () => import('@/views/leader/groups/MembersView.vue');
const LeaderApplicationsView = () => import('@/views/leader/applications/index.vue');
const LeaderAnnouncementView = () => import('@/views/leader/announcements/index.vue');
const LeaderMaterialsView = () => import('@/views/leader/materials/index.vue');
const LeaderTasksView = () => import('@/views/leader/tasks/index.vue');
const LeaderExportsView = () => import('@/views/leader/exports/index.vue');
const AdminAnnouncementsView = () => import('@/views/admin/announcements/index.vue');
const AdminMaterialsView = () => import('@/views/admin/materials/index.vue');
const AdminTasksView = () => import('@/views/admin/tasks/index.vue');
const AdminExportsView = () => import('@/views/admin/exports/index.vue');
const AdminAuditView = () => import('@/views/admin/audit/index.vue');
const AdminNotificationsView = () => import('@/views/admin/notifications/index.vue');
const ForbiddenView = () => import('@/views/error/ForbiddenView.vue');
const NotFoundView = () => import('@/views/error/NotFoundView.vue');

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/',
    component: AuthLayout,
    children: [
      {
        path: 'login',
        name: 'login',
        component: LoginView,
        meta: { title: '登录', layout: 'auth' }
      },
      {
        path: 'admin/login',
        name: 'admin-login',
        component: LoginView,
        meta: { title: '管理员登录', layout: 'auth' }
      },
      {
        path: 'register',
        name: 'register',
        component: RegisterView,
        meta: { title: '注册', layout: 'auth', periodHint: ['REGISTRATION'] }
      },
      {
        path: 'forgot-password',
        name: 'forgot-password',
        component: ForgotPasswordView,
        meta: { title: '找回密码', layout: 'auth' }
      },
      {
        path: 'reset-password',
        name: 'reset-password',
        component: ResetPasswordView,
        meta: { title: '重置密码', layout: 'auth' }
      }
    ]
  },
  {
    path: '/app',
    component: UserLayout,
    meta: { requiresAuth: true, roles: ['FRESHMAN'], layout: 'user' },
    children: [
      {
        path: '',
        name: 'app-dashboard',
        component: AppDashboardView,
        meta: { title: '首页', requiresAuth: true, roles: ['FRESHMAN'] }
      },
      {
        path: 'applications',
        name: 'app-applications',
        component: ApplicationsView,
        meta: {
          title: '我的报名',
          description: '查看、创建、编辑或撤回本人报名申请。',
          requiresAuth: true,
          roles: ['FRESHMAN'],
          periodHint: ['REGISTRATION']
        }
      },
      {
        path: 'applications/:id',
        name: 'app-application-detail',
        component: ApplicationsView,
        meta: {
          title: '报名详情',
          description: '展示报名申请详情、状态和分组结果。',
          requiresAuth: true,
          roles: ['FRESHMAN']
        }
      },
      {
        path: 'announcements',
        name: 'app-announcements',
        component: AnnouncementListView,
        meta: {
          title: '公告',
          description: '按全局和组内范围查看可见公告。',
          requiresAuth: true,
          roles: ['FRESHMAN']
        }
      },
      {
        path: 'announcements/:id',
        name: 'app-announcement-detail',
        component: AnnouncementDetailView,
        meta: {
          title: '公告详情',
          description: '展示公告 Markdown 内容、发布者和发布时间。',
          requiresAuth: true,
          roles: ['FRESHMAN']
        }
      },
      {
        path: 'materials',
        name: 'app-materials',
        component: MaterialListView,
        meta: {
          title: '学习资料',
          description: '按方向筛选学习资料并下载附件。',
          requiresAuth: true,
          roles: ['FRESHMAN']
        }
      },
      {
        path: 'materials/:id',
        name: 'app-material-detail',
        component: MaterialDetailView,
        meta: {
          title: '资料详情',
          description: '展示资料正文、方向标签和附件。',
          requiresAuth: true,
          roles: ['FRESHMAN']
        }
      },
      {
        path: 'groups',
        name: 'app-groups',
        component: MyGroupsView,
        meta: {
          title: '我的分组',
          description: '展示本人所在分组和分组详情入口。',
          requiresAuth: true,
          roles: ['FRESHMAN']
        }
      },
      {
        path: 'groups/:id',
        name: 'app-group-detail',
        component: GroupDetailView,
        meta: {
          title: '分组详情',
          description: '展示分组方向、年级、容量和负责人。',
          requiresAuth: true,
          roles: ['FRESHMAN']
        }
      },
      {
        path: 'tasks',
        name: 'app-tasks',
        component: TasksView,
        meta: {
          title: '任务',
          description: '查看本人可见任务、提交状态和截止时间。',
          requiresAuth: true,
          roles: ['FRESHMAN']
        }
      },
      {
        path: 'tasks/:id',
        name: 'app-task-detail',
        component: TaskDetailView,
        meta: {
          title: '任务详情',
          description: '查看任务说明、当前提交状态和批阅结果。',
          requiresAuth: true,
          roles: ['FRESHMAN'],
          periodHint: ['SELECTION']
        }
      },
      {
        path: 'scores',
        name: 'app-scores',
        component: ScoresView,
        meta: {
          title: '我的成绩',
          description: '展示任务成绩、满分、评语和批阅时间。',
          requiresAuth: true,
          roles: ['FRESHMAN']
        }
      },
      {
        path: 'settings',
        name: 'app-settings',
        component: SettingsView,
        meta: {
          title: '个人设置',
          description: '查看账号信息、修改密码和退出登录。',
          requiresAuth: true,
          roles: ['FRESHMAN']
        }
      }
    ]
  },
  {
    path: '/leader',
    component: UserLayout,
    meta: { requiresAuth: true, roles: ['LEADER'], layout: 'user' },
    children: [
      {
        path: '',
        name: 'leader-dashboard',
        component: LeaderDashboardView,
        meta: {
          title: '负责人工作台',
          description: '汇总责任包、组员、任务和待处理申请。',
          requiresAuth: true,
          roles: ['LEADER']
        }
      },
      {
        path: 'groups',
        name: 'leader-groups',
        component: LeaderGroupsView,
        meta: {
          title: '责任包',
          description: '查看负责人名下责任包信息。',
          requiresAuth: true,
          roles: ['LEADER']
        }
      },
      {
        path: 'announcements',
        name: 'leader-announcements',
        component: LeaderAnnouncementView,
        meta: {
          title: '组内公告管理',
          description: '负责人发布、编辑和删除自己负责组的公告。',
          requiresAuth: true,
          roles: ['LEADER']
        }
      },
      {
        path: 'applications',
        name: 'leader-applications',
        component: LeaderApplicationsView,
        meta: {
          title: '未分组申请处理',
          description: '处理待分组申请并加入责任包。',
          requiresAuth: true,
          roles: ['LEADER'],
          periodHint: ['SELECTION']
        }
      },
      {
        path: 'materials',
        name: 'leader-materials',
        component: LeaderMaterialsView,
        meta: {
          title: '组内资料管理',
          description: '维护责任包内资料和附件。',
          requiresAuth: true,
          roles: ['LEADER']
        }
      },
      {
        path: 'tasks',
        name: 'leader-tasks',
        component: LeaderTasksView,
        meta: {
          title: '组内任务管理',
          description: '负责人发布、编辑和删除自己负责组的任务。',
          requiresAuth: true,
          roles: ['LEADER'],
          periodHint: ['SELECTION']
        }
      },
      {
        path: 'tasks/:id/reviews',
        name: 'leader-task-reviews',
        component: LeaderTasksView,
        meta: {
          title: '任务批阅',
          description: '查看组内提交情况并进行评分和评语填写。',
          requiresAuth: true,
          roles: ['LEADER']
        }
      },
      {
        path: 'groups/:id/members',
        name: 'leader-group-members',
        component: LeaderGroupMembersView,
        meta: {
          title: '组员信息',
          description: '查看负责组成员、报名申请和方向信息。',
          requiresAuth: true,
          roles: ['LEADER']
        }
      },
      {
        path: 'exports',
        name: 'leader-exports',
        component: LeaderExportsView,
        meta: {
          title: '负责人导出',
          description: '导出责任包信息、任务成绩并批量下载提交。',
          requiresAuth: true,
          roles: ['LEADER']
        }
      }
    ]
  },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAuth: true, roles: ['ADMIN'], layout: 'admin' },
    children: [
      {
        path: '',
        name: 'admin-dashboard',
        component: AdminDashboardView,
        meta: { title: '仪表盘', requiresAuth: true, roles: ['ADMIN'] }
      },
      {
        path: 'periods',
        name: 'admin-periods',
        component: AdminPeriodsView,
        meta: {
          title: '时期管理',
          description: '配置报名期、选拔期和面试期。',
          requiresAuth: true,
          roles: ['ADMIN']
        }
      },
      {
        path: 'directions',
        name: 'admin-directions',
        component: AdminDirectionsView,
        meta: {
          title: '方向管理',
          description: '维护两级学习方向、启停和排序。',
          requiresAuth: true,
          roles: ['ADMIN']
        }
      },
      {
        path: 'users',
        name: 'admin-users',
        component: AdminUsersView,
        meta: {
          title: '用户管理',
          description: '按角色、申请状态、分组和关键词筛选用户。',
          requiresAuth: true,
          roles: ['ADMIN']
        }
      },
      {
        path: 'users/:id',
        name: 'admin-user-detail',
        component: AdminUsersView,
        meta: {
          title: '用户详情',
          description: '查看用户全量信息和报名申请。',
          requiresAuth: true,
          roles: ['ADMIN']
        }
      },
      {
        path: 'applications',
        name: 'admin-applications',
        component: AdminApplicationsView,
        meta: {
          title: '报名管理',
          description: '处理未分组申请、加入分组和拒绝申请。',
          requiresAuth: true,
          roles: ['ADMIN'],
          periodHint: ['SELECTION']
        }
      },
      {
        path: 'groups',
        name: 'admin-groups',
        component: AdminGroupsView,
        meta: {
          title: '分组管理',
          description: '创建分组、逐条分配申请、取消分组和查看成员。',
          requiresAuth: true,
          roles: ['ADMIN'],
          periodHint: ['SELECTION']
        }
      },
      {
        path: 'groups/:id',
        name: 'admin-group-detail',
        component: AdminGroupsView,
        meta: {
          title: '分组详情',
          description: '查看分组成员、容量和负责人。',
          requiresAuth: true,
          roles: ['ADMIN']
        }
      },
      {
        path: 'leaders',
        name: 'admin-leaders',
        component: AdminLeadersView,
        meta: {
          title: '负责人任命',
          description: '在分组成员中任命或撤销负责人。',
          requiresAuth: true,
          roles: ['ADMIN'],
          periodHint: ['SELECTION']
        }
      },
      {
        path: 'announcements',
        name: 'admin-announcements',
        component: AdminAnnouncementsView,
        meta: {
          title: '公告管理',
          description: '管理全局公告和全部组内公告。',
          requiresAuth: true,
          roles: ['ADMIN']
        }
      },
      {
        path: 'materials',
        name: 'admin-materials',
        component: AdminMaterialsView,
        meta: {
          title: '资料管理',
          description: '发布、编辑、删除学习资料和附件。',
          requiresAuth: true,
          roles: ['ADMIN']
        }
      },
      {
        path: 'tasks',
        name: 'admin-tasks',
        component: AdminTasksView,
        meta: {
          title: '任务管理',
          description: '按分组管理任务、提交、批阅和打回。',
          requiresAuth: true,
          roles: ['ADMIN'],
          periodHint: ['SELECTION']
        }
      },
      {
        path: 'export',
        name: 'admin-export',
        component: AdminExportsView,
        meta: {
          title: '成绩总览与导出',
          description: '导出报名信息、分组结果和任务成绩。',
          requiresAuth: true,
          roles: ['ADMIN']
        }
      },
      {
        path: 'task-downloads',
        name: 'admin-task-downloads',
        component: AdminExportsView,
        meta: {
          title: '任务批下载',
          description: '按责任包和任务批量下载提交附件。',
          requiresAuth: true,
          roles: ['ADMIN']
        }
      },
      {
        path: 'audit-logs',
        name: 'admin-audit-logs',
        component: AdminAuditView,
        meta: {
          title: '审计日志',
          description: '查看关键业务操作审计记录。',
          requiresAuth: true,
          roles: ['ADMIN']
        }
      },
      {
        path: 'notifications',
        name: 'admin-notifications',
        component: AdminNotificationsView,
        meta: {
          title: '通知中心',
          description: '创建和发送系统或邮件通知。',
          requiresAuth: true,
          roles: ['ADMIN']
        }
      }
    ]
  },
  {
    path: '/403',
    name: 'forbidden',
    component: ForbiddenView,
    meta: { title: '无权限', layout: 'blank' }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: NotFoundView,
    meta: { title: '页面不存在', layout: 'blank' }
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

const guestOnlyRouteNames = new Set(['login', 'admin-login', 'register', 'forgot-password', 'reset-password']);


router.beforeEach(async (to) => {
  const authStore = useAuthStore();
  const metaStore = useMetaStore();

  document.title = to.meta.title ? `${to.meta.title} - 实验室招新平台` : '实验室招新平台';

  if (!metaStore.initialized && !metaStore.loading) {
    await metaStore.bootstrap().catch(() => undefined);
  }

  if (!to.meta.requiresAuth) {
    if (authStore.user && guestOnlyRouteNames.has(String(to.name || ''))) {
      return getHomePathByRole(authStore.role);
    }

    return true;
  }

  if (!authStore.initialized && !authStore.loading) {
    await authStore.fetchMe().catch(() => undefined);
  }

  if (!authStore.user) {
    return {
      path: '/login',
      query: { redirect: to.fullPath }
    };
  }

  if (!authStore.hasRole(to.meta.roles)) {
    return getHomePathByRole(authStore.role);
  }

  return true;
});

export default router;
