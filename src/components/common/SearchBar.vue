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
  min-width: 0;
}

.search-bar__fields {
  flex: 1 1 420px;
}

.search-bar__actions {
  flex: 0 1 auto;
  justify-content: flex-end;
}

.search-bar__fields :deep(.el-input),
.search-bar__fields :deep(.el-select),
.search-bar__fields :deep(.el-cascader) {
  width: 200px;
  flex: 1 1 200px;
  min-width: 180px;
}

@media (max-width: 960px) {
  .search-bar {
    align-items: stretch;
  }

  .search-bar__fields,
  .search-bar__actions {
    width: 100%;
  }

  .search-bar__actions {
    justify-content: flex-start;
  }

  .search-bar__fields :deep(.el-input),
  .search-bar__fields :deep(.el-select),
  .search-bar__fields :deep(.el-cascader) {
    width: 100%;
    flex: 1 1 100%;
    min-width: 0;
  }
}
</style>
