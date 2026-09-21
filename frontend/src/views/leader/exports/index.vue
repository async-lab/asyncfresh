<template>
  <div class="page">
    <PageHeader title="负责人导出" description="导出责任包成员、任务成绩。任务附件请到任务评测页逐个下载。" />
    <section class="page-section export-grid">
      <el-select v-model="selectedGroupId" class="group-select" placeholder="选择责任包">
        <el-option v-for="group in groups" :key="group.id" :label="group.name" :value="group.id" />
      </el-select>
      <el-button
        :icon="Download"
        type="primary"
        :disabled="!selectedGroupId || Boolean(exporting)"
        :loading="exporting === 'members'"
        @click="exportMembers"
      >
        导出成员
      </el-button>
      <el-button
        :icon="Download"
        :disabled="!selectedGroupId || Boolean(exporting)"
        :loading="exporting === 'tasks'"
        @click="exportTaskResults"
      >
        导出任务成绩
      </el-button>
    </section>
  </div>
</template>

<script setup lang="ts">
import { Download } from '@element-plus/icons-vue';
import { onMounted, ref } from 'vue';

import { getGroups, getLeaderGroupExportUrl, getLeaderGroupTasksExportUrl } from '@/api/leader';
import PageHeader from '@/components/common/PageHeader.vue';
import type { Group } from '@/types/api';
import { downloadAuthenticatedFile } from '@/utils/download';

const groups = ref<Group[]>([]);
const selectedGroupId = ref<number>();
const exporting = ref<'members' | 'tasks' | ''>('');

onMounted(async () => {
  groups.value = (await getGroups({ page: 1, size: 100 })).list;
  selectedGroupId.value = groups.value[0]?.id;
});

async function exportMembers() {
  if (!selectedGroupId.value) {
    return;
  }
  await runExport('members', getLeaderGroupExportUrl(selectedGroupId.value), `group-members-${selectedGroupId.value}.xlsx`);
}

async function exportTaskResults() {
  if (!selectedGroupId.value) {
    return;
  }
  await runExport(
    'tasks',
    getLeaderGroupTasksExportUrl(selectedGroupId.value),
    `group-task-results-${selectedGroupId.value}.xlsx`
  );
}

async function runExport(type: 'members' | 'tasks', url: string, fileName: string) {
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
  gap: 12px;
  align-items: center;
}

.group-select {
  width: 240px;
}
</style>