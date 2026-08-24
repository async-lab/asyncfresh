<template>
  <el-container class="shell">
    <el-aside width="248px" class="side">
      <div class="side-title">实验室招新</div>
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

const menuRenderKey = computed(() => `${authStore.isLeader ? 'leader' : 'app'}-${activeMenuPath.value}-${defaultOpeneds.value[0] || 'none'}`);

async function handleLogout() {
  await authStore.logout();
  await router.push('/login');
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
