<template>
  <div class="page">
    <PageHeader title="通知中心" description="查看当前账号收到的系统通知并处理未读状态。" />

    <section class="page-section">
      <div class="page-toolbar notify-toolbar">
        <div class="toolbar-left">
          <el-checkbox v-model="query.unreadOnly" @change="loadNotifications">只看未读</el-checkbox>
          <el-button :disabled="!notifications.some((item) => !item.readAt)" @click="markAllRead">全部已读</el-button>
        </div>
        <el-button :icon="Refresh" :loading="loading" @click="loadNotifications">刷新</el-button>
      </div>

      <PageTable :data="notifications" :loading="loading">
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="content" label="内容" min-width="260" show-overflow-tooltip />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">{{ getTypeLabel(row.type) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.readAt ? 'info' : 'success'" effect="plain">
              {{ row.readAt ? '已读' : '未读' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" :disabled="Boolean(row.readAt)" @click="markRead(row.id)">标为已读</el-button>
          </template>
        </el-table-column>
      </PageTable>
    </section>
  </div>
</template>

<script setup lang="ts">
import { Refresh } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { onMounted, reactive, ref } from 'vue';

import { getCurrentNotifications, markAllNotificationsRead, markNotificationRead } from '@/api/admin';
import PageHeader from '@/components/common/PageHeader.vue';
import PageTable from '@/components/common/PageTable.vue';
import type { NotificationItem } from '@/types/api';

const loading = ref(false);
const notifications = ref<NotificationItem[]>([]);
const query = reactive({ unreadOnly: false, page: 1, size: 10 });

onMounted(loadNotifications);

async function loadNotifications() {
  loading.value = true;
  try {
    notifications.value = (await getCurrentNotifications(query)).list;
  } finally {
    loading.value = false;
  }
}

async function markRead(id: number) {
  await markNotificationRead(id);
  ElMessage.success('通知已标为已读');
  await loadNotifications();
}

async function markAllRead() {
  await markAllNotificationsRead();
  ElMessage.success('全部通知已标为已读');
  await loadNotifications();
}

function getTypeLabel(type?: string) {
  const labels: Record<string, string> = {
    SYSTEM: '系统',
    APPLICATION: '报名',
    GROUP: '分组',
    TASK: '任务',
    ANNOUNCEMENT: '公告'
  };
  return type ? labels[type] || type : '-';
}

function formatDateTime(value?: string) {
  if (!value) return '-';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN', { hour12: false });
}
</script>

<style scoped>
.notify-toolbar,
.toolbar-left {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.toolbar-left {
  flex: 1;
}
</style>
