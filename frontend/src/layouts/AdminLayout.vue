<template>
  <AppShell title="AWAIT-YOU 管理端">
    <template #side>
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
    </template>

    <template #brand>
      <PeriodBadge :period="metaStore.period" />
    </template>

    <template #actions>
      <div class="account">
        <span class="account-name">{{ authStore.user?.username }}</span>
        <el-button text @click="handleLogout">退出</el-button>
      </div>
    </template>

    <router-view />
  </AppShell>
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
import AppShell from '@/layouts/AppShell.vue';
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
.account {
  display: flex;
  min-width: 0;
  gap: 8px;
  align-items: center;
  color: var(--app-muted);
}

.account-name {
  max-width: 28vw;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 960px) {
  .account-name {
    max-width: 36vw;
  }
}

@media (max-width: 420px) {
  .account-name {
    display: none;
  }
}
</style>
