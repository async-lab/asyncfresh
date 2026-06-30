<template>
  <el-link v-if="href || url" type="primary" :href="href || url || undefined" target="_blank" :icon="Download">
    {{ label }}
  </el-link>
  <span v-else class="muted">无附件</span>
</template>

<script setup lang="ts">
import { Download } from '@element-plus/icons-vue';

withDefaults(
  defineProps<{
    url?: string | null;
    href?: string | null;
    label?: string;
  }>(),
  {
    label: '下载附件'
  }
);
</script>
