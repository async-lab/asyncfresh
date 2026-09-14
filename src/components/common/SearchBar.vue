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
  align-items: center;
}

.search-bar__fields,
.search-bar__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.search-bar__fields {
  flex: 1 1 360px;
  min-width: min(100%, 180px);
}

.search-bar__actions {
  flex: 0 0 auto;
  justify-content: flex-end;
}

.search-bar__fields :deep(.el-input),
.search-bar__fields :deep(.el-select),
.search-bar__fields :deep(.el-cascader),
.search-bar__fields :deep(.el-input-number) {
  width: 200px;
  max-width: 100%;
  flex: 1 1 180px;
  min-width: 160px;
}

.search-bar__fields :deep(.el-input-number) {
  position: relative;
  width: 180px;
  flex: 1 1 168px;
  min-width: 160px;
  overflow: hidden;
  border-radius: 14px;
}

.search-bar__fields :deep(.el-input-number .el-input) {
  width: 100%;
}

.search-bar__actions :deep(.el-button) {
  flex: none;
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
  .search-bar__fields :deep(.el-cascader),
  .search-bar__fields :deep(.el-input-number) {
    width: 100%;
    flex: 1 1 100%;
    min-width: 0;
  }
}
</style>
