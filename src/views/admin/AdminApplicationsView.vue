<template>
  <div class="page">
    <PageHeader title="报名管理" description="查看全部报名申请。待分组申请可加入分组或拒绝，已分组申请可取消分组。">
      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
      </template>
    </PageHeader>

    <el-alert v-if="!metaStore.isSelection" type="warning" show-icon :closable="false">
      当前不是选拔期，分组和拒绝操作应由后端时期校验最终拦截。
    </el-alert>

    <section class="page-section">
      <SearchBar>
        <el-input
          v-model="query.keyword"
          clearable
          placeholder="姓名、学院、专业、班级、手机号、用户名、邮箱"
          @keyup.enter="search"
        />
        <DirectionCascader v-model="directionPath" @change="handleDirectionChange" />
        <el-select v-model="query.grade" clearable placeholder="年级" @change="search">
          <el-option v-for="[value, label] in gradeOptions" :key="value" :label="label" :value="value" />
        </el-select>
        <el-select v-model="query.status" clearable placeholder="状态" @change="search">
          <el-option v-for="[value, label] in statusOptions" :key="value" :label="label" :value="value" />
        </el-select>
        <template #actions>
          <el-button type="primary" :icon="Search" @click="search">筛选</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </template>
      </SearchBar>
    </section>

    <section class="page-section">
      <PageTable
        :data="applications"
        :loading="loading"
        pagination
        :page="query.page"
        :size="query.size"
        :total="total"
        empty-text="暂无报名申请"
        @update:page="handlePageChange"
        @update:size="handleSizeChange"
      >
        <el-table-column prop="realName" label="姓名" width="110" />
        <el-table-column label="方向" min-width="180">
          <template #default="{ row }">{{ getDirectionLabel(row) }}</template>
        </el-table-column>
        <el-table-column prop="college" label="学院" min-width="140" />
        <el-table-column prop="major" label="专业" min-width="150" />
        <el-table-column label="年级" width="90">
          <template #default="{ row }">{{ getGradeLabel(row.grade) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <StatusTag :value="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="分组" min-width="220">
          <template #default="{ row }">
            <el-select
              v-if="row.status === 'SUBMITTED'"
              v-model="targetGroupIds[row.id]"
              :disabled="getCandidateGroups(row).length === 0"
              :placeholder="getCandidateGroups(row).length ? '选择分组' : '暂无匹配分组'"
              filterable
            >
              <el-option
                v-for="group in getCandidateGroups(row)"
                :key="group.id"
                :label="group.name"
                :value="group.id"
              />
            </el-select>
            <span v-else-if="row.status === 'GROUPED'">{{ row.groupName || '已分组' }}</span>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.statusRemark || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" :width="isMobile ? 84 : 240" fixed="right">
          <template #default="{ row }">
            <div v-if="row.status === 'SUBMITTED'" class="table-actions">
              <el-button
                text
                type="primary"
                :icon="Connection"
                :disabled="getCandidateGroups(row).length === 0"
                :loading="actionId === row.id"
                @click="assignApplication(row)"
              >
                加入分组
              </el-button>
              <el-button text type="danger" :icon="Close" :loading="actionId === row.id" @click="rejectApplication(row)">
                拒绝
              </el-button>
            </div>
            <div v-else-if="row.status === 'GROUPED'" class="table-actions">
              <el-button
                text
                type="warning"
                :icon="Remove"
                :loading="actionId === row.id"
                @click="unassignApplication(row)"
              >
                取消分组
              </el-button>
            </div>
            <span v-else class="muted">不可操作</span>
          </template>
        </el-table-column>
      </PageTable>
    </section>
  </div>
</template>

<script setup lang="ts">
import { Close, Connection, Refresh, Remove, Search } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { onMounted, reactive, ref } from 'vue';

import {
  addApplicationToGroup,
  getAdminApplications,
  getAdminGroups,
  rejectAdminApplication,
  unassignApplicationFromGroup
} from '@/api/admin';
import PageHeader from '@/components/common/PageHeader.vue';
import PageTable from '@/components/common/PageTable.vue';
import SearchBar from '@/components/common/SearchBar.vue';
import StatusTag from '@/components/common/StatusTag.vue';
import DirectionCascader from '@/components/forms/DirectionCascader.vue';
import { useIsMobile } from '@/composables/useMediaQuery';
import { useMetaStore } from '@/stores/meta';
import type { Application, ApplicationStatus, Grade, Group } from '@/types/api';
import { applicationStatusLabels, gradeLabels } from '@/utils/labels';

