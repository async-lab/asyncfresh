<template>
  <router-view />
  <NotificationAlertDialog />
</template>

<script setup lang="ts">
import { onBeforeUnmount, watch } from 'vue';

import { NotificationAlertDialog } from '@/components/common';
import { useAuthStore } from '@/stores/auth';
import { useNotificationStore } from '@/stores/notifications';

const authStore = useAuthStore();
const notificationStore = useNotificationStore();

// 登录后开始轮询站内通知，管理员或组长发布任务时新通知会以弹窗形式提醒组员
watch(
  () => authStore.user?.id ?? null,
  (userId) => {
    if (userId == null) {
      notificationStore.resetState();
      return;
    }

    notificationStore.start(userId);
  },
  { immediate: true }
);

onBeforeUnmount(() => {
  notificationStore.resetState();
});
</script>
