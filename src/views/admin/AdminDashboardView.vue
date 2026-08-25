<template>
  <div class="page dashboard-page">
    <PageHeader
      title="仪表盘"
      description="查看招新进度、报名分布与待处理事项，让管理动作更快落到关键数据上。"
    />

    <section class="dashboard-layout">
      <div class="dashboard-main">
        <div class="kpi-grid" v-loading="loading">
          <article class="kpi-card">
            <span class="muted">注册人数</span>
            <strong>{{ summary.userCount }}</strong>
            <small>系统累计注册用户</small>
          </article>
          <article class="kpi-card accent">
            <span class="muted">报名申请</span>
            <strong>{{ summary.applicationCount }}</strong>
            <small>当前周期报名总量</small>
          </article>
          <article class="kpi-card">
            <span class="muted">已分组申请</span>
            <strong>{{ summary.groupedApplicationCount }}</strong>
            <small>已进入分组流程</small>
          </article>
          <article class="kpi-card">
            <span class="muted">任务完成率</span>
            <strong>{{ Math.round(summary.taskCompletionRate * 100) }}%</strong>
            <small>任务批阅与提交进度</small>
          </article>
        </div>

        <div class="content-grid">
          <section class="panel chart-panel">
            <div class="panel-head">
              <div>
                <h2>报名处理概览</h2>
                <p>将已分组与待处理状态放在同一个视野里，方便快速分流。</p>
              </div>
              <el-tag effect="plain">本期概览</el-tag>
            </div>

            <div class="chart-layout">
              <div ref="chartRef" class="chart"></div>
              <div class="chart-legend">
                <div class="legend-item">
                  <span class="legend-dot primary"></span>
                  <div>
                    <strong>已分组</strong>
                    <p>{{ summary.groupedApplicationCount }} 条</p>
                  </div>
                </div>
                <div class="legend-item">
                  <span class="legend-dot accent"></span>
                  <div>
                    <strong>待处理</strong>
                    <p>{{ pendingApplications }} 条</p>
                  </div>
                </div>
                <div class="legend-item">
                  <span class="legend-dot muted"></span>
                  <div>
                    <strong>负责人</strong>
                    <p>{{ summary.leaderCount }} 位</p>
                  </div>
                </div>
              </div>
            </div>
          </section>

          <section class="panel">
            <div class="panel-head">
              <div>
                <h2>系统节奏</h2>
                <p>把关键动作拆成一眼可见的管理节奏。</p>
              </div>
            </div>

            <div class="status-stack">
              <div class="status-row">
                <div>
                  <strong>未分配申请</strong>
                  <p class="muted">需要优先处理的报名</p>
                </div>
                <el-progress :percentage="pendingRate" :stroke-width="10" />
              </div>
              <div class="status-row">
                <div>
                  <strong>任务完成率</strong>
                  <p class="muted">提交到批阅的整体进度</p>
                </div>
                <el-progress :percentage="Math.round(summary.taskCompletionRate * 100)" :stroke-width="10" />
              </div>
              <div class="status-row">
                <div>
                  <strong>已分组覆盖</strong>
                  <p class="muted">报名中进入分组的比例</p>
                </div>
                <el-progress :percentage="groupedRate" :stroke-width="10" />
              </div>
            </div>
          </section>
        </div>
      </div>

      <aside class="dashboard-rail">
        <section class="panel rail-card">
          <div class="panel-head">
            <div>
              <h2>快速操作</h2>
              <p>最常用的管理路径集中在这里。</p>
            </div>
          </div>

          <div class="action-list">
            <el-button type="primary" class="action-btn" @click="go('/admin/applications')">查看报名</el-button>
            <el-button class="action-btn" @click="go('/admin/groups')">处理分组</el-button>
            <el-button class="action-btn" @click="go('/admin/tasks')">管理任务</el-button>
            <el-button class="action-btn" @click="go('/admin/exports')">导出数据</el-button>
          </div>
        </section>

        <section class="panel rail-card">
          <div class="panel-head">
            <div>
              <h2>关键指标</h2>
              <p>用更紧凑的模块呈现需要盯住的数字。</p>
            </div>
          </div>

          <div class="mini-metrics">
            <div>
              <span>已分组用户</span>
              <strong>{{ summary.groupedUserCount }}</strong>
            </div>
            <div>
              <span>负责人数量</span>
              <strong>{{ summary.leaderCount }}</strong>
            </div>
            <div>
              <span>未分组申请</span>
              <strong>{{ summary.unassignedApplicationCount }}</strong>
            </div>
          </div>
        </section>
      </aside>
    </section>
  </div>
</template>

<script setup lang="ts">
import * as echarts from 'echarts';
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';

import { getDashboardSummary } from '@/api/admin';
import PageHeader from '@/components/common/PageHeader.vue';
import type { AdminDashboardSummary } from '@/types/api';

const router = useRouter();
const loading = ref(false);
const chartRef = ref<HTMLDivElement | null>(null);
let chart: echarts.ECharts | null = null;

const summary = reactive<AdminDashboardSummary>({
  userCount: 0,
  applicationCount: 0,
  groupedUserCount: 0,
  groupedApplicationCount: 0,
  unassignedApplicationCount: 0,
  leaderCount: 0,
  taskCompletionRate: 0
});

const pendingApplications = computed(() => computedPendingApplications(summary));
const pendingRate = computed(() => {
  if (!summary.applicationCount) return 0;
  return Math.round((summary.unassignedApplicationCount / summary.applicationCount) * 100);
});
const groupedRate = computed(() => {
  if (!summary.applicationCount) return 0;
  return Math.round((summary.groupedApplicationCount / summary.applicationCount) * 100);
});

