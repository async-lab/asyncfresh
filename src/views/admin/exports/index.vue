<template>
  <div class="page">
    <PageHeader :title="pageTitle" :description="pageDescription" />
    <section class="page-section export-grid">
      <el-select v-model="selectedGroupId" class="group-select" clearable placeholder="选择责任包">
        <el-option v-for="group in groups" :key="group.id" :label="group.name" :value="group.id" />
      </el-select>

      <el-select v-if="isTaskDownloadPage" v-model="selectedTaskId" class="group-select" clearable placeholder="选择任务">
        <el-option v-for="task in tasks" :key="task.id" :label="task.title" :value="task.id" />
      </el-select>

      <template v-if="isTaskDownloadPage">
        <el-button :icon="FolderOpened" type="primary" disabled>
          批量下载提交
        </el-button>
      </template>
      <template v-else>
        <el-button
          :icon="Download"
          type="primary"
          :disabled="Boolean(exporting)"
          :loading="exporting === 'applications'"
          @click="exportApplications"
        >
          报名导出
        </el-button>
        <el-button
          :icon="Download"
          :disabled="Boolean(exporting)"
          :loading="exporting === 'groups'"
          @click="exportGroups"
        >
          分组导出
        </el-button>
        <el-button
          :icon="Download"
          :disabled="!selectedGroupId || Boolean(exporting)"
          :loading="exporting === 'tasks'"
          @click="exportTaskResults"
        >
          任务成绩导出
        </el-button>
      </template>
    </section>
  </div>
</template>

<script setup lang="ts">
import { Download, FolderOpened } from '@element-plus/icons-vue';
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';

import {
  getAdminGroups,
  getAdminManagedTasks,
  getApplicationsExportUrl,
  getGroupsExportUrl,
  getGroupTasksExportUrl
} from '@/api/admin';
import PageHeader from '@/components/common/PageHeader.vue';
import type { Group, Task } from '@/types/api';
import { downloadAuthenticatedFile } from '@/utils/download';

const route = useRoute();
const groups = ref<Group[]>([]);
const tasks = ref<Task[]>([]);
const selectedGroupId = ref<number>();
const selectedTaskId = ref<number>();
const exporting = ref<'applications' | 'groups' | 'tasks' | ''>('');
const isTaskDownloadPage = computed(() => route.name === 'admin-task-downloads');
const pageTitle = computed(() => (isTaskDownloadPage.value ? '任务批下载' : 'Excel导出'));
const pageDescription = computed(() =>
  isTaskDownloadPage.value
    ? '后端尚未提供任务附件批量下载接口，请到任务评测页逐个下载提交附件。'
    : '导出报名信息、分组结果和任务成绩。'
);

onMounted(async () => {
  groups.value = await getAdminGroups();
});

watch(selectedGroupId, async (groupId) => {
  selectedTaskId.value = undefined;
  tasks.value = groupId && isTaskDownloadPage.value ? await getAdminManagedTasks(groupId) : [];
});

watch(isTaskDownloadPage, async (isDownloadPage) => {
  selectedTaskId.value = undefined;
  tasks.value = selectedGroupId.value && isDownloadPage ? await getAdminManagedTasks(selectedGroupId.value) : [];
});

async function exportApplications() {
  await runExport('applications', getApplicationsExportUrl(), 'applications.xlsx');
}

async function exportGroups() {
  await runExport('groups', getGroupsExportUrl(), 'group-members.xlsx');
}

async function exportTaskResults() {
  if (!selectedGroupId.value) {
    return;
  }
  await runExport(
    'tasks',
    getGroupTasksExportUrl(selectedGroupId.value),
    `group-task-results-${selectedGroupId.value}.xlsx`
  );
}

async function runExport(type: 'applications' | 'groups' | 'tasks', url: string, fileName: string) {
  if (exporting.value) {
    return;
  }

  exporting.value = type;
  try {
    await downloadAuthenticatedFile(url, fileName);
  } finally {
    exporting.value = '';
  }
}
</script>

<style scoped>
.export-grid {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.group-select {
  width: 240px;
}
</style>