<template>
  <div class="page">
    <PageHeader title="分组管理" description="创建分组、维护容量并查看分组成员。">
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">创建分组</el-button>
      </template>
    </PageHeader>

    <el-alert v-if="!metaStore.isSelection" type="warning" show-icon :closable="false">
      当前不是选拔期，创建、编辑、删除分组应由后端时期校验最终拦截。
    </el-alert>

    <section class="page-section">
      <SearchBar>
        <DirectionCascader v-model="directionPath" @change="handleDirectionChange" />
        <el-select v-model="query.grade" clearable placeholder="年级" @change="loadGroups">
          <el-option v-for="[value, label] in gradeOptions" :key="value" :label="label" :value="value" />
        </el-select>
        <el-input-number v-model="query.admissionYear" :min="2000" :max="2100" controls-position="right" placeholder="入学年份" />
        <template #actions>
          <el-button type="primary" :icon="Search" @click="loadGroups">筛选</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </template>
      </SearchBar>
    </section>

    <section class="page-section">
      <MobileList v-if="isMobile" :data="groups" :loading="loading" empty-text="暂无分组" key-field="id">
        <template #item="{ item }">
          <div class="group-card">
            <div class="group-card__head">
              <span class="group-card__name">{{ item.name }}</span>
              <el-tag effect="light" size="small">{{ getGradeLabel(item.grade) }}</el-tag>
            </div>
            <div class="group-card__meta">{{ getGroupDirectionLabel(item) }}</div>
            <div class="group-card__meta">入学年份：{{ item.admissionYear }} · 容量：{{ item.maxSize }}</div>
            <div class="group-card__meta">负责人：{{ getLeaderLabel(item) }}</div>
            <div class="group-card__actions">
              <el-button text type="primary" :icon="View" @click="openDetail(item.id)">详情</el-button>
              <el-button text :icon="EditPen" @click="openEditDialog(item)">编辑</el-button>
              <ConfirmAction title="确认删除该分组？已有成员的分组应由后端拒绝删除。" @confirm="handleDelete(item)">
                <el-button text type="danger" :icon="Delete">删除</el-button>
              </ConfirmAction>
            </div>
          </div>
        </template>
      </MobileList>

      <el-table v-else v-loading="loading" :data="groups" empty-text="暂无分组">
        <el-table-column prop="name" label="分组名称" min-width="180" />
        <el-table-column label="方向" min-width="180">
          <template #default="{ row }">{{ getGroupDirectionLabel(row) }}</template>
        </el-table-column>
        <el-table-column label="年级" width="90">
          <template #default="{ row }">{{ getGradeLabel(row.grade) }}</template>
        </el-table-column>
        <el-table-column prop="admissionYear" label="入学年份" width="110" />
        <el-table-column prop="maxSize" label="容量" width="90" />
        <el-table-column label="负责人" width="120">
          <template #default="{ row }">{{ getLeaderLabel(row) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button text type="primary" :icon="View" @click="openDetail(row.id)">详情</el-button>
              <el-button text :icon="EditPen" @click="openEditDialog(row)">编辑</el-button>
              <ConfirmAction title="确认删除该分组？已有成员的分组应由后端拒绝删除。" @confirm="handleDelete(row)">
                <el-button text type="danger" :icon="Delete">删除</el-button>
              </ConfirmAction>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑分组' : '创建分组'" :width="dialogWidth" :close-on-click-modal="false">
      <el-form label-position="top" :model="form">
        <el-form-item label="分组名称">
          <el-input v-model="form.name" maxlength="50" />
        </el-form-item>
        <el-form-item label="方向">
          <DirectionCascader v-model="formDirectionPath" @change="handleFormDirectionChange" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="年级">
            <el-select v-model="form.grade" class="full">
              <el-option v-for="[value, label] in gradeOptions" :key="value" :label="label" :value="value" />
            </el-select>
          </el-form-item>
          <el-form-item label="入学年份">
            <el-input-number v-model="form.admissionYear" class="full" :min="2000" :max="2100" controls-position="right" />
          </el-form-item>
          <el-form-item label="最大人数">
            <el-input-number v-model="form.maxSize" class="full" :min="1" :max="200" controls-position="right" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveGroup">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="分组详情" :size="drawerSize" @closed="handleDetailClosed">
      <div v-loading="detailLoading">
        <template v-if="detailGroup">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="分组名称">{{ detailGroup.name }}</el-descriptions-item>
            <el-descriptions-item label="方向">{{ getGroupDirectionLabel(detailGroup) }}</el-descriptions-item>
            <el-descriptions-item label="年级">{{ getGradeLabel(detailGroup.grade) }}</el-descriptions-item>
            <el-descriptions-item label="入学年份">{{ detailGroup.admissionYear }}</el-descriptions-item>
            <el-descriptions-item label="容量">{{ members.length }} / {{ detailGroup.maxSize }}</el-descriptions-item>
            <el-descriptions-item label="负责人">{{ getLeaderLabel(detailGroup) }}</el-descriptions-item>
          </el-descriptions>

          <div class="member-header">
            <h3>成员</h3>
            <el-button type="primary" :icon="Plus" :disabled="!canAddMember" @click="openAddMemberDialog">
              添加成员
            </el-button>
          </div>
          <p v-if="!metaStore.isSelection" class="muted">当前不是选拔期，无法补录成员。</p>
          <p v-else-if="isGroupFull" class="muted">当前分组已满员，无法继续添加成员。</p>
          <el-table :data="members" empty-text="暂无成员">
            <el-table-column prop="realName" label="姓名" width="110" />
            <el-table-column prop="username" label="用户名" width="130" />
            <el-table-column label="方向" min-width="160">
              <template #default="{ row }">{{ row.directionLevel1Name }} / {{ row.directionLevel2Name }}</template>
            </el-table-column>
            <el-table-column label="年级" width="90">
              <template #default="{ row }">{{ getGradeLabel(row.grade) }}</template>
            </el-table-column>
          </el-table>
        </template>
        <el-empty v-else description="分组不存在或已不可访问" />
      </div>
    </el-drawer>

    <el-dialog v-model="addMemberVisible" title="添加成员" :width="dialogWidth" :close-on-click-modal="false">
      <el-alert type="info" show-icon :closable="false" class="add-member-tip">
        方向、年级和入学年份将按当前分组写入报名申请，不会重新开放公众报名。
      </el-alert>
      <el-descriptions v-if="detailGroup" :column="1" border class="add-member-meta">
        <el-descriptions-item label="目标分组">{{ detailGroup.name }}</el-descriptions-item>
        <el-descriptions-item label="方向">{{ getGroupDirectionLabel(detailGroup) }}</el-descriptions-item>
        <el-descriptions-item label="年级">{{ getGradeLabel(detailGroup.grade) }}</el-descriptions-item>
        <el-descriptions-item label="入学年份">{{ detailGroup.admissionYear }}</el-descriptions-item>
      </el-descriptions>
      <el-form label-position="top" :model="addMemberForm">
        <el-form-item label="选择用户">
          <el-select
            v-model="addMemberForm.userId"
            class="full"
            filterable
            remote
            clearable
            reserve-keyword
            placeholder="搜索用户名或邮箱"
            :remote-method="searchAddMemberUsers"
            :loading="addMemberUserLoading"
          >
            <el-option
              v-for="user in addMemberUserOptions"
              :key="user.id"
              :label="`${user.username} (${user.email})`"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
        <div class="form-grid add-member-grid">
          <el-form-item label="真实姓名">
            <el-input v-model="addMemberForm.realName" maxlength="32" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="addMemberForm.phone" maxlength="11" />
          </el-form-item>
          <el-form-item label="学院">
            <el-input v-model="addMemberForm.college" maxlength="64" />
          </el-form-item>
          <el-form-item label="专业">
            <el-input v-model="addMemberForm.major" maxlength="64" />
          </el-form-item>
          <el-form-item label="班级">
            <el-input v-model="addMemberForm.className" maxlength="64" />
          </el-form-item>
        </div>
        <el-form-item label="自我介绍">
          <el-input
            v-model="addMemberForm.introduction"
            type="textarea"
            :rows="4"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addMemberVisible = false">取消</el-button>
        <el-button type="primary" :loading="addMemberSaving" :disabled="!canAddMember" @click="submitAddMember">
          确认添加
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { Delete, EditPen, Plus, Search, View } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import {
  addGroupMember,
  createGroup,
  deleteGroup,
  getAdminGroups,
  getAdminUsers,
  updateGroup,
  type GroupPayload
} from '@/api/admin';
import { getGroup, getGroupMembers } from '@/api/groups';
import ConfirmAction from '@/components/common/ConfirmAction.vue';
import MobileList from '@/components/common/MobileList.vue';
import PageHeader from '@/components/common/PageHeader.vue';
import SearchBar from '@/components/common/SearchBar.vue';
import DirectionCascader from '@/components/forms/DirectionCascader.vue';
import { useMetaStore } from '@/stores/meta';
import type { Grade, Group, GroupMember, User } from '@/types/api';
import { gradeLabels } from '@/utils/labels';
import { useIsMobile, useOverlayLayout } from '@/composables/useMediaQuery';

