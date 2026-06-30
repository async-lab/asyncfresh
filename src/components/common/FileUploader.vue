<template>
  <div class="file-uploader">
    <el-upload
      :auto-upload="false"
      :disabled="disabled || uploading"
      :limit="1"
      :show-file-list="false"
      :on-change="handleFileChange"
    >
      <el-button :icon="Upload" :loading="uploading" :disabled="disabled || uploading">
        {{ buttonText }}
      </el-button>
    </el-upload>
    <div v-if="fileName || modelValue" class="file-uploader__current">
      <el-tag effect="light" type="info">
        {{ fileName || `文件 #${modelValue}` }}
      </el-tag>
      <el-button v-if="clearable && !disabled" text type="danger" :icon="Close" @click="clearFile">移除</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Close, Upload } from '@element-plus/icons-vue';
import { ElMessage, type UploadFile } from 'element-plus';
import { ref, watch } from 'vue';

import { uploadFile } from '@/api/files';
import type { FilePurpose, UploadedFile } from '@/types/api';

const props = withDefaults(
  defineProps<{
    modelValue?: number | null;
    purpose: FilePurpose;
    disabled?: boolean;
    clearable?: boolean;
    buttonText?: string;
    existingFileName?: string | null;
  }>(),
  {
    clearable: true,
    buttonText: '上传附件',
    existingFileName: null
  }
);

const emit = defineEmits<{
  'update:modelValue': [value: number | null];
  uploaded: [file: UploadedFile];
  clear: [];
}>();

const uploading = ref(false);
const fileName = ref(props.existingFileName || '');

watch(
  () => props.existingFileName,
  (value) => {
    fileName.value = value || '';
  }
);

async function handleFileChange(uploadFileItem: UploadFile) {
  const raw = uploadFileItem.raw;
  if (!raw) {
    return;
  }

  uploading.value = true;
  try {
    const file = await uploadFile(raw, props.purpose);
    fileName.value = file.originalFileName || file.fileName;
    emit('update:modelValue', file.id);
    emit('uploaded', file);
    ElMessage.success('附件上传成功');
  } finally {
    uploading.value = false;
  }
}

function clearFile() {
  fileName.value = '';
  emit('update:modelValue', null);
  emit('clear');
}
</script>

<style scoped>
.file-uploader {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.file-uploader__current {
  display: inline-flex;
  gap: 6px;
  align-items: center;
}
</style>
