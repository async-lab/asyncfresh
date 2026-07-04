<template>
  <el-container class="shell">
    <el-aside width="236px" class="side">
      <div class="side-title">实验室招新</div>
      <el-menu :default-active="activePath" router>
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
          <el-menu-item index="/leader">
            <el-icon><House /></el-icon>
            <span>工作台</span>
          </el-menu-item>
          <el-menu-item index="/leader/groups">
            <el-icon><Grid /></el-icon>
            <span>责任包</span>
          </el-menu-item>
          <el-menu-item index="/leader/groups/0/members">
            <el-icon><Avatar /></el-icon>
            <span>组员信息</span>
          </el-menu-item>
          <el-menu-item index="/leader/applications">
            <el-icon><DocumentChecked /></el-icon>
            <span>未分组申请</span>
          </el-menu-item>
          <el-menu-item index="/leader/announcements">
            <el-icon><Notification /></el-icon>
            <span>组内公告</span>
          </el-menu-item>
          <el-menu-item index="/leader/materials">
            <el-icon><Files /></el-icon>
            <span>组内资料</span>
          </el-menu-item>
          <el-menu-item index="/leader/tasks">
            <el-icon><Notebook /></el-icon>
            <span>组内任务</span>
          </el-menu-item>
          <el-menu-item index="/leader/exports">
            <el-icon><Download /></el-icon>
            <span>导出与批下载</span>
          </el-menu-item>
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
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import PeriodBadge from '@/components/common/PeriodBadge.vue';
import { useAuthStore } from '@/stores/auth';
import { useMetaStore } from '@/stores/meta';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const metaStore = useMetaStore();

const activePath = computed(() => route.path);

async function handleLogout() {
  await authStore.logout();
  await router.push('/login');
}
</script>

<style scoped>
.shell {
  min-height: 100vh;
}

.side {
  border-right: 1px solid var(--app-border);
  background: #fff;
}

.side-title {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  font-weight: 700;
  border-bottom: 1px solid var(--app-border);
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--app-border);
  background: #fff;
}

.account {
  display: flex;
  gap: 12px;
  align-items: center;
}

.main {
  padding: 22px;
}
</style>