const { isMobile, dialogWidth, drawerSize } = useOverlayLayout({ dialogWidth: '620px', drawerSize: '620px' });
const route = useRoute();
const router = useRouter();
const metaStore = useMetaStore();
const groups = ref<Group[]>([]);
const users = ref<Record<number, string>>({});
const members = ref<GroupMember[]>([]);
const detailGroup = ref<Group | null>(null);
const loading = ref(false);
const detailLoading = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);
const detailVisible = ref(false);
const addMemberVisible = ref(false);
const addMemberSaving = ref(false);
const addMemberUserLoading = ref(false);
const addMemberUserOptions = ref<User[]>([]);
const addMemberForm = reactive({
  userId: undefined as number | undefined,
  realName: '',
  phone: '',
  college: '',
  major: '',
  className: '',
  introduction: ''
});
const isGroupFull = computed(() => {
  if (!detailGroup.value) return false;
  return members.value.length >= detailGroup.value.maxSize;
});
const canAddMember = computed(() => Boolean(detailGroup.value) && metaStore.isSelection && !isGroupFull.value);
const editingId = ref<number | null>(null);
const directionPath = ref<number[]>([]);
const formDirectionPath = ref<number[]>([]);
const query = reactive<{
  directionLevel1Id?: number;
  directionLevel2Id?: number;
  grade?: Grade;
  admissionYear?: number;
}>({});
const form = reactive<GroupPayload>(createEmptyForm());
const gradeOptions = Object.entries(gradeLabels) as Array<[Grade, string]>;

