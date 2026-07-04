<template>
  <div class="page">
    <PageHeader title="负责人导出" description="导出责任包成员、任务成绩，并批量下载任务提交附件。" />
    <section class="page-section export-grid">
      <el-select v-model="selectedGroupId" class="group-select" placeholder="选择责任包">
        <el-option v-for="group in groups" :key="group.id" :label="group.name" :value="group.id" />
      </el-select>
      <el-select v-model="selectedTaskId" class="group-select" clearable placeholder="选择任务">
        <el-option v-for="task in tasks" :key="task.id" :label="task.title" :value="task.id" />
      </el-select>
      <el-button :icon="Download" type="primary" @click="openUrl(groupExportUrl)">导出成员</el-button>
      <el-button :icon="Download" @click="openUrl(taskExportUrl)">导出任务成绩</el-button>
      <el-button :icon="FolderOpened" @click="openUrl(batchDownloadUrl)">任务批下载</el-button>
    </section>
  </div>
</template>

<script setup lang="ts">
import { Download, FolderOpened } from '@element-plus/icons-vue';
import { computed, onMounted, ref, watch } from 'vue';

import { getGroups, getLeaderGroupExportUrl, getLeaderGroupTasksExportUrl, getLeaderTaskBatchDownloadUrl, getTasks } from '@/api/leader';
import PageHeader from '@/components/common/PageHeader.vue';
import type { Group, Task } from '@/types/api';

const groups = ref<Group[]>([]);
const tasks = ref<Task[]>([]);
const selectedGroupId = ref<number>();
const selectedTaskId = ref<number>();
const groupExportUrl = computed(() => (selectedGroupId.value ? getLeaderGroupExportUrl(selectedGroupId.value) : ''));
const taskExportUrl = computed(() => (selectedGroupId.value ? getLeaderGroupTasksExportUrl(selectedGroupId.value) : ''));
const batchDownloadUrl = computed(() =>
  selectedGroupId.value ? getLeaderTaskBatchDownloadUrl(selectedGroupId.value, selectedTaskId.value) : ''
);

onMounted(async () => {
  groups.value = (await getGroups({ page: 1, size: 100 })).list;
  selectedGroupId.value = groups.value[0]?.id;
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
  gap: 12px;
  align-items: center;
}

.group-select {
  width: 240px;
}
</style>
