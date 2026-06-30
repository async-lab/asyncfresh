<template>
  <el-popconfirm
    :title="title"
    :confirm-button-text="confirmText"
    :cancel-button-text="cancelText"
    :confirm-button-type="confirmType"
    @confirm="emit('confirm')"
    @cancel="emit('cancel')"
  >
    <template #reference>
      <slot />
    </template>
  </el-popconfirm>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    title: string;
    confirmText?: string;
    cancelText?: string;
    confirmType?: 'primary' | 'success' | 'warning' | 'danger' | 'info';
  }>(),
  {
    confirmText: '确认',
    cancelText: '取消',
    confirmType: 'danger'
  }
);

const emit = defineEmits<{
  confirm: [];
  cancel: [];
}>();
</script>
