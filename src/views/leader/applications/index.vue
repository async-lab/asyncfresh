<template>
  <div class="page">
    <PageHeader title="未分组申请处理" description="筛选待分组申请，加入本人负责责任包或驳回申请。" />
    <section class="page-section">
      <div class="page-toolbar app-toolbar">
        <div class="toolbar-left">
          <el-select v-model="selectedGroupId" class="group-select" placeholder="目标责任包">
            <el-option v-for="group in groups" :key="group.id" :label="group.name" :value="group.id" />
          </el-select>
          <el-input v-model="keyword" class="keyword-input" clearable placeholder="搜索姓名、学院、专业" @keyup.enter="loadApplications" />
        </div>
        <el-button :icon="Refresh" :loading="loading" @click="loadApplications">刷新</el-button>
      </div>

      <PageTable :data="filteredApplications" :loading="loading">
        <el-table-column prop="realName" label="姓名" width="110" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column label="院系专业" min-width="220">
          <template #default="{ row }">{{ row.college }} / {{ row.major }}</template>
        </el-table-column>
        <el-table-column label="年级" width="90">
          <template #default="{ row }">{{ getGradeLabel(row.grade) }}</template>
        </el-table-column>
        <el-table-column prop="introduction" label="自我介绍" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="assign(row.id)">加入</el-button>
            <el-popconfirm title="确认驳回该申请？" confirm-button-text="驳回" cancel-button-text="取消" @confirm="reject(row.id)">
              <template #reference>
                <el-button text type="danger">驳回</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </PageTable>
    </section>
  </div>
</template>

<script setup lang="ts">
import { Refresh } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { computed, onMounted, ref } from 'vue';

import { addLeaderApplicationToGroup, getGroups, getLeaderUngroupedApplications, rejectLeaderApplication } from '@/api/leader';
import PageHeader from '@/components/common/PageHeader.vue';
import PageTable from '@/components/common/PageTable.vue';
import type { Application, Grade, Group } from '@/types/api';
import { gradeLabels } from '@/utils/labels';

const loading = ref(false);
const keyword = ref('');
const selectedGroupId = ref<number>();
const groups = ref<Group[]>([]);
const applications = ref<Application[]>([]);
const filteredApplications = computed(() => {
  const text = keyword.value.trim();
  if (!text) return applications.value;
  return applications.value.filter((item) =>
    [item.realName, item.phone, item.college, item.major, item.className, item.introduction].some((value) =>
      String(value || '').includes(text)
    )
  );
});

onMounted(async () => {
  groups.value = (await getGroups({ page: 1, size: 100 })).list;
  selectedGroupId.value = groups.value[0]?.id;
  await loadApplications();
});

async function loadApplications() {
  loading.value = true;
  try {
    applications.value = await getLeaderUngroupedApplications({ keyword: keyword.value || undefined });
  } finally {
    loading.value = false;
  }
}

async function assign(applicationId: number) {
  if (!selectedGroupId.value) return;
  await addLeaderApplicationToGroup(selectedGroupId.value, applicationId);
  ElMessage.success('已加入责任包');
  await loadApplications();
}

async function reject(applicationId: number) {
  await rejectLeaderApplication(applicationId, '负责人驳回');
  ElMessage.success('已驳回申请');
  await loadApplications();
}

function getGradeLabel(grade: Grade) {
  return gradeLabels[grade] || grade;
}
</script>

<style scoped>
.app-toolbar,
.toolbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.toolbar-left {
  flex: 1;
}

.group-select {
  width: 220px;
}

.keyword-input {
  width: 260px;
}
</style>
