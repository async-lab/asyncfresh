<template>
  <div class="page">
    <PageHeader title="用户管理" description="按角色、状态和关键词筛选用户，并查看详情、调整角色或启停账号。">
      <template #actions>
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
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button text type="primary" :icon="View" @click="openDetail(row.id)">详情</el-button>
              <el-button
                text
                :type="row.status === 'DISABLED' ? 'success' : 'danger'"
                :icon="SwitchButton"
                :disabled="!canManageRow(row)"
                @click="toggleStatus(row)"
              >
                {{ row.status === 'DISABLED' ? '启用' : '停用' }}
              </el-button>
            </div>
          </template>
        </el-table-column>
      </PageTable>
    </section>

    <el-drawer v-model="detailVisible" title="用户详情" :size="drawerSize" @closed="handleDetailClosed">
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
import { Refresh, Search, SwitchButton, View } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import {
  getAdminUser,
  getAdminUsers,
  updateUserRole,
  updateUserStatus
} from '@/api/admin';
import PageHeader from '@/components/common/PageHeader.vue';
import PageTable from '@/components/common/PageTable.vue';
import SearchBar from '@/components/common/SearchBar.vue';
import { useAuthStore } from '@/stores/auth';
import type { Role, SimpleGroup, User, UserStatus } from '@/types/api';
import { roleLabels, userStatusLabels } from '@/utils/labels';
import { useOverlayLayout } from '@/composables/useMediaQuery';

const { drawerSize } = useOverlayLayout({ drawerSize: '520px' });
const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const users = ref<User[]>([]);
const total = ref(0);
const loading = ref(false);
const detailLoading = ref(false);
const detailVisible = ref(false);
const detailUser = ref<User | null>(null);
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
</script>