onMounted(loadGroups);

watch(
  () => (route.name === 'admin-group-detail' ? route.params.id : undefined),
  async (id) => {
    if (!id) {
      detailVisible.value = false;
      detailGroup.value = null;
      members.value = [];
      return;
    }
    detailVisible.value = true;
    await loadDetail(String(id));
  },
  { immediate: true }
);

async function loadGroups() {
  loading.value = true;
  try {
    const [groupList, userPage] = await Promise.all([
      getAdminGroups(query),
      getAdminUsers({ page: 1, size: 50 })
    ]);
    groups.value = groupList;
    users.value = Object.fromEntries(userPage.list.map((user) => [user.id, user.username]));
  } finally {
    loading.value = false;
  }
}

async function loadDetail(id: string | number) {
  detailLoading.value = true;
  try {
    const [group, groupMembers] = await Promise.all([getGroup(id), getGroupMembers(id)]);
    detailGroup.value = group;
    members.value = groupMembers;
  } catch {
    detailGroup.value = null;
    members.value = [];
  } finally {
    detailLoading.value = false;
  }
}

function handleDirectionChange(level1Id?: number, level2Id?: number) {
  query.directionLevel1Id = level1Id;
  query.directionLevel2Id = level2Id;
  void loadGroups();
}

function resetSearch() {
  query.directionLevel1Id = undefined;
  query.directionLevel2Id = undefined;
  query.grade = undefined;
  query.admissionYear = undefined;
  directionPath.value = [];
  void loadGroups();
}

function openCreateDialog() {
  editingId.value = null;
  Object.assign(form, createEmptyForm());
  formDirectionPath.value = [];
  dialogVisible.value = true;
}

function openEditDialog(group: Group) {
  editingId.value = group.id;
  Object.assign(form, {
    name: group.name,
    directionLevel1Id: group.directionLevel1Id,
    directionLevel2Id: group.directionLevel2Id,
    grade: group.grade,
    admissionYear: group.admissionYear,
    maxSize: group.maxSize
  });
  formDirectionPath.value = [group.directionLevel1Id, group.directionLevel2Id];
  dialogVisible.value = true;
}

function handleFormDirectionChange(level1Id?: number, level2Id?: number) {
  form.directionLevel1Id = level1Id || 0;
  form.directionLevel2Id = level2Id || 0;
}

async function saveGroup() {
  if (!form.name.trim() || !form.directionLevel1Id || !form.directionLevel2Id) {
    ElMessage.warning('请填写分组名称和方向');
    return;
  }

  saving.value = true;
  try {
    const payload = { ...form, name: form.name.trim() };
    if (editingId.value) {
      await updateGroup(editingId.value, payload);
      ElMessage.success('分组已更新');
    } else {
      await createGroup(payload);
      ElMessage.success('分组已创建');
    }
    dialogVisible.value = false;
    await loadGroups();
  } finally {
    saving.value = false;
  }
}

async function handleDelete(group: Group) {
  await deleteGroup(group.id);
  ElMessage.success('分组已删除');
  await loadGroups();
}

function openDetail(id: number) {
  void router.push({ name: 'admin-group-detail', params: { id } });
}

