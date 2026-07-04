<template>
  <div class="page">
    <PageHeader :title="material?.title || '资料详情'" description="查看资料正文、方向和附件。">
      <template #actions>
        <el-button :icon="Back" @click="$router.push('/app/materials')">返回资料</el-button>
      </template>
    </PageHeader>

    <section class="page-section" v-loading="loading">
      <template v-if="material">
        <div class="detail-meta">
          <el-tag effect="plain">{{ directionLabel }}</el-tag>
          <span>发布时间：{{ formatDateTime(material.createdAt) }}</span>
          <AttachmentLink :href="attachmentHref" />
        </div>
        <p v-if="material.summary" class="summary">{{ material.summary }}</p>
        <MarkdownViewer :content="material.contentMarkdown || material.content || '暂无资料正文。'" />
      </template>
      <el-empty v-else-if="!loading" description="资料不存在或已不可访问" />
    </section>
  </div>
</template>

<script setup lang="ts">
import { Back } from '@element-plus/icons-vue';
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';

import { getMaterial } from '@/api/materials';
import AttachmentLink from '@/components/common/AttachmentLink.vue';
import PageHeader from '@/components/common/PageHeader.vue';
import MarkdownViewer from '@/components/markdown/MarkdownViewer.vue';
import { useMetaStore } from '@/stores/meta';
import type { Material } from '@/types/api';

const route = useRoute();
const metaStore = useMetaStore();
const loading = ref(false);
const material = ref<Material | null>(null);
const directionNameMap = computed(() => {
  const map = new Map<number, string>();
  metaStore.directions.forEach((level1) => {
    map.set(level1.id, level1.name);
    level1.children?.forEach((level2) => map.set(level2.id, level2.name));
  });
  return map;
});
const directionLabel = computed(() => {
  if (!material.value) return '全部方向';
  const level1 = material.value.directionLevel1Id ? directionNameMap.value.get(material.value.directionLevel1Id) : undefined;
  const level2 = material.value.directionLevel2Id ? directionNameMap.value.get(material.value.directionLevel2Id) : undefined;
  return [level1, level2].filter(Boolean).join(' / ') || '全部方向';
});
const attachmentHref = computed(() => {
  if (!material.value) return null;
  if (!(material.value.attachment || material.value.hasAttachment || material.value.attachmentFileId || material.value.attachmentUrl)) {
    return null;
  }
  return material.value.attachmentUrl || `/api/v1/materials/${material.value.id}/attachment`;
});

onMounted(loadMaterial);

watch(
  () => route.params.id,
  () => {
    void loadMaterial();
  }
);

async function loadMaterial() {
  loading.value = true;
  try {
    material.value = await getMaterial(String(route.params.id));
  } catch {
    material.value = null;
  } finally {
    loading.value = false;
  }
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

.summary {
  margin: 0 0 18px;
  padding: 12px 14px;
  border-radius: 8px;
  background: #f8fafc;
  color: var(--app-muted);
  line-height: 1.7;
}
</style>
