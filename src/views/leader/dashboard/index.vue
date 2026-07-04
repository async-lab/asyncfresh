<template>
  <div class="page">
    <PageHeader title="负责人工作台" description="汇总责任包、组员、公告、资料和任务推进情况。" />

    <section class="metric-grid">
      <div class="metric-card">
        <span class="muted">责任包</span>
        <strong>{{ groups.length }}</strong>
      </div>
      <div class="metric-card">
        <span class="muted">组员</span>
        <strong>{{ memberCount }}</strong>
      </div>
      <div class="metric-card">
        <span class="muted">任务</span>
        <strong>{{ tasks.length }}</strong>
      </div>
      <div class="metric-card">
        <span class="muted">待处理申请</span>
        <strong>{{ applications.length }}</strong>
      </div>
    </section>

    <section class="page-section">
      <div class="page-toolbar">
        <h3>近期任务</h3>
        <el-button :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
      </div>
      <PageTable :data="tasks.slice(0, 5)" :loading="loading">
        <el-table-column prop="title" label="任务" min-width="220" />
        <el-table-column label="责任包" min-width="160">
          <template #default="{ row }">{{ groupName(row.groupId) }}</template>
        </el-table-column>
        <el-table-column prop="maxScore" label="满分" width="90" />
        <el-table-column label="截止时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.deadlineAt) }}</template>
        </el-table-column>
      </PageTable>
    </section>
  </div>
</template>

<script setup lang="ts">
import { Refresh } from '@element-plus/icons-vue';
import { computed, onMounted, ref } from 'vue';

import { getGroups, getLeaderUngroupedApplications, getTasks } from '@/api/leader';
import { getGroupMembers } from '@/api/groups';
import PageHeader from '@/components/common/PageHeader.vue';
import PageTable from '@/components/common/PageTable.vue';
import type { Application, Group, Task } from '@/types/api';

const loading = ref(false);
const groups = ref<Group[]>([]);
const tasks = ref<Task[]>([]);
const applications = ref<Application[]>([]);
const memberCount = ref(0);
const groupMap = computed(() => new Map(groups.value.map((group) => [group.id, group.name])));

onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    const groupResult = await getGroups({ page: 1, size: 100 });
    groups.value = groupResult.list;
    const memberLists = await Promise.all(groups.value.map((group) => getGroupMembers(group.id)));
    memberCount.value = memberLists.reduce((sum, list) => sum + list.length, 0);
    tasks.value = await getTasks();
    applications.value = await getLeaderUngroupedApplications();
  } finally {
    loading.value = false;
  }
}

function groupName(groupId: number) {
  return groupMap.value.get(groupId) || `责任包 #${groupId}`;
}

function formatDateTime(value?: string) {
  if (!value) return '-';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN', { hour12: false });
}
</script>
