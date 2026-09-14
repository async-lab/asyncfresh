<template>
  <div class="markdown-editor">
    <div class="markdown-editor__toolbar">
      <el-button :disabled="disabled" @click="openMarkdownFile">导入 Markdown 文件</el-button>
      <input
        ref="fileInput"
        class="markdown-editor__file-input"
        type="file"
        accept=".md,.markdown,.txt"
        @change="handleMarkdownFileChange"
      />
    </div>
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
import { ElMessage } from 'element-plus';
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
const fileInput = ref<HTMLInputElement | null>(null);

function openMarkdownFile() {
  fileInput.value?.click();
}

async function handleMarkdownFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!file) {
    return;
  }

  const extension = file.name.split('.').pop()?.toLowerCase();
  if (extension && !['md', 'markdown', 'txt'].includes(extension)) {
    ElMessage.error("只能导入 .md / .markdown / .txt 文件");
    return;
  }

  const text = await file.text();
  emit('update:modelValue', text);
  ElMessage.success("已导入 Markdown 文件");
}
</script>

<style scoped>
.markdown-editor {
  width: 100%;
}

.markdown-editor__toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
}

.markdown-editor__file-input {
  display: none;
}

.markdown-editor__preview {
  min-height: 180px;
}
</style>
