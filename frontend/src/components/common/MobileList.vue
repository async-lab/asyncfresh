<template>
  <div v-loading="loading" class="mobile-list">
    <el-empty v-if="!loading && !data.length" :description="emptyText" />
    <template v-else>
      <div
        v-for="(item, index) in data"
        :key="keyField ? String((item as Record<string, unknown>)[keyField]) : index"
        class="mobile-list__card"
      >
        <slot name="item" :item="item" :index="index" />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts" generic="T">
withDefaults(
  defineProps<{
    data: T[];
    loading?: boolean;
    emptyText?: string;
    keyField?: string;
  }>(),
  {
    loading: false,
    emptyText: '暂无数据',
    keyField: undefined
  }
);
</script>

<style scoped>
.mobile-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

.mobile-list__card {
  min-width: 0;
  padding: 14px 16px;
  border: 1px solid rgba(126, 114, 97, 0.12);
  border-radius: 14px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.9), rgba(247, 241, 232, 0.86));
  box-shadow: 6px 8px 16px rgba(145, 128, 106, 0.08), -6px -6px 14px rgba(255, 255, 255, 0.7);
}
</style>
