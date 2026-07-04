<template>
  <div class="page">
    <PageHeader :title="task?.title || '任务详情'" description="查看任务说明、当前提交状态和批阅结果。">
      <template #actions>
        <el-button :icon="Back" @click="$router.push('/app/tasks')">返回任务</el-button>
        <el-button :icon="Medal" @click="$router.push('/app/scores')">我的成绩</el-button>
      </template>
    </PageHeader>

    <el-alert v-if="!metaStore.isSelection" type="warning" :closable="false" show-icon>
      当前不是选拔期，可以查看任务和当前提交状态，暂不能提交任务。
    </el-alert>
    <el-alert v-else-if="task && isExpired(task)" type="error" :closable="false" show-icon>
      该任务已截止，不能继续提交。
    </el-alert>

    <div class="detail-grid">
      <section class="page-section" v-loading="loading">
        <template v-if="task">
          <div class="task-meta">
            <span>满分：{{ task.maxScore }}</span>
            <span>截止：{{ formatDateTime(task.deadlineAt) }}</span>
            <AttachmentLink :href="taskAttachmentHref" label="下载任务附件" />
          </div>

          <div class="status-row">
            <span class="muted">当前提交状态</span>
            <StatusTag :value="currentSubmissionStatus" />
          </div>

          <h2>任务说明</h2>
          <MarkdownViewer :content="task.contentMarkdown || task.content || '暂无任务说明。'" />
        </template>
        <el-empty v-else-if="!loading" description="任务不存在或已不可访问" />
      </section>

      <aside class="page-section">
        <h2>当前提交</h2>
        <div class="progress-card">
          <span class="muted">状态</span>
          <StatusTag :value="currentSubmissionStatus" />
          <small>{{ currentSubmission?.submittedAt ? formatDateTime(currentSubmission.submittedAt) : submitHint }}</small>
        </div>

        <div v-if="currentSubmission?.contentMarkdown || currentSubmission?.content" class="submission-card">
          <span class="muted">提交说明</span>
          <p>{{ currentSubmission.contentMarkdown || currentSubmission.content }}</p>
        </div>

        <div v-if="submissionAttachmentHref" class="submission-card">
          <span class="muted">提交附件</span>
          <AttachmentLink :href="submissionAttachmentHref" label="下载提交附件" />
        </div>

        <div v-if="currentSubmission?.status === 'REVIEWED'" class="score-card">
          <span class="muted">批阅结果</span>
          <strong>{{ currentSubmission.score ?? '-' }} / {{ task?.maxScore ?? '-' }}</strong>
          <p>{{ currentSubmission.reviewComment || '暂无评语。' }}</p>
          <small>{{ formatDateTime(currentSubmission.reviewedAt || undefined) }}</small>
        </div>

        <el-button type="primary" class="full" :disabled="!canSubmit" @click="openSubmitDialog">
          {{ submitButtonText }}
        </el-button>
      </aside>
    </div>

    <el-dialog v-model="submitDialogVisible" title="提交任务" width="620px" :close-on-click-modal="false">
      <el-form ref="submitFormRef" :model="submitForm" label-position="top" :disabled="submitting">
        <el-form-item label="提交说明">
          <el-input
            v-model="submitForm.contentMarkdown"
            type="textarea"
            :rows="5"
            maxlength="1000"
            show-word-limit
            placeholder="说明完成内容、运行方式或需要负责人关注的问题"
          />
        </el-form-item>
        <el-form-item label="提交附件">
          <el-upload
            ref="uploadRef"
            v-model:file-list="fileList"
            drag
            :limit="1"
            :http-request="handleUploadRequest"
            :before-upload="beforeUpload"
            :on-remove="handleFileRemove"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽文件到此处，或点击上传</div>
            <template #tip>
              <div class="el-upload__tip">单个附件不超过 20MB，正文和附件至少填写一项。</div>
            </template>
          </el-upload>
          <div v-if="uploadedFile" class="uploaded-file">
            <span>{{ uploadedFile.fileName || uploadedFile.originalFileName }}</span>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="submitDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" :disabled="uploading" @click="handleSubmit">
          提交
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { Back, Medal, UploadFilled } from '@element-plus/icons-vue';
import {
  ElMessage,
  type FormInstance,
  type UploadInstance,
  type UploadProps,
  type UploadRequestOptions,
  type UploadUserFile
} from 'element-plus';
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';

import { uploadFile } from '@/api/files';
import { getMyTaskSubmission, getTask, submitTask } from '@/api/tasks';
import type { SubmissionPayload } from '@/api/tasks';
import AttachmentLink from '@/components/common/AttachmentLink.vue';
import PageHeader from '@/components/common/PageHeader.vue';
import StatusTag from '@/components/common/StatusTag.vue';
import MarkdownViewer from '@/components/markdown/MarkdownViewer.vue';
import { useMetaStore } from '@/stores/meta';
import type { DisplaySubmissionStatus, Task, TaskSubmission, UploadedFile } from '@/types/api';

const route = useRoute();
const metaStore = useMetaStore();
const task = ref<Task | null>(null);
const currentSubmission = ref<TaskSubmission | null>(null);
const loading = ref(false);
const submitting = ref(false);
const uploading = ref(false);
const submitDialogVisible = ref(false);
const submitFormRef = ref<FormInstance>();
const uploadRef = ref<UploadInstance>();
const fileList = ref<UploadUserFile[]>([]);
const uploadedFile = ref<UploadedFile | null>(null);
const submitForm = reactive<SubmissionPayload>({
  contentMarkdown: '',
  attachmentFileId: null
});