const isMobile = useIsMobile();
const metaStore = useMetaStore();
const loading = ref(false);
const actionId = ref<number | null>(null);
const applications = ref<Application[]>([]);
const groups = ref<Group[]>([]);
const total = ref(0);
const directionPath = ref<number[]>([]);
const targetGroupIds = reactive<Record<number, number | undefined>>({});
const query = reactive<{
  keyword: string;
  directionLevel1Id?: number;
  directionLevel2Id?: number;
  grade?: Grade;
  status?: ApplicationStatus;
  page: number;
  size: number;
}>({
  keyword: '',
  page: 1,
  size: 10
});
const gradeOptions = Object.entries(gradeLabels) as Array<[Grade, string]>;
const statusOptions = Object.entries(applicationStatusLabels) as Array<[ApplicationStatus, string]>;

onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    await Promise.all([loadGroups(), loadApplications(false)]);
  } finally {
    loading.value = false;
  }
}

async function loadGroups() {
  groups.value = await getAdminGroups();
}

async function loadApplications(manageLoading = true) {
  if (manageLoading) {
    loading.value = true;
  }
  try {
    const page = await getAdminApplications({
      keyword: query.keyword || undefined,
      status: query.status,
      directionLevel1Id: query.directionLevel1Id,
      directionLevel2Id: query.directionLevel2Id,
      grade: query.grade,
      page: query.page,
      size: query.size
    });
    applications.value = page.list;
    total.value = page.total;
    if (page.list.length === 0 && query.page > 1 && page.total > 0) {
      query.page = page.totalPages || 1;
      await loadApplications(false);
    }
  } finally {
    if (manageLoading) {
      loading.value = false;
    }
  }
}

function search() {
  query.page = 1;
  void loadApplications();
}

function resetSearch() {
  query.keyword = '';
  query.directionLevel1Id = undefined;
  query.directionLevel2Id = undefined;
  query.grade = undefined;
  query.status = undefined;
  directionPath.value = [];
  search();
}

function handleDirectionChange(level1Id?: number, level2Id?: number) {
  query.directionLevel1Id = level1Id;
  query.directionLevel2Id = level2Id;
  search();
}

function handlePageChange(page: number) {
  query.page = page;
  void loadApplications();
}

function handleSizeChange(size: number) {
  query.size = size;
  query.page = 1;
  void loadApplications();
}

function getCandidateGroups(application: Application) {
  return groups.value.filter(
    (group) =>
      group.directionLevel1Id === application.directionLevel1Id &&
      group.directionLevel2Id === application.directionLevel2Id &&
      group.grade === application.grade &&
      group.admissionYear === application.admissionYear
  );
}

async function assignApplication(application: Application) {
  const groupId = targetGroupIds[application.id];
  if (!groupId) {
    ElMessage.warning('请选择目标分组');
    return;
  }

  actionId.value = application.id;
  try {
    await addApplicationToGroup(groupId, application.id);
    ElMessage.success('申请已加入分组');
    await loadData();
  } finally {
    actionId.value = null;
  }
}

async function rejectApplication(application: Application) {
  const result = await ElMessageBox.prompt('请输入拒绝原因，原因会展示给申请人。', '拒绝申请', {
    confirmButtonText: '确认拒绝',
    cancelButtonText: '取消',
    inputPlaceholder: '例如：当前方向名额已满',
    inputValue: '当前暂不符合分组要求',
    inputValidator: (value) => Boolean(value && value.trim()) || '拒绝原因不能为空'
  }).catch(() => null);

  if (!result) {
    return;
  }

  actionId.value = application.id;
  try {
    await rejectAdminApplication(application.id, result.value.trim());
    ElMessage.success('申请已拒绝');
    await loadData();
  } finally {
    actionId.value = null;
  }
}

async function unassignApplication(application: Application) {
  if (!application.groupId) {
    ElMessage.warning('当前申请没有可取消的分组');
    return;
  }

  const result = await ElMessageBox.prompt('可填写取消分组原因，将展示给申请人。', '取消分组', {
    confirmButtonText: '确认取消',
    cancelButtonText: '返回',
    inputPlaceholder: '例如：调整分组',
    inputValue: '调整分组'
  }).catch(() => null);

  if (!result) {
    return;
  }

  actionId.value = application.id;
  try {
    await unassignApplicationFromGroup(application.groupId, application.id, result.value?.trim() || undefined);
    ElMessage.success('已取消分组');
    await loadData();
  } finally {
    actionId.value = null;
  }
}

function getDirectionLabel(application: Application) {
  const level1 = application.directionLevel1Name || findDirectionName(application.directionLevel1Id);
  const level2 = application.directionLevel2Name || findDirectionName(application.directionLevel2Id);
  return [level1, level2].filter(Boolean).join(' / ') || '未知方向';
}

function getGradeLabel(grade: Grade) {
  return gradeLabels[grade] || grade;
}

function findDirectionName(id: number) {
  const direction = metaStore.directions.flatMap((item) => [item, ...(item.children || [])]).find((item) => item.id === id);
  return direction?.name;
}
</script>

<style scoped>
.muted {
  color: var(--el-text-color-secondary);
}
</style>
