<template>
  <div class="placeholder-card">
    <el-icon :size="26">
      <Document />
    </el-icon>
    <div>
      <h2>{{ title }}</h2>
      <p>{{ description }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Document } from '@element-plus/icons-vue';

defineProps<{
  title: string;
  description: string;
}>();
</script>

<style scoped>
.placeholder-card {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 20px;
  border: 1px solid var(--app-border);
  border-radius: 16px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(244, 238, 229, 0.86)),
    var(--app-surface-strong);
  box-shadow:
    10px 10px 22px rgba(145, 128, 106, 0.08),
    -8px -8px 18px rgba(255, 255, 255, 0.82);
}

h2 {
  margin: 0 0 6px;
  font-size: 16px;
}

p {
  margin: 0;
  color: var(--app-muted);
}
</style>
