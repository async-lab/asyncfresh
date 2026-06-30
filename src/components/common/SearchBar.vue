<template>
  <div class="search-bar">
    <div class="search-bar__fields">
      <slot />
    </div>
    <div class="search-bar__actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<style scoped>
.search-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-end;
  justify-content: space-between;
}

.search-bar__fields,
.search-bar__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.search-bar__fields :deep(.el-input),
.search-bar__fields :deep(.el-select),
.search-bar__fields :deep(.el-cascader) {
  width: 200px;
}
</style>
