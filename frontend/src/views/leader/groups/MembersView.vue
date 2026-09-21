<template>
  <div class="page">
    <PageHeader title="组员信息" description="查看责任包内成员、报名申请和方向信息。" />
    <section class="page-section">
      <div class="page-toolbar member-toolbar">
        <el-select v-model="selectedGroupId" class="group-select" placeholder="选择责任包" @change="loadMembers">
          <el-option v-for="group in groups" :key="group.id" :label="group.name" :value="group.id" />
        </el-select>
        <el-button :icon="Refresh" :loading="loading" @click="loadMembers">刷新</el-button>
      </div>
      <PageTable :data="members" :loading="loading">
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="username" label="账号" width="140" />
        <el-table-column label="年级" width="100">
          <template #default="{ row }">{{ getGradeLabel(row.grade) }}</template>
        </el-table-column>
        <el-table-column prop="admissionYear" label="入学年份" width="110" />
        <el-table-column label="方向" min-width="180">
          <template #default="{ row }">{{ row.directionLevel1Name }} / {{ row.directionLevel2Name }}</template>
        </el-table-column>
        <el-table-column prop="applicationId" label="申请 ID" width="110" />
      </PageTable>
    </section>
  </div>
</template>

<script setup lang="ts">
import { Refresh } from '@element-plus/icons-vue';
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';

import { getGroupMembers, getGroups } from '@/api/leader';
import PageHeader from '@/components/common/PageHeader.vue';
import PageTable from '@/components/common/PageTable.vue';
import type { Grade, Group, GroupMember } from '@/types/api';
import { gradeLabels } from '@/utils/labels';

const route = useRoute();
const loading = ref(false);
const groups = ref<Group[]>([]);
const members = ref<GroupMember[]>([]);
const selectedGroupId = ref<number>();

onMounted(async () => {
  groups.value = (await getGroups({ page: 1, size: 100 })).list;
  selectedGroupId.value = Number(route.params.id) || groups.value[0]?.id;
  await loadMembers();
});

async function loadMembers() {
  if (!selectedGroupId.value) return;
  loading.value = true;
  try {
    members.value = await getGroupMembers(selectedGroupId.value);
  } finally {
    loading.value = false;
  }
}

function getGradeLabel(grade: Grade) {
  return gradeLabels[grade] || grade;
}
</script>

<style scoped>
.member-toolbar {
  margin-bottom: 14px;
}

.group-select {
  width: 240px;
}
</style>
