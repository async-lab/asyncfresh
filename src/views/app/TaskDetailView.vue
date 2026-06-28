<template>
  <div class="page">
    <PageHeader :title="task?.title || '任务详情'" description="查看任务说明、提交附件并浏览提交历史。">
      <template #actions>
        <el-button :icon="Back" @click="$router.push('/app/tasks')">返回任务</el-button>
        <el-button :icon="Medal" @click="$router.push('/app/scores')">我的成绩</el-button>
      </template>
    </PageHeader>

    <el-alert v-if="!metaStore.isSelection" type="warning" :closable="false" show-icon>
      当前不是选拔期，可以查看任务和提交历史，暂不能提交或重新提交。
    </el-alert>
    <el-alert v-else-if="task && isExpired(task)" type="error" :closable="false" show-icon>
      该任务已截止，不能继续提交。
    </el-alert>

    <div class="detail-grid">
      <section class="page-section" v-loading="loading">
        <template v-if="task">
          <div class="task-meta">
            <span>范围：{{ getScopeLabel(task.scope) }}</span>
            <span>满分：{{ task.maxScore }}</span>
            <span>截止：{{ formatDateTime(task.deadlineAt) }}</span>
          </div>

          <div class="status-row">
            <div>
              <span class="muted">提交状态</span>
              <StatusTag :value="currentSubmissionStatus" />
            </div>
            <div>
              <span class="muted">批阅状态</span>
              <StatusTag :value="task.reviewStatus || 'NOT_SUBMITTED'" />
            </div>
            <el-link v-if="task.attachmentUrl" type="primary" :href="task.attachmentUrl" target="_blank" :icon="Download">
              下载任务附件
            </el-link>
          </div>

          <h2>任务说明</h2>
          <MarkdownViewer :content="task.content || '暂无任务说明。'" />
        </template>
        <el-empty v-else-if="!loading" description="任务不存在或已不可访问" />
      </section>

      <aside class="page-section">
        <h2>当前进度</h2>
        <div class="progress-card">
          <span class="muted">最新提交</span>
          <strong>{{ latestSubmission ? `第 ${latestSubmission.submitVersion} 版` : '暂无提交' }}</strong>
          <small>{{ latestSubmission ? formatDateTime(latestSubmission.submittedAt) : submitHint }}</small>
        </div>
        <div v-if="taskScore" class="score-card">
          <span class="muted">批阅结果</span>
          <strong>{{ taskScore.score }} / {{ taskScore.maxScore }}</strong>
          <p>{{ taskScore.comment || '暂无评语。' }}</p>
        </div>
        <el-button type="primary" class="full" :disabled="!canSubmit" @click="openSubmitDialog">
          {{ latestSubmission ? '重新提交' : '提交任务' }}
        </el-button>
      </aside>
    </div>

    <section class="page-section">
      <div class="page-toolbar">
        <h2>提交记录</h2>
        <el-button :icon="Refresh" :loading="loading" @click="loadDetail">刷新</el-button>
      </div>
      <el-table :data="submissions" empty-text="暂无提交记录">
        <el-table-column prop="submitVersion" label="版本" width="90" />
        <el-table-column label="提交时间" min-width="190">
          <template #default="{ row }">{{ formatDateTime(row.submittedAt) }}</template>
        </el-table-column>
        <el-table-column label="提交说明" min-width="220">
          <template #default="{ row }">
            <span>{{ row.content || '未填写' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="附件" width="100">
          <template #default="{ row }">
            <el-link v-if="row.attachmentUrl" type="primary" :href="row.attachmentUrl" target="_blank">下载</el-link>
            <span v-else class="muted">无</span>
          </template>
        </el-table-column>
        <el-table-column label="当前版本" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.isLatest" type="success">是</el-tag>
            <el-tag v-else type="info">否</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="批阅结果" width="130">
          <template #default="{ row }">
            <el-tag v-if="row.isLatest && taskScore" type="success">{{ taskScore.score }} 分</el-tag>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="submitDialogVisible" title="提交任务" width="620px" :close-on-click-modal="false">
      <el-form ref="submitFormRef" :model="submitForm" :rules="submitRules" label-position="top" :disabled="submitting">
        <el-form-item label="提交说明" prop="content">
          <el-input
            v-model="submitForm.content"
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
              <div class="el-upload__tip">单个附件不超过 20MB，上传成功后会随提交一起保存。</div>
            </template>
          </el-upload>
          <div v-if="uploadedFile" class="uploaded-file">
            <span>{{ uploadedFile.fileName }}</span>
            <el-link type="primary" :href="uploadedFile.url" target="_blank">预览</el-link>
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
import { Back, Download, Medal, Refresh, UploadFilled } from '@element-plus/icons-vue';
import {
  ElMessage,
  type FormInstance,
  type FormRules,
  type UploadInstance,
  type UploadProps,
  type UploadRequestOptions,
  type UploadUserFile
} from 'element-plus';
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';

import { uploadFile } from '@/api/files';
import { getMyTaskScores, getMyTaskSubmissions, getTask, submitTask } from '@/api/tasks';
import type { SubmissionPayload } from '@/api/tasks';
import PageHeader from '@/components/common/PageHeader.vue';
import StatusTag from '@/components/common/StatusTag.vue';
import MarkdownViewer from '@/components/markdown/MarkdownViewer.vue';
import { useMetaStore } from '@/stores/meta';
import type { Scope, SubmissionStatus, Task, TaskScore, TaskSubmission, UploadedFile } from '@/types/api';
import { scopeLabels } from '@/utils/labels';

const route = useRoute();
const metaStore = useMetaStore();
const task = ref<Task | null>(null);
const submissions = ref<TaskSubmission[]>([]);
const scores = ref<TaskScore[]>([]);
const loading = ref(false);
const submitting = ref(false);
const uploading = ref(false);
const submitDialogVisible = ref(false);
const submitFormRef = ref<FormInstance>();
const uploadRef = ref<UploadInstance>();
const fileList = ref<UploadUserFile[]>([]);
const uploadedFile = ref<UploadedFile | null>(null);
const submitForm = reactive<SubmissionPayload>({
  content: '',
  attachmentUrl: null
});

const submitRules: FormRules<SubmissionPayload> = {
  content: [{ required: true, message: '请输入提交说明', trigger: 'blur' }]
};

const latestSubmission = computed(() => submissions.value.find((item) => item.isLatest) || submissions.value[0] || null);
const taskScore = computed(() => {
  if (task.value?.reviewStatus !== 'REVIEWED') {
    return null;
  }

  return scores.value.find((item) => String(item.taskId) === String(task.value?.id)) || null;
});
const currentSubmissionStatus = computed<SubmissionStatus>(() => {
  if (!task.value) return 'NOT_SUBMITTED';
  if (task.value.submissionStatus === 'NOT_SUBMITTED' && isExpired(task.value)) return 'EXPIRED';
  return task.value.submissionStatus || 'NOT_SUBMITTED';
});
const canSubmit = computed(() => Boolean(task.value && metaStore.isSelection && !isExpired(task.value) && !submitting.value));
const submitHint = computed(() => {
  if (!task.value) return '等待任务加载';
  if (!metaStore.isSelection) return '选拔期开放提交';
  if (isExpired(task.value)) return '任务已截止';
  return '可提交任务';
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
    const [taskData, submissionData, scoreData] = await Promise.all([
      getTask(id),
      getMyTaskSubmissions(id),
      getMyTaskScores()
    ]);
    task.value = taskData;
    submissions.value = [...submissionData].sort((left, right) => right.submitVersion - left.submitVersion);
    scores.value = scoreData;
  } finally {
    loading.value = false;
  }
}

function openSubmitDialog() {
  if (!canSubmit.value) {
    ElMessage.warning(submitHint.value);
    return;
  }

  submitForm.content = latestSubmission.value?.content || '';
  submitForm.attachmentUrl = null;
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

  const valid = await submitFormRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }

  submitting.value = true;
  try {
    await submitTask(task.value.id, {
      content: submitForm.content.trim(),
      attachmentUrl: submitForm.attachmentUrl || null
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
    const result = await uploadFile(options.file as File, 'SUBMISSION');
    uploadedFile.value = result;
    submitForm.attachmentUrl = result.url;
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
  submitForm.attachmentUrl = null;
}

function getScopeLabel(scope?: Scope) {
  return scope ? scopeLabels[scope] : '未知';
}

function isExpired(item: Task) {
  const deadline = Date.parse(item.deadlineAt);
  const now = metaStore.currentPeriod?.serverTime ? Date.parse(metaStore.currentPeriod.serverTime) : Date.now();
  return Number.isFinite(deadline) && Number.isFinite(now) && deadline < now;
}

function formatDateTime(value?: string) {
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

.status-row > div {
  display: flex;
  gap: 8px;
  align-items: center;
}

h2 {
  margin: 0 0 12px;
  font-size: 18px;
}

.progress-card,
.score-card {
  padding: 14px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
  margin-bottom: 14px;
}

.progress-card strong,
.score-card strong {
  display: block;
  margin-top: 8px;
  font-size: 22px;
}

.progress-card small {
  display: block;
  margin-top: 6px;
  color: var(--app-muted);
}

.score-card p {
  margin: 8px 0 0;
  color: var(--app-muted);
  line-height: 1.6;
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
