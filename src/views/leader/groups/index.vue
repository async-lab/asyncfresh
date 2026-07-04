<template>
  <div class="page">
    <PageHeader title="责任包" description="查看本人负责的责任包、容量和方向信息。" />
    <section class="page-section">
      <PageTable :data="groups" :loading="loading">
        <el-table-column prop="name" label="责任包" min-width="180" />
        <el-table-column label="年级" width="110">
          <template #default="{ row }">{{ getGradeLabel(row.grade) }}</template>
        </el-table-column>
        <el-table-column prop="admissionYear" label="入学年份" width="110" />
        <el-table-column prop="maxSize" label="容量" width="90" />
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button text type="primary" @click="$router.push(`/leader/groups/${row.id}/members`)">查看组员</el-button>
          </template>
        </el-table-column>
      </PageTable>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';

import { getGroups } from '@/api/leader';
import PageHeader from '@/components/common/PageHeader.vue';
import PageTable from '@/components/common/PageTable.vue';
import type { Grade, Group } from '@/types/api';
import { gradeLabels } from '@/utils/labels';

const loading = ref(false);
const groups = ref<Group[]>([]);

onMounted(async () => {
  loading.value = true;
  try {
    groups.value = (await getGroups({ page: 1, size: 100 })).list;
  } finally {
    loading.value = false;
  }
});

function getGradeLabel(grade: Grade) {
  return gradeLabels[grade] || grade;
}
</script>
