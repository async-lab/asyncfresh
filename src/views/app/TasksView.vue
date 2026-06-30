<template>
  <div class="page">
    <PageHeader title="任务" description="查看本人可见任务、提交状态和截止时间。">
      <template #actions>
        <el-button :icon="Medal" @click="$router.push('/app/scores')">我的成绩</el-button>
      </template>
    </PageHeader>

    <el-alert v-if="!metaStore.isSelection" type="warning" :closable="false" show-icon>
      当前不是选拔期，可以查看任务与当前提交状态，暂不能提交新任务。
    </el-alert>

    <div class="metric-grid">
      <div class="metric-card">
        <span class="muted">全部任务</span>
        <strong>{{ tasks.length }}</strong>
      </div>
      <div class="metric-card">
        <span class="muted">待提交</span>
        <strong>{{ pendingCount }}</strong>
      </div>
      <div class="metric-card">
        <span class="muted">已提交</span>
        <strong>{{ submittedCount }}</strong>
      </div>
      <div class="metric-card">
        <span class="muted">已批阅</span>
        <strong>{{ reviewedCount }}</strong>
      </div>
    </div>

    <div class="page-section">
      <div class="page-toolbar task-toolbar">
        <span class="muted">任务均为本人所在分组发布的组级任务</span>
        <el-button :icon="Refresh" :loading="loading" @click="loadTasks">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="tasks" empty-text="暂无任务">
        <el-table-column prop="title" label="任务标题" min-width="220" />
        <el-table-column prop="maxScore" label="满分" width="90" />
        <el-table-column label="截止时间" min-width="190">
          <template #default="{ row }">
            <div class="deadline-cell">
              <span>{{ formatDateTime(row.deadlineAt) }}</span>
              <el-tag v-if="isExpired(row)" type="danger" effect="light" size="small">已截止</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="提交状态" width="120">
          <template #default="{ row }">
            <StatusTag :value="getSubmissionStatus(row)" />
          </template>
        </el-table-column>
        <el-table-column label="附件" width="90">
          <template #default="{ row }">
            <AttachmentLink :href="getAttachmentHref(row)" label="下载" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" :icon="View" @click="$router.push(`/app/tasks/${row.id}`)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Medal, Refresh, View } from '@element-plus/icons-vue';
import { computed, onMounted, ref } from 'vue';

import { getTasks } from '@/api/tasks';
import AttachmentLink from '@/components/common/AttachmentLink.vue';
import PageHeader from '@/components/common/PageHeader.vue';
import StatusTag from '@/components/common/StatusTag.vue';
import { useMetaStore } from '@/stores/meta';
import type { DisplaySubmissionStatus, Task } from '@/types/api';

const metaStore = useMetaStore();
const tasks = ref<Task[]>([]);
const loading = ref(false);
const pendingCount = computed(() => tasks.value.filter((task) => getSubmissionStatus(task) === 'PENDING').length);
const submittedCount = computed(() => tasks.value.filter((task) => getSubmissionStatus(task) === 'SUBMITTED').length);
const reviewedCount = computed(() => tasks.value.filter((task) => getSubmissionStatus(task) === 'REVIEWED').length);

onMounted(loadTasks);

async function loadTasks() {
  loading.value = true;
  try {
    tasks.value = await getTasks();
  } finally {
    loading.value = false;
  }
}

function getSubmissionStatus(task: Task): DisplaySubmissionStatus {
  if ((task.submissionStatus || task.submission?.status || 'PENDING') === 'PENDING' && isExpired(task)) {
    return 'EXPIRED';
  }

  return task.submissionStatus || task.submission?.status || 'PENDING';
}

function getAttachmentHref(task: Task) {
  if (!(task.attachmentFileId || task.attachmentUrl)) return null;
  return task.attachmentUrl || `/api/v1/tasks/${task.id}/attachment`;
}

function isExpired(task: Task) {
  const deadline = Date.parse(task.deadlineAt);
  const now = metaStore.currentPeriod?.serverTime ? Date.parse(metaStore.currentPeriod.serverTime) : Date.now();
  return Number.isFinite(deadline) && Number.isFinite(now) && deadline < now;
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
.task-toolbar {
  margin-bottom: 14px;
}

.deadline-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}
</style>