onMounted(async () => {
  await loadSummary();
  initChart();
  window.addEventListener('resize', handleResize);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize);
  chart?.dispose();
  chart = null;
});

watch(
  () => [summary.groupedApplicationCount, summary.unassignedApplicationCount, summary.applicationCount],
  () => {
    renderChart();
  }
);

async function loadSummary() {
  loading.value = true;
  try {
    Object.assign(summary, await getDashboardSummary());
  } finally {
    loading.value = false;
  }
}

function initChart() {
  if (!chartRef.value) return;
  chart = echarts.init(chartRef.value);
  renderChart();
}

function renderChart() {
  if (!chart) return;
  const pending = Math.max(summary.applicationCount - summary.groupedApplicationCount, summary.unassignedApplicationCount, 0);
  const grouped = Math.max(summary.groupedApplicationCount, 0);

  chart.setOption({
    animationDuration: 700,
    color: ['#a59bd4', '#c6b38d'],
    tooltip: {
      trigger: 'item',
      borderWidth: 0,
      backgroundColor: 'rgba(251, 248, 242, 0.96)',
      textStyle: { color: '#2f2b26' }
    },
    series: [
      {
        name: '报名处理',
        type: 'pie',
        radius: ['62%', '82%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 12,
          borderColor: '#fbf8f2',
          borderWidth: 4
        },
        label: { show: false },
        labelLine: { show: false },
        data: [
          { value: grouped, name: '已分组' },
          { value: pending, name: '待处理' }
        ]
      }
    ],
    graphic: {
      type: 'text',
      left: 'center',
      top: 'center',
      style: {
        text: `${summary.applicationCount}\n报名总量`,
        textAlign: 'center',
        fill: '#2f2b26',
        fontSize: 18,
        fontWeight: 700,
        lineHeight: 24
      }
    }
  });
}

function handleResize() {
  chart?.resize();
}

function go(path: string) {
  void router.push(path);
}

function computedPendingApplications(source: AdminDashboardSummary) {
  return Math.max(source.applicationCount - source.groupedApplicationCount, source.unassignedApplicationCount, 0);
}

</script>

<style scoped>
.dashboard-page {
  gap: 20px;
}

.dashboard-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 316px;
  gap: 18px;
  align-items: start;
}

.dashboard-main {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-width: 0;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.kpi-card {
  padding: 18px;
  border: 1px solid rgba(126, 114, 97, 0.1);
  border-radius: 16px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(244, 238, 229, 0.84)),
    var(--app-surface-strong);
  box-shadow:
    10px 10px 22px rgba(145, 128, 106, 0.1),
    -8px -8px 18px rgba(255, 255, 255, 0.82);
}

.kpi-card.accent {
  background:
    linear-gradient(180deg, rgba(165, 155, 212, 0.16), rgba(255, 255, 255, 0.92)),
    var(--app-surface-strong);
}

.kpi-card strong {
  display: block;
  margin-top: 10px;
  color: var(--app-text);
  font-size: 28px;
  line-height: 1;
}

.kpi-card small {
  display: block;
  margin-top: 10px;
  color: var(--app-muted);
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.65fr) minmax(0, 1fr);
  gap: 18px;
}

.panel {
  padding: 20px;
  border: 1px solid rgba(126, 114, 97, 0.1);
  border-radius: 16px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(246, 240, 230, 0.88)),
    var(--app-surface-strong);
  box-shadow: var(--app-shadow-soft);
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.panel-head h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
}

.panel-head p {
  margin: 6px 0 0;
  color: var(--app-muted);
}

.chart-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 200px;
  gap: 18px;
  align-items: center;
}

.chart {
  width: 100%;
  min-height: 300px;
}

.chart-legend {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  flex: none;
}

.legend-dot.primary {
  background: var(--app-primary);
}

.legend-dot.accent {
  background: var(--app-accent);
}

.legend-dot.muted {
  background: #c0c6bf;
}

.legend-item strong {
  display: block;
  font-size: 14px;
}

.legend-item p {
  margin: 4px 0 0;
  color: var(--app-muted);
}

.status-stack {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.status-row {
  display: grid;
  grid-template-columns: minmax(0, 180px) minmax(0, 1fr);
  gap: 14px;
  align-items: center;
}

.status-row strong {
  display: block;
}

.status-row p {
  margin: 6px 0 0;
}

.status-row :deep(.el-progress) {
  width: 100%;
}

.dashboard-rail {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.rail-card {
  position: sticky;
  top: 18px;
}

.action-list {
  display: grid;
  gap: 10px;
}

.action-btn {
  width: 100%;
  justify-content: flex-start;
}

.mini-metrics {
  display: grid;
  gap: 14px;
}

.mini-metrics div {
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(247, 242, 234, 0.8);
}

.mini-metrics span {
  display: block;
  color: var(--app-muted);
  font-size: 13px;
}

.mini-metrics strong {
  display: block;
  margin-top: 10px;
  font-size: 24px;
  color: var(--app-text);
}

@media (max-width: 1280px) {
  .dashboard-layout,
  .content-grid,
  .chart-layout {
    grid-template-columns: 1fr;
  }

  .rail-card {
    position: static;
  }
}

@media (max-width: 1024px) {
  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .kpi-grid {
    grid-template-columns: 1fr;
  }

  .status-row {
    grid-template-columns: 1fr;
  }
}
</style>
