<template>
  <div class="page">
    <PageHeader title="审计日志" description="查看管理员和负责人关键业务操作记录。" />
    <section class="page-section">
      <div class="page-toolbar audit-toolbar">
        <div class="toolbar-left">
          <el-select v-model="query.module" class="module-select" clearable placeholder="业务模块">
            <el-option v-for="module in modules" :key="module" :label="module" :value="module" />
          </el-select>
          <el-input v-model="query.keyword" class="keyword-input" clearable placeholder="搜索操作人、对象或详情" @keyup.enter="loadLogs" />
        </div>
        <el-button :icon="Refresh" :loading="loading" @click="loadLogs">刷新</el-button>
      </div>
      <PageTable
        v-model:page="query.page"
        v-model:size="query.size"
        :data="logs"
        :loading="loading"
        :total="total"
        pagination
        @update:page="loadLogs"
        @update:size="handleSizeChange"
      >
        <el-table-column prop="operatorName" label="操作人" width="120" />
        <el-table-column prop="module" label="模块" width="130" />
        <el-table-column prop="action" label="动作" width="130" />
        <el-table-column prop="target" label="对象" min-width="180" show-overflow-tooltip />
        <el-table-column prop="detail" label="详情" min-width="220" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="130" />
        <el-table-column label="时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
      </PageTable>
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
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN', { hour12: false });
}
</script>

<style scoped>
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

.module-select {
  width: 160px;
}

.keyword-input {
  width: 260px;
}
</style>
