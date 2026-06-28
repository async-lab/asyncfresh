<template>
  <div class="page">
    <PageHeader :title="announcement?.title || '公告详情'" description="查看公告正文、范围和发布时间。">
      <template #actions>
        <el-button :icon="Back" @click="$router.push('/app/announcements')">返回公告</el-button>
      </template>
    </PageHeader>

    <section class="page-section" v-loading="loading">
      <template v-if="announcement">
        <div class="detail-meta">
          <el-tag effect="plain">{{ getScopeLabel(announcement.scope) }}</el-tag>
          <span>发布者：{{ announcement.publisherName }}</span>
          <span>发布时间：{{ formatDateTime(announcement.createdAt) }}</span>
        </div>
        <MarkdownViewer :content="announcement.content || '暂无公告内容。'" />
      </template>
      <el-empty v-else-if="!loading" description="公告不存在或已不可访问" />
    </section>
  </div>
</template>

<script setup lang="ts">
import { Back } from '@element-plus/icons-vue';
import { onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';

import { getAnnouncement } from '@/api/announcements';
import PageHeader from '@/components/common/PageHeader.vue';
import MarkdownViewer from '@/components/markdown/MarkdownViewer.vue';
import type { Announcement, Scope } from '@/types/api';
import { scopeLabels } from '@/utils/labels';

const route = useRoute();
const loading = ref(false);
const announcement = ref<Announcement | null>(null);

onMounted(loadAnnouncement);

watch(
  () => route.params.id,
  () => {
    void loadAnnouncement();
  }
);

async function loadAnnouncement() {
  loading.value = true;
  try {
    announcement.value = await getAnnouncement(String(route.params.id));
  } catch {
    announcement.value = null;
  } finally {
    loading.value = false;
  }
}

function getScopeLabel(scope?: Scope) {
  return scope ? scopeLabels[scope] : '未知';
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
.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  margin-bottom: 18px;
  color: var(--app-muted);
}
</style>
