<template>
  <div class="page">
    <PageHeader title="学习资料" description="按方向筛选学习资料并下载附件。" />

    <section class="page-section">
      <div class="page-toolbar list-toolbar">
        <DirectionCascader v-model="directionPath" @change="handleDirectionChange" />
        <el-input v-model="query.keyword" class="keyword-input" clearable placeholder="搜索资料标题或内容" @keyup.enter="loadMaterials" />
        <el-checkbox v-model="query.hasAttachment" :true-label="true" :false-label="false" @change="handleAttachmentChange">
          仅看附件
        </el-checkbox>
        <el-button :icon="Refresh" :loading="loading" @click="loadMaterials">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="pagedMaterials" empty-text="暂无资料">
        <el-table-column prop="title" label="标题" min-width="220" />
        <el-table-column prop="summary" label="摘要" min-width="220">
          <template #default="{ row }">
            <span>{{ row.summary || '暂无摘要' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="方向" min-width="150">
          <template #default="{ row }">{{ getDirectionLabel(row) }}</template>
        </el-table-column>
        <el-table-column label="附件" width="110">
          <template #default="{ row }">
            <AttachmentLink :href="getAttachmentHref(row)" label="下载" />
          </template>
        </el-table-column>
        <el-table-column label="发布时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" :icon="View" @click="$router.push(`/app/materials/${row.id}`)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        class="pager"
        layout="total, sizes, prev, pager, next"
        :page-sizes="[5, 10, 20]"
        :total="filteredMaterials.length"
        @current-change="loadMaterials"
        @size-change="handleSizeChange"
      />
    </section>
  </div>
</template>

<script setup lang="ts">
import { Refresh, View } from '@element-plus/icons-vue';
import { computed, onMounted, reactive, ref } from 'vue';

import { getMaterials } from '@/api/materials';
import AttachmentLink from '@/components/common/AttachmentLink.vue';
import PageHeader from '@/components/common/PageHeader.vue';
import DirectionCascader from '@/components/forms/DirectionCascader.vue';
import { useMetaStore } from '@/stores/meta';
import type { Material } from '@/types/api';

const metaStore = useMetaStore();
const loading = ref(false);
const materials = ref<Material[]>([]);
const directionPath = ref<number[]>([]);
const query = reactive({
  page: 1,
  size: 10,
  directionLevel1Id: undefined as number | undefined,
  directionLevel2Id: undefined as number | undefined,
  keyword: '',
  hasAttachment: false
});
const directionNameMap = computed(() => {
  const map = new Map<number, string>();
  metaStore.directions.forEach((level1) => {
    map.set(level1.id, level1.name);
    level1.children?.forEach((level2) => map.set(level2.id, level2.name));
  });
  return map;
});
const filteredMaterials = computed(() => {
  const keyword = query.keyword.trim();
  return materials.value.filter((item) => {
    const level1Matched = !query.directionLevel1Id || item.directionLevel1Id === query.directionLevel1Id;
    const level2Matched = !query.directionLevel2Id || item.directionLevel2Id === query.directionLevel2Id;
    const attachmentMatched = !query.hasAttachment || hasAttachment(item);
    const keywordMatched =
      !keyword ||
      [item.title, item.summary, item.contentMarkdown, item.content].filter(Boolean).some((value) => String(value).includes(keyword));
    return level1Matched && level2Matched && attachmentMatched && keywordMatched;
  });
});
const pagedMaterials = computed(() => {
  const start = (query.page - 1) * query.size;
  return filteredMaterials.value.slice(start, start + query.size);
});

onMounted(loadMaterials);

async function loadMaterials() {
  loading.value = true;
  try {
    materials.value = await getMaterials();
  } finally {
    loading.value = false;
  }
}

function handleDirectionChange(level1Id?: number, level2Id?: number) {
  query.directionLevel1Id = level1Id;
  query.directionLevel2Id = level2Id;
  query.page = 1;
  void loadMaterials();
}

function handleAttachmentChange() {
  query.page = 1;
  void loadMaterials();
}

function handleSizeChange() {
  query.page = 1;
  void loadMaterials();
}

function getDirectionLabel(material: Material) {
  const level1 = material.directionLevel1Id ? directionNameMap.value.get(material.directionLevel1Id) : undefined;
  const level2 = material.directionLevel2Id ? directionNameMap.value.get(material.directionLevel2Id) : undefined;
  return [level1, level2].filter(Boolean).join(' / ') || '全部方向';
}

function hasAttachment(material: Material) {
  return Boolean(material.attachment || material.hasAttachment || material.attachmentFileId || material.attachmentUrl);
}

function getAttachmentHref(material: Material) {
  if (!hasAttachment(material)) return null;
  return material.attachmentUrl || `/api/v1/materials/${material.id}/attachment`;
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
.list-toolbar {
  margin-bottom: 14px;
}

.list-toolbar :deep(.el-cascader) {
  width: 220px;
}

.keyword-input {
  max-width: 260px;
}

.pager {
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 760px) {
  .list-toolbar :deep(.el-cascader),
  .keyword-input {
    width: 100%;
    max-width: none;
  }
}
</style>
