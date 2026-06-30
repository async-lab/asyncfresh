<template>
  <div class="page-table">
    <el-table v-loading="loading" :data="data" :empty-text="emptyText" v-bind="$attrs">
      <slot />
    </el-table>
    <div v-if="pagination" class="page-table__pagination">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next"
        :current-page="page"
        :page-size="size"
        :page-sizes="pageSizes"
        :total="total"
        @update:current-page="emit('update:page', $event)"
        @update:page-size="emit('update:size', $event)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
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
</script>

<style scoped>
.page-table {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.page-table__pagination {
  display: flex;
  justify-content: flex-end;
}
</style>
