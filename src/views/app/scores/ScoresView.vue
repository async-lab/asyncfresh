<template>
  <div class="page">
    <PageHeader title="我的成绩" description="查看任务成绩、满分、评语和批阅时间。">
      <template #actions>
        <el-button :icon="Tickets" @click="$router.push('/app/tasks')">返回任务</el-button>
      </template>
    </PageHeader>

    <div class="metric-grid">
      <div class="metric-card">
        <span class="muted">已批阅任务</span>
        <strong>{{ scores.length }}</strong>
      </div>
      <div class="metric-card">
        <span class="muted">平均得分率</span>
        <strong>{{ averageRate }}</strong>
      </div>
      <div class="metric-card">
        <span class="muted">累计得分</span>
        <strong>{{ totalScore }}</strong>
      </div>
    </div>

    <section class="page-section">
      <div class="page-toolbar">
        <h2>成绩列表</h2>
        <el-button :icon="Refresh" :loading="loading" @click="loadScores">刷新</el-button>
      </div>
      <el-table v-loading="loading" :data="scores" empty-text="暂无成绩">
        <el-table-column prop="taskTitle" label="任务" min-width="220" />
        <el-table-column label="成绩" width="140">
          <template #default="{ row }">
            <strong>{{ row.score }} / {{ row.maxScore }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="得分率" width="110">
          <template #default="{ row }">{{ getRate(row) }}</template>
        </el-table-column>
        <el-table-column prop="comment" label="评语" min-width="240">
          <template #default="{ row }">{{ row.comment || '暂无评语' }}</template>
        </el-table-column>
        <el-table-column label="批阅时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.reviewedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" :icon="View" @click="$router.push(`/app/tasks/${row.taskId}`)">
              任务详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { Refresh, Tickets, View } from '@element-plus/icons-vue';
import { computed, onMounted, ref } from 'vue';

import { getMyScores } from '@/api/tasks';
import PageHeader from '@/components/common/PageHeader.vue';
import type { TaskScore } from '@/types/api';

const loading = ref(false);
const scores = ref<TaskScore[]>([]);
const totalScore = computed(() => scores.value.reduce((sum, item) => sum + item.score, 0));
const totalMaxScore = computed(() => scores.value.reduce((sum, item) => sum + item.maxScore, 0));
const averageRate = computed(() => {
  if (!totalMaxScore.value) return '-';
  return `${Math.round((totalScore.value / totalMaxScore.value) * 100)}%`;
});

onMounted(loadScores);

async function loadScores() {
  loading.value = true;
  try {
    scores.value = await getMyScores();
  } finally {
    loading.value = false;
  }
}

function getRate(score: TaskScore) {
  if (!score.maxScore) return '-';
  return `${Math.round((score.score / score.maxScore) * 100)}%`;
}

function formatDateTime(value?: string) {
  if (!value) return '-';

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date);
}
</script>

<style scoped>
h2 {
  margin: 0;
  font-size: 18px;
}
</style>
