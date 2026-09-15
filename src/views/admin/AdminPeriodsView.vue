<template>
  <div class="page">
    <PageHeader title="时期管理" description="配置报名、选拔、面试等招新时期的起止时间和启用状态。">
      <template #actions>
        <el-button v-if="!periods.length" type="primary" :loading="initializing" @click="initializePeriods">
          初始化时期配置
        </el-button>
        <el-button :icon="Refresh" :loading="loading" @click="loadPeriods">刷新</el-button>
      </template>
    </PageHeader>

    <section class="page-section">
      <MobileList v-if="isMobile" :data="periods" :loading="loading" empty-text="暂无时期配置" key-field="id">
        <template #item="{ item }">
          <div class="period-card">
            <div class="period-card__head">
              <span class="period-card__name">{{ getPeriodLabel(item.periodType) }}</span>
              <el-tag :type="item.enabled ? 'success' : 'info'" effect="light" size="small">
                {{ item.enabled ? '启用' : '停用' }}
              </el-tag>
            </div>
            <div class="period-card__meta">开始：{{ formatDateTime(item.startTime) }}</div>
            <div class="period-card__meta">结束：{{ formatDateTime(item.endTime) }}</div>
            <div class="period-card__actions">
              <el-button text type="primary" :icon="EditPen" @click="openDialog(item)">编辑</el-button>
            </div>
          </div>
        </template>
      </MobileList>

      <el-table v-else v-loading="loading" :data="periods" empty-text="暂无时期配置">
        <el-table-column label="时期" width="120">
          <template #default="{ row }">{{ getPeriodLabel(row.periodType) }}</template>
        </el-table-column>
        <el-table-column label="开始时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.endTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" effect="light">
              {{ row.enabled ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button text type="primary" :icon="EditPen" @click="openDialog(row)">编辑</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" title="编辑时期" :width="dialogWidth" :close-on-click-modal="false">
      <el-form label-position="top" :model="form">
        <el-form-item label="时期">
          <el-select v-model="form.periodType" class="full">
            <el-option v-for="item in periodOptions" :key="item" :label="periodLabels[item]" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="form.startTime"
            class="full"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ssZ"
            placeholder="请选择开始时间"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="form.endTime"
            class="full"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ssZ"
            placeholder="请选择结束时间"
          />
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="savePeriod">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { EditPen, Refresh } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { onMounted, reactive, ref } from 'vue';

import { getAdminPeriods, saveAdminPeriods, updateAdminPeriod, type PeriodConfig } from '@/api/admin';
import MobileList from '@/components/common/MobileList.vue';
import PageHeader from '@/components/common/PageHeader.vue';
import type { PeriodType } from '@/types/api';
import { periodLabels } from '@/utils/labels';
import { useIsMobile, useOverlayLayout } from '@/composables/useMediaQuery';

const { isMobile, dialogWidth } = useOverlayLayout({ dialogWidth: '560px' });
const periodOptions: PeriodType[] = ['REGISTRATION', 'SELECTION', 'INTERVIEW', 'NOT_OPEN', 'FINISHED'];
const periods = ref<PeriodConfig[]>([]);
const loading = ref(false);
const saving = ref(false);
const initializing = ref(false);
const dialogVisible = ref(false);
const form = reactive<PeriodConfig>(createEmptyPeriod());

onMounted(loadPeriods);

async function loadPeriods() {
  loading.value = true;
  try {
    periods.value = await getAdminPeriods();
  } finally {
    loading.value = false;
  }
}

function openDialog(period: PeriodConfig) {
  Object.assign(form, period);
  dialogVisible.value = true;
}

async function savePeriod() {
  if (!form.id) {
    ElMessage.warning('当前时期缺少 ID，无法单独更新');
    return;
  }

  saving.value = true;
  try {
    await updateAdminPeriod(form.id, { ...form });
    ElMessage.success('时期配置已更新');
    dialogVisible.value = false;
    await loadPeriods();
  } finally {
    saving.value = false;
  }
}

async function initializePeriods() {
  initializing.value = true;
  try {
    await saveAdminPeriods(createDefaultPeriods());
    ElMessage.success('时期配置已初始化');
    await loadPeriods();
  } finally {
    initializing.value = false;
  }
}

function createEmptyPeriod(): PeriodConfig {
  return {
    periodType: 'REGISTRATION',
    startTime: '',
    endTime: '',
    enabled: true
  };
}

function createDefaultPeriods(): PeriodConfig[] {
  const now = new Date();
  const registrationStart = addDays(now, -1);
  const registrationEnd = addDays(now, 14);
  const selectionStart = addDays(registrationEnd, 1);
  const selectionEnd = addDays(selectionStart, 14);
  const interviewStart = addDays(selectionEnd, 1);
  const interviewEnd = addDays(interviewStart, 7);

  return [
    {
      periodType: 'REGISTRATION',
      startTime: formatInputDateTime(registrationStart),
      endTime: formatInputDateTime(registrationEnd),
      enabled: true
    },
    {
      periodType: 'SELECTION',
      startTime: formatInputDateTime(selectionStart),
      endTime: formatInputDateTime(selectionEnd),
      enabled: true
    },
    {
      periodType: 'INTERVIEW',
      startTime: formatInputDateTime(interviewStart),
      endTime: formatInputDateTime(interviewEnd),
      enabled: true
    }
  ];
}

function addDays(date: Date, days: number) {
  const next = new Date(date);
  next.setDate(next.getDate() + days);
  return next;
}

function formatInputDateTime(date: Date) {
  const pad = (value: number) => String(value).padStart(2, '0');
  const offsetMinutes = -date.getTimezoneOffset();
  const offsetSign = offsetMinutes >= 0 ? '+' : '-';
  const offsetHours = Math.floor(Math.abs(offsetMinutes) / 60);
  const offsetRemainderMinutes = Math.abs(offsetMinutes) % 60;

  return [
    date.getFullYear(),
    pad(date.getMonth() + 1),
    pad(date.getDate())
  ].join('-') +
    `T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}` +
    `${offsetSign}${pad(offsetHours)}:${pad(offsetRemainderMinutes)}`;
}

function getPeriodLabel(period: PeriodType) {
  return periodLabels[period] || period;
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
.full {
  width: 100%;
}

.period-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.period-card__head {
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: space-between;
  min-width: 0;
}

.period-card__name {
  font-size: 16px;
  font-weight: 600;
  color: var(--app-text);
}

.period-card__meta {
  color: var(--app-muted);
  font-size: 13px;
  overflow-wrap: anywhere;
}

.period-card__actions :deep(.el-button) {
  margin-left: 0 !important;
  min-height: 32px;
}
</style>
