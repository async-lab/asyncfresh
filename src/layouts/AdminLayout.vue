<template>
  <el-container class="shell">
    <el-aside width="248px" class="side">
      <div class="side-title">招新管理端</div>
      <el-menu :key="menuRenderKey" :default-active="activeMenuPath" :default-openeds="defaultOpeneds" unique-opened router>
        <el-sub-menu v-for="group in navGroups" :key="group.key" :index="group.key">
          <template #title>
            <div class="menu-group-title">
              <el-icon><component :is="group.icon" /></el-icon>
              <span>{{ group.label }}</span>
            </div>
          </template>
          <el-menu-item
            v-for="item in group.items"
            :key="item.path"
            :index="item.path"
            class="menu-item"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="topbar">
        <PeriodBadge :period="metaStore.period" />
        <div class="account">
          <span>{{ authStore.user?.username }}</span>
          <el-button text @click="handleLogout">退出</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import {
  Calendar,
  DataAnalysis,
  DocumentChecked,
  Download,
  Files,
  Grid,
  Guide,
  List,
  Medal,
  Message,
  Notification,
  Tickets,
  UserFilled
} from '@element-plus/icons-vue';
import { computed, type Component } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import PeriodBadge from '@/components/common/PeriodBadge.vue';
import { useAuthStore } from '@/stores/auth';
import { useMetaStore } from '@/stores/meta';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const metaStore = useMetaStore();

const navGroups = [
  {
    key: 'dashboard',
    label: '仪表盘',
    icon: DataAnalysis,
    items: [{ path: '/admin', label: '仪表盘', icon: DataAnalysis }]
  },
  {
    key: 'base',
    label: '基础配置',
    icon: Guide,
    items: [
      { path: '/admin/periods', label: '时期管理', icon: Calendar },
      { path: '/admin/directions', label: '方向管理', icon: Guide },
      { path: '/admin/users', label: '用户管理', icon: UserFilled }
    ]
  },
  {
    key: 'business',
    label: '招新业务',
    icon: DocumentChecked,
    items: [
      { path: '/admin/applications', label: '报名管理', icon: DocumentChecked },
      { path: '/admin/groups', label: '分组管理', icon: Grid },
      { path: '/admin/leaders', label: '负责人任命', icon: Medal },
      { path: '/admin/announcements', label: '公告管理', icon: Notification },
      { path: '/admin/materials', label: '资料管理', icon: Files },
      { path: '/admin/tasks', label: '任务管理', icon: List }
    ]
  },
  {
    key: 'tools',
    label: '工具与日志',
    icon: Tickets,
    items: [
      {
        path: '/admin/exports',
        label: '导出与批下载',
        icon: Download,
        matchPaths: ['/admin/exports', '/admin/export', '/admin/task-downloads']
      },
      { path: '/admin/audit-logs', label: '审计日志', icon: Tickets },
      { path: '/admin/notifications', label: '通知中心', icon: Message }
    ]
  }
] satisfies Array<{
  key: string;
  label: string;
  icon: Component;
  items: Array<{
    path: string;
    label: string;
    icon: Component;
    matchPaths?: string[];
  }>;
}>;

const activeMenuPath = computed(() =>
  route.path === '/admin/task-downloads' || route.path === '/admin/export' ? '/admin/exports' : route.path
);
const defaultOpeneds = computed(() => {
  const currentGroup = navGroups.find((group) =>
    group.items.some((item) =>
      (item.matchPaths || [item.path]).some((path) =>
        path === '/admin' ? activeMenuPath.value === path : activeMenuPath.value.startsWith(path)
      )
    )
  );
  return currentGroup ? [currentGroup.key] : [];
});
const menuRenderKey = computed(() => `${activeMenuPath.value}-${defaultOpeneds.value[0] || 'none'}`);

async function handleLogout() {
  await authStore.logout();
  await router.push('/admin/login');
}
</script>

<style scoped>
.shell {
  min-height: 100vh;
  padding: 18px;
  gap: 18px;
}

.side {
  flex-shrink: 0;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 20px;
  background: linear-gradient(180deg, #2f2b27 0%, #26221f 100%);
  box-shadow: 14px 16px 32px rgba(145, 128, 106, 0.14);
  color: #f5f1ea;
}

.side-title {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  font-weight: 700;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  color: #fffaf4;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.06), transparent);
}

.side :deep(.el-menu) {
  --el-menu-bg-color: transparent;
  --el-menu-text-color: #d7d2cb;
  --el-menu-hover-bg-color: transparent;
  --el-menu-active-color: #ffffff;
  border-right: 0;
  background: transparent;
}

.side :deep(.el-menu-item),
.side :deep(.el-sub-menu__title) {
  margin: 4px 10px;
  border-radius: 12px;
}

.side :deep(.el-sub-menu__title) {
  height: 46px;
  color: #f5f1ea;
  background: rgba(255, 255, 255, 0.03);
}

.side :deep(.el-sub-menu__title:hover) {
  background: rgba(165, 155, 212, 0.12);
}

.side :deep(.el-sub-menu__icon-arrow) {
  color: rgba(245, 241, 234, 0.72);
}

.menu-group-title {
  display: flex;
  gap: 10px;
  align-items: center;
  font-weight: 700;
}

.menu-item {
  padding-left: 36px !important;
}

.side :deep(.el-menu-item.is-active) {
  background: rgba(165, 155, 212, 0.22);
  box-shadow:
    inset 1px 1px 0 rgba(255, 255, 255, 0.1),
    inset -1px -1px 0 rgba(0, 0, 0, 0.22);
}

.side :deep(.el-menu-item:not(.is-active):hover) {
  background: rgba(165, 155, 212, 0.12);
}

.side :deep(.el-menu-item .el-icon),
.side :deep(.el-sub-menu__title .el-icon) {
  color: inherit;
}

.shell > .el-container {
  min-width: 0;
  border: 1px solid rgba(126, 114, 97, 0.12);
  border-radius: 20px;
  overflow: hidden;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(247, 242, 234, 0.94));
  box-shadow: var(--app-shadow-raised);
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid rgba(126, 114, 97, 0.1);
  min-height: 64px;
  padding-inline: 20px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.82), rgba(247, 242, 234, 0.72));
}

.account {
  display: flex;
  gap: 12px;
  align-items: center;
  color: var(--app-muted);
}

.main {
  padding: 24px;
  background:
    radial-gradient(circle at 90% 0%, rgba(198, 179, 141, 0.05), transparent 24%),
    radial-gradient(circle at 8% 14%, rgba(165, 155, 212, 0.06), transparent 22%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.66), rgba(249, 244, 235, 0.94));
}

@media (max-width: 1100px) {
  .shell {
    flex-direction: column;
    padding: 12px;
  }

  .side {
    width: 100% !important;
    border-radius: 18px;
  }

  .shell > .el-container {
    border-radius: 18px;
  }

  .main {
    padding: 18px;
  }

  .topbar {
    padding-inline: 16px;
  }
}
</style>
