<template>
  <AppShell title="实验室招新">
    <template #side>
      <el-menu :key="menuRenderKey" :default-active="activeMenuPath" :default-openeds="defaultOpeneds" unique-opened router>
        <template v-if="!authStore.isLeader">
          <el-menu-item index="/app">
            <el-icon><House /></el-icon>
            <span>首页</span>
          </el-menu-item>
          <el-menu-item index="/app/applications">
            <el-icon><EditPen /></el-icon>
            <span>我的报名</span>
          </el-menu-item>
          <el-menu-item index="/app/announcements">
            <el-icon><Bell /></el-icon>
            <span>公告</span>
          </el-menu-item>
          <el-menu-item index="/app/materials">
            <el-icon><Reading /></el-icon>
            <span>学习资料</span>
          </el-menu-item>
          <el-menu-item index="/app/groups">
            <el-icon><User /></el-icon>
            <span>我的分组</span>
          </el-menu-item>
          <el-menu-item index="/app/tasks">
            <el-icon><Tickets /></el-icon>
            <span>任务</span>
          </el-menu-item>
          <el-menu-item index="/app/scores">
            <el-icon><Finished /></el-icon>
            <span>我的成绩</span>
          </el-menu-item>
          <el-menu-item index="/app/settings">
            <el-icon><Setting /></el-icon>
            <span>个人设置</span>
          </el-menu-item>
        </template>
        <template v-else>
          <el-sub-menu v-for="group in leaderNavGroups" :key="group.key" :index="group.key">
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
        </template>
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
  Avatar,
  Bell,
  DataAnalysis,
  DocumentChecked,
  Download,
  EditPen,
  Files,
  Finished,
  Grid,
  House,
  Notebook,
  Notification,
  Reading,
  Setting,
  Tickets,
  User
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

type MenuMatchPath = string | RegExp;

type MenuItem = {
  path: string;
  label: string;
  icon: Component;
  matchPaths?: MenuMatchPath[];
};

type MenuGroup = {
  key: string;
  label: string;
  icon: Component;
  items: MenuItem[];
};

const leaderNavGroups = [
  {
    key: 'dashboard',
    label: '仪表盘',
    icon: DataAnalysis,
    items: [{ path: '/leader', label: '工作台', icon: House }]
  },
  {
    key: 'business',
    label: '招新业务',
    icon: DocumentChecked,
    items: [
      { path: '/leader/groups', label: '责任包', icon: Grid },
      {
        path: '/leader/groups/0/members',
        label: '组员信息',
        icon: Avatar,
        matchPaths: [/^\/leader\/groups\/\d+\/members$/]
      },
      { path: '/leader/applications', label: '未分组申请', icon: DocumentChecked },
      { path: '/leader/announcements', label: '组内公告', icon: Notification },
      { path: '/leader/materials', label: '组内资料', icon: Files },
      {
        path: '/leader/tasks',
        label: '组内任务',
        icon: Notebook,
        matchPaths: [/^\/leader\/tasks\/\d+\/reviews$/]
      }
    ]
  },
  {
    key: 'tools',
    label: '工具与导出',
    icon: Tickets,
    items: [{ path: '/leader/exports', label: '导出与批下载', icon: Download }]
  }
] satisfies MenuGroup[];

function matchesMenuPath(item: MenuItem, path: string) {
  const matchPaths = item.matchPaths?.length ? item.matchPaths : [item.path];

  return matchPaths.some((matchPath) => {
    if (matchPath instanceof RegExp) {
      return matchPath.test(path);
    }

    if (matchPath === '/leader' || matchPath === '/app') {
      return path === matchPath;
    }

    return path === matchPath || path.startsWith(`${matchPath}/`);
  });
}

function normalizeLeaderMenuPath(path: string) {
  if (/^\/leader\/groups\/\d+\/members$/.test(path)) {
    return '/leader/groups/0/members';
  }

  if (/^\/leader\/tasks\/\d+\/reviews$/.test(path)) {
    return '/leader/tasks';
  }

  return path;
}

const activeMenuPath = computed(() =>
  authStore.isLeader ? normalizeLeaderMenuPath(route.path) : route.path
);

const defaultOpeneds = computed(() => {
  if (!authStore.isLeader) {
    return [];
  }

  const currentGroup = leaderNavGroups.find((group) =>
    group.items.some((item) => matchesMenuPath(item, route.path))
  );

  return currentGroup ? [currentGroup.key] : [];
});

const menuRenderKey = computed(
  () => `${authStore.isLeader ? 'leader' : 'app'}-${activeMenuPath.value}-${defaultOpeneds.value[0] || 'none'}`
);

async function handleLogout() {
  await authStore.logout();
  await router.push('/login');
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
