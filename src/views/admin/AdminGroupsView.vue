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
      <el-table v-loading="loading" :data="groups" empty-text="暂无分组">
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
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" :icon="View" @click="openDetail(row.id)">详情</el-button>
            <el-button text :icon="EditPen" @click="openEditDialog(row)">编辑</el-button>
            <ConfirmAction title="确认删除该分组？已有成员的分组应由后端拒绝删除。" @confirm="handleDelete(row)">
              <el-button text type="danger" :icon="Delete">删除</el-button>
            </ConfirmAction>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑分组' : '创建分组'" width="620px" :close-on-click-modal="false">
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

    <el-drawer v-model="detailVisible" title="分组详情" size="620px" @closed="handleDetailClosed">
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

          <h3>成员</h3>
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
  </div>
</template>

<script setup lang="ts">
import { Delete, EditPen, Plus, Search, View } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { createGroup, deleteGroup, getAdminGroups, updateGroup, type GroupPayload } from '@/api/admin';
import { getGroup, getGroupMembers } from '@/api/groups';
import ConfirmAction from '@/components/common/ConfirmAction.vue';
import PageHeader from '@/components/common/PageHeader.vue';
import SearchBar from '@/components/common/SearchBar.vue';
import DirectionCascader from '@/components/forms/DirectionCascader.vue';
import { useMetaStore } from '@/stores/meta';
import type { Grade, Group, GroupMember } from '@/types/api';
import { gradeLabels } from '@/utils/labels';

const route = useRoute();
const router = useRouter();
const metaStore = useMetaStore();
const groups = ref<Group[]>([]);
const members = ref<GroupMember[]>([]);
const detailGroup = ref<Group | null>(null);
const loading = ref(false);
const detailLoading = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);
const detailVisible = ref(false);
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
    groups.value = await getAdminGroups(query);
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

function handleDetailClosed() {
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
  return group.leaderUserId ? `用户 #${group.leaderUserId}` : '未任命';
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

@media (max-width: 720px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
