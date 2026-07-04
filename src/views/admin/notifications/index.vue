<template>
  <div class="page">
    <PageHeader title="通知中心" description="创建系统或邮件通知，面向角色发送招新提醒。" />
    <section class="page-section">
      <div class="page-toolbar notify-toolbar">
        <div class="toolbar-left">
          <el-button type="primary" :icon="Plus" @click="openCreate">新建通知</el-button>
          <el-select v-model="query.status" class="status-select" clearable placeholder="发送状态">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已发送" value="SENT" />
          </el-select>
          <el-input v-model="query.keyword" class="keyword-input" clearable placeholder="搜索通知标题或内容" @keyup.enter="loadNotifications" />
        </div>
        <el-button :icon="Refresh" :loading="loading" @click="loadNotifications">刷新</el-button>
      </div>

      <PageTable :data="notifications" :loading="loading">
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column label="渠道" width="100">
          <template #default="{ row }">{{ row.channel === 'EMAIL' ? '邮件' : '系统' }}</template>
        </el-table-column>
        <el-table-column label="目标角色" width="110">
          <template #default="{ row }">{{ getRoleLabel(row.targetRole) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SENT' ? 'success' : 'info'" effect="plain">{{ row.status === 'SENT' ? '已发送' : '草稿' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" :disabled="row.status === 'SENT'" @click="send(row.id)">发送</el-button>
            <el-popconfirm title="确认删除该通知？" confirm-button-text="删除" cancel-button-text="取消" @confirm="remove(row.id)">
              <template #reference>
                <el-button text type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </PageTable>
    </section>

    <el-dialog v-model="dialogVisible" title="新建通知" width="520px">
      <el-form :model="form" label-width="86px">
        <el-form-item label="标题">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="渠道">
          <el-radio-group v-model="form.channel">
            <el-radio-button label="SYSTEM">系统</el-radio-button>
            <el-radio-button label="EMAIL">邮件</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="目标角色">
          <el-select v-model="form.targetRole" clearable placeholder="全部角色">
            <el-option label="新生" value="FRESHMAN" />
            <el-option label="负责人" value="LEADER" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="5" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="create">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { Plus, Refresh } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { onMounted, reactive, ref } from 'vue';

import { createAdminNotification, deleteAdminNotification, getAdminNotifications, sendAdminNotification } from '@/api/admin';
import PageHeader from '@/components/common/PageHeader.vue';
import PageTable from '@/components/common/PageTable.vue';
import type { NotificationItem, Role } from '@/types/api';
import { roleLabels } from '@/utils/labels';

const loading = ref(false);
const submitting = ref(false);
const dialogVisible = ref(false);
const notifications = ref<NotificationItem[]>([]);
const query = reactive({ status: '' as '' | NotificationItem['status'], keyword: '', page: 1, size: 10 });
const form = reactive({
  title: '',
  content: '',
  channel: 'SYSTEM' as NotificationItem['channel'],
  targetRole: undefined as Role | undefined
});

onMounted(loadNotifications);

async function loadNotifications() {
  loading.value = true;
  try {
    notifications.value = (await getAdminNotifications({ ...query, status: query.status || undefined })).list;
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  form.title = '';
  form.content = '';
  form.channel = 'SYSTEM';
  form.targetRole = undefined;
  dialogVisible.value = true;
}

async function create() {
  submitting.value = true;
  try {
    await createAdminNotification(form);
    ElMessage.success('通知已保存');
    dialogVisible.value = false;
    await loadNotifications();
  } finally {
    submitting.value = false;
  }
}

async function send(id: number) {
  await sendAdminNotification(id);
  ElMessage.success('通知已发送');
  await loadNotifications();
}

async function remove(id: number) {
  await deleteAdminNotification(id);
  ElMessage.success('通知已删除');
  await loadNotifications();
}

function getRoleLabel(role?: Role) {
  return role ? roleLabels[role] : '全部';
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

.status-select {
  width: 130px;
}

.keyword-input {
  width: 260px;
}
</style>
