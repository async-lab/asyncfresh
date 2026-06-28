<template>
  <div>
    <PageHeader title="找回密码" description="输入邮箱后发送找回验证码。" />
    <el-form class="auth-form" label-position="top" @submit.prevent="handleSubmit">
      <el-form-item label="邮箱">
        <el-input v-model="email" placeholder="user@example.com" />
      </el-form-item>
      <el-button type="primary" native-type="submit" :loading="submitting" :disabled="Boolean(codeCooldown)" class="full">
        {{ codeButtonText }}
      </el-button>
    </el-form>
    <div class="links">
      <router-link :to="resetLink">已有验证码，去重置</router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus';
import { computed, onBeforeUnmount, ref } from 'vue';
import { useRouter } from 'vue-router';

import { forgotPassword } from '@/api/auth';
import PageHeader from '@/components/common/PageHeader.vue';
import { isValidEmail, normalizeEmail } from '@/utils/authValidation';

const router = useRouter();
const email = ref('');
const submitting = ref(false);
const codeCooldown = ref(0);
let cooldownTimer: number | undefined;

const codeButtonText = computed(() => (codeCooldown.value ? `${codeCooldown.value}s 后重发` : '发送验证码'));
const resetLink = computed(() => ({
  path: '/reset-password',
  query: normalizeEmail(email.value) ? { email: normalizeEmail(email.value) } : {}
}));

onBeforeUnmount(() => {
  if (cooldownTimer) {
    window.clearInterval(cooldownTimer);
  }
});

async function handleSubmit() {
  if (!isValidEmail(email.value)) {
    ElMessage.warning('请输入有效邮箱');
    return;
  }

  if (codeCooldown.value) {
    return;
  }

  const normalizedEmail = normalizeEmail(email.value);
  submitting.value = true;
  try {
    await forgotPassword(normalizedEmail);
    ElMessage.success('验证码已发送');
    startCodeCooldown();
    await router.push({ path: '/reset-password', query: { email: normalizedEmail } });
  } finally {
    submitting.value = false;
  }
}

function startCodeCooldown() {
  codeCooldown.value = 60;
  if (cooldownTimer) {
    window.clearInterval(cooldownTimer);
  }

  cooldownTimer = window.setInterval(() => {
    codeCooldown.value -= 1;
    if (codeCooldown.value <= 0 && cooldownTimer) {
      window.clearInterval(cooldownTimer);
      cooldownTimer = undefined;
      codeCooldown.value = 0;
    }
  }, 1000);
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
  margin-top: 16px;
  color: var(--app-primary);
}
</style>