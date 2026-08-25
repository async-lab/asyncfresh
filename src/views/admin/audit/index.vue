<template>
  <div class="page">
    <PageHeader title="审计日志" description="查看管理员和负责人关键业务操作记录。" />
    <section class="page-section audit-section">
      <div class="page-toolbar audit-toolbar">
        <div class="toolbar-left">
          <el-select v-model="query.module" class="module-select" clearable placeholder="业务模块">
            <el-option v-for="module in modules" :key="module" :label="module" :value="module" />
          </el-select>
          <el-input v-model="query.keyword" class="keyword-input" clearable placeholder="搜索操作人、对象或详情" @keyup.enter="loadLogs" />
        </div>
        <el-button :icon="Refresh" :loading="loading" @click="loadLogs">刷新</el-button>
      </div>
      <div class="audit-table-shell">
        <div class="audit-table-scroll">
          <PageTable
            class="audit-table"
            v-model:page="query.page"
            v-model:size="query.size"
            :data="logs"
            :loading="loading"
            :total="total"
            pagination
            table-layout="fixed"
            @update:page="loadLogs"
            @update:size="handleSizeChange"
          >
            <el-table-column prop="operatorName" label="操作人" />
            <el-table-column prop="module" label="模块" />
            <el-table-column prop="action" label="动作" />
            <el-table-column prop="target" label="对象" class-name="audit-wrap-col" />
            <el-table-column prop="detail" label="详情" class-name="audit-wrap-col" />
            <el-table-column prop="ip" label="IP" />
            <el-table-column label="时间">
              <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
            </el-table-column>
          </PageTable>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { Refresh } from '@element-plus/icons-vue';
import { onMounted, reactive, ref } from 'vue';

import { getAdminAuditLogs } from '@/api/admin';
import PageHeader from '@/components/common/PageHeader.vue';
import PageTable from '@/components/common/PageTable.vue';
import type { AuditLog } from '@/types/api';

const modules = ['公告管理', '资料管理', '任务管理', '分组管理', '用户管理'];
const loading = ref(false);
const logs = ref<AuditLog[]>([]);
const total = ref(0);
const query = reactive({ module: '', keyword: '', page: 1, size: 10 });

onMounted(loadLogs);

async function loadLogs() {
  loading.value = true;
  try {
    const result = await getAdminAuditLogs(query);
    logs.value = result.list;
    total.value = result.total;
  } finally {
    loading.value = false;
  }
}

function handleSizeChange() {
  query.page = 1;
  void loadLogs();
}

function formatDateTime(value?: string) {
  if (!value) return '-';
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');

  return `${month}/${day} ${hours}:${minutes}`;
}
</script>

<style scoped>
.audit-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.audit-toolbar,
.toolbar-left {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.toolbar-left {
  flex: 1;
}

.audit-toolbar {
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(126, 114, 97, 0.1);
}

.audit-table-shell {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.audit-table-scroll {
  overflow-x: auto;
  padding-bottom: 2px;
}

.audit-table {
  min-width: 880px;
}

.audit-table :deep(.el-table) {
  width: 100%;
  table-layout: fixed;
}

.audit-table :deep(.el-table__header-wrapper th .cell),
.audit-table :deep(.el-table__body td .cell) {
  padding: 8px 12px;
  text-align: left;
  vertical-align: middle;
}

.audit-table :deep(.el-table__header-wrapper th .cell) {
  min-height: 22px;
}

.audit-table :deep(.audit-wrap-col .cell) {
  white-space: normal;
  overflow-wrap: anywhere;
  word-break: break-word;
  line-height: 1.45;
}

.audit-table :deep(.audit-wrap-col) {
  vertical-align: middle;
}

.audit-table :deep(.el-table__header colgroup col:nth-child(1)),
.audit-table :deep(.el-table__body colgroup col:nth-child(1)) {
  width: 14% !important;
}

.audit-table :deep(.el-table__header colgroup col:nth-child(2)),
.audit-table :deep(.el-table__body colgroup col:nth-child(2)) {
  width: 14% !important;
}

.audit-table :deep(.el-table__header colgroup col:nth-child(3)),
.audit-table :deep(.el-table__body colgroup col:nth-child(3)) {
  width: 14% !important;
}

.audit-table :deep(.el-table__header colgroup col:nth-child(4)),
.audit-table :deep(.el-table__body colgroup col:nth-child(4)) {
  width: 14% !important;
}

.audit-table :deep(.el-table__header colgroup col:nth-child(5)),
.audit-table :deep(.el-table__body colgroup col:nth-child(5)) {
  width: 16% !important;
}

.audit-table :deep(.el-table__header colgroup col:nth-child(6)),
.audit-table :deep(.el-table__body colgroup col:nth-child(6)) {
  width: 14% !important;
}

.audit-table :deep(.el-table__header colgroup col:nth-child(7)),
.audit-table :deep(.el-table__body colgroup col:nth-child(7)) {
  width: 14% !important;
}

.module-select {
  width: 160px;
}

.keyword-input {
  width: 260px;
}
</style>
