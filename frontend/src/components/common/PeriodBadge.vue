<template>
  <el-tag :type="tagType" effect="light">
    {{ label }}
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue';

import type { PeriodType } from '@/types/api';
import { periodLabels } from '@/utils/labels';

const props = defineProps<{
  period?: PeriodType;
}>();

const label = computed(() => (props.period ? periodLabels[props.period] : '时期未知'));
const tagType = computed(() => {
  if (props.period === 'REGISTRATION') return 'success';
  if (props.period === 'SELECTION') return 'warning';
  if (props.period === 'INTERVIEW') return 'primary';
  if (props.period === 'FINISHED') return 'info';
  return 'info';
});
</script>
