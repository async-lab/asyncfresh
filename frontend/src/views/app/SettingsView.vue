<template>
  <div class="page">
    <PageHeader title="个人设置" description="查看账号信息、修改登录密码。">
      <template #actions>
        <el-button type="danger" plain :loading="logoutLoading" @click="handleLogout">退出登录</el-button>
      </template>
    </PageHeader>

    <div class="settings-grid">
      <section class="page-section">
        <h2>账号信息</h2>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="用户名">{{ user?.username || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ user?.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="角色">
            <el-tag effect="light">{{ roleText }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType" effect="light">{{ statusText }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="邮箱验证">
            <el-tag :type="user?.emailVerified ? 'success' : 'warning'" effect="light">
              {{ user?.emailVerified ? '已验证' : '未验证' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="user?.leaderGroupId" label="负责小组">
            #{{ user.leaderGroupId }}
          </el-descriptions-item>
          <el-descriptions-item label="所属小组">
            <div v-if="groups.length" class="group-list">
              <el-tag v-for="group in groups" :key="group.id" effect="plain">
                {{ group.name }}
              </el-tag>
            </div>
            <span v-else class="muted">暂无</span>
          </el-descriptions-item>
        </el-descriptions>
      </section>

      <section class="page-section">
        <h2>修改密码</h2>
        <el-form class="password-form" label-position="top" @submit.prevent="handleChangePassword">
          <el-form-item label="旧密码">
            <el-input v-model="form.oldPassword" autocomplete="current-password" show-password />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="form.newPassword" autocomplete="new-password" :placeholder="passwordRuleMessage" show-password />
          </el-form-item>
          <el-form-item label="确认新密码">
            <el-input v-model="form.confirmPassword" autocomplete="new-password" show-password />
          </el-form-item>
          <el-button type="primary" native-type="submit" :loading="submitting" class="full">
            修改密码
          </el-button>
        </el-form>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus';
import { computed, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

import PageHeader from '@/components/common/PageHeader.vue';
import { useAuthStore } from '@/stores/auth';
import { isValidPassword, passwordRuleMessage } from '@/utils/authValidation';
import { roleLabels, userStatusLabels } from '@/utils/labels';

const router = useRouter();
const authStore = useAuthStore();
const submitting = ref(false);
const logoutLoading = ref(false);
const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});

const user = computed(() => authStore.user);
const groups = computed(() => user.value?.groups || []);
const roleText = computed(() => (user.value?.role ? roleLabels[user.value.role] : '-'));
const statusText = computed(() => (user.value?.status ? userStatusLabels[user.value.status] : '-'));
const statusTagType = computed(() => (user.value?.status === 'ACTIVE' ? 'success' : 'danger'));

async function handleChangePassword() {
  if (!validateForm()) {
    return;
  }

  submitting.value = true;
  try {
    await authStore.changePassword({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword,
      confirmPassword: form.confirmPassword
    });
    ElMessage.success('修改密码成功，请重新登录');
    await router.push('/login');
  } finally {
    submitting.value = false;
  }
}

async function handleLogout() {
  const confirmed = await confirmLogout();

  if (!confirmed) {
    return;
  }

  logoutLoading.value = true;
  try {
    await authStore.logout();
    await router.push('/login');
  } finally {
    logoutLoading.value = false;
  }
}

async function confirmLogout() {
  try {
    await ElMessageBox.confirm('确认退出当前账号？', '退出登录', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning'
    });
    return true;
  } catch {
    return false;
  }
}

function validateForm() {
  if (!form.oldPassword) {
    ElMessage.warning('请输入旧密码');
    return false;
  }

  if (!isValidPassword(form.newPassword)) {
    ElMessage.warning(passwordRuleMessage);
    return false;
  }

  if (form.oldPassword === form.newPassword) {
    ElMessage.warning('新密码不能与旧密码相同');
    return false;
  }

  if (form.newPassword !== form.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致');
    return false;
  }

  return true;
}
</script>

<style scoped>
.settings-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 420px);
  gap: 16px;
}

h2 {
  margin: 0 0 16px;
  font-size: 18px;
}

.group-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.password-form {
  max-width: 420px;
}

.full {
  width: 100%;
}

@media (max-width: 900px) {
  .settings-grid {
    grid-template-columns: 1fr;
  }
}
</style>