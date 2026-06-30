<template>
  <div class="page">
    <PageHeader title="时期管理" description="配置报名、选拔、面试等招新时期的起止时间和启用状态。">
      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="loadPeriods">刷新</el-button>
      </template>
    </PageHeader>

    <section class="page-section">
      <el-table v-loading="loading" :data="periods" empty-text="暂无时期配置">
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
            <el-button text type="primary" :icon="EditPen" @click="openDialog(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" title="编辑时期" width="560px" :close-on-click-modal="false">
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
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="请选择开始时间"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="form.endTime"
            class="full"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
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

import { getAdminPeriods, updateAdminPeriod, type PeriodConfig } from '@/api/admin';
import PageHeader from '@/components/common/PageHeader.vue';
import type { PeriodType } from '@/types/api';
import { periodLabels } from '@/utils/labels';

const periodOptions: PeriodType[] = ['REGISTRATION', 'SELECTION', 'INTERVIEW', 'NOT_OPEN', 'FINISHED'];
const periods = ref<PeriodConfig[]>([]);
const loading = ref(false);
const saving = ref(false);
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

function createEmptyPeriod(): PeriodConfig {
  return {
    periodType: 'REGISTRATION',
    startTime: '',
    endTime: '',
    enabled: true
  };
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
</style>
