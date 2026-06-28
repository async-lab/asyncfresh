<template>
  <el-link v-if="url" type="primary" :href="url" target="_blank" :icon="Download">
    {{ label }}
  </el-link>
  <span v-else class="muted">无附件</span>
</template>

<script setup lang="ts">
import { Download } from '@element-plus/icons-vue';

withDefaults(
  defineProps<{
    url?: string | null;
    label?: string;
  }>(),
  {
    label: '下载附件'
  }
);
</script>
