<template>
  <el-cascader
    :model-value="modelValue"
    :options="options"
    :props="{ value: 'id', label: 'name', children: 'children', emitPath: true }"
    :disabled="disabled"
    clearable
    filterable
    placeholder="请选择方向"
    @update:model-value="handleChange"
  />
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue';

import { useMetaStore } from '@/stores/meta';

defineProps<{
  modelValue?: number[];
  disabled?: boolean;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: number[]];
  change: [level1Id?: number, level2Id?: number];
}>();

const metaStore = useMetaStore();
const options = computed(() => metaStore.directions);

onMounted(() => {
  void metaStore.fetchDirections();
});

function handleChange(value: unknown) {
  const path = Array.isArray(value) ? (value as number[]) : [];
  emit('update:modelValue', path);
  emit('change', path[0], path[1]);
}
</script>