const currentSubmissionStatus = computed<DisplaySubmissionStatus>(() => {
  const status = currentSubmission.value?.status || task.value?.submission?.status || task.value?.submissionStatus || 'PENDING';
  if (status === 'PENDING' && task.value && isExpired(task.value)) {
    return 'EXPIRED';
  }
  return status;
});
const canSubmit = computed(() =>
  Boolean(
    task.value &&
      metaStore.isSelection &&
      !isExpired(task.value) &&
      currentSubmissionStatus.value === 'PENDING' &&
      !submitting.value
  )
);
const submitHint = computed(() => {
  if (!task.value) return '等待任务加载';
  if (!metaStore.isSelection) return '选拔期开放提交';
  if (isExpired(task.value)) return '任务已截止';
  if (currentSubmissionStatus.value === 'SUBMITTED') return '已提交，等待批阅或打回';
  if (currentSubmissionStatus.value === 'REVIEWED') return '已批阅，打回后才可重新提交';
  return '可提交任务';
});
const submitButtonText = computed(() => (canSubmit.value ? '提交任务' : submitHint.value));
const taskAttachmentHref = computed(() => {
  if (!task.value || !(task.value.attachment || task.value.attachmentFileId || task.value.attachmentUrl)) return null;
  return task.value.attachmentUrl || `/api/v1/tasks/${task.value.id}/attachment`;
});
const submissionAttachmentHref = computed(() => {
  if (!task.value || !currentSubmission.value) return null;
  if (!(currentSubmission.value.attachment || currentSubmission.value.attachmentFileId || currentSubmission.value.attachmentUrl)) {
    return null;
  }
  return currentSubmission.value.attachmentUrl || `/api/v1/tasks/${task.value.id}/submission/attachment`;
});

onMounted(loadDetail);

watch(
  () => route.params.id,
  () => {
    void loadDetail();
  }
);

async function loadDetail() {
  loading.value = true;
  try {
    const id = String(route.params.id);
    const [taskData, submissionData] = await Promise.all([
      getTask(id),
      getMyTaskSubmission(id).catch(() => null)
    ]);
    task.value = taskData;
    currentSubmission.value = submissionData || taskData.submission || null;
  } finally {
    loading.value = false;
  }
}

function openSubmitDialog() {
  if (!canSubmit.value) {
    ElMessage.warning(submitHint.value);
    return;
  }

  submitForm.contentMarkdown = currentSubmission.value?.contentMarkdown || currentSubmission.value?.content || '';
  submitForm.attachmentFileId = null;
  uploadedFile.value = null;
  fileList.value = [];
  submitDialogVisible.value = true;
  void nextTick(() => submitFormRef.value?.clearValidate());
}

async function handleSubmit() {
  if (!canSubmit.value || !task.value) {
    ElMessage.warning(submitHint.value);
    return;
  }

  if (uploading.value) {
    ElMessage.warning('附件正在上传，请稍候');
    return;
  }

  const contentMarkdown = submitForm.contentMarkdown?.trim() || '';
  if (!contentMarkdown && !submitForm.attachmentFileId) {
    ElMessage.warning('提交说明和附件至少填写一项');
    return;
  }

  submitting.value = true;
  try {
    await submitTask(task.value.id, {
      contentMarkdown: contentMarkdown || undefined,
      attachmentFileId: submitForm.attachmentFileId || null
    });
    ElMessage.success('任务已提交');
    submitDialogVisible.value = false;
    await loadDetail();
  } finally {
    submitting.value = false;
  }
}

const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  const maxSize = 20 * 1024 * 1024;
  if (file.size > maxSize) {
    ElMessage.warning('附件大小不能超过 20MB');
    return false;
  }

  return true;
};

async function handleUploadRequest(options: UploadRequestOptions) {
  uploading.value = true;
  try {
    const result = await uploadFile(options.file as File, 'TASK_SUBMISSION_ATTACHMENT');
    uploadedFile.value = result;
    submitForm.attachmentFileId = result.id;
    options.onSuccess?.(result);
    ElMessage.success('附件上传成功');
  } catch (error) {
    options.onError?.(error as Parameters<UploadRequestOptions['onError']>[0]);
  } finally {
    uploading.value = false;
  }
}

function handleFileRemove() {
  uploadedFile.value = null;
  submitForm.attachmentFileId = null;
}

function isExpired(item: Task) {
  const deadline = Date.parse(item.deadlineAt);
  const now = metaStore.currentPeriod?.serverTime ? Date.parse(metaStore.currentPeriod.serverTime) : Date.now();
  return Number.isFinite(deadline) && Number.isFinite(now) && deadline < now;
}

function formatDateTime(value?: string | null) {
  if (!value) return '-';

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date);
}
</script>

<style scoped>
.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(280px, 360px);
  gap: 16px;
}

.task-meta,
.status-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
  margin-bottom: 16px;
  color: var(--app-muted);
}

h2 {
  margin: 0 0 12px;
  font-size: 18px;
}

.progress-card,
.submission-card,
.score-card {
  padding: 14px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
  margin-bottom: 14px;
}

.progress-card small,
.score-card small {
  display: block;
  margin-top: 8px;
  color: var(--app-muted);
}

.submission-card p,
.score-card p {
  margin: 8px 0 0;
  color: var(--app-muted);
  line-height: 1.6;
  white-space: pre-wrap;
}

.score-card strong {
  display: block;
  margin-top: 8px;
  font-size: 22px;
}

.full {
  width: 100%;
}

.uploaded-file {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-top: 10px;
  color: var(--app-muted);
}

@media (max-width: 960px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
