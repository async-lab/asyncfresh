<template>
  <div class="page-table">
    <div class="page-table__scroll">
      <el-table v-loading="loading" :data="data" :empty-text="emptyText" v-bind="$attrs">
        <slot />
      </el-table>
    </div>
    <div v-if="pagination" class="page-table__pagination">
      <el-pagination
        background
        :small="isMobile"
        :layout="paginationLayout"
        :current-page="page"
        :page-size="size"
        :page-sizes="pageSizes"
        :total="total"
        :pager-count="isMobile ? 5 : 7"
        @update:current-page="emit('update:page', $event)"
        @update:page-size="emit('update:size', $event)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

import { useIsMobile } from '@/composables/useMediaQuery';

defineOptions({ inheritAttrs: false });

withDefaults(
  defineProps<{
    data: unknown[];
    loading?: boolean;
    pagination?: boolean;
    page?: number;
    size?: number;
    total?: number;
    pageSizes?: number[];
    emptyText?: string;
  }>(),
  {
    loading: false,
    pagination: false,
    page: 1,
    size: 10,
    total: 0,
    pageSizes: () => [10, 20, 50],
    emptyText: '暂无数据'
  }
);

const emit = defineEmits<{
  'update:page': [value: number];
  'update:size': [value: number];
}>();

const isMobile = useIsMobile();
const paginationLayout = computed(() =>
  isMobile.value ? 'total, prev, next' : 'total, sizes, prev, pager, next'
);
</script>

<style scoped>
.page-table {
  display: flex;
  flex-direction: column;
  gap: 14px;
  max-width: 100%;
  min-width: 0;
}

.page-table__scroll {
  width: 100%;
  max-width: 100%;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  overscroll-behavior-x: contain;
}

.page-table__pagination {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 960px) {
  .page-table__pagination {
    justify-content: center;
  }

  .page-table__pagination :deep(.el-pagination) {
    flex-wrap: wrap;
    justify-content: center;
    row-gap: 8px;
  }
}
</style>
