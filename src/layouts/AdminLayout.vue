<template>
  <el-container class="shell">
    <el-aside width="248px" class="side">
      <div class="side-title">招新管理端</div>
      <el-menu :default-active="activePath" router>
        <el-menu-item index="/admin">
          <el-icon><DataAnalysis /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/admin/periods">
          <el-icon><Calendar /></el-icon>
          <span>时期管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/directions">
          <el-icon><Guide /></el-icon>
          <span>方向管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><UserFilled /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/applications">
          <el-icon><DocumentChecked /></el-icon>
          <span>报名管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/groups">
          <el-icon><Grid /></el-icon>
          <span>分组管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/leaders">
          <el-icon><Medal /></el-icon>
          <span>负责人任命</span>
        </el-menu-item>
        <el-menu-item index="/admin/announcements">
          <el-icon><Notification /></el-icon>
          <span>公告管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/materials">
          <el-icon><Files /></el-icon>
          <span>资料管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/tasks">
          <el-icon><List /></el-icon>
          <span>任务管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/export">
          <el-icon><Download /></el-icon>
          <span>成绩与导出</span>
        </el-menu-item>
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
  Notification,
  UserFilled
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
  await router.push('/admin/login');
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
