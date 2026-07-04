<template>
  <div class="page">
    <PageHeader title="Excel导出" description="导出报名、分组、任务成绩，并批量下载任务提交附件。" />
    <section class="page-section export-grid">
      <el-select v-model="selectedGroupId" class="group-select" clearable placeholder="选择责任包">
        <el-option v-for="group in groups" :key="group.id" :label="group.name" :value="group.id" />
      </el-select>
      <el-select v-model="selectedTaskId" class="group-select" clearable placeholder="选择任务">
        <el-option v-for="task in tasks" :key="task.id" :label="task.title" :value="task.id" />
      </el-select>
      <el-button :icon="Download" type="primary" @click="openUrl(getApplicationsExportUrl())">报名导出</el-button>
      <el-button :icon="Download" @click="openUrl(getGroupsExportUrl())">分组导出</el-button>
      <el-button :icon="Download" :disabled="!selectedGroupId" @click="openUrl(taskExportUrl)">任务成绩导出</el-button>
      <el-button :icon="FolderOpened" :disabled="!selectedGroupId" @click="openUrl(batchDownloadUrl)">任务批下载</el-button>
    </section>
  </div>
</template>

<script setup lang="ts">
import { Download, FolderOpened } from '@element-plus/icons-vue';
import { computed, onMounted, ref, watch } from 'vue';

import {
  getAdminGroups,
  getAdminTaskBatchDownloadUrl,
  getApplicationsExportUrl,
  getGroupsExportUrl,
  getGroupTasksExportUrl,
  getTasks
} from '@/api/admin';
import PageHeader from '@/components/common/PageHeader.vue';
import type { Group, Task } from '@/types/api';

const groups = ref<Group[]>([]);
const tasks = ref<Task[]>([]);
const selectedGroupId = ref<number>();
const selectedTaskId = ref<number>();
const taskExportUrl = computed(() => (selectedGroupId.value ? getGroupTasksExportUrl(selectedGroupId.value) : ''));
const batchDownloadUrl = computed(() =>
  selectedGroupId.value ? getAdminTaskBatchDownloadUrl(selectedGroupId.value, selectedTaskId.value) : ''
);

onMounted(async () => {
  groups.value = await getAdminGroups();
});

watch(selectedGroupId, async (groupId) => {
  selectedTaskId.value = undefined;
  tasks.value = groupId ? await getTasks({ groupId }) : [];
});

function openUrl(url: string) {
  if (url) window.open(url, '_blank');
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