function resetAddMemberForm() {
  addMemberForm.userId = undefined;
  addMemberForm.realName = '';
  addMemberForm.phone = '';
  addMemberForm.college = '';
  addMemberForm.major = '';
  addMemberForm.className = '';
  addMemberForm.introduction = '';
  addMemberUserOptions.value = [];
}

function openAddMemberDialog() {
  if (!canAddMember.value) {
    ElMessage.warning(isGroupFull.value ? '当前分组已满员' : '当前不是选拔期，无法补录成员');
    return;
  }
  resetAddMemberForm();
  addMemberVisible.value = true;
  void searchAddMemberUsers('');
}

async function searchAddMemberUsers(keyword = '') {
  addMemberUserLoading.value = true;
  try {
    const page = await getAdminUsers({
      keyword: String(keyword || '').trim() || undefined,
      status: 'ACTIVE',
      page: 1,
      size: 20
    });
    const memberIds = new Set(members.value.map((item) => item.userId));
    addMemberUserOptions.value = page.list.filter((user) => user.role !== 'ADMIN' && !memberIds.has(user.id));
  } finally {
    addMemberUserLoading.value = false;
  }
}

async function submitAddMember() {
  if (!detailGroup.value) return;
  if (!canAddMember.value) {
    ElMessage.warning(isGroupFull.value ? '当前分组已满员' : '当前不是选拔期，无法补录成员');
    return;
  }
  if (!addMemberForm.userId) {
    ElMessage.warning('请选择要添加的用户');
    return;
  }
  if (!addMemberForm.realName.trim() || !addMemberForm.phone.trim() || !addMemberForm.college.trim() || !addMemberForm.major.trim() || !addMemberForm.className.trim()) {
    ElMessage.warning('请填写姓名、手机号、学院、专业和班级');
    return;
  }
  if (!/^1\d{10}$/.test(addMemberForm.phone.trim())) {
    ElMessage.warning('手机号格式不正确');
    return;
  }

  addMemberSaving.value = true;
  try {
    await addGroupMember(detailGroup.value.id, {
      userId: addMemberForm.userId,
      realName: addMemberForm.realName.trim(),
      phone: addMemberForm.phone.trim(),
      college: addMemberForm.college.trim(),
      major: addMemberForm.major.trim(),
      className: addMemberForm.className.trim(),
      introduction: addMemberForm.introduction.trim() || undefined
    });
    ElMessage.success('成员添加成功');
    addMemberVisible.value = false;
    await Promise.all([loadDetail(detailGroup.value.id), loadGroups()]);
  } finally {
    addMemberSaving.value = false;
  }
}

function handleDetailClosed() {
  addMemberVisible.value = false;
  if (route.name === 'admin-group-detail') {
    void router.push({ name: 'admin-groups' });
  }
}

function createEmptyForm(): GroupPayload {
  return {
    name: '',
    directionLevel1Id: 0,
    directionLevel2Id: 0,
    grade: 'YEAR_1',
    admissionYear: new Date().getFullYear(),
    maxSize: 20
  };
}

function getGroupDirectionLabel(group: Group) {
  const level1 = findDirectionName(group.directionLevel1Id);
  const level2 = findDirectionName(group.directionLevel2Id);
  return [level1, level2].filter(Boolean).join(' / ') || '未知方向';
}

function getGradeLabel(grade: Grade) {
  return gradeLabels[grade] || grade;
}

function findDirectionName(id: number) {
  const direction = metaStore.directions.flatMap((item) => [item, ...(item.children || [])]).find((item) => item.id === id);
  return direction?.name;
}

function getLeaderLabel(group: Group) {
  if (!group.leaderUserId) return '未任命';
  return users.value[group.leaderUserId] || `用户 #${group.leaderUserId}`;
}
</script>

<style scoped>
.form-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0 12px;
}

.full,
:deep(.el-cascader) {
  width: 100%;
}

h3 {
  margin: 18px 0 10px;
  font-size: 16px;
}

.member-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 18px 0 10px;
}

.member-header h3 {
  margin: 0;
}

.add-member-tip,
.add-member-meta {
  margin-bottom: 16px;
}

.add-member-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.muted {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin: 0 0 10px;
}

.group-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.group-card__head {
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: space-between;
  min-width: 0;
}

.group-card__name {
  min-width: 0;
  overflow-wrap: anywhere;
  font-size: 16px;
  font-weight: 600;
  color: var(--app-text);
}

.group-card__meta {
  color: var(--app-muted);
  font-size: 13px;
}

.group-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 8px;
  margin-top: 2px;
}

.group-card__actions :deep(.el-button) {
  margin-left: 0 !important;
  min-height: 32px;
}

@media (max-width: 720px) {
  .form-grid,
  .add-member-grid {
    grid-template-columns: 1fr;
  }
}
</style>
