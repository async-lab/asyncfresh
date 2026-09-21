<template>
  <div class="page">
    <PageHeader :title="title" :description="description" />
    <PlaceholderCard title="页面待实现" :description="implementationHint" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';

import PageHeader from '@/components/common/PageHeader.vue';
import PlaceholderCard from '@/components/common/PlaceholderCard.vue';

const route = useRoute();
const title = computed(() => route.meta.title || '页面');
const description = computed(() => route.meta.description || '该页面已经接入路由，等待补充业务实现。');
const implementationHint = computed(() => `建议在 ${route.path} 对应视图中补充表格、表单、接口和状态处理。`);
</script>
