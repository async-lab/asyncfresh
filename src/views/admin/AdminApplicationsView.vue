<template>
  <div class="page">
    <PageHeader title="报名管理" description="处理未分组申请，将申请加入目标分组或按规则拒绝。">
      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
      </template>
    </PageHeader>

    <el-alert v-if="!metaStore.isSelection" type="warning" show-icon :closable="false">
      当前不是选拔期，分组和拒绝操作应由后端时期校验最终拦截。
    </el-alert>

    <section class="page-section">
      <SearchBar>
        <el-input v-model="query.keyword" clearable placeholder="姓名、学院、专业" @keyup.enter="loadApplications" />
        <DirectionCascader v-model="directionPath" @change="handleDirectionChange" />
        <el-select v-model="query.grade" clearable placeholder="年级" @change="loadApplications">
          <el-option v-for="[value, label] in gradeOptions" :key="value" :label="label" :value="value" />
        </el-select>
        <template #actions>
          <el-button type="primary" :icon="Search" @click="loadApplications">筛选</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </template>
      </SearchBar>
    </section>

    <section class="page-section">
      <el-table v-loading="loading" :data="applications" empty-text="暂无未分组申请">
        <el-table-column prop="realName" label="姓名" width="110" />
        <el-table-column label="方向" min-width="180">
          <template #default="{ row }">{{ getDirectionLabel(row) }}</template>
        </el-table-column>
        <el-table-column prop="college" label="学院" min-width="140" />
        <el-table-column prop="major" label="专业" min-width="150" />
        <el-table-column label="年级" width="90">
          <template #default="{ row }">{{ getGradeLabel(row.grade) }}</template>
        </el-table-column>
        <el-table-column label="目标分组" min-width="220">
          <template #default="{ row }">
            <el-select v-model="targetGroupIds[row.id]" placeholder="选择分组" filterable>
              <el-option
                v-for="group in getCandidateGroups(row)"
                :key="group.id"
                :label="group.name"
                :value="group.id"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button
              text
              type="primary"
              :icon="Connection"
              :loading="actionId === row.id"
              @click="assignApplication(row)"
            >
              加入分组
            </el-button>
            <el-button text type="danger" :icon="Close" :loading="actionId === row.id" @click="rejectApplication(row)">
              拒绝
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { Close, Connection, Refresh, Search } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { onMounted, reactive, ref } from 'vue';

import {
  addApplicationToGroup,
  getAdminGroups,
  getAdminUngroupedApplications,
  rejectAdminApplication
} from '@/api/admin';
import PageHeader from '@/components/common/PageHeader.vue';
import SearchBar from '@/components/common/SearchBar.vue';
import DirectionCascader from '@/components/forms/DirectionCascader.vue';
import { useMetaStore } from '@/stores/meta';
import type { Application, Grade, Group } from '@/types/api';
import { gradeLabels } from '@/utils/labels';

const metaStore = useMetaStore();
const loading = ref(false);
const actionId = ref<number | null>(null);
const applications = ref<Application[]>([]);
const groups = ref<Group[]>([]);
const directionPath = ref<number[]>([]);
const targetGroupIds = reactive<Record<number, number | undefined>>({});
const query = reactive<{
  keyword: string;
  directionLevel1Id?: number;
  directionLevel2Id?: number;
  grade?: Grade;
}>({
  keyword: ''
});
const gradeOptions = Object.entries(gradeLabels) as Array<[Grade, string]>;

onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    const [applicationList, groupList] = await Promise.all([getAdminUngroupedApplications(query), getAdminGroups()]);
    applications.value = applicationList;
    groups.value = groupList;
  } finally {
    loading.value = false;
  }
}

async function loadApplications() {
  applications.value = await getAdminUngroupedApplications({
    keyword: query.keyword || undefined,
    directionLevel1Id: query.directionLevel1Id,
    directionLevel2Id: query.directionLevel2Id,
    grade: query.grade
  });
}

function resetSearch() {
  query.keyword = '';
  query.directionLevel1Id = undefined;
  query.directionLevel2Id = undefined;
  query.grade = undefined;
  directionPath.value = [];
  void loadApplications();
}

function handleDirectionChange(level1Id?: number, level2Id?: number) {
  query.directionLevel1Id = level1Id;
  query.directionLevel2Id = level2Id;
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
    inputValue: '当前暂不符合分组要求'
  }).catch(() => null);

  if (!result) {
    return;
  }

  actionId.value = application.id;
  try {
    await rejectAdminApplication(application.id, result.value);
    ElMessage.success('申请已拒绝');
    await loadData();
  } finally {
    actionId.value = null;
  }
}

function getDirectionLabel(application: Application) {
  const level1 = findDirectionName(application.directionLevel1Id);
  const level2 = findDirectionName(application.directionLevel2Id);
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
