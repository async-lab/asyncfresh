<template>
  <div class="page">
    <PageHeader title="用户管理" description="按角色、状态和关键词筛选用户，并创建、编辑、停用或删除账号。">
      <template #actions>
        <el-button :icon="Plus" type="primary" @click="openCreateDialog">新增用户</el-button>
        <el-button :icon="Refresh" :loading="loading" @click="loadUsers">刷新</el-button>
      </template>
    </PageHeader>

    <section class="page-section">
      <SearchBar>
        <el-input v-model="query.keyword" clearable placeholder="用户名或邮箱" @keyup.enter="search" />
        <el-select v-model="query.role" clearable placeholder="角色" @change="search">
          <el-option v-for="[value, label] in roleOptions" :key="value" :label="label" :value="value" />
        </el-select>
        <el-select v-model="query.status" clearable placeholder="状态" @change="search">
          <el-option v-for="[value, label] in statusOptions" :key="value" :label="label" :value="value" />
        </el-select>
        <template #actions>
          <el-button :icon="Search" type="primary" @click="search">筛选</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </template>
      </SearchBar>
    </section>

    <section class="page-section">
      <PageTable
        :data="users"
        :loading="loading"
        pagination
        :page="query.page"
        :size="query.size"
        :total="total"
        @update:page="handlePageChange"
        @update:size="handleSizeChange"
      >
        <el-table-column prop="username" label="用户名" min-width="130" />
        <el-table-column prop="email" label="邮箱" min-width="220" />
        <el-table-column label="角色" width="150">
          <template #default="{ row }">
            <el-select
              v-if="canManageRow(row)"
              :model-value="row.role"
              size="small"
              @change="(value: Role) => handleRoleChange(row, value)"
            >
              <el-option v-for="role in managedRoleOptions" :key="role" :label="roleLabels[role]" :value="role" />
            </el-select>
            <el-tag v-else type="danger" effect="light">{{ getRoleLabel(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 'DISABLED' ? 'info' : 'success'" effect="light">
              {{ getUserStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="所在分组" min-width="180">
          <template #default="{ row }">
            {{ row.groups?.length ? row.groups.map((group: SimpleGroup) => group.name).join('、') : '暂无' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="500" fixed="right">
          <template #default="{ row }">
            <div class="user-actions">
              <el-button text type="primary" :icon="View" @click="openDetail(row.id)">详情</el-button>
              <el-button text :icon="EditPen" :disabled="!canManageRow(row)" @click="openEditDialog(row)">编辑</el-button>
              <el-button
                text
                :type="row.status === 'DISABLED' ? 'success' : 'danger'"
                :icon="SwitchButton"
                :disabled="!canManageRow(row)"
                @click="toggleStatus(row)"
              >
                {{ row.status === 'DISABLED' ? '启用' : '停用' }}
              </el-button>
              <ConfirmAction title="确认删除该用户？" @confirm="handleDelete(row)">
                <el-button text type="danger" :icon="Delete" :disabled="!canManageRow(row)">删除</el-button>
              </ConfirmAction>
            </div>
          </template>
        </el-table-column>
      </PageTable>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑用户' : '新增用户'"
      width="720px"
      :close-on-click-modal="false"
      @closed="handleDialogClosed"
    >
      <el-form label-position="top" :model="form">
        <div class="form-grid">
          <el-form-item label="用户名">
            <el-input v-model="form.username" maxlength="32" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="form.email" />
          </el-form-item>
        </div>

        <div class="form-grid">
          <el-form-item label="密码">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              :placeholder="editingId ? '留空表示不修改' : '请输入初始密码'"
            />
          </el-form-item>
          <el-form-item label="确认密码">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              show-password
              :placeholder="editingId ? '留空表示不修改' : '请再次输入密码'"
            />
          </el-form-item>
        </div>

        <div class="form-grid">
          <el-form-item label="角色">
            <el-select v-model="form.role" class="full">
              <el-option v-for="role in managedRoleOptions" :key="role" :label="roleLabels[role]" :value="role" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="form.status" class="full">
              <el-option v-for="[value, label] in statusOptions" :key="value" :label="label" :value="value" />
            </el-select>
          </el-form-item>
          <el-form-item label="邮箱已验证">
            <el-switch v-model="form.emailVerified" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveUser">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="用户详情" size="520px" @closed="handleDetailClosed">
      <div v-loading="detailLoading">
        <template v-if="detailUser">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="用户 ID">{{ detailUser.id }}</el-descriptions-item>
            <el-descriptions-item label="用户名">{{ detailUser.username }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ detailUser.email }}</el-descriptions-item>
            <el-descriptions-item label="角色">{{ getRoleLabel(detailUser.role) }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ getUserStatusLabel(detailUser.status) }}</el-descriptions-item>
            <el-descriptions-item label="邮箱验证">
              {{ detailUser.emailVerified ? '已验证' : '未验证' }}
            </el-descriptions-item>
            <el-descriptions-item label="所在分组">
              {{ detailUser.groups?.length ? detailUser.groups.map((group) => group.name).join('、') : '暂无' }}
            </el-descriptions-item>
            <el-descriptions-item label="负责分组">
              {{ detailUser.leaderGroups?.length ? detailUser.leaderGroups.map((group) => group.name).join('、') : '暂无' }}
            </el-descriptions-item>
          </el-descriptions>
        </template>
        <el-empty v-else description="用户不存在或已不可访问" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { Delete, EditPen, Plus, Refresh, Search, SwitchButton, View } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import {
  createAdminUser,
  deleteAdminUser,
  getAdminUser,
  getAdminUsers,
  updateAdminUser,
  updateUserRole,
  updateUserStatus,
  type AdminUserCreatePayload,
  type AdminUserUpdatePayload
} from '@/api/admin';
import ConfirmAction from '@/components/common/ConfirmAction.vue';
import PageHeader from '@/components/common/PageHeader.vue';
import PageTable from '@/components/common/PageTable.vue';
import SearchBar from '@/components/common/SearchBar.vue';
import { useAuthStore } from '@/stores/auth';
import type { Role, SimpleGroup, User, UserStatus } from '@/types/api';
import { isValidEmail, isValidPassword, isValidUsername, normalizeEmail, passwordRuleMessage } from '@/utils/authValidation';
import { roleLabels, userStatusLabels } from '@/utils/labels';

interface UserFormState {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
  role: Exclude<Role, 'ADMIN'>;
  status: UserStatus;
  emailVerified: boolean;
}

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const users = ref<User[]>([]);
const total = ref(0);
const loading = ref(false);
const saving = ref(false);
const detailLoading = ref(false);
const detailVisible = ref(false);
const detailUser = ref<User | null>(null);
const dialogVisible = ref(false);
const editingId = ref<number | null>(null);
const currentUserId = ref<number | null>(null);
const query = reactive<{
  keyword: string;
  role?: Role;
  status?: UserStatus;
  page: number;
  size: number;
}>({
  keyword: '',
  page: 1,
  size: 10
});
const form = reactive<UserFormState>(createEmptyForm());

const managedRoleOptions: Array<Exclude<Role, 'ADMIN'>> = ['FRESHMAN', 'LEADER'];
const roleOptions = Object.entries(roleLabels) as Array<[Role, string]>;
const statusOptions = Object.entries(userStatusLabels) as Array<[UserStatus, string]>;

onMounted(() => {
  currentUserId.value = authStore.user?.id ?? null;
  void loadUsers();
});

watch(
  () => authStore.user?.id,
  (id) => {
    currentUserId.value = id ?? null;
  },
  { immediate: true }
);

watch(
  () => (route.name === 'admin-user-detail' ? route.params.id : undefined),
  async (id) => {
    if (!id) {
      detailVisible.value = false;
      detailUser.value = null;
      return;
    }
    detailVisible.value = true;
    await loadDetail(String(id));
  },
  { immediate: true }
);

async function loadUsers() {
  loading.value = true;
  try {
    const page = await getAdminUsers({
      keyword: query.keyword || undefined,
      role: query.role,
      status: query.status,
      page: query.page,
      size: query.size
    });
    users.value = page.list;
    total.value = page.total;
  } finally {
    loading.value = false;
  }
}

async function loadDetail(id: string | number) {
  detailLoading.value = true;
  try {
    detailUser.value = await getAdminUser(id);
  } catch {
    detailUser.value = null;
  } finally {
    detailLoading.value = false;
  }
}

function search() {
  query.page = 1;
  void loadUsers();
}

function resetSearch() {
  query.keyword = '';
  query.role = undefined;
  query.status = undefined;
  search();
}

function handlePageChange(page: number) {
  query.page = page;
  void loadUsers();
}

function handleSizeChange(size: number) {
  query.size = size;
  query.page = 1;
  void loadUsers();
}

function openDetail(id: number) {
  void router.push({ name: 'admin-user-detail', params: { id } });
}

function openCreateDialog() {
  editingId.value = null;
  Object.assign(form, createEmptyForm());
  dialogVisible.value = true;
}

function openEditDialog(user: User) {
  if (!canManageRow(user)) {
    ElMessage.warning('当前账号不支持在此处编辑');
    return;
  }

  editingId.value = user.id;
  Object.assign(form, {
    username: user.username,
    email: user.email,
    password: '',
    confirmPassword: '',
    role: user.role as Exclude<Role, 'ADMIN'>,
    status: user.status || 'ACTIVE',
    emailVerified: Boolean(user.emailVerified)
  });
  dialogVisible.value = true;
}

function handleDialogClosed() {
  editingId.value = null;
  Object.assign(form, createEmptyForm());
}

function getUserStatusLabel(status?: UserStatus) {
  return userStatusLabels[status || 'ACTIVE'];
}

function getRoleLabel(role: Role) {
  return roleLabels[role];
}

function handleDetailClosed() {
  if (route.name === 'admin-user-detail') {
    void router.push({ name: 'admin-users' });
  }
}

function canManageRow(user: User) {
  return user.role !== 'ADMIN' && user.id !== currentUserId.value;
}

async function handleRoleChange(user: User, role: Role) {
  if (!canManageRow(user)) {
    ElMessage.warning('当前账号不支持修改角色');
    return;
  }

  if (role === 'ADMIN') {
    ElMessage.warning('不能将用户设置为管理员');
    return;
  }

  await updateUserRole(user.id, role);
  ElMessage.success('用户角色已更新');
  await loadUsers();
  if (detailUser.value?.id === user.id) {
    await loadDetail(user.id);
  }
}

async function toggleStatus(user: User) {
  if (!canManageRow(user)) {
    ElMessage.warning('当前账号不支持修改状态');
    return;
  }

  const nextStatus: UserStatus = user.status === 'DISABLED' ? 'ACTIVE' : 'DISABLED';
  await updateUserStatus(user.id, nextStatus);
  ElMessage.success(nextStatus === 'ACTIVE' ? '用户已启用' : '用户已停用');
  await loadUsers();
  if (detailUser.value?.id === user.id) {
    await loadDetail(user.id);
  }
}

async function saveUser() {
  if (!validateForm()) {
    return;
  }

  saving.value = true;
  const payload = buildBasePayload();
  const targetId = editingId.value;

  try {
    if (editingId.value) {
      const updatePayload: AdminUserUpdatePayload = {
        ...payload
      };
      if (form.password) {
        updatePayload.password = form.password;
      }
      await updateAdminUser(editingId.value, updatePayload);
      ElMessage.success('用户已更新');
    } else {
      const createPayload: AdminUserCreatePayload = {
        ...payload,
        password: form.password
      };
      await createAdminUser(createPayload);
      ElMessage.success('用户已创建');
    }

    dialogVisible.value = false;
    await loadUsers();
    if (targetId && detailUser.value?.id === targetId) {
      await loadDetail(targetId);
    }
  } finally {
    saving.value = false;
  }
}

async function handleDelete(user: User) {
  if (!canManageRow(user)) {
    ElMessage.warning('当前账号不支持删除');
    return;
  }

  await deleteAdminUser(user.id);
  ElMessage.success('用户已删除');
  if (detailUser.value?.id === user.id) {
    detailVisible.value = false;
    detailUser.value = null;
    await router.push({ name: 'admin-users' });
  }
  await loadUsers();
}

function validateForm() {
  const username = form.username.trim();
  const email = form.email.trim();

  if (!isValidUsername(username)) {
    ElMessage.warning('用户名需要为 3 到 32 位字母、数字或下划线');
    return false;
  }

  if (!isValidEmail(email)) {
    ElMessage.warning('请输入有效邮箱');
    return false;
  }

  if (editingId.value) {
    if (form.password || form.confirmPassword) {
      if (!isValidPassword(form.password)) {
        ElMessage.warning(passwordRuleMessage);
        return false;
      }

      if (form.password !== form.confirmPassword) {
        ElMessage.warning('两次输入的密码不一致');
        return false;
      }
    }
  } else {
    if (!isValidPassword(form.password)) {
      ElMessage.warning(passwordRuleMessage);
      return false;
    }

    if (form.password !== form.confirmPassword) {
      ElMessage.warning('两次输入的密码不一致');
      return false;
    }
  }

  return true;
}

function buildBasePayload() {
  return {
    username: form.username.trim(),
    email: normalizeEmail(form.email),
    role: form.role,
    status: form.status,
    emailVerified: form.emailVerified
  };
}

function createEmptyForm(): UserFormState {
  return {
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
    role: 'FRESHMAN',
    status: 'ACTIVE',
    emailVerified: false
  };
}
</script>

<style scoped>
.full {
  width: 100%;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.user-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 10px;
  white-space: normal;
  max-width: 100%;
}

.user-actions :deep(.el-button) {
  margin-left: 0;
}

@media (max-width: 1280px) {
  .user-actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .user-actions :deep(.el-button) {
    width: 100%;
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
