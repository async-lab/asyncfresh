<template>
  <div>
    <PageHeader :title="isAdminLogin ? '管理员登录' : '登录'" description="使用邮箱和密码进入招新平台。" />

    <el-form class="auth-form" label-position="top" @submit.prevent="handleSubmit">
      <el-form-item label="邮箱">
        <el-input v-model="form.email" autocomplete="email" placeholder="user@example.com" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="form.password" autocomplete="current-password" placeholder="请输入密码" show-password />
      </el-form-item>
      <el-form-item>
        <el-checkbox v-model="form.rememberMe">记住我</el-checkbox>
      </el-form-item>
      <el-button type="primary" native-type="submit" :loading="authStore.loading" class="full">
        登录
      </el-button>
    </el-form>

    <div class="links">
      <router-link to="/register">注册账号</router-link>
      <router-link to="/forgot-password">找回密码</router-link>
    </div>

    <div v-if="showDemoAccounts" class="demo-box">
      <div class="demo-title">演示账号</div>
      <el-button
        v-for="account in mockAccounts"
        :key="account.email"
        size="small"
        @click="loginWithMock(account)"
      >
        {{ account.label }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus';
import { computed, reactive } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import PageHeader from '@/components/common/PageHeader.vue';
import { useAuthStore } from '@/stores/auth';
import type { Role } from '@/types/api';
import { isValidEmail, normalizeEmail } from '@/utils/authValidation';
import { mockAccounts, type MockAccount } from '@/utils/mockAuth';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const enableMockAuth = import.meta.env.VITE_ENABLE_MOCK_AUTH === 'true';
const showDemoAccounts = enableMockAuth || import.meta.env.VITE_USE_MOCK_API === 'true';

const isAdminLogin = computed(() => route.name === 'admin-login');
const form = reactive({
  email: '',
  password: '',
  rememberMe: false
});

function getHomePathByRole(role: Role) {
  if (role === 'ADMIN') {
    return '/admin';
  }

  if (role === 'LEADER') {
    return '/leader/announcements';
  }

  return '/app';
}

function resolvePostLoginPath(role: Role, redirect?: string): string {
  if (!redirect || !redirect.startsWith('/') || redirect.startsWith('//')) {
    return getHomePathByRole(role);
  }

  if (role === 'ADMIN') {
    return redirect.startsWith('/admin') ? redirect : '/admin';
  }

  if (role === 'LEADER') {
    return redirect.startsWith('/leader') ? redirect : '/leader/announcements';
  }

  return redirect.startsWith('/app') ? redirect : '/app';
}

async function handleSubmit() {
  if (!isValidEmail(form.email)) {
    ElMessage.warning('请输入有效邮箱');
    return;
  }

  if (!form.password) {
    ElMessage.warning('请输入密码');
    return;
  }

  const user = await authStore.login({
    email: normalizeEmail(form.email),
    password: form.password,
    rememberMe: form.rememberMe
  });

  if (isAdminLogin.value && user.role !== 'ADMIN') {
    ElMessage.warning('当前账号不是管理员，已进入对应工作台');
    await router.push(getHomePathByRole(user.role));
    return;
  }

  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : undefined;
  await router.push(resolvePostLoginPath(user.role, redirect));
}

async function loginWithMock(account: MockAccount) {
  form.email = account.email;
  form.password = account.password;
  await handleSubmit();
}
</script>

<style scoped>
.auth-form {
  margin-top: 18px;
}

.full {
  width: 100%;
}

.links {
  display: flex;
  justify-content: space-between;
  margin-top: 16px;
  color: var(--app-primary);
}

.demo-box {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid var(--app-border);
}

.demo-title {
  width: 100%;
  color: var(--app-muted);
  font-size: 13px;
}
</style>
