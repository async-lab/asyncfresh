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
      <router-link v-if="!isAdminLogin" to="/register">注册账号</router-link>
      <router-link v-else to="/login">普通登录</router-link>
      <router-link to="/forgot-password">找回密码</router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus';
import { computed, onMounted, reactive } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import PageHeader from '@/components/common/PageHeader.vue';
import { useAuthStore } from '@/stores/auth';
import { getHomePathByRole, resolvePostLoginPath } from '@/utils/authRouting';
import { isValidEmail, normalizeEmail } from '@/utils/authValidation';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const isAdminLogin = computed(() => route.name === 'admin-login');
const form = reactive({
  email: typeof route.query.email === 'string' ? route.query.email : '',
  password: '',
  rememberMe: false
});

onMounted(() => {
  if (authStore.user) {
    void router.replace(getHomePathByRole(authStore.user.role));
  }
});

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
</style>
