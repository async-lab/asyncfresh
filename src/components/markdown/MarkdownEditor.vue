<template>
  <div class="markdown-editor">
    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane label="编辑" name="edit">
        <el-input
          :model-value="modelValue"
          type="textarea"
          :rows="rows"
          :maxlength="maxlength"
          :show-word-limit="Boolean(maxlength)"
          :placeholder="placeholder"
          :disabled="disabled"
          @update:model-value="emit('update:modelValue', $event)"
        />
      </el-tab-pane>
      <el-tab-pane label="预览" name="preview">
        <div class="markdown-editor__preview">
          <MarkdownViewer v-if="modelValue" :content="modelValue" />
          <el-empty v-else description="暂无预览内容" :image-size="80" />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';

import MarkdownViewer from './MarkdownViewer.vue';

withDefaults(
  defineProps<{
    modelValue?: string;
    placeholder?: string;
    rows?: number;
    maxlength?: number;
    disabled?: boolean;
  }>(),
  {
    modelValue: '',
    placeholder: '请输入 Markdown 内容',
    rows: 8,
    maxlength: undefined
  }
);

const emit = defineEmits<{
  'update:modelValue': [value: string];
}>();

const activeTab = ref('edit');
</script>

<style scoped>
.markdown-editor {
  width: 100%;
}

.markdown-editor__preview {
  min-height: 180px;
}
</style>
