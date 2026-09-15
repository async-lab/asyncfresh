<template>
  <el-badge :value="notificationStore.unreadCount" :max="99" :hidden="!notificationStore.unreadCount">
    <button class="bell-button" type="button" aria-label="通知中心" @click="openPanel">
      <el-icon :size="18"><Bell /></el-icon>
    </button>
  </el-badge>

  <el-drawer v-model="visible" :size="drawerSize" append-to-body :with-header="true">
    <template #header>
      <div class="panel-header">
        <span class="panel-title">通知中心</span>
        <div class="panel-actions">
          <el-button text :disabled="!notificationStore.unreadCount" @click="handleReadAll">全部已读</el-button>
          <el-button text :icon="Refresh" :loading="notificationStore.loading" @click="load">刷新</el-button>
        </div>
      </div>
    </template>

    <div v-loading="notificationStore.loading" class="panel-body">
      <el-empty v-if="!notificationStore.items.length" description="暂无通知" :image-size="72" />
      <ul v-else class="notification-list">
        <li
          v-for="item in notificationStore.items"
          :key="item.id"
          class="notification-item"
          :class="{ 'is-unread': !item.readAt }"
        >
          <button class="item-main" type="button" @click="handleOpen(item)">
            <div class="item-head">
              <el-tag :type="getNotificationTagType(item.type)" effect="plain" size="small">
                {{ getTypeLabel(item.type) }}
              </el-tag>
              <span v-if="!item.readAt" class="unread-dot" />
              <span class="item-time">{{ formatDateTime(item.createdAt) }}</span>
            </div>
            <p class="item-title">{{ item.title }}</p>
            <p class="item-content">{{ item.content }}</p>
          </button>
        </li>
      </ul>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { Bell, Refresh } from '@element-plus/icons-vue';
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';

import { useIsMobile } from '@/composables/useMediaQuery';
import { useAuthStore } from '@/stores/auth';
import { useNotificationStore } from '@/stores/notifications';
import type { NotificationItem, NotificationType } from '@/types/api';
import { formatDateTime } from '@/utils/format';
import { notificationTypeLabels } from '@/utils/labels';
import { getNotificationTagType, resolveNotificationPath } from '@/utils/notification';

const router = useRouter();
const authStore = useAuthStore();
const notificationStore = useNotificationStore();
const isMobile = useIsMobile();

const visible = ref(false);

const drawerSize = computed(() => (isMobile.value ? '88vw' : 'min(420px, 36vw)'));

async function openPanel() {
  visible.value = true;
  await load();
}

async function load() {
  await notificationStore.refresh();
}

async function handleOpen(item: NotificationItem) {
  if (!item.readAt) {
    await notificationStore.read(item.id).catch(() => undefined);
  }

  const path = resolveNotificationPath(item, authStore.role);

  if (path) {
    visible.value = false;
    await router.push(path);
  }
}

async function handleReadAll() {
  await notificationStore.readAll();
}

function getTypeLabel(type?: NotificationType) {
  return type ? notificationTypeLabels[type] : '系统通知';
}
</script>

<script lang="ts">
export default { name: 'NotificationBell' };
</script>

<style scoped>
.bell-button {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  padding: 0;
  border: 1px solid rgba(126, 114, 97, 0.14);
  border-radius: 13px;
  background: linear-gradient(180deg, #fbf8f3, #f2ece2);
  color: var(--app-text);
  box-shadow: var(--app-shadow-press);
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.bell-button:focus-visible {
  outline: 2px solid rgba(165, 155, 212, 0.7);
  outline-offset: 2px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
}

.panel-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--app-text);
}

.panel-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.panel-body {
  min-height: 200px;
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.notification-item {
  border: 1px solid rgba(126, 114, 97, 0.12);
  border-radius: 14px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.9), rgba(247, 242, 234, 0.86));
}

.notification-item.is-unread {
  border-color: rgba(165, 155, 212, 0.42);
  box-shadow: inset 3px 0 0 rgba(165, 155, 212, 0.7);
}

.item-main {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
  padding: 12px 14px;
  border: 0;
  border-radius: 14px;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.item-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.unread-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #f08a5d;
}

.item-time {
  margin-left: auto;
  font-size: 12px;
  color: var(--app-muted);
}

.item-title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--app-text);
}

.item-content {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--app-muted);
  word-break: break-word;
}
</style>
