<template>
  <div class="file-uploader">
    <el-upload
      ref="uploadRef"
      accept=".md,.markdown,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.zip,.rar,.7z,.txt,.png,.jpg,.jpeg,.gif,.webp,.json,.java,.py,.c,.cpp,.js,.ts"
      :auto-upload="false"
      :disabled="disabled || uploading"
      :limit="1"
      :show-file-list="false"
      :on-change="handleFileChange"
      :on-exceed="handleExceed"
    >
      <el-button :icon="Upload" :loading="uploading" :disabled="disabled || uploading">
        {{ buttonText }}
      </el-button>
    </el-upload>
    <p class="file-uploader__hint">支持 .md / .markdown、PDF、Office、图片和常见代码文件</p>
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
import { ElMessage, genFileId, type UploadFile, type UploadInstance, type UploadRawFile } from 'element-plus';
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

const uploadRef = ref<UploadInstance>();
const uploading = ref(false);
const fileName = ref(props.existingFileName || '');

watch(
  () => props.existingFileName,
  (value) => {
    fileName.value = value || '';
  }
);

// 外部把选中值清空（如关闭/重置抽屉）时，必须同步清掉 el-upload 内部列表，
// 否则 :limit="1" 会一直认为已选满，之后再也选不了文件。
watch(
  () => props.modelValue,
  (value) => {
    if (value == null) {
      uploadRef.value?.clearFiles();
    }
  }
);

async function handleFileChange(uploadFileItem: UploadFile) {
  const raw = uploadFileItem.raw;
  if (!raw) {
    return;
  }

  if (uploading.value) {
    return;
  }

  uploading.value = true;
  try {
    const file = await uploadFile(raw, props.purpose);
    fileName.value = file.originalFileName || file.fileName;
    emit('update:modelValue', file.id);
    emit('uploaded', file);
    ElMessage.success('附件上传成功');
  } catch (error) {
    const message = error instanceof Error ? error.message : '';
    // axios / 业务错误已由 http 拦截器统一提示，这里只兜底其它异常
    if (message && !(error as { isAxiosError?: boolean }).isAxiosError) {
      ElMessage.error(message);
    }
    fileName.value = '';
    emit('update:modelValue', null);
  } finally {
    uploading.value = false;
    // 无论成功还是失败都清空内部列表，保证下一个文件可以正常选择
    uploadRef.value?.clearFiles();
  }
}

function handleExceed(files: File[]) {
  uploadRef.value?.clearFiles();
  const [nextFile] = files;
  if (nextFile) {
    uploadRef.value?.handleStart(Object.assign(nextFile, { uid: genFileId() }) as UploadRawFile);
  }
}

function clearFile() {
  fileName.value = '';
  uploadRef.value?.clearFiles();
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

.file-uploader__hint {
  margin: 0;
  width: 100%;
  color: var(--app-muted, #8a8175);
  font-size: 12px;
  line-height: 1.5;
}

.file-uploader__current {
  display: inline-flex;
  gap: 6px;
  align-items: center;
}
</style>
