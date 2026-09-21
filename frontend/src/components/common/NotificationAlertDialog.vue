<template>
  <el-dialog
    :model-value="Boolean(notificationStore.current)"
    :title="`${typeLabel}提醒`"
    width="min(440px, 92vw)"
    align-center
    append-to-body
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
    class="notification-alert"
    @update:model-value="handleLater"
  >
    <div v-if="current" class="alert-body">
      <div class="alert-meta">
        <el-tag :type="getNotificationTagType(current.type)" effect="dark" size="small">{{ typeLabel }}</el-tag>
        <span class="alert-time">{{ formatDateTime(current.createdAt) }}</span>
      </div>
      <h4 class="alert-title">{{ current.title }}</h4>
      <p class="alert-content">{{ current.content }}</p>
    </div>

    <template #footer>
      <div class="alert-footer">
        <el-button @click="handleLater">稍后处理</el-button>
        <el-button v-if="detailPath" type="primary" @click="handleView">立即查看</el-button>
        <el-button v-else type="primary" @click="handleKnown">知道了</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';

import { useAuthStore } from '@/stores/auth';
import { useNotificationStore } from '@/stores/notifications';
import { formatDateTime } from '@/utils/format';
import { notificationTypeLabels } from '@/utils/labels';
import { getNotificationTagType, resolveNotificationPath } from '@/utils/notification';

const router = useRouter();
const authStore = useAuthStore();
const notificationStore = useNotificationStore();

const current = computed(() => notificationStore.current);

const typeLabel = computed(() =>
  current.value?.type ? notificationTypeLabels[current.value.type] : '系统通知'
);

const detailPath = computed(() =>
  current.value ? resolveNotificationPath(current.value, authStore.role) : null
);

function handleLater() {
  void notificationStore.acknowledgeCurrent(false);
}

function handleKnown() {
  void notificationStore.acknowledgeCurrent(true);
}

async function handleView() {
  const path = detailPath.value;
  await notificationStore.acknowledgeCurrent(true);

  if (path) {
    await router.push(path);
  }
}
</script>

<script lang="ts">
export default { name: 'NotificationAlertDialog' };
</script>

<style scoped>
.alert-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.alert-meta {
  display: flex;
  align-items: center;
  gap: 10px;
}

.alert-time {
  font-size: 12px;
  color: var(--app-muted);
}

.alert-title {
  margin: 0;
  font-size: 17px;
  color: var(--app-text);
}

.alert-content {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: var(--app-text-soft, var(--app-muted));
  white-space: pre-wrap;
  word-break: break-word;
}

.alert-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
