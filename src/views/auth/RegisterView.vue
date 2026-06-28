<template>
  <div>
    <PageHeader title="注册账号" description="注册仅在报名期开放。" />

    <el-alert v-if="!canRegister" class="notice" type="warning" :closable="false" show-icon>
      当前不是报名期，注册表单已禁用。
    </el-alert>

    <el-form class="auth-form" label-position="top" @submit.prevent="handleSubmit">
      <el-form-item label="用户名">
        <el-input v-model="form.username" :disabled="!canRegister" placeholder="3 到 32 位字母、数字或下划线" />
      </el-form-item>
      <el-form-item label="邮箱">
        <el-input v-model="form.email" :disabled="!canRegister" placeholder="user@example.com">
          <template #append>
            <el-button :disabled="!canSendCode || sendingCode" @click="handleSendCode">
              {{ codeButtonText }}
            </el-button>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="form.password" :disabled="!canRegister" :placeholder="passwordRuleMessage" show-password />
      </el-form-item>
      <el-form-item label="确认密码">
        <el-input v-model="form.confirmPassword" :disabled="!canRegister" show-password />
      </el-form-item>
      <el-form-item label="验证码">
        <el-input v-model="form.code" :disabled="!canRegister" maxlength="6" placeholder="6 位数字验证码" />
      </el-form-item>
      <el-button type="primary" native-type="submit" :disabled="!canRegister" :loading="submitting" class="full">
        注册
      </el-button>
    </el-form>

    <div class="links">
      <router-link to="/login">已有账号，去登录</router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus';
import { computed, onBeforeUnmount, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

import { register, sendEmailCode } from '@/api/auth';
import PageHeader from '@/components/common/PageHeader.vue';
import { useMetaStore } from '@/stores/meta';
import {
  isValidEmail,
  isValidEmailCode,
  isValidPassword,
  isValidUsername,
  normalizeEmail,
  passwordRuleMessage
} from '@/utils/authValidation';

const router = useRouter();
const metaStore = useMetaStore();
const sendingCode = ref(false);
const submitting = ref(false);
const codeCooldown = ref(0);
let cooldownTimer: number | undefined;

const form = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  code: ''
});

const canRegister = computed(() => metaStore.period === 'REGISTRATION');
const canSendCode = computed(() => canRegister.value && isValidEmail(form.email) && codeCooldown.value === 0);
const codeButtonText = computed(() => (codeCooldown.value ? `${codeCooldown.value}s 后重发` : '发送验证码'));

onBeforeUnmount(() => {
  if (cooldownTimer) {
    window.clearInterval(cooldownTimer);
  }
});

async function handleSendCode() {
  if (!isValidEmail(form.email)) {
    ElMessage.warning('请输入有效邮箱');
    return;
  }

  sendingCode.value = true;
  try {
    await sendEmailCode({ email: normalizeEmail(form.email), scene: 'REGISTER' });
    ElMessage.success('验证码已发送');
    startCodeCooldown();
  } finally {
    sendingCode.value = false;
  }
}

async function handleSubmit() {
  if (!validateForm()) {
    return;
  }

  submitting.value = true;
  try {
    await register({
      username: form.username.trim(),
      email: normalizeEmail(form.email),
      password: form.password,
      confirmPassword: form.confirmPassword,
      code: form.code.trim()
    });
    ElMessage.success('注册成功，请登录');
    await router.push({ path: '/login', query: { email: normalizeEmail(form.email) } });
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

function validateForm() {
  if (!canRegister.value) {
    ElMessage.warning('当前不是报名期，暂不能注册');
    return false;
  }

  if (!isValidUsername(form.username)) {
    ElMessage.warning('用户名需为 3 到 32 位字母、数字或下划线');
    return false;
  }

  if (!isValidEmail(form.email)) {
    ElMessage.warning('请输入有效邮箱');
    return false;
  }

  if (!isValidPassword(form.password)) {
    ElMessage.warning(passwordRuleMessage);
    return false;
  }

  if (form.password !== form.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致');
    return false;
  }

  if (!isValidEmailCode(form.code)) {
    ElMessage.warning('请输入 6 位数字验证码');
    return false;
  }

  return true;
}
</script>

<style scoped>
.auth-form {
  margin-top: 18px;
}

.notice {
  margin-top: 16px;
}

.full {
  width: 100%;
}

.links {
  margin-top: 16px;
  color: var(--app-primary);
}
</style>