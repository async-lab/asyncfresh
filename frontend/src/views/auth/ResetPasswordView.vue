<template>
  <div>
    <PageHeader title="重置密码" description="填写验证码和新密码完成重置。" />
    <el-form class="auth-form" label-position="top" @submit.prevent="handleSubmit">
      <el-form-item label="邮箱">
        <el-input v-model="form.email" placeholder="user@example.com" />
      </el-form-item>
      <el-form-item label="验证码">
        <el-input v-model="form.code" maxlength="6" placeholder="6 位数字验证码" />
      </el-form-item>
      <el-form-item label="新密码">
        <el-input v-model="form.newPassword" :placeholder="passwordRuleMessage" show-password />
      </el-form-item>
      <el-form-item label="确认新密码">
        <el-input v-model="form.confirmPassword" show-password />
      </el-form-item>
      <el-button type="primary" native-type="submit" :loading="submitting" class="full">
        重置密码
      </el-button>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus';
import { reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { resetPassword } from '@/api/auth';
import PageHeader from '@/components/common/PageHeader.vue';
import {
  isValidEmail,
  isValidEmailCode,
  isValidPassword,
  normalizeEmail,
  passwordRuleMessage
} from '@/utils/authValidation';

const route = useRoute();
const router = useRouter();
const submitting = ref(false);
const form = reactive({
  email: typeof route.query.email === 'string' ? route.query.email : '',
  code: '',
  newPassword: '',
  confirmPassword: ''
});

async function handleSubmit() {
  if (!validateForm()) {
    return;
  }

  submitting.value = true;
  try {
    await resetPassword({
      email: normalizeEmail(form.email),
      code: form.code.trim(),
      newPassword: form.newPassword,
      confirmPassword: form.confirmPassword
    });
    ElMessage.success('密码已重置，请登录');
    await router.push({ path: '/login', query: { email: normalizeEmail(form.email) } });
  } finally {
    submitting.value = false;
  }
}

function validateForm() {
  if (!isValidEmail(form.email)) {
    ElMessage.warning('请输入有效邮箱');
    return false;
  }

  if (!isValidEmailCode(form.code)) {
    ElMessage.warning('请输入 6 位数字验证码');
    return false;
  }

  if (!isValidPassword(form.newPassword)) {
    ElMessage.warning(passwordRuleMessage);
    return false;
  }

  if (form.newPassword !== form.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致');
    return false;
  }

  return true;
}
</script>

<style scoped>
.auth-form {
  margin-top: 18px;
}

.full {
  width: 100%;
}
</style>