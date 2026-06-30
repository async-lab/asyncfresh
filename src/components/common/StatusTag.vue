<template>
  <el-tag :type="tagType" effect="light">
    {{ displayLabel }}
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue';

import type { ApplicationStatus, DisplaySubmissionStatus } from '@/types/api';
import { applicationStatusLabels, displaySubmissionStatusLabels } from '@/utils/labels';

type StatusValue = ApplicationStatus | DisplaySubmissionStatus;

const props = defineProps<{
  value?: StatusValue;
  label?: string;
}>();

const displayLabel = computed(() => {
  if (props.label) return props.label;
  if (!props.value) return '未知';
  return (
    applicationStatusLabels[props.value as keyof typeof applicationStatusLabels] ||
    displaySubmissionStatusLabels[props.value as keyof typeof displaySubmissionStatusLabels] ||
    props.value
  );
});

const tagType = computed(() => {
  if (props.value === 'GROUPED' || props.value === 'REVIEWED') return 'success';
  if (props.value === 'SUBMITTED') return 'warning';
  if (props.value === 'WITHDRAWN' || props.value === 'EXPIRED') return 'info';
  if (props.value === 'REJECTED') return 'danger';
  return 'info';
});
</script>
