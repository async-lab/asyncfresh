<template>
  <div class="page">
    <PageHeader title="首页" description="查看当前招新阶段、报名状态、分组和任务概览。">
      <template #actions>
        <el-button type="primary" :icon="EditPen" @click="$router.push('/app/applications')">
          我的报名
        </el-button>
      </template>
    </PageHeader>

    <el-alert v-if="!metaStore.isRegistration" type="warning" :closable="false" show-icon>
      当前不是报名期，报名新增和编辑入口已关闭。
    </el-alert>

    <div class="metric-grid" v-loading="loading">
      <div class="metric-card">
        <span class="muted">当前时期</span>
        <strong>{{ currentPeriodLabel }}</strong>
        <small>{{ serverTimeLabel }}</small>
      </div>
      <div class="metric-card">
        <span class="muted">报名状态</span>
        <strong>{{ applicationStatusText }}</strong>
        <StatusTag v-if="primaryApplication?.status" :value="primaryApplication.status" />
      </div>
      <div class="metric-card">
        <span class="muted">意向方向</span>
        <strong>{{ directionText }}</strong>
        <small>{{ applicationCountText }}</small>
      </div>
      <div class="metric-card">
        <span class="muted">所在分组</span>
        <strong>{{ groupCount }}</strong>
        <small>{{ groupText }}</small>
      </div>
    </div>

    <div class="page-section">
      <div class="page-toolbar">
        <h2>报名概览</h2>
        <el-button text type="primary" @click="$router.push('/app/applications')">查看全部</el-button>
      </div>

      <div v-if="recentApplications.length" class="application-list">
        <div v-for="application in recentApplications" :key="application.id" class="application-item">
          <div>
            <strong>{{ getDirectionLabel(application) }}</strong>
            <span class="muted">更新于 {{ formatDateTime(application.updatedAt) }}</span>
          </div>
          <StatusTag :value="application.status" />
        </div>
      </div>
      <el-empty v-else description="暂无报名申请" :image-size="80">
        <el-button type="primary" :disabled="!metaStore.isRegistration" @click="$router.push('/app/applications')">
          新增申请
        </el-button>
      </el-empty>
    </div>

    <div class="page-section">
      <h2>快捷入口</h2>
      <el-space wrap>
        <el-button type="primary" @click="$router.push('/app/applications')">我的报名</el-button>
        <el-button @click="$router.push('/app/tasks')">查看任务</el-button>
        <el-button @click="$router.push('/app/announcements')">公告通知</el-button>
        <el-button @click="$router.push('/app/materials')">学习资料</el-button>
      </el-space>
    </div>
  </div>
</template>

<script setup lang="ts">
import { EditPen } from '@element-plus/icons-vue';
import { computed, onMounted, ref } from 'vue';

import { getApplications, getApplicationSummary } from '@/api/applications';
import PageHeader from '@/components/common/PageHeader.vue';
import StatusTag from '@/components/common/StatusTag.vue';
import { useAuthStore } from '@/stores/auth';
import { useMetaStore } from '@/stores/meta';
import type { Application, ApplicationSummary } from '@/types/api';
import { applicationStatusLabels, periodLabels } from '@/utils/labels';

const authStore = useAuthStore();
const metaStore = useMetaStore();
const applications = ref<Application[]>([]);
const summary = ref<ApplicationSummary | null>(null);
const loading = ref(false);

const currentPeriodLabel = computed(() => (metaStore.period ? periodLabels[metaStore.period] : '未知'));
const groupCount = computed(() => authStore.user?.groups?.length || 0);
const groupText = computed(() => {
  const groups = authStore.user?.groups || [];
  return groups.length ? groups.map((group) => group.name).join('、') : '暂未分组';
});
const sortedApplications = computed(() =>
  [...applications.value].sort((left, right) => Date.parse(right.updatedAt) - Date.parse(left.updatedAt))
);
const recentApplications = computed(() => sortedApplications.value.slice(0, 3));
const primaryApplication = computed(
  () => sortedApplications.value.find((item) => item.status !== 'WITHDRAWN') || sortedApplications.value[0] || null
);
const applicationStatusText = computed(() =>
  primaryApplication.value?.status ? applicationStatusLabels[primaryApplication.value.status] : '未报名'
);
const directionText = computed(() => getDirectionLabel(primaryApplication.value));
const applicationCountText = computed(() => {
  const data = summary.value;
  if (!data) return `${applications.value.length} 份申请`;
  return `${data.applicationCount} 份申请，${data.groupedCount} 份已分组`;
});
const serverTimeLabel = computed(() => {
  const serverTime = metaStore.currentPeriod?.serverTime;
  return serverTime ? `服务器时间 ${formatDateTime(serverTime)}` : '等待时期同步';
});
const directionNameMap = computed(() => {
  const map = new Map<number, string>();
  metaStore.directions.forEach((level1) => {
    map.set(level1.id, level1.name);
    level1.children?.forEach((level2) => map.set(level2.id, level2.name));
  });
  return map;
});

onMounted(loadDashboard);

async function loadDashboard() {
  loading.value = true;
  try {
    const [applicationList, applicationSummary] = await Promise.all([
      getApplications(),
      getApplicationSummary()
    ]);
    applications.value = applicationList;
    summary.value = applicationSummary;
  } finally {
    loading.value = false;
  }
}

function getDirectionLabel(application?: Application | null) {
  if (!application) {
    return '暂未选择';
  }

  const level1 = directionNameMap.value.get(application.directionLevel1Id);
  const level2 = directionNameMap.value.get(application.directionLevel2Id);
  return [level1, level2].filter(Boolean).join(' / ') || '未知方向';
}

function formatDateTime(value?: string) {
  if (!value) {
    return '-';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

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
  margin: 0 0 12px;
  font-size: 18px;
}

.metric-card {
  min-height: 128px;
}

.metric-card small {
  display: block;
  margin-top: 8px;
  color: var(--app-muted);
  line-height: 1.5;
}

.application-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.application-item {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid var(--app-border);
}

.application-item:last-child {
  border-bottom: 0;
}

.application-item strong,
.application-item span {
  display: block;
}

.application-item span {
  margin-top: 4px;
  font-size: 13px;
}

@media (max-width: 640px) {
  .application-item {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
