<template>
  <div class="page">
    <PageHeader title="公告" description="查看全局公告和本人所在分组可见公告。" />

    <section class="page-section">
      <div class="page-toolbar list-toolbar">
        <el-segmented v-model="query.scope" :options="scopeOptions" />
        <el-input v-model="query.keyword" class="keyword-input" clearable placeholder="搜索公告标题或内容" @keyup.enter="loadAnnouncements" />
        <el-button :icon="Refresh" :loading="loading" @click="loadAnnouncements">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="pageData.list" empty-text="暂无公告">
        <el-table-column prop="title" label="标题" min-width="240" />
        <el-table-column label="范围" width="100">
          <template #default="{ row }">
            <el-tag effect="plain">{{ getScopeLabel(row.scope) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publisherName" label="发布者" width="120" />
        <el-table-column label="发布时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" :icon="View" @click="$router.push(`/app/announcements/${row.id}`)">
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
        :total="pageData.total"
        @current-change="loadAnnouncements"
        @size-change="handleSizeChange"
      />
    </section>
  </div>
</template>

<script setup lang="ts">
import { Refresh, View } from '@element-plus/icons-vue';
import { onMounted, reactive, ref } from 'vue';

import { getAnnouncements } from '@/api/announcements';
import PageHeader from '@/components/common/PageHeader.vue';
import type { Announcement, PageResult, Scope } from '@/types/api';
import { scopeLabels } from '@/utils/labels';

type ScopeFilter = 'ALL' | Scope;

const loading = ref(false);
const query = reactive({
  page: 1,
  size: 10,
  scope: 'ALL' as ScopeFilter,
  keyword: ''
});
const pageData = reactive<PageResult<Announcement>>({
  list: [],
  page: 1,
  size: 10,
  total: 0,
  totalPages: 1
});
const scopeOptions = [
  { label: '全部', value: 'ALL' },
  { label: scopeLabels.GLOBAL, value: 'GLOBAL' },
  { label: scopeLabels.GROUP, value: 'GROUP' }
];

onMounted(loadAnnouncements);

async function loadAnnouncements() {
  loading.value = true;
  try {
    const data = await getAnnouncements({
      page: query.page,
      size: query.size,
      scope: query.scope === 'ALL' ? undefined : query.scope,
      keyword: query.keyword.trim() || undefined
    });
    Object.assign(pageData, data);
  } finally {
    loading.value = false;
  }
}

function handleSizeChange() {
  query.page = 1;
  void loadAnnouncements();
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
.list-toolbar {
  margin-bottom: 14px;
}

.keyword-input {
  max-width: 260px;
}

.pager {
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
