import { defineStore } from 'pinia';

import {
  getNotifications,
  getNotificationSummary,
  markAllNotificationsRead,
  markNotificationRead
} from '@/api/notifications';
import type { NotificationItem } from '@/types/api';

const POLL_INTERVAL_MS = 20000;
const LIST_SIZE = 20;
const STORAGE_KEY_PREFIX = 'await-you:notification:last-seen:';

interface NotificationState {
  userId: number | null;
  items: NotificationItem[];
  unreadCount: number;
  queue: NotificationItem[];
  current: NotificationItem | null;
  lastSeenId: number;
  baselineReady: boolean;
  loading: boolean;
  timer: ReturnType<typeof setInterval> | null;
}

function readLastSeenId(userId: number) {
  try {
    const parsed = Number(window.localStorage.getItem(`${STORAGE_KEY_PREFIX}${userId}`));
    return Number.isFinite(parsed) && parsed > 0 ? parsed : 0;
  } catch {
    return 0;
  }
}

function writeLastSeenId(userId: number, lastSeenId: number) {
  try {
    window.localStorage.setItem(`${STORAGE_KEY_PREFIX}${userId}`, String(lastSeenId));
  } catch {
    // 隐私模式下 localStorage 不可用时忽略，退化为内存记录
  }
}

export const useNotificationStore = defineStore('notification', {
  state: (): NotificationState => ({
    userId: null,
    items: [],
    unreadCount: 0,
    queue: [],
    current: null,
    lastSeenId: 0,
    baselineReady: false,
    loading: false,
    timer: null
  }),

  getters: {
    hasUnread: (state) => state.unreadCount > 0,
    unreadItems: (state) => state.items.filter((item) => !item.readAt)
  },

  actions: {
    /** 绑定当前登录用户并开始轮询，重复调用同一用户时不会重复建立定时器 */
    start(userId: number) {
      if (this.userId !== userId) {
        this.resetState();
        this.userId = userId;
        this.lastSeenId = readLastSeenId(userId);
      }

      if (this.timer != null) {
        return;
      }

      void this.refresh();
      this.timer = setInterval(() => {
        if (typeof document !== 'undefined' && document.hidden) {
          return;
        }

        void this.refresh();
      }, POLL_INTERVAL_MS);
    },

    stop() {
      if (this.timer != null) {
        clearInterval(this.timer);
        this.timer = null;
      }
    },

    resetState() {
      this.stop();
      this.userId = null;
      this.items = [];
      this.unreadCount = 0;
      this.queue = [];
      this.current = null;
      this.lastSeenId = 0;
      this.baselineReady = false;
    },

    async refresh() {
      if (this.userId == null || this.loading) {
        return;
      }

      this.loading = true;

      try {
        const [page, summary] = await Promise.all([
          getNotifications({ page: 1, size: LIST_SIZE }),
          getNotificationSummary()
        ]);

        this.items = page.list;
        this.unreadCount = summary.unreadCount;
        this.enqueueNew(page.list);
      } catch {
        // 轮询失败静默处理，避免打断用户操作
      } finally {
        this.loading = false;
      }
    },

    /** 将本轮新出现的未读通知放入弹窗队列 */
    enqueueNew(list: NotificationItem[]) {
      const maxId = list.reduce((max, item) => Math.max(max, item.id), 0);

      // 首次拉取只建立基线，避免历史通知在登录后集中弹出
      if (!this.baselineReady) {
        this.baselineReady = true;
        this.lastSeenId = maxId;
        this.persistLastSeen();
        return;
      }

      const fresh = list
        .filter((item) => item.id > this.lastSeenId && !item.readAt)
        .sort((a, b) => a.id - b.id);

      if (maxId > this.lastSeenId) {
        this.lastSeenId = maxId;
        this.persistLastSeen();
      }

      for (const item of fresh) {
        if (this.current?.id === item.id || this.queue.some((queued) => queued.id === item.id)) {
          continue;
        }

        this.queue.push(item);
      }

      this.showNext();
    },

    showNext() {
      if (this.current == null && this.queue.length > 0) {
        this.current = this.queue.shift() ?? null;
      }
    },

    /** 关闭当前弹窗；markRead=true 时同时标记为已读 */
    async acknowledgeCurrent(markRead = true) {
      const item = this.current;
      this.current = null;

      if (item && markRead) {
        await this.read(item.id).catch(() => undefined);
      }

      this.showNext();
    },

    async read(id: number) {
      await markNotificationRead(id);

      const target = this.items.find((item) => item.id === id);

      if (target && !target.readAt) {
        target.readAt = new Date().toISOString();
        this.unreadCount = Math.max(0, this.unreadCount - 1);
      }
    },

    async readAll() {
      await markAllNotificationsRead();
      this.items = this.items.map((item) =>
        item.readAt ? item : { ...item, readAt: new Date().toISOString() }
      );
      this.unreadCount = 0;
    },

    persistLastSeen() {
      if (this.userId != null) {
        writeLastSeenId(this.userId, this.lastSeenId);
      }
    }
  }
});
