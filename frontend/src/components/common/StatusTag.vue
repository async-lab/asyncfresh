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
type StatusKind = 'application' | 'submission';

const props = withDefaults(
  defineProps<{
    value?: StatusValue;
    label?: string;
    kind?: StatusKind;
  }>(),
  { kind: 'application' }
);

const displayLabel = computed(() => {
  if (props.label) return props.label;
  if (!props.value) return '未知';
  const labels = props.kind === 'submission' ? displaySubmissionStatusLabels : applicationStatusLabels;
  return labels[props.value as keyof typeof labels] || props.value;
});

const tagType = computed(() => {
  if (props.kind === 'submission') {
    if (props.value === 'REVIEWED') return 'success';
    if (props.value === 'SUBMITTED') return 'warning';
    return 'info';
  }
  if (props.value === 'GROUPED') return 'success';
  if (props.value === 'SUBMITTED') return 'warning';
  if (props.value === 'WITHDRAWN' || props.value === 'EXPIRED') return 'info';
  if (props.value === 'REJECTED') return 'danger';
  return 'info';
});
</script>
